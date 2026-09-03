package Zeze.Raft;

import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

import harness.Fast;
import org.junit.jupiter.api.Test;

import Zeze.Config;
import Zeze.Net.RpcTimeoutException;
import Zeze.Util.Task;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Agent pending 自动重发间隔与 rpc 超时的参数关系（FND-R1-2）。
 * 旧实现重发门槛为 AppendEntriesTimeout*3（默认 6000ms），而 rpc 判死门槛是
 * rpc.getTimeout()（Rpc 构造默认 5000ms、AgentTimeout 4000ms）——判死先于重发成立，
 * 默认配置下重发永不触发：请求超时即报 RpcTimeoutException，上层只能用新
 * requestId 重试，服务器去重失效，可能双执行（对非幂等操作即双成交）。
 * 修复：重发间隔改为 AppendEntriesTimeout（默认 2000ms < 常见 rpc 超时）。
 * 这里不启动 raft server：Agent 的周期 resend 任务对 pending 中长超时 rpc
 * 的重发尝试会更新 rpc.sendTime（即使发送失败），以 sendTime 变化观测重发送达；
 * 旧代码 6000ms 门槛在观测窗口（4.5s < 5000ms rpc 超时）内不触发，有区分度。
 * leader 切换触发的 resend(true) 快速路径属集群竞态序列，由提交信息中的推演核查覆盖。
 */
@Fast
public class TestAgentResendInterval {

	// 3个无监听的节点地址。
	private static RaftConfig newRaftConfig() {
		return RaftConfig.loadFromString("""
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="127.0.0.1:19993">
					<node Host="127.0.0.1" Port="19993"/>
					<node Host="127.0.0.1" Port="19994"/>
					<node Host="127.0.0.1" Port="19995"/>
				</raft>
				""");
	}

	// 默认配置（AppendEntriesTimeout=2000）下，pending 中长超时 rpc 必须在
	// 常见 rpc 超时（5000ms）之前被重发——sendTime 在观测窗口内被 resend 任务更新。
	@Test
	public void testResendBeforeCommonRpcTimeout() throws Exception {
		Task.tryInitThreadPool();
		var agent = new Agent("TestAgentResendInterval", newRaftConfig(), new Config());
		try {
			agent.getClient().start();
			var rpc = new GetLeader();
			rpc.setTimeout(30_000); // 大于重发门槛：请求在窗口内只重发、不判死
			agent.sendForWait(rpc);
			var createTime = rpc.getCreateTime();
			var deadline = System.currentTimeMillis() + 4500; // 4.5s < 5000ms rpc 超时
			while (rpc.getSendTime() <= createTime && System.currentTimeMillis() < deadline)
				Thread.sleep(100);
			assertTrue(rpc.getSendTime() > createTime,
					"resend must happen before common rpc timeout(5000ms), AppendEntriesTimeout="
							+ agent.getRaftConfig().getAppendEntriesTimeout());
		} finally {
			agent.stop();
		}
	}

	// rpc 判死路径保持：短超时请求最终仍以 RpcTimeoutException 完成（判死语义未动）。
	@Test
	public void testRpcDeathStillTriggers() throws Exception {
		Task.tryInitThreadPool();
		var agent = new Agent("TestAgentResendInterval", newRaftConfig(), new Config());
		try {
			agent.getClient().start();
			var rpc = new GetLeader();
			rpc.setTimeout(2500);
			var future = agent.sendForWait(rpc);
			try {
				future.get(8, TimeUnit.SECONDS); // resend 任务1s一轮，判死后异步trigger
				fail("expect RpcTimeoutException");
			} catch (CompletionException e) {
				assertTrue(e.getCause() instanceof RpcTimeoutException,
						"unexpected cause: " + e.getCause());
			}
		} finally {
			agent.stop();
		}
	}
}
