package Zeze.Net;


/**
 * 协议处理回调：处理一个协议，返回过程结果码。
 */
@FunctionalInterface
public interface ProtocolHandle<P extends Protocol<?>> {
	long handle(P p) throws Exception;
}
