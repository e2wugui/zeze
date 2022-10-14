package Zeze.Transaction.Logs;

import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Serialize.ByteBuffer;

public abstract class LogLong extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Transaction.Log<long>");

	public long value;

	public LogLong() {
		super(TYPE_ID);
	}

	public LogLong(Bean belong, int varId, long value) {
		this();
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteLong(value);
	}

	@Override
	public void decode(ByteBuffer bb) {
		value = bb.ReadLong();
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}
}
