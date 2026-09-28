package Zeze.Net;

import org.jetbrains.annotations.NotNull;

/**
 * 协议处理回调：处理一个协议，返回过程结果码。
 */
@FunctionalInterface
public interface ProtocolHandle<P extends Protocol<?>> {
	long handle(@NotNull P p) throws Exception;
}
