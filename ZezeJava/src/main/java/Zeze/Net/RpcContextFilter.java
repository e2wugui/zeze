package Zeze.Net;

@FunctionalInterface
public interface RpcContextFilter {
	boolean invoke(Protocol p);
}
