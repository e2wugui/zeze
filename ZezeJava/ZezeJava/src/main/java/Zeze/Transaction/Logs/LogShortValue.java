package Zeze.Transaction.Logs;

import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Serialize.ByteBuffer;

/** Complete field change value, independent of any local transaction target. */
public class LogShortValue extends Log {
	public static final int TYPE_ID = Bean.hash32("Zeze.Transaction.Log<short>");

	public short value;

	protected LogShortValue(int varId, short value) {
		super(varId);
		this.value = value;
	}

	@Override
	public int getTypeId() {
		return TYPE_ID;
	}

	@Override
	public String getTypeName() {
		return "LogShort";
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
	public short shortValue() {
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

	public static LogShortValue decode(int varId, IByteBuffer bb) {
		return new LogShortValue(varId, (short)bb.ReadInt());
	}
}
