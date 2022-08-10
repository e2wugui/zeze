package Zeze.Netty;

@FunctionalInterface
public interface HttpFullRequestHandle {
	void onFullRequest(HttpExchange x) throws Throwable;
}
