package Zeze.Net;

@FunctionalInterface
public interface ProtocolErrorHandle {
	void handle(Protocol<?> p, long code) throws Exception;
}
