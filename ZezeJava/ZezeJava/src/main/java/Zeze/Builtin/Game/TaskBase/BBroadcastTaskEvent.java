// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BBroadcastTaskEvent extends Zeze.Transaction.Bean implements BBroadcastTaskEventReadOnly {
    public static final long TYPEID = 2627115510834301728L;

    private boolean _isBreakIfAccepted;

    @Override
    public boolean isIsBreakIfAccepted() {
        if (!isManaged())
            return _isBreakIfAccepted;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _isBreakIfAccepted;
        var log = (Log__isBreakIfAccepted)txn.getLog(objectId() + 1);
        return log != null ? log.value : _isBreakIfAccepted;
    }

    public void setIsBreakIfAccepted(boolean value) {
        if (!isManaged()) {
            _isBreakIfAccepted = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__isBreakIfAccepted(this, 1, value));
    }

    @SuppressWarnings("deprecation")
    public BBroadcastTaskEvent() {
    }

    @SuppressWarnings("deprecation")
    public BBroadcastTaskEvent(boolean _isBreakIfAccepted_) {
        _isBreakIfAccepted = _isBreakIfAccepted_;
    }

    public void assign(BBroadcastTaskEvent other) {
        setIsBreakIfAccepted(other.isIsBreakIfAccepted());
    }

    @Deprecated
    public void Assign(BBroadcastTaskEvent other) {
        assign(other);
    }

    public BBroadcastTaskEvent copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BBroadcastTaskEvent copy() {
        var copy = new BBroadcastTaskEvent();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BBroadcastTaskEvent Copy() {
        return copy();
    }

    public static void swap(BBroadcastTaskEvent a, BBroadcastTaskEvent b) {
        BBroadcastTaskEvent save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__isBreakIfAccepted extends Zeze.Transaction.Logs.LogBool {
        public Log__isBreakIfAccepted(BBroadcastTaskEvent bean, int varId, boolean value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BBroadcastTaskEvent)getBelong())._isBreakIfAccepted = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.TaskBase.BBroadcastTaskEvent: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("isBreakIfAccepted=").append(isIsBreakIfAccepted()).append(System.lineSeparator());
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
            boolean _x_ = isIsBreakIfAccepted();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setIsBreakIfAccepted(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
    }

    @Override
    protected void resetChildrenRootInfo() {
    }

    @Override
    public boolean negativeCheck() {
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
                case 1: _isBreakIfAccepted = ((Zeze.Transaction.Logs.LogBool)vlog).value; break;
            }
        }
    }
}
