package Zeze.Transaction.Collections;

import java.util.Objects;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Changes;
import Zeze.Transaction.Log;
import Zeze.Transaction.TransactionLog;
import Zeze.Transaction.Savepoint;
import Zeze.Util.Task;
import org.jspecify.annotations.Nullable;

/** CollOne 的修改日志：整体换值或收集子 Bean 的修改。 */
@SuppressWarnings("unchecked")
public class LogOne<V extends Bean> extends LogBean implements TransactionLog {
	private final @Nullable Bean belong;

	@Override
	public final @Nullable Bean getBelong() {
		return belong;
	}

	@Override
	public final Category category() {
		return Category.eHistory;
	}

	private final LogOneMeta<V> meta;
	@Nullable V value;
	@Nullable LogBean logBean;

	public LogOne(@Nullable Bean belong, int varId, @Nullable Bean self, V value) {
		super(varId, self);
		this.belong = belong;
		// 声明类优先：装子类实例须按声明类建typeId，与读端注册对称
		// （Helper.dependsBean→registerLogOne）；宿主meta未提供时退回运行时类。
		var hostMeta = self instanceof CollOne<?> collOne ? collOne.meta : null;
		meta = hostMeta != null ? (LogOneMeta<V>)hostMeta : LogOneMeta.get((Class<V>)value.getClass());
		this.value = value;
	}

	public LogOne(int varId, LogOneMeta<V> meta) {
		super(varId, null);
		belong = null;
		this.meta = meta; // for decode
	}

	public void setValue(V value) {
		this.value = value;
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
	public TransactionLog beginSavepoint() {
		return new LogOne<>(getBelong(), getVariableId(), getThis(), Objects.requireNonNull(value));
	}

	@Override
	public void endSavepoint(Savepoint currentSp) {
		// 结束保存点，直接覆盖到当前的日志里面即可。
		currentSp.putLog(this);
	}
	// 收集内部的Bean发生了改变。

	@Override
	public boolean addChild(Bean recent, Log vlog) {
		if (logBean == null) {
			logBean = (LogBean)vlog;
			return true;
		}
		return false;
	}

	@Override
	public void commit() {
		if (value != null)
			//noinspection DataFlowIssue
			((CollOne<V>)getThis()).value = value;
	}

	@Override
	public void encode(ByteBuffer bb) {
		if (value != null) {
			bb.WriteBool(true);
			value.encode(bb);
		} else {
			// 此分支目前不可达：写端构造LogOne时value恒非null（CollOne.createLogBean传getValue、
			// setValue传非null值），仓内也没有"解码后再次编码转发"的路径。
			// 仍按家族标准encodeLogBean写子类判别字节（同LogMap2/LogSortedMap2），保证万一
			// 将来可达（如one-dynamic、日志转发）时线格式对称，不会静默错位。
			bb.WriteBool(false); // value tag
			if (logBean != null) {
				bb.WriteBool(true);
				encodeLogBean(bb, logBean);
			} else
				bb.WriteBool(false);
		}
	}

	private void read(IByteBuffer bb) {
		var hasValue = bb.ReadBool();
		if (hasValue) {
			try {
				//noinspection DataFlowIssue
				value = (V)meta.valueFactory.invoke();
			} catch (Throwable e) { // MethodHandle.invoke
				throw Task.forceThrow(e);
			}
			value.decode(bb);
		} else if (bb.ReadBool()) { // hasLogBean。分支不可达性见encode注释；decodeLogBean按判别字节重建子类。
			logBean = decodeLogBean(bb);
		}
	}

	@Override
	public String toString() {
		// decode可构造出value==null的LogOne（hasValue=false&&hasLogBean=true，见decode），
		// encode/commit都处理了null，这里不处理的话日志打印级联调用会抛NPE掩盖原始异常。
		return value != null ? value.toString() : "LogOne(logBean=" + logBean + ")";
	}

	public static <V extends Bean> LogOne<V> decode(int varId, LogOneMeta<V> meta, IByteBuffer bb) {
		var log = new LogOne<>(varId, meta);
		log.read(bb);
		return log;
	}

	@Override
	public void collectChanges(Changes changes) {
		var owner = getBelong();
		if (owner != null && owner.isManaged())
			changes.collect(owner, this);
	}
}
