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
 * Register 的同连接重放幂等：接管循环的冲突判别只认"现存 socket 活着=真重复"，
 * 缺自归属判别（old==sender）——同连接对自己已注册成功的名字重发 Register（客户端
 * RPC 超时在同连接重试的常规形态）必得 eDuplicateZoker：注册实际已成功却回报
 * "Zoker名字重复了"（名字被他人占用的误诊信号），重试型客户端在同连接上永不收敛。
 * 修复后自归属重放幂等成功应答；他方活连接的真重复语义不变。
 * 直构 ZokerAgent（不起网络）：Register 协议 + null-service 桩 socket
 * （TestAgentRegisterRename 同形态）。
 */
@Fast
public class TestAgentRegisterReplaySameConnection {

	/** 桩socket：Send 恒成功、doClose 空转——close() 仅置 isClosed。 */
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

	/** 核心红点：同连接重发已注册名（RPC 超时重试形态）必须幂等成功。
	 * 修复前红：eDuplicateZoker——已成功的注册被误报为名字冲突。 */
	@Test
	public void testSameSocketReplayIsIdempotentSuccess() throws Exception {
		var agent = new ZokerAgent(new Config());
		var sock = new StubSocket();

		assertEquals(0, register(agent, sock, "zk-replay"));
		// 同连接重放（应答超时客户端重试的常规形态）：幂等成功，非 eDuplicateZoker
		assertEquals(0, register(agent, sock, "zk-replay"), "自归属重放必须幂等成功（修复前 eDuplicateZoker）");
		assertEquals(0, register(agent, sock, "zk-replay"), "多次重放同样幂等");
		assertSame(sock, agent.zokers().get("zk-replay"), "条目仍指向本连接");
		assertEquals(1, agent.zokers().size());

		// 幂等成功后关闭照常回收（重放不破坏收殓路径）
		var agentService = new ZokerAgentService(agent, new Config());
		sock.close();
		agentService.OnSocketClose(sock, null);
		assertTrue(agent.zokers().isEmpty(), "重放过的连接关闭后零残留");
	}

	/** 重放与换名正交：换名摘旧语义不受自归属判别影响（重放不摘旧名、换名不误判重放）。 */
	@Test
	public void testReplayDoesNotDisturbRenameSemantics() throws Exception {
		var agent = new ZokerAgent(new Config());
		var sock = new StubSocket();

		assertEquals(0, register(agent, sock, "n1"));
		assertEquals(0, register(agent, sock, "n2"), "换名注册");
		assertNull(agent.zokers().get("n1"), "换名摘旧不受影响");
		assertEquals(0, register(agent, sock, "n2"), "换名后对新名的重放幂等成功");
		assertSame(sock, agent.zokers().get("n2"));
		assertEquals(1, agent.zokers().size(), "重放不得复活已摘旧名");
	}

	/** 边界固化：他方活连接的同名注册仍真重复（自归属判别不得放宽重复拒绝）。 */
	@Test
	public void testOtherLiveSocketStillDuplicate() throws Exception {
		var agent = new ZokerAgent(new Config());
		var sock1 = new StubSocket();
		var sock2 = new StubSocket();

		assertEquals(0, register(agent, sock1, "zk-owned"));
		assertEquals(ZokerAgent.eDuplicateZoker, register(agent, sock2, "zk-owned"),
				"他方活连接占用仍是真重复");
		assertSame(sock1, agent.zokers().get("zk-owned"), "被拒注册不得改写现主人");
	}
}
