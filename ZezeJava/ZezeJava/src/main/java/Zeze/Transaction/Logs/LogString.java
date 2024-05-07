package Zeze.Transaction.Logs;

import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Serialize.ByteBuffer;

public class LogString extends Log {
	private static final int TYPE_ID = Bean.hash32("Zeze.Transaction.Log<string>");

	public String value;

	public LogString(Bean belong, int varId, String value) {
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	public LogString() {

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
		bb.WriteString(value);
	}

	@Override
	public void decode(IByteBuffer bb) {
		value = bb.ReadString();
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}
}
