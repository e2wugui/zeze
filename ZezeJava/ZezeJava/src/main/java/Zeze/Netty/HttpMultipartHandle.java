package Zeze.Netty;

import java.util.Objects;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostMultipartRequestDecoder;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.EndOfDataDecoderException;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpPostRequestDecoder;
import io.netty.util.AttributeKey;
import org.jspecify.annotations.Nullable;

/**
 * multipart/form-data 上传处理接口：基于 Netty 解码器分发属性与文件字段回调。
 * 正常结束回调 {@link #onEndRequest}；提前中止由 {@link #onAbortStream} 释放解码器并回调 {@link #onAbortRequest}。
 */
@SuppressWarnings("RedundantThrows")
public interface HttpMultipartHandle extends HttpBeginStreamHandle, HttpStreamContentHandle, HttpEndStreamHandle, HttpAbortStreamHandle {
	AttributeKey<@Nullable InterfaceHttpPostRequestDecoder> decoderKey = AttributeKey.valueOf("HttpMultipartHandleContext");
	HttpDataFactory defaultHttpDataFactory = newDefaultHttpDataFactory();

	private static HttpDataFactory newDefaultHttpDataFactory() {
		var factory = new DefaultHttpDataFactory();
		factory.setDeleteOnExit(true); // 溢出临时文件的 JVM 退出兜底清理
		return factory;
	}

	/**
	 * 请求过程中上传完一个属性字段时回调
	 */
	default void onAttribute(HttpExchange x, Attribute attr) throws Exception {
	}

	/**
	 * 请求过程中上传完一个文件字段时回调。
	 * 可用 fileUpload.renameTo(new File("目标目录", "目标文件")) 把临时文件/数据移动到指定位置的文件。
	 */
	default void onFileCompleted(HttpExchange x, FileUpload fileUpload) throws Exception {
	}

	/**
	 * 请求正常完成时的回调。可用 decoder.getBodyHttpDatas() 获取所有的Multipart字段。
	 */
	default void onEndRequest(HttpExchange x,
							  InterfaceHttpPostRequestDecoder decoder) throws Exception {
		x.close(x.sendPlainText(HttpResponseStatus.OK, (String)null));
	}

	/** 请求流提前中止时的业务清理回调（调用前框架已销毁 decoder，连接此时可能已关闭）。 */
	default void onAbortRequest(HttpExchange x) throws Exception {
	}

	default HttpDataFactory getHttpDataFactory(HttpExchange x) {
		return defaultHttpDataFactory;
	}

	default InterfaceHttpPostRequestDecoder newDecoder(HttpExchange x) {
		assert x.request != null;
		return new HttpPostMultipartRequestDecoder(getHttpDataFactory(x), Objects.requireNonNull(x.request));
	}

	default @Nullable InterfaceHttpPostRequestDecoder getDecoder(HttpExchange x) {
		return x.channel().attr(decoderKey).get();
	}

	default @Nullable InterfaceHttpPostRequestDecoder getAndSetDecoder(HttpExchange x,
																	   @Nullable InterfaceHttpPostRequestDecoder decoder) {
		return x.channel().attr(decoderKey).getAndSet(decoder);
	}

	/** 安全销毁已从 attr 取走的解码器（null 容忍，异常记日志；Netty destroy 非幂等，不可重复调用）。 */
	static void destroyDecoder(@Nullable InterfaceHttpPostRequestDecoder decoder) {
		if (decoder == null)
			return;
		try {
			decoder.destroy();
		} catch (Throwable e) {
			Netty.logger.error("multipart decoder destroy", e);
		}
	}

	/** 从 channel attr 取出并销毁解码器（幂等）。 */
	static void destroyChannelDecoder(HttpExchange x) {
		destroyDecoder(x.channel().attr(decoderKey).getAndSet(null));
	}

	@Override
	default void onBeginStream(HttpExchange x, long from, long to, long size) throws Exception {
		var oldDecoder = getAndSetDecoder(x, newDecoder(x));
		destroyDecoder(oldDecoder);
	}

	@Override
	default void onStreamContent(HttpExchange x, HttpContent content) throws Exception {
		var decoder = getDecoder(x);
		if (decoder == null)
			throw new IllegalStateException("no decoder");
		try {
			decoder.offer(content);
			for (;;) {
				InterfaceHttpData data;
				try {
					data = decoder.next();
				} catch (EndOfDataDecoderException ignored) {
					break; // 正常读到末尾
				}
				if (data == null)
					break;
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
		} catch (Exception e) {
			// 立即取出销毁 decoder（后续 onEndStream 见空跳过 200），回 400 并断连。
			destroyDecoder(getAndSetDecoder(x, null));
			Netty.logger.error("multipart decode failed from {}", x.channel().remoteAddress(), e);
			x.closeConnectionOnFlush(x.send(HttpResponseStatus.BAD_REQUEST, "text/plain; charset=utf-8",
					"bad multipart"));
		}
	}

	@Override
	default void onEndStream(HttpExchange x) throws Exception {
		if (x.isStreamAborted()) {
			// 兼容未显式绑定 AbortStreamHandle 的流式处理器构造路径。
			onAbortStream(x);
			return;
		}
		var decoder = getAndSetDecoder(x, null);
		if (decoder != null) {
			try {
				onEndRequest(x, decoder);
			} finally {
				decoder.destroy();
			}
		}
	}

	@Override
	default void onAbortStream(HttpExchange x) throws Exception {
		destroyDecoder(getAndSetDecoder(x, null));
		onAbortRequest(x);
	}
}
