package UnitTest.Zeze.Trans;

import Zeze.Serialize.*;
import Zeze.Transaction.*;
import Zeze.Transaction.Record;
import junit.framework.TestCase;

public class TestBegin extends TestCase{
	
	public static class MyBean extends Bean {
		@Override
		public void Decode(ByteBuffer bb) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void Encode(ByteBuffer bb) {
			throw new UnsupportedOperationException();
		}

		@Override
		protected void InitChildrenRootInfo(Record.RootInfo root) {
		}

		public int _i;

		private static class MyLog extends Log1<MyBean, Integer> {
			public MyLog(MyBean bean, int value) {
				super(bean, value);

			}

			@Override
			public long getLogKey() {
				return getBean().getObjectId() + 0;
			}

			@Override
			public void Commit() {
				((MyBean)getBean())._i = getValue();
			}
		}
		public final int getI() {
			MyLog log = (MyLog)Transaction.getCurrent().GetLog(this.getObjectId() + 0);
			return (null != log) ? log.getValue() : _i;
		}
		public final void setI(int value) {
			Transaction.getCurrent().PutLog(new MyLog(this, value));
		}
	}

	public final void testRollback() {
		Transaction.Create();
		try {
			Transaction.getCurrent().Begin();

			// process
			MyBean bean = new MyBean();
			assert bean.getI() == 0;

			bean.setI(1);
			assert bean.getI() == 1;

			Transaction.getCurrent().Rollback();
			assert bean.getI() == 0;
		}
		finally {
			Transaction.Destroy();
		}
	}

	public final void testCommit() {
		Transaction.Create();
		try {
			Transaction.getCurrent().Begin();

			// process
			MyBean bean = new MyBean();
			assert bean.getI() == 0;

			bean.setI(1);
			assert bean.getI() == 1;

			Transaction.getCurrent().Commit();
			assert bean.getI() == 1;
		}
		finally {
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
		Transaction.Create();
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
		}
		finally {
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
		Transaction.Create();
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
		}
		finally {
			Transaction.Destroy();
		}
	}
}