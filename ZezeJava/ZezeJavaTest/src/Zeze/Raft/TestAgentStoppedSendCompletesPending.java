package Zeze.Raft;

import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import Zeze.Net.RpcTimeoutException;
import Zeze.Transaction.Procedure;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Agent stop后的send/sendForWait不得静默注册pending（FND29 dbh2-02）：
 * stop取消resendTask、置空client/leader之后注册进pending的rpc没有任何驱动路径，
 * 等待线程永久悬挂（无超时await）。契约=stop后的发送必须立即补完成（异常终局）：
 * sendForWait的future以RpcTimeoutException("AgentStopped")异常完成（等待者有界醒来的
 * 可见失败），send的handle以Timeout终局码被调用。openBucket交付与并发close的两线程
 * 窄交错窗口本身不可确定性注入（豁免，见案卷），本用例锁死"stop完全先于send"的确定性半边，
 * 该半边正是交错窗口中"stop排空先完成、发送后到"的分支。
 */
@Fast
public class TestAgentStoppedSendCompletesPending {

	private static final String RaftConfigString = """
			<?xml version="1.0" encoding="utf-8"?>
			<raft Name="">
				<node Host="127.0.0.1" Port="19570"/>
				<node Host="127.0.0.1" Port="19571"/>
			</raft>
			""";

	@Test
	public void testSendForWaitAfterStopCompletesExceptionally() throws Exception {
		Task.tryInitThreadPool();
		var agent = new Agent("testAgentStoppedSend.1",
				RaftConfig.loadFromString(RaftConfigString), null);
		agent.stop();
		var future = agent.sendForWait(new GetLeader());
		Assertions.assertTrue(future.isDone(), "stop后sendForWait的future必须立即补完成，不得滞留pending无驱动");
		Assertions.assertTrue(future.isCompletedExceptionally(), "必须异常完成（可见失败）而非静默");
		var ex = Assertions.assertThrows(CompletionException.class,
				() -> future.await(1000, TimeUnit.MILLISECONDS),
				"等待方必须得到异常（有界醒来）而非永久悬挂");
		Assertions.assertInstanceOf(RpcTimeoutException.class, ex.getCause());
		Assertions.assertTrue(ex.getCause().getMessage().contains("AgentStopped"),
				"终局原因必须可辨识: " + ex.getCause().getMessage());
	}

	@Test
	public void testSendAfterStopInvokesHandleWithTimeout() throws Exception {
		Task.tryInitThreadPool();
		var agent = new Agent("testAgentStoppedSend.2",
				RaftConfig.loadFromString(RaftConfigString), null);
		agent.stop();
		var invoked = new boolean[1];
		var resultCode = new long[1];
		var timeoutFlag = new boolean[1];
		agent.getLeaderAsync((get) -> {
			invoked[0] = true;
			resultCode[0] = get.getResultCode();
			timeoutFlag[0] = get.isTimeout();
			return 0;
		});
		Assertions.assertTrue(invoked[0], "stop后send的handle必须被终局调用，不得静默滞留pending");
		Assertions.assertEquals(Procedure.Timeout, resultCode[0], "终局码=Timeout（对齐resend判死路径）");
		Assertions.assertTrue(timeoutFlag[0], "isTimeout必须置位");
	}
}
