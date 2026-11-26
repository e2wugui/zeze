package Zeze.Netty;

/**
 * 流式上传中止回调：请求流在正常收到 {@code LastHttpContent} 之前被中止时触发
 * （如请求体超限 413、连接断开、空闲超时、解码失败、提前关闭或服务停机）。
 * <p>用于释放流式处理过程中分配的资源并执行中止收尾；与正常完成回调
 * {@link HttpEndStreamHandle#onEndStream(HttpExchange)} 严格互斥，单次流请求至多触发其中之一。
 * 回调触发时 {@link HttpExchange#isStreamAborted()} 为 {@code true}，连接可能已关闭或即将关闭，
 * 不应假设仍可写出响应。
 */
@FunctionalInterface
public interface HttpAbortStreamHandle {
	void onAbortStream(HttpExchange x) throws Exception;
}
