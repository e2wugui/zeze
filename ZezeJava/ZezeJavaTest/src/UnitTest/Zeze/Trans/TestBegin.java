package UnitTest.Zeze.Trans;

import UnitTest.Zeze.MyBean;
import Zeze.Transaction.Locks;
import Zeze.Transaction.Transaction;
import junit.framework.TestCase;

public class TestBegin extends TestCase {

	private Zeze.Transaction.Locks Locks = new Locks();

	public final void testRollback() {
		Transaction.Create(Locks);
		try {
			Transaction.getCurrent().Begin();

			// process
			MyBean bean = new MyBean();
			assert bean.getI() == 0;

			bean.setI(1);
			assert bean.getI() == 1;

			Transaction.getCurrent().Rollback();
			assert bean.getI() == 0;
		} finally {
			Transaction.Destroy();
		}
	}

	public final void testCommit() {
		Transaction.Create(Locks);
		try {
			Transaction.getCurrent().Begin();

			// process
			MyBean bean = new MyBean();
			assert bean.getI() == 0;

			bean.setI(1);
			assert bean.getI() == 1;

			Transaction.getCurrent().Commit();
			assert bean.getI() == 1;
		} finally {
			Transaction.Destroy();
		}
	}

	private void processNestRollback(MyBean bean) {
		assert bean.getI() == 1;
		Transaction.getCurrent().Begin();
		assert bean.getI() == 1;
		bean.setI(2);
		assert bean.getI() == 2;
		Transaction.getCurrent().Rollback();
		assert bean.getI() == 1;
	}

	public final void testNestRollback() {
		Transaction.Create(Locks);
		try {
			Transaction.getCurrent().Begin();

			// process
			MyBean bean = new MyBean();
			assert bean.getI() == 0;

			bean.setI(1);
			assert bean.getI() == 1;
			processNestRollback(bean);
			assert bean.getI() == 1;

			Transaction.getCurrent().Commit();
			assert bean.getI() == 1;
		} finally {
			Transaction.Destroy();
		}
	}

	private void ProcessNestCommit(MyBean bean) {
		assert bean.getI() == 1;
		Transaction.getCurrent().Begin();
		assert bean.getI() == 1;
		bean.setI(2);
		assert bean.getI() == 2;
		Transaction.getCurrent().Commit();
		assert bean.getI() == 2;
	}

	public final void testNestCommit() {
		Transaction.Create(Locks);
		try {
			Transaction.getCurrent().Begin();

			// process
			MyBean bean = new MyBean();
			assert bean.getI() == 0;

			bean.setI(1);
			assert bean.getI() == 1;
			ProcessNestCommit(bean);
			assert bean.getI() == 2;

			Transaction.getCurrent().Commit();
			assert bean.getI() == 2;
		} finally {
			Transaction.Destroy();
		}
	}
}