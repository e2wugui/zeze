package Zeze.Net;

@FunctionalInterface
public interface ProtocolHandle<P extends Protocol<?>> {
	long handle(P p) throws Exception;
}
