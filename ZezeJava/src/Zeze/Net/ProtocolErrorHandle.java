package Zeze.Net;

@FunctionalInterface
public interface ProtocolErrorHandle {
	void handle(Protocol p, int code);
}
