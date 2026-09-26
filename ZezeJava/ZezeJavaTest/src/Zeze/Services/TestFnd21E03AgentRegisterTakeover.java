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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND21 GE-C03：ZokerAgent 注册表无存活校验——死条目锁死 zokerName，无接管路径。
 * 注册条目的唯一常规出口是旧连接 OnSocketClose 的 remove；从连接死亡（isClosed 已置位）到
 * 回调被执行存在窗口（半开连接可达 keepalive 周期、未配置时更长），期间 daemon 重连的
 * Register 被 putIfAbsent 恒拒 eDuplicateZoker——manager 侧 getZoker 恒抛且无自愈。
 * 修复：冲突时检查现存 socket——已死则 CAS 接管（putIfAbsent→isClosed→replace）；接管后
 * 旧连接迟到的 close 回调由条件移除（remove(key, so)）兜底，不误摘继承者条目。
 * 直构 ZokerAgent（不起网络）：Register 协议 + null-service 桩 socket（AsyncSocket 构造器
 * 注释认可的事实用法），close() 只翻转生命周期标志，模拟"连接已死、OnSocketClose 尚未执行"
 * 的窗口；迟到回调用独立构造的 ZokerAgentService.OnSocketClose 直调（不依赖网络层）。
 */
@Fast
public class TestFnd21E03AgentRegisterTakeover {

	/** 桩socket：Send 恒成功、doClose 空转——close() 仅置 isClosed（真实的生命周期标志）。 */
	private static final class StubSocket extends AsyncSocket {
		StubSocket() {
			super(null); // null service：测试桩认可形态（AsyncSocket 构造器注释），发号走共享基址流
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

	/** 核心红点：现存条目的 socket 已死（回调未及执行）时，重连 Register 必须接管成功；
	 * 迟到的旧 close 回调不得摘除继承者条目（条件移除）。
	 * 修复前：重连 Register 恒 eDuplicateZoker；接管（不存在）后无条件 remove 也会把
	 * 新条目一并摘除（健康新连接立即失联）。 */
	@Test
	public void testDeadEntryTakeoverAndLateCloseNotEvictSuccessor() throws Exception {
		var agent = new ZokerAgent(new Config());
		var agentService = new ZokerAgentService(agent, new Config()); // 只用 OnSocketClose，不 start
		var oldSock = new StubSocket();
		var newSock = new StubSocket();

		assertEquals(0, register(agent, oldSock, "zk1"));
		assertSame(oldSock, agent.zokers().get("zk1"));

		// 窗口复现：旧连接死亡（isClosed=true）但 OnSocketClose 尚未执行（半开/回调排队）
		oldSock.close();
		assertTrue(oldSock.isClosed());

		// daemon 立刻重连：必须接管死条目（修复前：恒 eDuplicateZoker，zokerName 被死条目锁死）
		assertEquals(0, register(agent, newSock, "zk1"), "死条目必须可被重连 Register 接管");
		assertSame(newSock, agent.zokers().get("zk1"), "接管后条目指向新连接");

		// 旧连接迟到的 close 回调：不得摘除继承者条目（修复前 remove(key) 无条件摘除）
		agentService.OnSocketClose(oldSock, null);
		assertSame(newSock, agent.zokers().get("zk1"), "迟到 close 不得误摘接管者");
		// 新连接（现主人）自己的 close 回调仍正常回收条目——条件移除不堵正常出口
		newSock.close();
		agentService.OnSocketClose(newSock, null);
		assertNull(agent.zokers().get("zk1"));
	}

	/** 边界：现存 socket 活着时仍是真重复（接管不得放宽重复拒绝）；不同名字互不影响。 */
	@Test
	public void testLiveOwnerStillRejectsDuplicate() throws Exception {
		var agent = new ZokerAgent(new Config());
		var sock1 = new StubSocket();
		var sock2 = new StubSocket();

		assertEquals(0, register(agent, sock1, "zk2"));
		assertFalse(sock1.isClosed());
		assertEquals(ZokerAgent.eDuplicateZoker, register(agent, sock2, "zk2"), "活条目上的重名必须仍被拒");
		assertSame(sock1, agent.zokers().get("zk2"), "被拒注册不得改写现主人");

		// 未注册成功的 socket 断链：userState 未设置，不影响任何条目
		var agentService = new ZokerAgentService(agent, new Config());
		sock2.close();
		agentService.OnSocketClose(sock2, null);
		assertSame(sock1, agent.zokers().get("zk2"));
	}
}
