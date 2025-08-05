package UnitTest.Zeze;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Transaction;
import org.junit.Assert;

public class BMyBean extends Bean {
	@Override
	public void decode(IByteBuffer bb) {
		_i = bb.ReadInt();
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteInt(_i);
	}

	public int _i;

	private static class MyLog extends Zeze.Transaction.Logs.LogInt {
		public MyLog(BMyBean bean, int value) {
			super(bean, 0, null, value);
		}

		@Override
		public void commit() {
			((BMyBean)getBelong())._i = value;
		}
	}

	public final int getI() {
		var txn = Transaction.getCurrent();
		if (null == txn)
			return _i;
		MyLog log = (MyLog)txn.getLog(this.objectId());
		return (null != log) ? log.value : _i;
	}

	public final void setI(int value) {
		var txn = Transaction.getCurrent();
		Assert.assertNotNull(txn);
		txn.putLog(new MyLog(this, value));
	}
}
