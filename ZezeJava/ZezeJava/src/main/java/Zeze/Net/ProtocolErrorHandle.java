package Zeze.Net;

/**
 * 协议错误处理回调：按结果码处置协议错误。
 */
@FunctionalInterface
public interface ProtocolErrorHandle {
	void handle(Protocol<?> p, long code) throws Exception;
}
