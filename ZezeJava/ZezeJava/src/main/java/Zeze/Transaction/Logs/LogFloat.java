package Zeze.Transaction.Logs;

import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Serialize.ByteBuffer;

public abstract class LogFloat extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Transaction.Log<float>");

	public float value;

	public LogFloat() {
		super(TYPE_ID);
	}

	public LogFloat(Bean belong, int varId, float value) {
		this();
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteFloat(value);
	}

	@Override
	public void decode(ByteBuffer bb) {
		value = bb.ReadFloat();
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}
}
