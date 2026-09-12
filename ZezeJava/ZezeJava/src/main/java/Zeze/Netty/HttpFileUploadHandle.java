package Zeze.Netty;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.MixedFileUpload;
import io.netty.util.AttributeKey;
import org.jetbrains.annotations.NotNull;

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
					"application/octet-stream", "binary", StandardCharsets.UTF_8, Math.max(size, 0), MemoryBufSize));
			if (oldFileUpload != null) // 以防万一
				oldFileUpload.release();
		}
	}

	@Override
	default void onStreamContent(@NotNull HttpExchange x, @NotNull HttpContent content) throws Exception {
		var fileUpload = x.channel().attr(fileUploadKey).get();
		if (fileUpload == null)
			HttpMultipartHandle.super.onStreamContent(x, content);
		else
			fileUpload.addContent(content.content().retain(), false);
	}

	@Override
	default void onEndStream(@NotNull HttpExchange x) throws Exception {
		var fileUpload = x.channel().attr(fileUploadKey).getAndSet(null);
		if (fileUpload == null)
			HttpMultipartHandle.super.onEndStream(x);
		else {
			try {
				fileUpload.addContent(Unpooled.EMPTY_BUFFER, true);
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
