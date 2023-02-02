// auto-generated @formatter:off
package Zeze.Builtin.Collections.Queue;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BQueueNodeValue extends Zeze.Transaction.Bean implements BQueueNodeValueReadOnly {
    public static final long TYPEID = 486912310764000976L;

    private long _Timestamp;
    private final Zeze.Transaction.DynamicBean _Value;

    public static Zeze.Transaction.DynamicBean newDynamicBean_Value() {
        return new Zeze.Transaction.DynamicBean(2, Zeze.Collections.Queue::getSpecialTypeIdFromBean, Zeze.Collections.Queue::createBeanFromSpecialTypeId);
    }

    public static long getSpecialTypeIdFromBean_2(Zeze.Transaction.Bean bean) {
        return Zeze.Collections.Queue.getSpecialTypeIdFromBean(bean);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_2(long typeId) {
        return Zeze.Collections.Queue.createBeanFromSpecialTypeId(typeId);
    }

    @Override
    public long getTimestamp() {
        if (!isManaged())
            return _Timestamp;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Timestamp;
        var log = (Log__Timestamp)txn.getLog(objectId() + 1);
        return log != null ? log.value : _Timestamp;
    }

    public void setTimestamp(long value) {
        if (!isManaged()) {
            _Timestamp = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Timestamp(this, 1, value));
    }

    public Zeze.Transaction.DynamicBean getValue() {
        return _Value;
    }

    @Override
    public Zeze.Transaction.DynamicBeanReadOnly getValueReadOnly() {
        return _Value;
    }

    @SuppressWarnings("deprecation")
    public BQueueNodeValue() {
        _Value = newDynamicBean_Value();
    }

    @SuppressWarnings("deprecation")
    public BQueueNodeValue(long _Timestamp_) {
        _Timestamp = _Timestamp_;
        _Value = newDynamicBean_Value();
    }

    public void assign(BQueueNodeValue other) {
        setTimestamp(other.getTimestamp());
        _Value.assign(other._Value);
    }

    @Deprecated
    public void Assign(BQueueNodeValue other) {
        assign(other);
    }

    public BQueueNodeValue copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BQueueNodeValue copy() {
        var copy = new BQueueNodeValue();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BQueueNodeValue Copy() {
        return copy();
    }

    public static void swap(BQueueNodeValue a, BQueueNodeValue b) {
        BQueueNodeValue save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Timestamp extends Zeze.Transaction.Logs.LogLong {
        public Log__Timestamp(BQueueNodeValue bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BQueueNodeValue)getBelong())._Timestamp = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Collections.Queue.BQueueNodeValue: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Timestamp=").append(getTimestamp()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Value=").append(System.lineSeparator());
        _Value.getBean().buildString(sb, level + 4);
        sb.append(System.lineSeparator());
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append('}');
    }

    private static int _PRE_ALLOC_SIZE_ = 16;

    @Override
    public int preAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void preAllocSize(int size) {
        _PRE_ALLOC_SIZE_ = size;
    }

    @Override
    public void encode(ByteBuffer _o_) {
        int _i_ = 0;
        {
            long _x_ = getTimestamp();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = _Value;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.DYNAMIC);
                _x_.encode(_o_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setTimestamp(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _o_.ReadDynamic(_Value, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Value.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _Value.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getTimestamp() < 0)
            return true;
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void followerApply(Zeze.Transaction.Log log) {
        var vars = ((Zeze.Transaction.Collections.LogBean)log).getVariables();
        if (vars == null)
            return;
        for (var it = vars.iterator(); it.moveToNext(); ) {
            var vlog = it.value();
            switch (vlog.getVariableId()) {
                case 1: _Timestamp = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 2: _Value.followerApply(vlog); break;
            }
        }
    }
}
