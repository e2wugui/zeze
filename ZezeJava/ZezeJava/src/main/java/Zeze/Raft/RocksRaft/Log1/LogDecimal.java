package Zeze.Raft.RocksRaft.Log1;

import java.math.BigDecimal;
import java.math.MathContext;
import Zeze.Raft.RocksRaft.Bean;
import Zeze.Raft.RocksRaft.Log;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

public class LogDecimal extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Raft.RocksRaft.Log<decimal>");

	public BigDecimal value;

	public LogDecimal() {
		super(TYPE_ID);
	}

	public LogDecimal(Bean belong, int varId, BigDecimal value) {
		this();
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteString(value.toString());
	}

	@Override
	public void decode(IByteBuffer bb) {
		value = new BigDecimal(bb.ReadString(), MathContext.DECIMAL128);
	}

	@Override
	public String toString() {
		return "Value=" + value;
	}
}
