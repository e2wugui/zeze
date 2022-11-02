package Zeze.Transaction.Collections;

import java.lang.invoke.MethodHandle;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Transaction.Transaction;
import Zeze.Transaction.Record;
import Zeze.Util.Reflect;

public class CollOne<V extends Bean> extends Collection {
    V _Value;
    private final MethodHandle valueFactory;

    public CollOne(Class<V> valueClass) {
        valueFactory = Reflect.getDefaultConstructor(valueClass);
    }

    CollOne(MethodHandle vf) {
        valueFactory = vf;
    }

    public V getValue() {
        if (!isManaged())
            return _Value;

        var txn = Transaction.getCurrent();
        if (null == txn)
            return _Value;

        var log = (LogOne<V>)txn.getLog(parent().objectId() + variableId());
        if (null == log)
            return _Value;

        return log.value;
    }

    public void setValue(V value) {
        if (null == value)
            throw new IllegalArgumentException("value");

        if (isManaged()) {
            value.initRootInfoWithRedo(rootInfo, this);
            var log = (LogOne<V>)Transaction.getCurrentVerifyWrite(this)
                    .logGetOrAdd(parent().objectId() + variableId(), this::createLogBean);
            log.setValue(value);
        } else {
            _Value = value;
        }
    }

    public void assign(CollOne<V> other) {
        setValue((V)other.getValue().copy());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj instanceof CollOne)
            return getValue().equals(((CollOne<V>)obj).getValue());
        return false;
    }

    @Override
    public int hashCode() {
        return getValue().hashCode();
    }

    @Override
    public LogBean createLogBean() {
        var log = new LogOne<V>();
        log.setBelong(parent());
        log.setThis(this);
        log.setVariableId(variableId());
        log.setValue(getValue());
        return log;
    }

    @Override
    public void decode(ByteBuffer bb) {
        var Value = getValue();
        if (null == Value) {
            try {
                Value = (V)valueFactory.invoke();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
        Value.decode(bb);
    }

    @Override
    public void encode(ByteBuffer bb) {
        getValue().encode(bb);
    }

    @Override
    protected void initChildrenRootInfo(Record.RootInfo root) {
        getValue().initRootInfo(root, this);
    }

    @Override
    protected void resetChildrenRootInfo() {
        getValue().resetRootInfo();
    }

    @Override
    public void followerApply(Log _log) {
        var log = (LogOne<V>)_log;
        if (null != log.value) {
            _Value = log.value;
        } else if (null != log.logBean) {
            _Value.followerApply(log.logBean);
        }
    }

    @Override
    public CollOne<V> copy() {
        var copy = new CollOne<V>(valueFactory);
        copy._Value = (V)getValue().copy();
        return copy;
    }
}
