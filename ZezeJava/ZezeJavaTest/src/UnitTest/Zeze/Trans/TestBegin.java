package UnitTest.Zeze.Trans;

import UnitTest.Zeze.BMyBean;
import Zeze.Transaction.Locks;
import Zeze.Transaction.Transaction;
import junit.framework.TestCase;
import org.junit.Assert;

public class TestBegin extends TestCase {

	private final Zeze.Transaction.Locks Locks = new Locks();

	public final void testRollback() {
		Transaction.Create(Locks);
		try {
			Transaction.getCurrent().Begin();

			// process
			BMyBean bean = new BMyBean();
			Assert.assertEquals(bean.getI(), 0);

			bean.setI(1);
			Assert.assertEquals(bean.getI(), 1);

			Transaction.getCurrent().Rollback();
			Assert.assertEquals(bean.getI(), 0);
		} finally {
			Transaction.Destroy();
		}
	}

	public final void testCommit() {
		Transaction.Create(Locks);
		try {
			Transaction.getCurrent().Begin();

			// process
			BMyBean bean = new BMyBean();
			Assert.assertEquals(bean.getI(), 0);

			bean.setI(1);
			Assert.assertEquals(bean.getI(), 1);

			Transaction.getCurrent().Commit();
			Assert.assertEquals(bean.getI(), 1);
		} finally {
			Transaction.Destroy();
		}
	}

	private static void processNestRollback(BMyBean bean) {
		Assert.assertEquals(bean.getI(), 1);
		Transaction.getCurrent().Begin();
		Assert.assertEquals(bean.getI(), 1);
		bean.setI(2);
		Assert.assertEquals(bean.getI(), 2);
		Transaction.getCurrent().Rollback();
		Assert.assertEquals(bean.getI(), 1);
	}

	public final void testNestRollback() {
		Transaction.Create(Locks);
		try {
			Transaction.getCurrent().Begin();

			// process
			BMyBean bean = new BMyBean();
			Assert.assertEquals(bean.getI(), 0);

			bean.setI(1);
			Assert.assertEquals(bean.getI(), 1);
			processNestRollback(bean);
			Assert.assertEquals(bean.getI(), 1);

			Transaction.getCurrent().Commit();
			Assert.assertEquals(bean.getI(), 1);
		} finally {
			Transaction.Destroy();
		}
	}

	private static void ProcessNestCommit(BMyBean bean) {
		Assert.assertEquals(bean.getI(), 1);
		Transaction.getCurrent().Begin();
		Assert.assertEquals(bean.getI(), 1);
		bean.setI(2);
		Assert.assertEquals(bean.getI(), 2);
		Transaction.getCurrent().Commit();
		Assert.assertEquals(bean.getI(), 2);
	}

	public final void testNestCommit() {
		Transaction.Create(Locks);
		try {
			Transaction.getCurrent().Begin();

			// process
			BMyBean bean = new BMyBean();
			Assert.assertEquals(bean.getI(), 0);

			bean.setI(1);
			Assert.assertEquals(bean.getI(), 1);
			ProcessNestCommit(bean);
			Assert.assertEquals(bean.getI(), 2);

			Transaction.getCurrent().Commit();
			Assert.assertEquals(bean.getI(), 2);
		} finally {
			Transaction.Destroy();
		}
	}
}
