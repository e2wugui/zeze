package Zeze.Net;

import java.nio.channels.SelectionKey;
import org.jetbrains.annotations.NotNull;

/**
 * 网络事件处理接口。
 * <p>
 * 由于这个接口没有定义close（比如 doClose）相关方法，对于关闭连接需要实现者自行处理。 <b>参考，doException的说明。</b>
 */
public interface SelectorHandle {
	void doHandle(@NotNull SelectionKey key) throws Exception;

	/**
	 * 在调用 doHandle 时，如果捕捉到异常，就调用这个方法处理错误。
	 * <p>
	 * <b>注意，在调用之前，网路 channel 已经被关闭。</b>忘了当时为什么这么实现了，
	 * 可能的原因是想在内部把关键资源(channel)提前释放掉，防止后续的错误处理遗漏。 但是，在 doHandle
	 * 或者应用主动关闭连接时，需要自己明确的关闭channel。 也就是说，doException 和 doHandle 对于 channel
	 * 的释放规则不大一致。
	 * <p>
	 * 建议，不管doHandle，doException，还是主动关闭，都调用是一个close实现。 在close实现中明确关闭 channel.close。
	 *
	 * @param key selection key
	 * @param e   exception
	 * @throws Exception can throw anything
	 */
	void doException(@NotNull SelectionKey key, @NotNull Throwable e) throws Exception;
}
