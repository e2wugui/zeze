package Zeze.Transaction.Logs;

import java.lang.invoke.VarHandle;
import java.math.BigDecimal;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import org.jetbrains.annotations.NotNull;

public class LogDecimal extends Log {
	private static final int TYPE_ID = Bean.hash32("Zeze.Transaction.Log<decimal>");

	private final VarHandle vh;
	public BigDecimal value;

	public LogDecimal(Bean belong, int varId, VarHandle vh, BigDecimal value) {
		super(belong, varId);
		this.vh = vh;
		this.value = value;
	}

	public LogDecimal(int varId) {
		super(null, varId);
		vh = null;
	}

	@Override
	public @NotNull Category category() {
		return Category.eHistory;
	}

	@Override
	public int getTypeId() {
		return TYPE_ID;
	}

	@Override
	public void commit() {
		//noinspection DataFlowIssue
		vh.set(getBelong(), value);
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		bb.WriteString(value.toString());
	}

		@Override
		public void decode(@NotNull IByteBuffer bb) {
			// 不限精度：encode 写全精度字符串（value.toString()），decode 必须原样还原，
			// 保持 encode/decode 双射。用 DECIMAL128 会把 >34 位有效数字静默截断，
			// 与 leader 内存值分叉且无任何检测手段（FND3-06）。
			value = new BigDecimal(bb.ReadString());
		}

	@Override
	public @NotNull String toString() {
		return String.valueOf(value);
	}

	@Override
	public @NotNull Binary binaryValue() {
		return new Binary(value.toString());
	}

	@Override
	public @NotNull String stringValue() {
		return value.toString();
	}

	@Override
	public @NotNull BigDecimal decimalValue() {
		return value;
	}
}
