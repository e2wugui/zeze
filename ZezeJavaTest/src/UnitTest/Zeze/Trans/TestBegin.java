package UnitTest.Zeze.Trans;

import Zeze.Serialize.*;
import Zeze.Transaction.*;
import UnitTest.*;

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [TestClass] public class TestBegin
public class TestBegin {
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

		private static class MyLog extends Log<MyBean, Integer> {
			public MyLog(MyBean bean, int value) {
				super(bean, value);

			}

			@Override
			public long getLogKey() {
				return Bean.ObjectId + 0;
			}

			@Override
			public void Commit() {
				((MyBean)Bean)._i = getValue();
			}
		}
		public final int getI() {
			MyLog log = (MyLog)Transaction.Current.GetLog(this.getObjectId() + 0);
			return (null != log) ? log.getValue() : _i;
		}
		public final void setI(int value) {
			Transaction.Current.PutLog(new MyLog(this, value));
		}
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [TestMethod] public void TestRollback()
	public final void TestRollback() {
		Transaction.Create();
		try {
			Transaction.Current.Begin();

			// process
			MyBean bean = new MyBean();
			assert bean.getI() == 0;

			bean.setI(1);
			assert bean.getI() == 1;

			Transaction.Current.Rollback();
			assert bean.getI() == 0;
		}
		finally {
			Transaction.Destroy();
		}
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [TestMethod] public void TestCommit()
	public final void TestCommit() {
		Transaction.Create();
		try {
			Transaction.Current.Begin();

			// process
			MyBean bean = new MyBean();
			assert bean.getI() == 0;

			bean.setI(1);
			assert bean.getI() == 1;

			Transaction.Current.Commit();
			assert bean.getI() == 1;
		}
		finally {
			Transaction.Destroy();
		}
	}

	private void ProcessNestRollback(MyBean bean) {
		assert bean.getI() == 1;
		Transaction.Current.Begin();
		assert bean.getI() == 1;
		bean.setI(2);
		assert bean.getI() == 2;
		Transaction.Current.Rollback();
		assert bean.getI() == 1;
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [TestMethod] public void TestNestRollback()
	public final void TestNestRollback() {
		Transaction.Create();
		try {
			Transaction.Current.Begin();

			// process
			MyBean bean = new MyBean();
			assert bean.getI() == 0;

			bean.setI(1);
			assert bean.getI() == 1;
			ProcessNestRollback(bean);
			assert bean.getI() == 1;

			Transaction.Current.Commit();
			assert bean.getI() == 1;
		}
		finally {
			Transaction.Destroy();
		}
	}

	private void ProcessNestCommit(MyBean bean) {
		assert bean.getI() == 1;
		Transaction.Current.Begin();
		assert bean.getI() == 1;
		bean.setI(2);
		assert bean.getI() == 2;
		Transaction.Current.Commit();
		assert bean.getI() == 2;
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [TestMethod] public void TestNestCommit()
	public final void TestNestCommit() {
		Transaction.Create();
		try {
			Transaction.Current.Begin();

			// process
			MyBean bean = new MyBean();
			assert bean.getI() == 0;

			bean.setI(1);
			assert bean.getI() == 1;
			ProcessNestCommit(bean);
			assert bean.getI() == 2;

			Transaction.Current.Commit();
			assert bean.getI() == 2;
		}
		finally {
			Transaction.Destroy();
		}
	}
}