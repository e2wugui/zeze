package Zeze.Transaction.Logs;

import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Serialize.ByteBuffer;

/** Complete field change value, independent of any local transaction target. */
public class LogLongValue extends Log {
	public static final int TYPE_ID = Bean.hash32("Zeze.Transaction.Log<long>");

	public long value;

	protected LogLongValue(int varId, long value) {
		super(varId);
		this.value = value;
	}

	@Override
	public int getTypeId() {
		return TYPE_ID;
	}

	@Override
	public String getTypeName() {
		return "LogLong";
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteLong(value);
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}

	@Override
	public long longValue() {
		return value;
	}

	@Override
	public double doubleValue() {
		return value;
	}

	public static LogLongValue decode(int varId, IByteBuffer bb) {
		return new LogLongValue(varId, bb.ReadLong());
	}
}
