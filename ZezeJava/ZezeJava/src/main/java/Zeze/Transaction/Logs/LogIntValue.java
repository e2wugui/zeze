package Zeze.Transaction.Logs;

import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Serialize.ByteBuffer;

/** Complete field change value, independent of any local transaction target. */
public class LogIntValue extends Log {
	public static final int TYPE_ID = Bean.hash32("Zeze.Transaction.Log<int>");

	public int value;

	protected LogIntValue(int varId, int value) {
		super(varId);
		this.value = value;
	}

	@Override
	public int getTypeId() {
		return TYPE_ID;
	}

	@Override
	public String getTypeName() {
		return "LogInt";
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteInt(value);
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}

	@Override
	public int intValue() {
		return value;
	}

	@Override
	public long longValue() {
		return value;
	}

	@Override
	public double doubleValue() {
		return value;
	}

	public static LogIntValue decode(int varId, IByteBuffer bb) {
		return new LogIntValue(varId, bb.ReadInt());
	}
}
