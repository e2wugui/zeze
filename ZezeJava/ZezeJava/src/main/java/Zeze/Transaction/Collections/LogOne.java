package Zeze.Transaction.Collections;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Changes;
import Zeze.Transaction.Log;
import Zeze.Transaction.Savepoint;

public class LogOne<V extends Bean> extends LogBean {
	public V value;
	public LogBean logBean;

	public void setValue(V value) {
		this.value = value;
	}

	@Override
	public Log beginSavepoint() {
		var dup = new LogOne<>();
		dup.setThis(getThis());
		dup.setBelong(getBelong());
		dup.setVariableId(getVariableId());
		dup.value = value;
		return dup;
	}

	@Override
	public void endSavepoint(Savepoint currentSp) {
		// 结束保存点，直接覆盖到当前的日志里面即可。
		currentSp.putLog(this);
	}
	// 收集内部的Bean发生了改变。

	@Override
	public void collect(Changes changes, Bean recent, Log vlog) {
		if (logBean == null) {
			logBean = (LogBean)vlog;
			changes.collect(recent, this);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void commit() {
		if (value != null) {
			((CollOne<V>)getThis())._Value = value;
		}
	}

	@Override
	public void encode(ByteBuffer bb) {
		if (null != value) {
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
	public void decode(ByteBuffer bb) {
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
	public String toString() {
		return value.toString();
	}
}
