package Zeze.Netty;

import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostMultipartRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpPostRequestDecoder;
import io.netty.util.AttributeKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("RedundantThrows")
public interface HttpMultipartHandle extends HttpBeginStreamHandle, HttpStreamContentHandle, HttpEndStreamHandle {
	@NotNull AttributeKey<InterfaceHttpPostRequestDecoder> decoderKey = AttributeKey.valueOf("HttpMultipartDecoder");
	HttpDataFactory defaultHttpDataFactory = new DefaultHttpDataFactory();

	/**
	 * 请求过程中上传完一个属性字段时回调
	 */
	default void onAttribute(@NotNull HttpExchange x, @NotNull Attribute attr) throws Exception {
//		System.out.println("onAttribute: " + attr.getName() + " = " + attr.getValue());
	}

	/**
	 * 请求过程中上传完一个文件字段时回调
	 */
	default void onFileCompleted(@NotNull HttpExchange x, @NotNull FileUpload fileUpload) throws Exception {
//		System.out.println("onFileCompleted: " + fileUpload.getName() + " = " + fileUpload.getFilename());
//		if (fileUpload.isInMemory())
//			fileUpload.getByteBuf(); // 可写入到指定文件
//		else {
//			var r = fileUpload.getFile().renameTo(new File("目标目录", "目标文件")); // 可把临时文件移动到指定位置
//			if (!r)
//				System.err.println("rename failed");
//		}
	}

	/**
	 * 请求完成时的回调
	 */
	default void onEndRequest(@NotNull HttpExchange x,
							  @NotNull InterfaceHttpPostRequestDecoder decoder) throws Exception {
//		decoder.getBodyHttpDatas(); // 可获取所有的Multipart字段
		x.close(x.sendPlainText(HttpResponseStatus.OK, (String)null));
	}

	default @NotNull HttpDataFactory getHttpDataFactory(@NotNull HttpExchange x) {
		return defaultHttpDataFactory;
	}

	default @NotNull InterfaceHttpPostRequestDecoder newDecoder(@NotNull HttpExchange x) {
		assert x.request != null;
		return new HttpPostMultipartRequestDecoder(getHttpDataFactory(x), x.request);
	}

	default @Nullable InterfaceHttpPostRequestDecoder getDecoder(@NotNull HttpExchange x) {
		return x.channel().attr(decoderKey).get();
	}

	default @Nullable InterfaceHttpPostRequestDecoder getAndSetDecoder(@NotNull HttpExchange x,
																	   @Nullable InterfaceHttpPostRequestDecoder decoder) {
		return x.channel().attr(decoderKey).getAndSet(decoder);
	}

	@Override
	default void onBeginStream(@NotNull HttpExchange x, long from, long to, long size) throws Exception {
		var oldDecoder = getAndSetDecoder(x, newDecoder(x));
		if (oldDecoder != null) // 以防万一
			oldDecoder.destroy();
	}

	@Override
	default void onStreamContent(@NotNull HttpExchange x, @NotNull HttpContent content) throws Exception {
		var decoder = getDecoder(x);
		if (decoder == null)
			throw new IllegalStateException("no decoder");
		decoder.offer(content);
		for (InterfaceHttpData data; (data = decoder.next()) != null; ) {
			switch (data.getHttpDataType()) {
			case Attribute:
				onAttribute(x, (Attribute)data);
				break;
			case FileUpload:
				var fileUpload = (FileUpload)data;
				if (fileUpload.isCompleted())
					onFileCompleted(x, fileUpload);
				break;
			}
		}
	}

	@Override
	default void onEndStream(@NotNull HttpExchange x) throws Exception {
		var decoder = getAndSetDecoder(x, null);
		if (decoder != null) {
			try {
				onEndRequest(x, decoder);
			} finally {
				decoder.destroy();
			}
		}
	}
}
