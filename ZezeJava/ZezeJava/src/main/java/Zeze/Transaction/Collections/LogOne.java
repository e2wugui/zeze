package Zeze.Transaction.Collections;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Changes;
import Zeze.Transaction.Log;
import Zeze.Transaction.Savepoint;
import org.jetbrains.annotations.NotNull;

public class LogOne<V extends Bean> extends LogBean {
	public @NotNull V value;
	public LogBean logBean;

	LogOne(@NotNull V value) {
		this.value = value;
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
		//noinspection ConstantValue
		if (value != null) { // value是否真的可以为null,目前没看到哪里可以让它为null
			((CollOne<V>)getThis())._Value = value;
		}
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		//noinspection ConstantValue
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

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		throw new UnsupportedOperationException();
        /*
        var hasValue = bb.ReadBool();
        if (hasValue) {
            Value = new V();
            Value.decode(bb);
        } else {
            var hasLogBean = bb.ReadBool();
            if (hasLogBean) {
                LogBean = new LogBean();
                LogBean.decode(bb);
            }
        }
        */
	}

	@Override
	public @NotNull String toString() {
		return value.toString();
	}
}
