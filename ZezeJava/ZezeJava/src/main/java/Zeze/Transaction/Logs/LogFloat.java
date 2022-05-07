package Zeze.Transaction.Logs;

import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Serialize.ByteBuffer;

public abstract class LogFloat extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.Hash32("Zeze.Raft.RocksRaft.Log<float>");

	public float Value;

	public LogFloat() {
		super(TYPE_ID);
	}

	public LogFloat(Bean belong, int varId, float value) {
		this();
		setBelong(belong);
		setVariableId(varId);
		Value = value;
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteFloat(Value);
	}

	@Override
	public void Decode(ByteBuffer bb) {
		Value = bb.ReadFloat();
	}

	@Override
	public String toString() {
		return String.valueOf(Value);
	}
}
