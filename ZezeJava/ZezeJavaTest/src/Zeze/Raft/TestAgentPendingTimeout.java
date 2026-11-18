package Zeze.Raft;

import java.util.concurrent.CompletionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import Zeze.Config;
import Zeze.Net.RpcTimeoutException;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Fast
public class TestAgentPendingTimeout {
	@Test
	public void disconnectedHeadDoesNotBlockLaterTimeout() throws Exception {
		Task.tryInitThreadPool();
		var agent = new Agent("TestAgentPendingTimeout", RaftConfig.loadFromSortedNames(
				"-127.0.0.1_19993-127.0.0.1_19994-127.0.0.1_19995"), new Config());
		try {
			// 单次扫描固定队列顺序和时间，不依赖周期任务调度或实际网络。
			var timer = Agent.class.getDeclaredField("resendTask");
			timer.setAccessible(true);
			((Future<?>)timer.get(agent)).cancel(true);

			var retry = new GetLeader();
			retry.setTimeout(-1);
			agent.sendForWait(retry);
			retry.setSendTime(0);

			var expired = new GetLeader();
			expired.setTimeout(1000);
			var result = agent.sendForWait(expired);
			expired.setCreateTime(0);

			var resend = Agent.class.getDeclaredMethod("resend");
			resend.setAccessible(true);
			resend.invoke(agent);

			assertTrue(retry.getSendTime() > 0);
			var failure = assertThrows(CompletionException.class, () -> result.get(5, TimeUnit.SECONDS));
			assertInstanceOf(RpcTimeoutException.class, failure.getCause());
		} finally {
			agent.stop();
		}
	}
}
