package UnitTest.Zeze.Trans;

import harness.Fast;
import org.junit.jupiter.api.Test;
import UnitTest.Zeze.BMyBean;
import Zeze.Transaction.Locks;
import Zeze.Transaction.Transaction;
import org.junit.jupiter.api.Assertions;

@SuppressWarnings("DataFlowIssue")
@Fast
public class TestBegin {
	private final Locks Locks = new Locks();

	@Test
	public final void testRollback() {
		Transaction.create(Locks);
		try {
			Transaction.getCurrent().begin();

			// process
			BMyBean bean = new BMyBean();
			Assertions.assertEquals(0, bean.getI());

			bean.setI(1);
			Assertions.assertEquals(1, bean.getI());

			Transaction.getCurrent().rollback();
			Assertions.assertEquals(0, bean.getI());
		} finally {
			Transaction.destroy();
		}
	}

	@Test
	public final void testCommit() {
		Transaction.create(Locks);
		try {
			Transaction.getCurrent().begin();

			// process
			BMyBean bean = new BMyBean();
			Assertions.assertEquals(0, bean.getI());

			bean.setI(1);
			Assertions.assertEquals(1, bean.getI());

			Transaction.getCurrent().commit();
			Assertions.assertEquals(1, bean.getI());
		} finally {
			Transaction.destroy();
		}
	}

	private static void processNestRollback(BMyBean bean) {
		Assertions.assertEquals(1, bean.getI());
		Transaction.getCurrent().begin();
		Assertions.assertEquals(1, bean.getI());
		bean.setI(2);
		Assertions.assertEquals(2, bean.getI());
		Transaction.getCurrent().rollback();
		Assertions.assertEquals(1, bean.getI());
	}

	@Test
	public final void testNestRollback() {
		Transaction.create(Locks);
		try {
			Transaction.getCurrent().begin();

			// process
			BMyBean bean = new BMyBean();
			Assertions.assertEquals(0, bean.getI());

			bean.setI(1);
			Assertions.assertEquals(1, bean.getI());
			processNestRollback(bean);
			Assertions.assertEquals(1, bean.getI());

			Transaction.getCurrent().commit();
			Assertions.assertEquals(1, bean.getI());
		} finally {
			Transaction.destroy();
		}
	}

	private static void ProcessNestCommit(BMyBean bean) {
		Assertions.assertEquals(1, bean.getI());
		Transaction.getCurrent().begin();
		Assertions.assertEquals(1, bean.getI());
		bean.setI(2);
		Assertions.assertEquals(2, bean.getI());
		Transaction.getCurrent().commit();
		Assertions.assertEquals(2, bean.getI());
	}

	@Test
	public final void testNestCommit() {
		Transaction.create(Locks);
		try {
			Transaction.getCurrent().begin();

			// process
			BMyBean bean = new BMyBean();
			Assertions.assertEquals(0, bean.getI());

			bean.setI(1);
			Assertions.assertEquals(1, bean.getI());
			ProcessNestCommit(bean);
			Assertions.assertEquals(2, bean.getI());

			Transaction.getCurrent().commit();
			Assertions.assertEquals(2, bean.getI());
		} finally {
			Transaction.destroy();
		}
	}
}
