package Zeze.Transaction.Logs;

import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Serialize.ByteBuffer;

/** Complete field change value, independent of any local transaction target. */
public class LogDoubleValue extends Log {
	public static final int TYPE_ID = Bean.hash32("Zeze.Transaction.Log<double>");

	public double value;

	protected LogDoubleValue(int varId, double value) {
		super(varId);
		this.value = value;
	}

	@Override
	public int getTypeId() {
		return TYPE_ID;
	}

	@Override
	public String getTypeName() {
		return "LogDouble";
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteDouble(value);
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}

	@Override
	public long longValue() {
		return (long)value;
	}

	@Override
	public double doubleValue() {
		return value;
	}

	public static LogDoubleValue decode(int varId, IByteBuffer bb) {
		return new LogDoubleValue(varId, bb.ReadDouble());
	}
}
