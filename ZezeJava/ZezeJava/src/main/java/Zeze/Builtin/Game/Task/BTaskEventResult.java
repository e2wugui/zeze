// auto-generated @formatter:off
package Zeze.Builtin.Game.Task;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BTaskEventResult extends Zeze.Transaction.Bean implements BTaskEventResultReadOnly {
    public static final long TYPEID = -4357525030735911735L;

    private boolean _success;

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

    @SuppressWarnings("deprecation")
    public BTaskEventResult() {
    }

    @SuppressWarnings("deprecation")
    public BTaskEventResult(boolean _success_) {
        _success = _success_;
    }

    public void assign(BTaskEventResult other) {
        setSuccess(other.isSuccess());
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
        sb.append(Zeze.Util.Str.indent(level)).append("success").append('=').append(isSuccess()).append(System.lineSeparator());
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
                case 1: _success = ((Zeze.Transaction.Logs.LogBool)vlog).value; break;
            }
        }
    }
}
