package Zeze.Netty;

@FunctionalInterface
public interface HttpBeginStreamHandle {
	void onBeginStream(HttpExchange x, int from, int to, int size);
}
