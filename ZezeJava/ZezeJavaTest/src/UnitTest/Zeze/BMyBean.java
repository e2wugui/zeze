package UnitTest.Zeze;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Record;
import Zeze.Transaction.Transaction;
import org.junit.Assert;

public class BMyBean extends Bean {
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

	@Override
	protected void ResetChildrenRootInfo() {
	}

	public int _i;

	private static class MyLog extends Zeze.Transaction.Logs.LogInt {
		public MyLog(BMyBean bean, int value) {
			super(bean, 0, value);
		}

		@Override
		public void Commit() {
			((BMyBean)getBean())._i = Value;
		}
	}

	public final int getI() {
		var txn = Transaction.getCurrent();
		if (null == txn)
			return _i;
		BMyBean.MyLog log = (BMyBean.MyLog)txn.GetLog(this.objectId());
		return (null != log) ? log.Value : _i;
	}

	public final void setI(int value) {
		var txn = Transaction.getCurrent();
		Assert.assertNotNull(txn);
		txn.PutLog(new BMyBean.MyLog(this, value));
	}
}
