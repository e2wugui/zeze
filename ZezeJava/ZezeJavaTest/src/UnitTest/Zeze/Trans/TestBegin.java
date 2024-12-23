package UnitTest.Zeze.Trans;

import UnitTest.Zeze.BMyBean;
import Zeze.Transaction.Locks;
import Zeze.Transaction.Transaction;
import junit.framework.TestCase;
import org.junit.Assert;

@SuppressWarnings("DataFlowIssue")
public class TestBegin extends TestCase {
	private final Locks Locks = new Locks();

	public final void testRollback() {
		Transaction.create(Locks);
		try {
			Transaction.getCurrent().begin();

			// process
			BMyBean bean = new BMyBean();
			Assert.assertEquals(0, bean.getI());

			bean.setI(1);
			Assert.assertEquals(1, bean.getI());

			Transaction.getCurrent().rollback();
			Assert.assertEquals(0, bean.getI());
		} finally {
			Transaction.destroy();
		}
	}

	public final void testCommit() {
		Transaction.create(Locks);
		try {
			Transaction.getCurrent().begin();

			// process
			BMyBean bean = new BMyBean();
			Assert.assertEquals(0, bean.getI());

			bean.setI(1);
			Assert.assertEquals(1, bean.getI());

			Transaction.getCurrent().commit();
			Assert.assertEquals(1, bean.getI());
		} finally {
			Transaction.destroy();
		}
	}

	private static void processNestRollback(BMyBean bean) {
		Assert.assertEquals(1, bean.getI());
		Transaction.getCurrent().begin();
		Assert.assertEquals(1, bean.getI());
		bean.setI(2);
		Assert.assertEquals(2, bean.getI());
		Transaction.getCurrent().rollback();
		Assert.assertEquals(1, bean.getI());
	}

	public final void testNestRollback() {
		Transaction.create(Locks);
		try {
			Transaction.getCurrent().begin();

			// process
			BMyBean bean = new BMyBean();
			Assert.assertEquals(0, bean.getI());

			bean.setI(1);
			Assert.assertEquals(1, bean.getI());
			processNestRollback(bean);
			Assert.assertEquals(1, bean.getI());

			Transaction.getCurrent().commit();
			Assert.assertEquals(1, bean.getI());
		} finally {
			Transaction.destroy();
		}
	}

	private static void ProcessNestCommit(BMyBean bean) {
		Assert.assertEquals(1, bean.getI());
		Transaction.getCurrent().begin();
		Assert.assertEquals(1, bean.getI());
		bean.setI(2);
		Assert.assertEquals(2, bean.getI());
		Transaction.getCurrent().commit();
		Assert.assertEquals(2, bean.getI());
	}

	public final void testNestCommit() {
		Transaction.create(Locks);
		try {
			Transaction.getCurrent().begin();

			// process
			BMyBean bean = new BMyBean();
			Assert.assertEquals(0, bean.getI());

			bean.setI(1);
			Assert.assertEquals(1, bean.getI());
			ProcessNestCommit(bean);
			Assert.assertEquals(2, bean.getI());

			Transaction.getCurrent().commit();
			Assert.assertEquals(2, bean.getI());
		} finally {
			Transaction.destroy();
		}
	}
}
