package Zeze.Transaction.Logs;

import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Serialize.ByteBuffer;
import org.jetbrains.annotations.NotNull;

public class LogInt extends Log {
	private static final int TYPE_ID = Bean.hash32("Zeze.Transaction.Log<int>");

	public int value;

	public LogInt(Bean belong, int varId, int value) {
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	public LogInt() {
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
		bb.WriteInt(value);
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		value = bb.ReadInt();
	}

	@Override
	public @NotNull String toString() {
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
}
