package Zeze.Net;

@FunctionalInterface
public interface ReliableUdpHandle {
	void handle(ReliableUdp.Session session, ReliableUdp.Packet packet) throws Exception;

	/**
	 * 会话代际变化通知，在网络线程内联执行，不要做耗时操作：
	 * 1. 本端为发送方向：检测到对端重启（或对端报告无会话），在途包已被丢弃、序号从 1
	 *    重新开始——需要可靠投递的业务数据请在此重新发送；
	 * 2. 本端为接收方向：对端换了新代际，接收状态已重置，后续按新序号空间从 1 接收。
	 */
	default void onSessionReset(ReliableUdp.Session session) {
	}
}
