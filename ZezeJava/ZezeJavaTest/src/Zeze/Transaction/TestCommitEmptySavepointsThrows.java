package Zeze.Trans;

import Zeze.Transaction.Locks;
import Zeze.Transaction.Transaction;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Transaction.commit 对空 savepoints 栈静默放过，而 rollback 显式抛 ISE：
 * 手动 create+begin 后配对失误只在 rollback 一侧暴露，多余/错位的 commit
 * 被静默吞掉（同一配对 bug 两种表现），违反对称契约。
 *
 * 修复：commit 在空栈时同样显式抛 ISE（风格对齐 rollback）。
 * 框架内唯一调用方（Procedure.call 与 Raft Procedure 嵌套路径）都紧跟
 * begin()，不受影响。
 */
@Fast
public class TestCommitEmptySavepointsThrows {

	@Test
	public void testCommitEmptySavepointsThrows() {
		var t = Transaction.create(new Locks());
		try {
			Assertions.assertThrows(IllegalStateException.class, t::commit,
					"空栈commit必须显式报错（对齐rollback），不得静默放过");
		} finally {
			Transaction.destroy();
		}
	}

	@Test
	public void testBeginCommitPairingUnaffected() {
		var t = Transaction.create(new Locks());
		try {
			t.begin();
			Assertions.assertDoesNotThrow(t::commit, "正常begin/commit配对不受空栈守卫影响");
		} finally {
			Transaction.destroy();
		}
	}
}
