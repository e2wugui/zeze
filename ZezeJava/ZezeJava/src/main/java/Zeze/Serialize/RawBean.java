package Zeze.Serialize;

import java.util.ArrayList;
import Zeze.Builtin.HotDistribute.BVariable;
import Zeze.Net.Binary;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import org.jspecify.annotations.Nullable;

/** 未解码的原始 Bean：仅持有 typeId 与原始字节，编码时按透明字节原样转发。 */
public class RawBean extends Bean {
	private final long typeId;
	private Binary rawData = Binary.Empty;

	public RawBean(long typeId) {
		this.typeId = typeId;
	}

	public RawBean(long typeId, Binary rawData) {
		this.typeId = typeId;
		this.rawData = rawData;
	}

	public Binary getRawData() {
		return rawData;
	}

	@Override
	public void reset() {
		throw new UnsupportedOperationException();
	}

	public RawBean copyIfManaged() {
		return this;
	}

	@Override
	public RawBean copy() {
		return this;
	}

	@Override
	public long typeId() {
		return typeId;
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		buildString(sb, 0);
		return sb.toString();
	}

	@Override
	public void buildString(StringBuilder sb, int level) {
		sb.append("RawBean:{typeId=").append(typeId).append(",rawData=").append(getRawData()).append('}');
	}

	@Override
	public void encode(ByteBuffer bb) {
		var rd = getRawData();
		if (rd.size() == 0)
			bb.WriteByte(0); // 表示一个空Bean的结束
		else
			bb.Append(rd.bytesUnsafe(), rd.getOffset(), rd.size());
	}

	@Override
	public void decode(IByteBuffer bb) {
		int i = bb.getReadIndex();
		bb.skipAllUnknownFields(bb.ReadByte());
		rawData = new Binary(bb.getBytes(i, bb.getReadIndex() - i));
	}

	@Override
	public int hashCode() {
		return Long.hashCode(typeId) ^ getRawData().hashCode();
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (o == this)
			return true;
		if (!(o instanceof RawBean))
			return false;
		//noinspection PatternVariableCanBeUsed
		var rb = (RawBean)o;
		return typeId == rb.typeId && getRawData().equals(rb.getRawData());
	}

	@Override
	public void followerApply(Log log) {
		// 代理Bean没有可应用的字段日志，空实现。
	}

	@Override
	public ArrayList<BVariable.Data> variables() {
		var vs = super.variables();
		vs.add(new BVariable.Data(1, "rawData", "binary", "", ""));
		return vs;
	}
}
