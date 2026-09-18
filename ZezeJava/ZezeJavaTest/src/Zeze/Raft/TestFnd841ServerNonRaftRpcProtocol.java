package Zeze.Raft;

import java.io.File;

import Zeze.Config;
import Zeze.Net.Service;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND8-41回归：Raft.Server对非IRaftRpc用户协议在Leader态裸强转
 * （IRaftRpc)p——CCE从IO线程一路抛出杀掉整个连接（含其上全部Raft流量，
 * follower态却静默丢弃，行为不一致）；文档未写明注册类型约束。
 * 修复：dispatchProtocol对非IRaftRpc按普通协议经基类TaskSpec派发
 * （尊重DispatchMode，不在IO线程跑handle，连接不中断）；注册期守卫
 * AddFactoryHandle拒绝"非IRaftRpc且不在内部/握手白名单"的注册；
 * 类文档写明约束。
 * 纯单元：不起server（构造Raft.Server不start，无端口占用）。
 */
@Fast
public class TestFnd841ServerNonRaftRpcProtocol {
	private static final String dbHome = "a3_TestFnd841ServerNonRaftRpc.raft";
	private static final long fakeTypeId = 0x7a3_0411L; // 非任何白名单typeId

	private Raft raft;
	private Server server;

	private static RaftConfig newRaftConfig() {
		return RaftConfig.loadFromString("""
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="127.0.0.1:26375" DbHome="a3_TestFnd841ServerNonRaftRpc.raft">
					<node Host="127.0.0.1" Port="26375"/>
					<node Host="127.0.0.1" Port="26376"/>
					<node Host="127.0.0.1" Port="26377"/>
				</raft>
				""");
	}

	@BeforeEach
	public void setUp() throws Exception {
		Task.tryInitThreadPool();
		LogSequence.deletedDirectoryAndCheck(new File(dbHome), 100);
		var sm = new StateMachine() {
			@Override
			public SnapshotResult snapshot(String path) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void loadSnapshot(String path) {
			}
		};
		raft = new Raft(sm, "127.0.0.1:26375", newRaftConfig());
		server = new Server(raft, "a3Fnd841Srv", new Config());
	}

	@AfterEach
	public void tearDown() throws Exception {
		if (raft != null) {
			try {
				raft.getLogSequence().close();
				raft.shutdown();
			} catch (Exception ignore) {
			}
		}
		LogSequence.deleteDirectory(new File(dbHome)); // best-effort
	}

	private static Service.ProtocolFactoryHandle<ProxyRequest> proxyFh() {
		// (Class,typeId)构造器不带Factory，字段补设（typeId用伪造值，脱离一切白名单）。
		var fh = new Service.ProtocolFactoryHandle<>(ProxyRequest.class, fakeTypeId);
		fh.Factory = ProxyRequest::new;
		fh.Level = TransactionLevel.None;
		fh.Mode = DispatchMode.Normal;
		return fh;
	}

	// 注册期守卫：非IRaftRpc且不在白名单的注册被拒（拦在启动期）。
	@Test
	public void testRegisterGuardRejectsNonRaftRpc() {
		var e = assertThrows(IllegalArgumentException.class, () -> server.AddFactoryHandle(fakeTypeId, proxyFh()),
				"非IRaftRpc注册必须被拒");
		assertTrue(e.getMessage().contains("IRaftRpc"), "报错必须指名约束，实际: " + e.getMessage());
	}

	// 白名单放行：内部Raft协议（LeaderIs，非IRaftRpc）与IRaftRpc族（GetLeader）注册正常。
	@Test
	public void testRegisterGuardAcceptsWhitelisted() {
		var leaderIsFh = new Service.ProtocolFactoryHandle<>(LeaderIs.class, LeaderIs.TypeId_);
		leaderIsFh.Factory = LeaderIs::new;
		leaderIsFh.Level = TransactionLevel.None;
		assertDoesNotThrow(() -> server.AddFactoryHandle(LeaderIs.TypeId_, leaderIsFh),
				"内部协议（非IRaftRpc）必须放行");
		var getLeaderFh = new Service.ProtocolFactoryHandle<>(GetLeader.class, GetLeader.TypeId_);
		getLeaderFh.Factory = GetLeader::new;
		getLeaderFh.Level = TransactionLevel.None;
		assertDoesNotThrow(() -> server.AddFactoryHandle(GetLeader.TypeId_, getLeaderFh),
				"IRaftRpc族必须放行");
	}

	// Leader态到达非IRaftRpc协议：不再CCE杀连接，按普通协议派发（基类TaskSpec兜住
	// 无handle/处理失败，Rpc族可回错误码）。直接注入factorys模拟绕过注册守卫的
	// 误注册形态（守卫之前注册的存量/直接操作map）。
	@Test
	public void testLeaderDispatchNonRaftRpcNoKill() throws Exception {
		var stateField = Raft.class.getDeclaredField("state");
		stateField.setAccessible(true);
		stateField.set(raft, Raft.RaftState.Leader);

		var fh = proxyFh();
		server.getFactorys().put(fakeTypeId, fh); // 绕过守卫模拟误注册存量
		assertDoesNotThrow(() -> server.dispatchProtocol(new ProxyRequest(), fh),
				"Leader态非IRaftRpc不得抛CCE杀连接（原实现裸强转）");
	}
}
