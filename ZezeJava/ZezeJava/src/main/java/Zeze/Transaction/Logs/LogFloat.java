package Zeze.Transaction.Logs;

import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Serialize.ByteBuffer;
import org.jetbrains.annotations.NotNull;

public class LogFloat extends Log {
	private static final int TYPE_ID = Bean.hash32("Zeze.Transaction.Log<float>");

	public float value;

	public LogFloat(Bean belong, int varId, float value) {
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	public LogFloat() {
	}

	@Override
	public @NotNull Category category() {
		return Category.eHistory;
	}

	@Override
	public int getTypeId() {
		return TYPE_ID;
	}

	@Override
	public void commit() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		bb.WriteFloat(value);
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		value = bb.ReadFloat();
	}

	@Override
	public @NotNull String toString() {
		return String.valueOf(value);
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
}
