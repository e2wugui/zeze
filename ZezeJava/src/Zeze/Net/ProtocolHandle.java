package Zeze.Net;

@FunctionalInterface
public interface ProtocolHandle {
	long handle(Protocol p) throws Throwable;
}
