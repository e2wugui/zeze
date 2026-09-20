package UnitTest.Zeze.Trans;

import java.util.Objects;
import Zeze.Transaction.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestTid {
	@BeforeEach
	public void before() throws Exception {
		demo.App.getInstance().Start();
	}

	@Test
	public void printTid() {
		var tid = demo.App.getInstance().getZeze().getTransactionIdAutoKey().nextId();
		System.out.println("direct TransactionId=" + tid);
		// 原先纯打印零断言（2026-09-20审核）：发号必须非零且单调
		org.junit.jupiter.api.Assertions.assertNotEquals(0L, tid);
		var tid2 = demo.App.getInstance().getZeze().getTransactionIdAutoKey().nextId();
		org.junit.jupiter.api.Assertions.assertTrue(tid2 > tid, "AutoKey发号必须单调递增");
	}

	@Test
	public void printTidInTransaction() {
		var holder = new long[1];
		var rc = demo.App.getInstance().getZeze().newProcedure(() -> {
			var tid = Objects.requireNonNull(Transaction.getCurrent()).getTransactionId();
			holder[0] = tid;
			System.out.println("TransactionId=" + tid);
			return 0;
		}, "printTidInTransaction").call();
		org.junit.jupiter.api.Assertions.assertEquals(0L, rc, "事务必须成功");
		org.junit.jupiter.api.Assertions.assertNotEquals(0L, holder[0], "事务内tid必须已分配");
	}

	public static void x(Object obj) {
		System.out.println(obj);
	}

	public static void main(String[] args) {
		x(1);
		x(2L);
		x("str");
	}
}
