package Zeze.Net;

@FunctionalInterface
public interface ProtocolHandle<P extends Protocol<?>> {
	long handle(P p) throws Exception;

	@SuppressWarnings("unchecked")
	default long handleProtocol(Protocol<?> p) throws Exception {
		return handle((P)p);
	}
}
