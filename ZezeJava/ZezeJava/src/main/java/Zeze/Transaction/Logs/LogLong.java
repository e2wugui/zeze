package Zeze.Transaction.Logs;

import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Serialize.ByteBuffer;

public class LogLong extends Log {
	private static final int TYPE_ID = Bean.hash32("Zeze.Transaction.Log<long>");

	public long value;

	public LogLong(Bean belong, int varId, long value) {
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	public LogLong() {

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
		value = bb.ReadLong();
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}
}
