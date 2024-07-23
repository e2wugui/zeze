package Zeze.Net;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ProtocolHandle<P extends Protocol<?>> {
	long handle(@NotNull P p) throws Exception;
}
