package Zeze.Transaction.Logs;

import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Serialize.ByteBuffer;

public abstract class LogBool extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Transaction.Log<bool>");

	public boolean value;

	public LogBool() {
		super(TYPE_ID);
	}

	public LogBool(Bean belong, int varId, boolean value) {
		this();
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteBool(value);
	}

	@Override
	public void decode(ByteBuffer bb) {
		value = bb.ReadBool();
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}
}
