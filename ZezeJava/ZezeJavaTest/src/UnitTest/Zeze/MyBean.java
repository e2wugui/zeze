package UnitTest.Zeze;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log1;
import Zeze.Transaction.Record;
import Zeze.Transaction.Transaction;

public class MyBean extends Bean {
	@Override
	public void Decode(ByteBuffer bb) {
		_i = bb.ReadInt();
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteInt(_i);
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
		var txn = Transaction.getCurrent();
		if(null == txn)
			return _i;
		MyBean.MyLog log = (MyBean.MyLog)txn.GetLog(this.getObjectId() + 0);
		return (null != log) ? log.getValue() : _i;
	}

	public final void setI(int value) {
		var txn = Transaction.getCurrent();
		assert null != txn;
		txn.PutLog(new MyBean.MyLog(this, value));
	}
}
