package Zeze.Transaction.Logs;

import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Serialize.ByteBuffer;

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
	public int getTypeId() {
		return TYPE_ID;
	}

	@Override
	public void commit() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteLong(value);
	}

	@Override
	public void decode(IByteBuffer bb) {
		value = (byte)bb.ReadLong();
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}
}
