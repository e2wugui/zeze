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

	private static final VarHandle vh_rawData;

	static {
		try {
			vh_rawData = MethodHandles.lookup().findVarHandle(RawBean.class, "rawData", Binary.class);
		} catch (ReflectiveOperationException e) {
			throw Task.forceThrow(e);
		}
	}

	public RawBean(long typeId) {
		this.typeId = typeId;
	}

	public @NotNull Binary getRawData() {
		if (!isManaged())
			return rawData;
		var t = Transaction.getCurrentVerifyRead(this);
		if (t == null)
			return rawData;
		var log = (LogBinary)t.getLog(objectId() + 1);
		return log != null ? log.value : rawData;
	}

	/**
	 * @param rawData 必须是标准的Bean序列化的数据
	 */
	public void setRawData(@NotNull Binary rawData) {
		//noinspection ConstantValue
		if (rawData == null)
			throw new IllegalArgumentException();
		if (!isManaged()) {
			this.rawData = rawData;
			return;
		}
		var t = Transaction.getCurrentVerifyWrite(this);
		t.putLog(new LogBinary(this, 1, vh_rawData, rawData));
	}

	@Override
	public void reset() {
		setRawData(Binary.Empty);
	}

	public RawBean copyIfManaged() {
		return isManaged() ? copy() : this;
	}

	@Override
	public RawBean copy() {
		var c = new RawBean(typeId);
		c.setRawData(getRawData());
		return c;
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
	public void buildString(@NotNull StringBuilder sb, int level) {
		sb.append("RawBean:{typeId=").append(typeId).append(",rawData=").append(getRawData()).append('}');
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		if (rawData.size() == 0)
			bb.WriteByte(0); // 表示一个空Bean的结束
		else
			bb.Append(rawData.bytesUnsafe(), rawData.getOffset(), rawData.size());
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		int i = bb.getReadIndex();
		bb.skipAllUnknownFields(bb.ReadByte());
		rawData = new Binary(bb.getBytes(i, bb.getReadIndex() - i));
	}

	@Override
	public int hashCode() {
		return Long.hashCode(typeId) ^ rawData.hashCode();
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
		var vs = ((LogBean)log).getVariables();
		if (vs == null)
			return;
		for (var i = vs.iterator(); i.moveToNext(); ) {
			var v = i.value();
			if (v.getVariableId() == 1)
				rawData = v.binaryValue();
		}
	}

	@Override
	public void decodeResultSet(ArrayList<String> parents, ResultSet rs) throws SQLException {
		setRawData(new Binary(rs.getBytes(Bean.parentsToName(parents) + "rawData")));
	}

	@Override
	public void encodeSQLStatement(ArrayList<String> parents, SQLStatement st) {
		st.appendBinary(Bean.parentsToName(parents) + "rawData", getRawData());
	}

	@Override
	public @NotNull ArrayList<BVariable.Data> variables() {
		var vs = super.variables();
		vs.add(new BVariable.Data(1, "rawData", "binary", "", ""));
		return vs;
	}
}
