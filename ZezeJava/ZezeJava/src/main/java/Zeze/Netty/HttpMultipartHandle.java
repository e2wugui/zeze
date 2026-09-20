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
	@NotNull AttributeKey<InterfaceHttpPostRequestDecoder> decoderKey = AttributeKey.valueOf("HttpMultipartHandleContext");
	HttpDataFactory defaultHttpDataFactory = newDefaultHttpDataFactory();

	private static HttpDataFactory newDefaultHttpDataFactory() {
		var factory = new DefaultHttpDataFactory();
		// FND8-58可选加固：multipart溢出临时文件（>16KB落盘）的JVM退出兜底，对齐raw路径
		// MixedFileUpload静态默认deleteOnExit=true的语义；JVM内驻留场景由fireEndStreamHandle
		// 的cancel补偿主修解决。代价是DeleteFileOnExitHook的路径集合驻留，可接受。
		factory.setDeleteOnExit(true);
		return factory;
	}

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
//		fileUpload.renameTo(new File("目标目录", "目标文件")); // 可把临时文件/数据移动到指定位置的文件
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

	/**
	 * 取走即负责：安全销毁已从attr取走的解码器。null容忍；防御性清理（cancel补偿/
	 * 防残留分支）不得打断调用路径，异常吞并记日志。netty destroy()非幂等，
	 * 同一实例只允许经"取走"（getAndSet）交入一次，不得二次调用。
	 */
	static void destroyDecoder(@Nullable InterfaceHttpPostRequestDecoder decoder) {
		if (decoder == null)
			return;
		try {
			decoder.destroy();
		} catch (Throwable e) {
			Netty.logger.error("multipart decoder destroy", e);
		}
	}

	/** channel attr取走即销毁（"取走即负责"幂等语义），attr无值时no-op。 */
	static void destroyChannelDecoder(@NotNull HttpExchange x) {
		destroyDecoder(x.channel().attr(decoderKey).getAndSet(null));
	}

	@Override
	default void onBeginStream(@NotNull HttpExchange x, long from, long to, long size) throws Exception {
		var oldDecoder = getAndSetDecoder(x, newDecoder(x));
		destroyDecoder(oldDecoder); // 以防万一：attr残留旧解码器时销毁
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
