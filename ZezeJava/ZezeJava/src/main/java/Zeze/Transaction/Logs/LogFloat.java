package Zeze.Transaction.Logs;

import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Serialize.ByteBuffer;

public abstract class LogFloat extends Log {
	private static final int TYPE_ID = Bean.hash32("Zeze.Transaction.Log<float>");

	public float value;

	public LogFloat(Bean belong, int varId, float value) {
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	@Override
	public int getTypeId() {
		return TYPE_ID;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteFloat(value);
	}

	@Override
	public void decode(IByteBuffer bb) {
		value = bb.ReadFloat();
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}
}
