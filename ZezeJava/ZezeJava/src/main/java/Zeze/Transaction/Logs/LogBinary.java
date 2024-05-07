package Zeze.Transaction.Logs;

import Zeze.Net.Binary;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Serialize.ByteBuffer;

public class LogBinary extends Log {
	private static final int TYPE_ID = Bean.hash32("Zeze.Transaction.Log<binary>");

	public Binary value;

	public LogBinary(Bean belong, int varId, Binary value) {
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	public LogBinary() {

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
		bb.WriteBinary(value);
	}

	@Override
	public void decode(IByteBuffer bb) {
		value = bb.ReadBinary();
	}

	@Override
	public String toString() {
		return value.toString();
	}
}
