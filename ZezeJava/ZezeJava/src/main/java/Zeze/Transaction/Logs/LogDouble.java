package Zeze.Transaction.Logs;

import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Serialize.ByteBuffer;

public abstract class LogDouble extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Transaction.Log<double>");

	public double value;

	public LogDouble() {
		super(TYPE_ID);
	}

	public LogDouble(Bean belong, int varId, double value) {
		this();
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteDouble(value);
	}

	@Override
	public void decode(ByteBuffer bb) {
		value = bb.ReadDouble();
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}
}
