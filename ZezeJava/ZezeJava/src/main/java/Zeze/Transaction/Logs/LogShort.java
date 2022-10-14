package Zeze.Transaction.Logs;

import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Serialize.ByteBuffer;

public abstract class LogShort extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Transaction.Log<short>");

	public short value;

	public LogShort() {
		super(TYPE_ID);
	}

	public LogShort(Bean belong, int varId, short value) {
		this();
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteInt(value);
	}

	@Override
	public void decode(ByteBuffer bb) {
		value = (short)bb.ReadInt();
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}
}
