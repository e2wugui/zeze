package Zeze.Netty;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.MixedFileUpload;
import io.netty.util.AttributeKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface HttpFileUploadHandle extends HttpMultipartHandle {
	@NotNull AttributeKey<MixedFileUpload> fileUploadKey = AttributeKey.valueOf("HttpFileUploadHandleContext");
	int MemoryBufSize = 16 * 1024;

	/**
	 * 净化客户端可控的上传文件名并解析落盘目标（FND4-70）：仅保留basename（'/'与'\'都视为
	 * 分隔符）且canonical路径必须落在uploadDir内，双保险拦截"../"穿越——原实现直接
	 * new File(uploadDir, filename)并先delete，以JVM工作目录为基准越权删除/覆盖任意文件
	 * （可覆盖启动脚本/jar，结合重启形成RCE链）。非法名抛IllegalArgumentException，
	 * 调用方应答400。
	 */
	static @NotNull File sanitizeDestFile(@NotNull File uploadDir, @NotNull String clientFileName) throws IOException {
		var name = clientFileName.replace('\\', '/');
		name = name.substring(name.lastIndexOf('/') + 1); // 仅留basename，剥掉全部目录成分
		var destFile = new File(uploadDir, name);
		if (!destFile.getCanonicalPath().startsWith(uploadDir.getCanonicalPath() + File.separator))
			throw new IllegalArgumentException("illegal upload filename: " + clientFileName);
		return destFile;
	}

	default @NotNull String getFileNameQueryKey() {
		return "filename";
	}

	default @NotNull String getDefaultFileName() {
		return "upload";
	}

	/**
	 * 取走即负责：安全释放已从attr取走的上传缓冲（堆数据/临时文件）。null容忍；
	 * 防御性清理（cancel补偿/防残留分支）不得打断调用路径，异常吞并记日志。
	 * 同一实例只允许经"取走"（getAndSet）交入一次，不得二次调用。
	 */
	static void releaseFileUpload(@Nullable MixedFileUpload fileUpload) {
		if (fileUpload == null)
			return;
		try {
			fileUpload.release();
		} catch (Throwable e) {
			Netty.logger.error("file upload release", e);
		}
	}

	/** channel attr取走即释放（"取走即负责"幂等语义），attr无值时no-op。 */
	static void releaseChannelFileUpload(@NotNull HttpExchange x) {
		releaseFileUpload(x.channel().attr(fileUploadKey).getAndSet(null));
	}

	@Override
	default void onBeginStream(@NotNull HttpExchange x, long from, long to, long size) throws Exception {
		assert x.request != null;
		if (HttpPostRequestDecoder.isMultipart(x.request))
			HttpMultipartHandle.super.onBeginStream(x, from, to, size);
		else {
			var fileNameKey = getFileNameQueryKey();
			var fileName = x.queryMap().get(fileNameKey);
			if (fileName == null)
				fileName = getDefaultFileName();
			var oldFileUpload = x.channel().attr(fileUploadKey).getAndSet(new MixedFileUpload(fileNameKey, fileName,
					"application/octet-stream", "binary", StandardCharsets.UTF_8,
					// 【FND11 net-02】size=-1是"未声明长度"（chunked上传合法形态），钳成0会被Netty
					// checkSize(maxSize>=0 && newSize>maxSize)当成"上限为零"——任何数据必413且
					// 错误信息误导。未声明长度按不限处理（对流模式的总量上限口径一致）。
					size >= 0 ? size : Long.MAX_VALUE, MemoryBufSize));
			releaseFileUpload(oldFileUpload); // 以防万一：attr残留旧上传缓冲时释放
		}
	}

	@Override
	default void onStreamContent(@NotNull HttpExchange x, @NotNull HttpContent content) throws Exception {
		var fileUpload = x.channel().attr(fileUploadKey).get();
		if (fileUpload == null)
			HttpMultipartHandle.super.onStreamContent(x, content);
		else {
			try {
				fileUpload.addContent(content.content().retain(), false);
			} catch (IOException e) {
				// NY1-F4：raw上传超过声明大小时addContent抛IOException且被任务框架吞掉，
				// onEndRequest永不执行、invokeEndStream的close(null)空写无响应体——客户端零字节
				// 挂到空闲超时。对齐HttpExchange的streamContentTotal超限处置：回413并断连，
				// 同时取走释放attr上的上传缓冲（取走即负责，后续LastHttpContent因exchange已终结不再路由进来）。
				HttpFileUploadHandle.releaseFileUpload(x.channel().attr(fileUploadKey).getAndSet(null));
				Netty.logger.error("upload size exceeds defined size from {}", x.channel().remoteAddress(), e);
				x.closeConnectionOnFlush(x.send(HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE,
						"text/plain; charset=utf-8", "upload too large"));
			}
		}
	}

	@Override
	default void onEndStream(@NotNull HttpExchange x) throws Exception {
		var fileUpload = x.channel().attr(fileUploadKey).getAndSet(null);
		if (fileUpload == null)
			HttpMultipartHandle.super.onEndStream(x);
		else {
			try {
				try {
					fileUpload.addContent(Unpooled.EMPTY_BUFFER, true);
				} catch (IOException e) {
					// NY1-F4：终结帧同样可能越过声明大小，处置同onStreamContent（413+断连，跳过onFileCompleted/onEndRequest）。
					Netty.logger.error("upload size exceeds defined size from {}", x.channel().remoteAddress(), e);
					x.closeConnectionOnFlush(x.send(HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE,
							"text/plain; charset=utf-8", "upload too large"));
					return;
				}
				onFileCompleted(x, fileUpload);
				assert x.request != null;
				var decoder = new InterfaceHttpPostRequestDecoder() {
					private final @NotNull FileUpload fileUploadInDecoder = fileUpload.retain();
					private boolean nextCalled;

					@Override
					public boolean isMultipart() {
						return false;
					}

					@Override
					public void setDiscardThreshold(int discardThreshold) {
						throw new UnsupportedOperationException();
					}

					@Override
					public int getDiscardThreshold() {
						return 0;
					}

					@Override
					public List<InterfaceHttpData> getBodyHttpDatas() {
						return List.of(fileUploadInDecoder);
					}

					@Override
					public List<InterfaceHttpData> getBodyHttpDatas(String name) {
						return List.of();
					}

					@Override
					public InterfaceHttpData getBodyHttpData(String name) {
						return fileUploadInDecoder.getName().equals(name) ? fileUploadInDecoder : null;
					}

					@Override
					public InterfaceHttpPostRequestDecoder offer(HttpContent content) {
						throw new UnsupportedOperationException();
					}

					@Override
					public boolean hasNext() {
						return !nextCalled;
					}

					@Override
					public InterfaceHttpData next() {
						if (nextCalled)
							return null;
						nextCalled = true;
						return fileUpload;
					}

					@Override
					public InterfaceHttpData currentPartialHttpData() {
						return null;
					}

					@Override
					public void destroy() {
						fileUpload.release();
					}

					@Override
					public void cleanFiles() {
					}

					@Override
					public void removeHttpDataFromClean(InterfaceHttpData data) {
					}
				};
				try {
					onEndRequest(x, decoder);
				} finally {
					decoder.destroy();
				}
			} finally {
				fileUpload.release();
			}
		}
	}
}
