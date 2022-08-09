package Zeze.Netty;

@FunctionalInterface
public interface HttpEndStreamHandle {
	void onEndStream(HttpExchange x);
}
