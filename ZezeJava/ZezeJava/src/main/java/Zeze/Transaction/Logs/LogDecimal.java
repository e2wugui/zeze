package Zeze.Transaction.Logs;

import java.util.Objects;

import java.lang.invoke.VarHandle;
import java.math.BigDecimal;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import org.jspecify.annotations.Nullable;

/** decimal 字段原子修改日志：经 VarHandle 提交新值，序列化为全精度字符串。 */
public class LogDecimal extends Log {
	private static final int TYPE_ID = Bean.hash32("Zeze.Transaction.Log<decimal>");

	private final @Nullable VarHandle vh;
	public @Nullable BigDecimal value;

	public LogDecimal(@Nullable Bean belong, int varId, @Nullable VarHandle vh, BigDecimal value) {
		super(belong, varId);
		this.vh = vh;
		this.value = value;
	}

	public LogDecimal(int varId) {
		super(null, varId);
		vh = null;
	}

	@Override
	public Category category() {
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
	public void encode(ByteBuffer bb) {
		//noinspection DataFlowIssue
		bb.WriteString(value.toString());
	}

		@Override
		public void decode(IByteBuffer bb) {
			// 不限精度：encode 写全精度字符串（Objects.requireNonNull(value).toString()），decode 必须原样还原，
			// 保持 encode/decode 双射。用 DECIMAL128 会把 >34 位有效数字静默截断，
			// 与 leader 内存值分叉且无任何检测手段。
			value = new BigDecimal(bb.ReadString());
		}

	@Override
	public String toString() {
		return String.valueOf(value);
	}

	@Override
	public Binary binaryValue() {
		//noinspection DataFlowIssue
		return new Binary(value.toString());
	}

	@Override
	public String stringValue() {
		//noinspection DataFlowIssue
		return value.toString();
	}

	@Override
	public BigDecimal decimalValue() {
		return Objects.requireNonNull(value);
	}
}
