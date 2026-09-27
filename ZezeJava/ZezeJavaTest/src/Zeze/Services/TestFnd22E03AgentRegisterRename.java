package Zeze.Services;

import java.net.SocketAddress;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Util.TimeThrottle;
import Zeze.Services.ZokerImpl.ZokerAgentService;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND22 GE-C03：ZokerAgent 注册表"一连接多名"泄漏——Register 可在同一 socket 上反复换名，
 * userState 单值只记末名，OnSocketClose 的条件移除只摘末名一个：其余名字条目永久滞留
 * （值指向已关闭 socket），无认证 acceptor 上单连接 Register 洪泛=无界内存增长。
 * 修复：ProcessRegisterRequest 装账成功后对 prev 名条件摘除（remove(prev, sender)——值仍是
 * 本 socket 才摘，防误摘 FND21 的 CAS 接管继承者）。多名字注册在正常 daemon 流程不存在，
 * 触发方为恶意/带 bug 客户端；换名摘旧后注册表始终"一连接至多一名"，关闭零残留。
 * 直构 ZokerAgent（不起网络）：Register 协议 + null-service 桩 socket
 * （TestFnd21E03 同形态），close 回调用独立构造的 ZokerAgentService.OnSocketClose 直调。
 */
@Fast
public class TestFnd22E03AgentRegisterRename {

	/** 桩socket：Send 恒成功、doClose 空转——close() 仅置 isClosed（TestFnd21E03 同形态）。 */
	private static final class StubSocket extends AsyncSocket {
		StubSocket() {
			super(null);
		}

		@Override
		public Type getType() {
			return Type.eClient;
		}

		@Override
		public @Nullable SocketAddress getRemoteAddress() {
			return null;
		}

		@Override
		public @Nullable TimeThrottle getTimeThrottle() {
			return null;
		}

		@Override
		protected void doClose(@Nullable Throwable ex, boolean gracefully) {
		}

		@Override
		public boolean Send(byte @NotNull [] bytes, int offset, int length) {
			return true;
		}
	}

	private static long register(ZokerAgent agent, AsyncSocket socket, String zokerName) {
		var r = new Zeze.Builtin.Zoker.Register();
		r.Argument.setZokerName(zokerName);
		r.setSender(socket);
		return agent.ProcessRegisterRequest(r);
	}

	/** 核心红点：同 socket 连续换名注册——表内只存末名（旧名即时摘除），关闭零残留。
	 * 修复前红：zokers 留下全部 N 个名字（userState 只记末名，close 只摘一个）。 */
	@Test
	public void testRenameReleasesOldName() throws Exception {
		var agent = new ZokerAgent(new Config());
		var sock = new StubSocket();

		assertEquals(0, register(agent, sock, "zk-a"));
		assertEquals(0, register(agent, sock, "zk-b"));
		assertEquals(0, register(agent, sock, "zk-c"));

		assertEquals(1, agent.zokers().size(), "一连接至多一名: " + agent.zokers().keySet());
		assertSame(sock, agent.zokers().get("zk-c"), "末名条目指向本连接");
		assertNull(agent.zokers().get("zk-a"), "换名后旧名即时摘除（不留死条目）");
		assertNull(agent.zokers().get("zk-b"));

		// 关闭：零残留（修复前：zk-a/zk-b 永久滞留，值指向已关闭 socket）
		var agentService = new ZokerAgentService(agent, new Config());
		sock.close();
		agentService.OnSocketClose(sock, null);
		assertTrue(agent.zokers().isEmpty(), "关闭后注册表零残留");
	}

	/** 换名摘旧只摘自己的：他连接的条目不受影响（条件移除值=本 socket）。 */
	@Test
	public void testRenameDoesNotEvictOtherSockets() throws Exception {
		var agent = new ZokerAgent(new Config());
		var sock1 = new StubSocket();
		var sock2 = new StubSocket();

		assertEquals(0, register(agent, sock1, "mine-1"));
		assertEquals(0, register(agent, sock2, "theirs"));
		assertEquals(0, register(agent, sock1, "mine-2")); // sock1 换名

		assertEquals(2, agent.zokers().size(), "sock1 一名 + sock2 一名: " + agent.zokers().keySet());
		assertSame(sock2, agent.zokers().get("theirs"), "他连接条目不得被换名摘除波及");
		assertSame(sock1, agent.zokers().get("mine-2"));
		assertNull(agent.zokers().get("mine-1"));
	}

	/** 换名释放的旧名可被新连接立即注册（不因死值滞留被拒）；新注册后原连接再换名不得
	 * 误摘已易主条目（条件移除防误摘）。 */
	@Test
	public void testFreedOldNameRegisterableAndNotEvictedByLateRename() throws Exception {
		var agent = new ZokerAgent(new Config());
		var sock1 = new StubSocket();
		var sock2 = new StubSocket();

		assertEquals(0, register(agent, sock1, "n1"));
		assertEquals(0, register(agent, sock1, "n2")); // n1 释放
		assertEquals(0, register(agent, sock2, "n1"), "释放的旧名可被新连接注册（修复前死值滞留→eDuplicateZoker）");
		assertSame(sock2, agent.zokers().get("n1"));

		// sock1 已不持有 n1：其后续换名的摘旧动作不得摘掉 sock2 的 n1（条件移除）
		assertEquals(0, register(agent, sock1, "n3"));
		assertSame(sock2, agent.zokers().get("n1"), "已易主条目不得被原连接的换名误摘");
		assertEquals(2, agent.zokers().size(), "sock1(n3)+sock2(n1) 两名并存: " + agent.zokers());
	}
}
