package Zeze.Transaction.Logs;

import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Serialize.ByteBuffer;
import org.jetbrains.annotations.NotNull;

public class LogByte extends Log {
	private static final int TYPE_ID = Bean.hash32("Zeze.Transaction.Log<byte>");

	public byte value;

	public LogByte(Bean belong, int varId, byte value) {
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	public LogByte() {
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
		bb.WriteLong(value);
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		value = (byte)bb.ReadLong();
	}

	@Override
	public @NotNull String toString() {
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
}
