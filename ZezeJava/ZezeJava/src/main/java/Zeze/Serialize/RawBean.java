package Zeze.Serialize;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import Zeze.Builtin.HotDistribute.BVariable;
import Zeze.Net.Binary;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Collections.LogBean;
import Zeze.Transaction.Log;
import Zeze.Transaction.Logs.LogBinary;
import Zeze.Transaction.Transaction;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RawBean extends Bean {
	private final long typeId;
	private @NotNull Binary rawData = Binary.Empty;

	/*
	private static final @NotNull VarHandle vh_rawData;

	static {
		try {
			vh_rawData = MethodHandles.lookup().findVarHandle(RawBean.class, "rawData", Binary.class);
		} catch (ReflectiveOperationException e) {
			throw Task.forceThrow(e);
		}
	}
	*/

	public RawBean(long typeId) {
		this.typeId = typeId;
	}

	public RawBean(long typeId, @NotNull Binary rawData) {
		this.typeId = typeId;
		this.rawData = rawData;
	}

	public @NotNull Binary getRawData() {
		return rawData;
	}

	@Override
	public void reset() {
		throw new UnsupportedOperationException();
	}

	public @NotNull RawBean copyIfManaged() {
		return this;
	}

	@Override
	public @NotNull RawBean copy() {
		return this;
	}

	@Override
	public long typeId() {
		return typeId;
	}

	@Override
	public @NotNull String toString() {
		var sb = new StringBuilder();
		buildString(sb, 0);
		return sb.toString();
	}

	@Override
	public void buildString(@NotNull StringBuilder sb, int level) {
		sb.append("RawBean:{typeId=").append(typeId).append(",rawData=").append(getRawData()).append('}');
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		var rd = getRawData();
		if (rd.size() == 0)
			bb.WriteByte(0); // 表示一个空Bean的结束
		else
			bb.Append(rd.bytesUnsafe(), rd.getOffset(), rd.size());
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
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
	public void followerApply(@NotNull Log log) {
		// 本来想在这里几个日志，因为此时这个代理Bean已经没法使用了，后面肯定会知道发生了某个问题。先不记录了。
	}

	@Override
	public void decodeResultSet(ArrayList<String> parents, ResultSet rs) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void encodeSQLStatement(ArrayList<String> parents, SQLStatement st) {
		throw new UnsupportedOperationException();
	}

	@Override
	public @NotNull ArrayList<BVariable.Data> variables() {
		var vs = super.variables();
		vs.add(new BVariable.Data(1, "rawData", "binary", "", ""));
		return vs;
	}
}
