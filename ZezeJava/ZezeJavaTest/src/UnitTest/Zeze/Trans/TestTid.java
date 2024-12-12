package UnitTest.Zeze.Trans;

import java.util.Objects;
import Zeze.Transaction.Transaction;
import org.junit.Before;
import org.junit.Test;

public class TestTid {
	@Before
	public void before() throws Exception {
		demo.App.getInstance().Start();
	}

	@Test
	public void printTid() {
		var tid = demo.App.getInstance().getZeze().getTransactionIdAutoKey().nextId();
		System.out.println("direct TransactionId=" + tid);
	}

	@Test
	public void printTidInTransaction() {
		demo.App.getInstance().getZeze().newProcedure(() -> {
			var tid = Objects.requireNonNull(Transaction.getCurrent()).getTransactionId();
			System.out.println("TransactionId=" + tid);
			return 0;
		}, "printTidInTransaction").call();
	}
}
