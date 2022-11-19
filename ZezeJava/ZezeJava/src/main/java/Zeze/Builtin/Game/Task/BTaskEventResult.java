// auto-generated @formatter:off
package Zeze.Builtin.Game.Task;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BTaskEventResult extends Zeze.Transaction.Bean implements BTaskEventResultReadOnly {
    public static final long TYPEID = -4357525030735911735L;

    private boolean _success; // 是否发送成功（如果失败，可能是没有找到对应的Task等等）
    private int _acceptedCount; // 本次发送的事件被接受的次数
    private final Zeze.Transaction.Collections.PList1<String> _acceptedCondition; // 本次发送的事件被接受的Condition的Name

    @Override
    public boolean isSuccess() {
        if (!isManaged())
            return _success;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _success;
        var log = (Log__success)txn.getLog(objectId() + 1);
        return log != null ? log.value : _success;
    }

    public void setSuccess(boolean value) {
        if (!isManaged()) {
            _success = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__success(this, 1, value));
    }

    @Override
    public int getAcceptedCount() {
        if (!isManaged())
            return _acceptedCount;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _acceptedCount;
        var log = (Log__acceptedCount)txn.getLog(objectId() + 2);
        return log != null ? log.value : _acceptedCount;
    }

    public void setAcceptedCount(int value) {
        if (!isManaged()) {
            _acceptedCount = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__acceptedCount(this, 2, value));
    }

    public Zeze.Transaction.Collections.PList1<String> getAcceptedCondition() {
        return _acceptedCondition;
    }

    @Override
    public Zeze.Transaction.Collections.PList1ReadOnly<String> getAcceptedConditionReadOnly() {
        return new Zeze.Transaction.Collections.PList1ReadOnly<>(_acceptedCondition);
    }

    @SuppressWarnings("deprecation")
    public BTaskEventResult() {
        _acceptedCondition = new Zeze.Transaction.Collections.PList1<>(String.class);
        _acceptedCondition.variableId(3);
    }

    @SuppressWarnings("deprecation")
    public BTaskEventResult(boolean _success_, int _acceptedCount_) {
        _success = _success_;
        _acceptedCount = _acceptedCount_;
        _acceptedCondition = new Zeze.Transaction.Collections.PList1<>(String.class);
        _acceptedCondition.variableId(3);
    }

    public void assign(BTaskEventResult other) {
        setSuccess(other.isSuccess());
        setAcceptedCount(other.getAcceptedCount());
        _acceptedCondition.clear();
        _acceptedCondition.addAll(other._acceptedCondition);
    }

    @Deprecated
    public void Assign(BTaskEventResult other) {
        assign(other);
    }

    public BTaskEventResult copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTaskEventResult copy() {
        var copy = new BTaskEventResult();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BTaskEventResult Copy() {
        return copy();
    }

    public static void swap(BTaskEventResult a, BTaskEventResult b) {
        BTaskEventResult save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__success extends Zeze.Transaction.Logs.LogBool {
        public Log__success(BTaskEventResult bean, int varId, boolean value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTaskEventResult)getBelong())._success = value; }
    }

    private static final class Log__acceptedCount extends Zeze.Transaction.Logs.LogInt {
        public Log__acceptedCount(BTaskEventResult bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTaskEventResult)getBelong())._acceptedCount = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.Task.BTaskEventResult: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("success=").append(isSuccess()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("acceptedCount=").append(getAcceptedCount()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("acceptedCondition=[");
        if (!_acceptedCondition.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _acceptedCondition) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(_item_).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append(']').append(System.lineSeparator());
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
            boolean _x_ = isSuccess();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        {
            int _x_ = getAcceptedCount();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = _acceptedCondition;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BYTES);
                for (var _v_ : _x_)
                    _o_.WriteString(_v_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setSuccess(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setAcceptedCount(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            var _x_ = _acceptedCondition;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadString(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _acceptedCondition.initRootInfo(root, this);
    }

    @Override
    protected void resetChildrenRootInfo() {
        _acceptedCondition.resetRootInfo();
    }

    @Override
    public boolean negativeCheck() {
        if (getAcceptedCount() < 0)
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
                case 1: _success = ((Zeze.Transaction.Logs.LogBool)vlog).value; break;
                case 2: _acceptedCount = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 3: _acceptedCondition.followerApply(vlog); break;
            }
        }
    }
}
