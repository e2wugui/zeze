package Zeze.Net;

@FunctionalInterface
public interface ProtocolHandle<P extends Protocol<?>> {
	long handle(P p) throws Throwable;

	@SuppressWarnings("unchecked")
	default long handleProtocol(Protocol<?> p) throws Throwable {
		return handle((P)p);
	}
}
