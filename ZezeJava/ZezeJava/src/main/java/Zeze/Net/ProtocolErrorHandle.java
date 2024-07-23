package Zeze.Net;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ProtocolErrorHandle {
	void handle(@NotNull Protocol<?> p, long code) throws Exception;
}
