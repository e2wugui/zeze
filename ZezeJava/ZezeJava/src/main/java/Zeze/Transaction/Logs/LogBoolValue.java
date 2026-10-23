package Zeze.Transaction.Logs;

import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Serialize.ByteBuffer;

/** Complete field change value, independent of any local transaction target. */
public class LogBoolValue extends Log {
	public static final int TYPE_ID = Bean.hash32("Zeze.Transaction.Log<bool>");

	public boolean value;

	protected LogBoolValue(int varId, boolean value) {
		super(varId);
		this.value = value;
	}

	@Override
	public int getTypeId() {
		return TYPE_ID;
	}

	@Override
	public String getTypeName() {
		return "LogBool";
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteBool(value);
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}

	@Override
	public boolean booleanValue() {
		return value;
	}

	@Override
	public long longValue() {
		return value ? 1 : 0;
	}

	@Override
	public double doubleValue() {
		return value ? 1 : 0;
	}

	public static LogBoolValue decode(int varId, IByteBuffer bb) {
		return new LogBoolValue(varId, bb.ReadBool());
	}
}
