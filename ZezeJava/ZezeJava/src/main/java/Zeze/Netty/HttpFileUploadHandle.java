package Zeze.Netty;

import java.nio.charset.StandardCharsets;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.multipart.HttpPostMultipartRequestDecoder;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.MixedFileUpload;
import io.netty.util.AttributeKey;
import org.jetbrains.annotations.NotNull;

public interface HttpFileUploadHandle extends HttpMultipartHandle {
	@NotNull AttributeKey<MixedFileUpload> fileUploadKey = AttributeKey.valueOf("HttpFileUploadHandleContext");
	int MemoryBufSize = 16 * 1024;

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
				var decoder = new HttpPostMultipartRequestDecoder(getHttpDataFactory(x), x.request) {
					{
						addHttpData(fileUpload.retain());
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
