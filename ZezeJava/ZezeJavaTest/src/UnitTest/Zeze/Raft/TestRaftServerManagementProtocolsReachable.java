package UnitTest.Zeze.Raft;

import java.io.File;

import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Config;
import Zeze.Raft.GetLeader;
import Zeze.Raft.LogSequence;
import Zeze.Raft.RaftConfig;
import Zeze.Raft.RocksRaft.Rocks;
import Zeze.Raft.RocksRaft.RocksMode;
import Zeze.Transaction.Procedure;
import Zeze.Util.Task;

/**
 * R3-F2 回归：GetLeader/StartServerConnector/StopServerConnector三条管理协议的
 * 拦截分支原来转Server.processRequest，被强制leader-ready门槛+唯一请求createTime校验：
 * Agent直发不填createTime（=0），isUniqueRequestCreateTimeValid对(now-0)/86400_000>=7
 * 恒拒→RaftExpired，processGetLeader/processStartServer/processStopServer在唯一发送
 * 路径上不可达（驱赶等功能确定性死亡）。
 * 修复：拦截分支直接派发p.handle（保留dispatchRaftRequest包装与错误码回发）。
 * headless构造Rocks（不start server），直接调用Server.dispatchProtocol验证处理函数可达。
 */
@Fast
public class TestRaftServerManagementProtocolsReachable {
	private static final String raftName = "127.0.0.1:17741";
	private static final String dbHome = "TestRaftMgmtProto.raft";

	private Rocks rocks;

	private static RaftConfig newRaftConfig() {
		return RaftConfig.loadFromString("""
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="127.0.0.1:17741" DbHome="TestRaftMgmtProto.raft">
					<node Host="127.0.0.1" Port="17741"/>
					<node Host="127.0.0.1" Port="17742"/>
					<node Host="127.0.0.1" Port="17743"/>
				</raft>
				""");
	}

	@BeforeEach
	public void setUp() throws Exception {
		Task.tryInitThreadPool();
		LogSequence.deletedDirectoryAndCheck(new File(dbHome), 100);
		rocks = new Rocks(raftName, RocksMode.Pessimism, newRaftConfig(), new Config(), false);
	}

	@AfterEach
	public void tearDown() throws Exception {
		rocks.close();
		LogSequence.deleteDirectory(new File(dbHome)); // best-effort
	}

	/**
	 * Follower状态（headless无选举）下，GetLeader拦截分支必须到达注册的处理函数
	 * （Raft构造时registerInternalRpc注册的processGetLeader：无leaderId时返回Unknown并
	 * 经trySendResultCode回发——即p.sendResultDone被置位且resultCode=Unknown）；
	 * 修复前经processRequest走leader-ready门槛（waitLeaderReady=false）后trySendLeaderIs，
	 * 处理函数不可达（p永远保持初始态）。
	 */
	@Test
	public void testGetLeaderHandleReachableWithoutLeaderReady() throws Exception {
		var server = rocks.getRaft().getServer();
		Assertions.assertFalse(rocks.getRaft().isLeader(), "headless构造无选举，必须处于Follower");

		var p = new GetLeader();
		server.dispatchProtocol(p, server.findProtocolFactoryHandle(GetLeader.TypeId_));

		// 派发经taskOneByOneByKey异步执行，轮询处理结果（上限5秒）
		var deadline = System.currentTimeMillis() + 5_000;
		while (!p.isSendResultDone() && System.currentTimeMillis() < deadline)
			//noinspection BusyWait
			Thread.sleep(20);
		Assertions.assertTrue(p.isSendResultDone(),
				"GetLeader处理函数必须可达（修复前被processRequest的leader-ready门槛拦为RaftExpired，处理函数不可达）");
		Assertions.assertEquals(Procedure.Unknown, p.getResultCode(),
				"无leader时processGetLeader返回Unknown并回发错误码");
	}
}
