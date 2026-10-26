package Zeze.Transaction.Collections;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Util.Task;
import org.jspecify.annotations.Nullable;

/** CollOne change payload, independent of any local transaction target. */
public class LogOneValue<V extends Bean> extends LogBean {
	private final LogOneMeta<V> meta;
	@Nullable V value;
	@Nullable LogBean logBean;

	protected LogOneValue(int varId, LogOneMeta<V> meta, Bean self) {
		super(varId, self);
		this.meta = meta;
	}

	private LogOneValue(int varId, LogOneMeta<V> meta) {
		super(varId, null);
		this.meta = meta;
	}

	@Override
	public int getTypeId() {
		return meta.logTypeId;
	}

	@Override
	public String getTypeName() {
		return meta.name;
	}

	@Override
	public void encode(ByteBuffer bb) {
		if (value != null) {
			bb.WriteBool(true);
			value.encode(bb);
		} else {
			bb.WriteBool(false); // value tag
			if (logBean != null) {
				bb.WriteBool(true);
				encodeLogBean(bb, logBean);
			} else
				bb.WriteBool(false);
		}
	}

	private void read(IByteBuffer bb) {
		if (bb.ReadBool()) { // hasValue
			try {
				//noinspection DataFlowIssue,unchecked
				value = (V)meta.valueFactory.invoke();
			} catch (Throwable e) { // MethodHandle.invoke
				throw Task.forceThrow(e);
			}
			value.decode(bb);
		} else if (bb.ReadBool()) { // hasLogBean
			logBean = decodeLogBean(bb);
		}
	}

	@Override
	public String toString() {
		return value != null ? value.toString() : "LogOne(logBean=" + logBean + ")";
	}

	public static <V extends Bean> LogOneValue<V> decode(int varId, LogOneMeta<V> meta, IByteBuffer bb) {
		var log = new LogOneValue<>(varId, meta);
		log.read(bb);
		return log;
	}
}
