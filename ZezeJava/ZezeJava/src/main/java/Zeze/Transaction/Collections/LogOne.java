package Zeze.Transaction.Collections;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Changes;
import Zeze.Transaction.Log;
import Zeze.Transaction.Savepoint;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;

public class LogOne<V extends Bean> extends LogBean {
	public V value;
	public LogBean logBean;
	public final Meta1<V> meta;

	public LogOne(@NotNull V value) {
		this.value = value;
		meta = null; // 事务使用不需要动态创建，decode。
	}

	public LogOne(@NotNull Class<V> beanClass) {
		meta = Meta1.getBeanMeta(beanClass); // for decode
	}

	public void setValue(@NotNull V value) {
		this.value = value;
	}

	@Override
	public @NotNull Log beginSavepoint() {
		var dup = new LogOne<>(value);
		dup.setThis(getThis());
		dup.setBelong(getBelong());
		dup.setVariableId(getVariableId());
		return dup;
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

	@SuppressWarnings("unchecked")
	@Override
	public void commit() {
		if (value != null) { // value是否真的可以为null,目前没看到哪里可以让它为null
			((CollOne<V>)getThis())._Value = value;
		}
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		if (null != value) { // value是否真的可以为null,目前没看到哪里可以让它为null
			bb.WriteBool(true);
			value.encode(bb);
		} else {
			bb.WriteBool(false); // Value Tag
			if (null != logBean) {
				bb.WriteBool(true);
				logBean.encode(bb);
			} else {
				bb.WriteBool(false);
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void decode(@NotNull IByteBuffer bb) {
        var hasValue = bb.ReadBool();
        if (hasValue) {
			try {
				value = (V)meta.valueFactory.invoke();
			} catch (Throwable e) {
				Task.forceThrow(e);
			}
			value.decode(bb);
        } else {
            var hasLogBean = bb.ReadBool();
            if (hasLogBean) {
                logBean = new LogBean();
                logBean.decode(bb);
            }
        }
	}

	@Override
	public @NotNull String toString() {
		return value.toString();
	}
}
