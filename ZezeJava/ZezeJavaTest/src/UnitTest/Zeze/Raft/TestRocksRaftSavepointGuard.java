package UnitTest.Zeze.Raft;

import Zeze.Raft.RocksRaft.Transaction;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND5-15 回归：RocksRaft事务的savepoint begin/commit/rollback均为public，
 * 业务在process体内手动rollback多于begin后，putLog/leaderApply/
 * _final_commit_/runWhile*在空栈上savepoints.getLast()抛
 * NoSuchElementException——不属于FlushException/RocksDBException，不受
 * followerApply的fatalKill兜底也不被tryApply捕获，沿tryCommit上抛到
 * 复制应答线程，lastApplied楔死且无统一终止。
 * 修复：lastSavepoint守卫——空栈抛带定位上下文的IllegalStateException
 * （业务误用从未受控异常变为明确失败）；正常配对路径零变化
 * （getLog空栈返回null的既有语义不变）。
 */
@Fast
public class TestRocksRaftSavepointGuard {

	@Test
	public void testEmptySavepointsThrowsISEWithContext() {
		var t = new Transaction(); // savepoints为空
		var ex = Assertions.assertThrows(IllegalStateException.class, () -> t.putLog(null),
				"空栈putLog必须抛带上下文的IllegalStateException（FND5-15）");
		Assertions.assertTrue(ex.getMessage().contains("putLog"),
				"异常消息必须带定位上下文: " + ex.getMessage());
		Assertions.assertThrows(IllegalStateException.class, () -> t.runWhileCommit(() -> { }),
				"空栈runWhileCommit必须抛ISE（FND5-15）");
		Assertions.assertThrows(IllegalStateException.class, () -> t.runWhileRollback(() -> { }),
				"空栈runWhileRollback必须抛ISE（FND5-15）");
		// 兼容红线：getLog空栈返回null的既有语义不变。
		Assertions.assertNull(t.getLog(1));
	}
}
