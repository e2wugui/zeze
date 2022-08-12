package Zeze.Netty;

@FunctionalInterface
public interface HttpBeginStreamHandle {
	void onBeginStream(HttpExchange x, long from, long to, long size) throws Exception;
}
