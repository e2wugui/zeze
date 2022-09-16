package UnitTest.Zeze.Trans;

import UnitTest.Zeze.BMyBean;
import Zeze.Transaction.Locks;
import Zeze.Transaction.Transaction;
import junit.framework.TestCase;
import org.junit.Assert;

public class TestBegin extends TestCase {

	private final Zeze.Transaction.Locks Locks = new Locks();

	public final void testRollback() {
		Transaction.create(Locks);
		try {
			Transaction.getCurrent().begin();

			// process
			BMyBean bean = new BMyBean();
			Assert.assertEquals(bean.getI(), 0);

			bean.setI(1);
			Assert.assertEquals(bean.getI(), 1);

			Transaction.getCurrent().rollback();
			Assert.assertEquals(bean.getI(), 0);
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
			Assert.assertEquals(bean.getI(), 0);

			bean.setI(1);
			Assert.assertEquals(bean.getI(), 1);

			Transaction.getCurrent().commit();
			Assert.assertEquals(bean.getI(), 1);
		} finally {
			Transaction.destroy();
		}
	}

	private static void processNestRollback(BMyBean bean) {
		Assert.assertEquals(bean.getI(), 1);
		Transaction.getCurrent().begin();
		Assert.assertEquals(bean.getI(), 1);
		bean.setI(2);
		Assert.assertEquals(bean.getI(), 2);
		Transaction.getCurrent().rollback();
		Assert.assertEquals(bean.getI(), 1);
	}

	public final void testNestRollback() {
		Transaction.create(Locks);
		try {
			Transaction.getCurrent().begin();

			// process
			BMyBean bean = new BMyBean();
			Assert.assertEquals(bean.getI(), 0);

			bean.setI(1);
			Assert.assertEquals(bean.getI(), 1);
			processNestRollback(bean);
			Assert.assertEquals(bean.getI(), 1);

			Transaction.getCurrent().commit();
			Assert.assertEquals(bean.getI(), 1);
		} finally {
			Transaction.destroy();
		}
	}

	private static void ProcessNestCommit(BMyBean bean) {
		Assert.assertEquals(bean.getI(), 1);
		Transaction.getCurrent().begin();
		Assert.assertEquals(bean.getI(), 1);
		bean.setI(2);
		Assert.assertEquals(bean.getI(), 2);
		Transaction.getCurrent().commit();
		Assert.assertEquals(bean.getI(), 2);
	}

	public final void testNestCommit() {
		Transaction.create(Locks);
		try {
			Transaction.getCurrent().begin();

			// process
			BMyBean bean = new BMyBean();
			Assert.assertEquals(bean.getI(), 0);

			bean.setI(1);
			Assert.assertEquals(bean.getI(), 1);
			ProcessNestCommit(bean);
			Assert.assertEquals(bean.getI(), 2);

			Transaction.getCurrent().commit();
			Assert.assertEquals(bean.getI(), 2);
		} finally {
			Transaction.destroy();
		}
	}
}
