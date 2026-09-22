package Zeze.Transaction.Collections;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Changes;
import Zeze.Transaction.Log;
import Zeze.Transaction.Savepoint;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** CollOne 的修改日志：整体换值或收集子 Bean 的修改。 */
@SuppressWarnings("unchecked")
public class LogOne<V extends Bean> extends LogBean {
	private final @NotNull LogOneMeta<V> meta;
	V value;
	@Nullable LogBean logBean;

	public LogOne(Bean belong, int varId, Bean self, @NotNull V value) {
		super(belong, varId, self);
		// 声明类优先（FND7-80）：装子类实例须按声明类建typeId，与读端注册对称
		// （Helper.dependsBean→registerLogOne）；宿主meta未提供时退回运行时类。
		var hostMeta = self instanceof CollOne<?> collOne ? collOne.meta : null;
		meta = hostMeta != null ? (LogOneMeta<V>)hostMeta : LogOneMeta.get((Class<V>)value.getClass());
		this.value = value;
	}

	public LogOne(int varId, @NotNull LogOneMeta<V> meta) {
		super(null, varId, null);
		this.meta = meta; // for decode
	}

	public void setValue(@NotNull V value) {
		this.value = value;
	}

	@Override
	public int getTypeId() {
		return meta.logTypeId;
	}

	@Override
	public @NotNull String getTypeName() {
		return meta.name;
	}

	@Override
	public @NotNull Log beginSavepoint() {
		return new LogOne<>(getBelong(), getVariableId(), getThis(), value);
	}

	@Override
	public void endSavepoint(@NotNull Savepoint currentSp) {
		// 结束保存点，直接覆盖到当前的日志里面即可。
		currentSp.putLog(this);
	}
	// 收集内部的Bean发生了改变。

	@Override
	public void collect(@NotNull Changes changes, @NotNull Bean recent, @NotNull Log vlog) {
		if (logBean == null) {
			logBean = (LogBean)vlog;
			changes.collect(recent, this);
		}
	}

	@Override
	public void commit() {
		if (value != null) // value是否真的可以为null,目前没看到哪里可以让它为null
			((CollOne<V>)getThis()).value = value;
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		if (value != null) { // value是否真的可以为null,目前没看到哪里可以让它为null
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

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		var hasValue = bb.ReadBool();
		if (hasValue) {
			try {
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
	public @NotNull String toString() {
		// decode可构造出value==null的LogOne（hasValue=false&&hasLogBean=true，见decode），
		// encode/commit都处理了null，这里不处理的话日志打印级联调用会抛NPE掩盖原始异常。
		return value != null ? value.toString() : "LogOne(logBean=" + logBean + ")";
	}
}
