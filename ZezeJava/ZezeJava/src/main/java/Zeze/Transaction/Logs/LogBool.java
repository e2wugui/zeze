package Zeze.Transaction.Logs;

import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Serialize.ByteBuffer;
import org.jetbrains.annotations.NotNull;

public class LogBool extends Log {
	private static final int TYPE_ID = Bean.hash32("Zeze.Transaction.Log<bool>");

	public boolean value;

	public LogBool(Bean belong, int varId, boolean value) {
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	public LogBool() {
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
		bb.WriteBool(value);
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		value = bb.ReadBool();
	}

	@Override
	public @NotNull String toString() {
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
}
