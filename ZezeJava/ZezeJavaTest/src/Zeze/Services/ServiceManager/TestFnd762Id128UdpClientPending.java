package Zeze.Services.ServiceManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import harness.Fast;
import org.junit.jupiter.api.Test;

/**
 * FND7-62 回归：allocateFuture 发送异常路径（compute内catch）只 setException 未
 * current.pending.set(0)，节点残留"待设置"状态一拍（最迟到超时检查器才清理）。
 * 修复：catch内补 pending.set(0)。
 * 测试：client.stop() 关闭udp socket 使 send 确定性抛 SocketException（构造即送路径，
 * eSendImmediatelyGuard=1下首次allocateFuture即触发立即发送），断言future异常完成
 * 且pending==0（修复前pending==1）。
 */
@Fast
public class TestFnd762Id128UdpClientPending {
	@Test
	public void testSendFailClearsPending() throws Exception {
		var nextSessionId = new AtomicLong();
		// 端口无监听者即可（udp connect不发包）；stop()关闭socket使send确定性失败
		var client = new Id128UdpClient(null, "127.0.0.1", 1, nextSessionId::incrementAndGet);
		client.stop(); // 关闭udp socket；worker未start，join立即返回

		var future = client.allocateFuture("fnd762Global", 128);
		assertTrue(future.isCompletedExceptionally(), "发送失败必须异常完成future");

		var pendingField = Id128UdpClient.FutureNode.class.getDeclaredField("pending");
		pendingField.setAccessible(true);
		var pending = (AtomicInteger)pendingField.get(future);
		assertEquals(0, pending.get(), "发送异常路径必须清零pending（修复前残留为1）");
	}
}
