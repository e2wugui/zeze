package Zeze.Raft.RocksRaft.Log1;

import java.math.BigDecimal;
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
		// 不限精度，保持与 encode（全精度字符串）的双射（FND3-06）。
		value = new BigDecimal(bb.ReadString());
	}

	@Override
	public String toString() {
		return "Value=" + value;
	}
}
