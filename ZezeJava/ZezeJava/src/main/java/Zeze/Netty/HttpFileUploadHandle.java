package Zeze.Netty;

import java.util.Objects;
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
import org.jspecify.annotations.Nullable;

/**
 * 文件上传处理接口：支持 multipart 与 raw（查询参数指定文件名）两种上传方式。
 * 正常完成触发 {@link #onFileCompleted}/{@link #onEndRequest}；提前中止由 {@link #onAbortStream} 释放资源并回调 {@link #onAbortRequest}。
 */
public interface HttpFileUploadHandle extends HttpMultipartHandle {
	AttributeKey<@Nullable MixedFileUpload> fileUploadKey = AttributeKey.valueOf("HttpFileUploadHandleContext");
	int MemoryBufSize = 16 * 1024;

	/** 净化客户端上传文件名并解析落盘路径：仅保留 basename 且校验 canonical 路径位于 uploadDir 内，非法抛 IllegalArgumentException。 */
	static File sanitizeDestFile(File uploadDir, String clientFileName) throws IOException {
		var name = clientFileName.replace('\\', '/');
		name = name.substring(name.lastIndexOf('/') + 1);
		var destFile = new File(uploadDir, name);
		if (!destFile.getCanonicalPath().startsWith(uploadDir.getCanonicalPath() + File.separator))
			throw new IllegalArgumentException("illegal upload filename: " + clientFileName);
		return destFile;
	}

	default String getFileNameQueryKey() {
		return "filename";
	}

	default String getDefaultFileName() {
		return "upload";
	}

	/** 安全释放已从 attr 取走的上传缓冲（null 容忍，异常记日志；同一实例不可重复调用）。 */
	static void releaseFileUpload(@Nullable MixedFileUpload fileUpload) {
		if (fileUpload == null)
			return;
		try {
			fileUpload.release();
		} catch (Throwable e) {
			Netty.logger.error("file upload release", e);
		}
	}

	/** 从 channel attr 取出并释放上传缓冲（幂等）。 */
	static void releaseChannelFileUpload(HttpExchange x) {
		releaseFileUpload(x.channel().attr(fileUploadKey).getAndSet(null));
	}

	@Override
	default void onBeginStream(HttpExchange x, long from, long to, long size) throws Exception {
		assert x.request != null;
		if (HttpPostRequestDecoder.isMultipart(Objects.requireNonNull(x.request)))
			HttpMultipartHandle.super.onBeginStream(x, from, to, size);
		else {
			var fileNameKey = getFileNameQueryKey();
			var fileName = x.queryMap().get(fileNameKey);
			if (fileName == null)
				fileName = getDefaultFileName();
			var oldFileUpload = x.channel().attr(fileUploadKey).getAndSet(new MixedFileUpload(fileNameKey, fileName,
					"application/octet-stream", "binary", StandardCharsets.UTF_8,
					// size < 0 表示未声明长度（如 chunked 上传），按无固定上限处理。
					size >= 0 ? size : Long.MAX_VALUE, MemoryBufSize));
			releaseFileUpload(oldFileUpload);
		}
	}

	@Override
	default void onStreamContent(HttpExchange x, HttpContent content) throws Exception {
		var fileUpload = x.channel().attr(fileUploadKey).get();
		if (fileUpload == null)
			HttpMultipartHandle.super.onStreamContent(x, content);
		else {
			try {
				fileUpload.addContent(content.content().retain(), false);
			} catch (IOException e) {
				// 超过声明大小时立即释放缓冲、回 413 并断连，避免客户端悬挂。
				HttpFileUploadHandle.releaseFileUpload(x.channel().attr(fileUploadKey).getAndSet(null));
				Netty.logger.error("upload size exceeds defined size from {}", x.channel().remoteAddress(), e);
				x.closeConnectionOnFlush(x.send(HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE,
						"text/plain; charset=utf-8", "upload too large"));
			}
		}
	}

	@Override
	default void onEndStream(HttpExchange x) throws Exception {
		if (x.isStreamAborted()) {
			// 兼容未显式绑定 AbortStreamHandle 的流式处理器构造路径。
			onAbortStream(x);
			return;
		}
		var fileUpload = x.channel().attr(fileUploadKey).getAndSet(null);
		if (fileUpload == null)
			HttpMultipartHandle.super.onEndStream(x);
		else {
			try {
				try {
					fileUpload.addContent(Unpooled.EMPTY_BUFFER, true);
				} catch (IOException e) {
					Netty.logger.error("upload size exceeds defined size from {}", x.channel().remoteAddress(), e);
					x.closeConnectionOnFlush(x.send(HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE,
							"text/plain; charset=utf-8", "upload too large"));
					return;
				}
				onFileCompleted(x, fileUpload);
				assert x.request != null;
				var decoder = new InterfaceHttpPostRequestDecoder() {
					private final FileUpload fileUploadInDecoder = fileUpload.retain();
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
						var data = getBodyHttpData(name);
						return data != null ? List.of(data) : List.of();
					}

					@Override
					public @Nullable InterfaceHttpData getBodyHttpData(String name) {
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
					public @Nullable InterfaceHttpData next() {
						if (nextCalled)
							return null;
						nextCalled = true;
						return fileUpload;
					}

					@Override
					public @Nullable InterfaceHttpData currentPartialHttpData() {
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

	@Override
	default void onAbortStream(HttpExchange x) throws Exception {
		releaseFileUpload(x.channel().attr(fileUploadKey).getAndSet(null));
		HttpMultipartHandle.super.onAbortStream(x);
	}
}
