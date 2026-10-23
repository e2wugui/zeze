package Zeze;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Transaction;
import Zeze.Transaction.Logs.LogInt;
import Zeze.Util.Task;
import org.junit.jupiter.api.Assertions;

public class BMyBean extends Bean {
	@Override
	public void decode(IByteBuffer bb) {
		_i = bb.ReadInt();
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteInt(_i);
	}

	@Override
	public BMyBean copy() {
		var copy = new BMyBean();
		copy._i = getI();
		return copy;
	}

	public int _i;

	private static final VarHandle VH_I;

	static {
		try {
			VH_I = MethodHandles.lookup().findVarHandle(BMyBean.class, "_i", int.class);
		} catch (ReflectiveOperationException e) {
			throw Task.forceThrow(e);
		}
	}

	public final int getI() {
		var txn = Transaction.getCurrent();
		if (null == txn)
			return _i;
		LogInt log = (LogInt)txn.getLog(this.objectId());
		return (null != log) ? log.value : _i;
	}

	public final void setI(int value) {
		var txn = Transaction.getCurrent();
		Assertions.assertNotNull(txn);
		txn.putLog(new LogInt(this, 0, VH_I, value));
	}
}
