package Zeze.Transaction.Logs;

import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Serialize.ByteBuffer;

/** float 字段变更值日志，独立于本地事务目标。 */
public class LogFloatValue extends Log {
	public static final int TYPE_ID = Bean.hash32("Zeze.Transaction.Log<float>");

	public float value;

	protected LogFloatValue(int varId, float value) {
		super(varId);
		this.value = value;
	}

	@Override
	public int getTypeId() {
		return TYPE_ID;
	}

	@Override
	public String getTypeName() {
		return "LogFloat";
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteFloat(value);
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}

	@Override
	public boolean booleanValue() {
		return value != 0;
	}

	@Override
	public int intValue() {
		return (int)value;
	}

	@Override
	public long longValue() {
		return (long)value;
	}

	@Override
	public float floatValue() {
		return value;
	}

	@Override
	public double doubleValue() {
		return value;
	}

	public static LogFloatValue decode(int varId, IByteBuffer bb) {
		return new LogFloatValue(varId, bb.ReadFloat());
	}
}
