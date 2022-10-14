package Zeze.Transaction.Logs;

import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Serialize.ByteBuffer;

public abstract class LogString extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Transaction.Log<string>");

	public String value;

	public LogString() {
		super(TYPE_ID);
	}

	public LogString(Bean belong, int varId, String value) {
		this();
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteString(value);
	}

	@Override
	public void decode(ByteBuffer bb) {
		value = bb.ReadString();
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}
}
