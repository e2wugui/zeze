package Zeze.Transaction.Logs;

import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Serialize.ByteBuffer;

/** Complete field change value, independent of any local transaction target. */
public class LogByteValue extends Log {
	public static final int TYPE_ID = Bean.hash32("Zeze.Transaction.Log<byte>");

	public byte value;

	protected LogByteValue(int varId, byte value) {
		super(varId);
		this.value = value;
	}

	@Override
	public int getTypeId() {
		return TYPE_ID;
	}

	@Override
	public String getTypeName() {
		return "LogByte";
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
	public byte byteValue() {
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

	public static LogByteValue decode(int varId, IByteBuffer bb) {
		return new LogByteValue(varId, (byte)bb.ReadLong());
	}
}
