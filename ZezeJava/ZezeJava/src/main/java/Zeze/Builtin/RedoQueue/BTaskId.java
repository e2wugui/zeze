// auto-generated @formatter:off
package Zeze.Builtin.RedoQueue;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BTaskId extends Zeze.Transaction.Bean {
    private long _TaskId;

    public long getTaskId() {
        if (!isManaged())
            return _TaskId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _TaskId;
        var log = (Log__TaskId)txn.GetLog(objectId() + 1);
        return log != null ? log.Value : _TaskId;
    }

    public void setTaskId(long value) {
        if (!isManaged()) {
            _TaskId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.PutLog(new Log__TaskId(this, 1, value));
    }

    @SuppressWarnings("deprecation")
    public BTaskId() {
    }

    @SuppressWarnings("deprecation")
    public BTaskId(long _TaskId_) {
        _TaskId = _TaskId_;
    }

    public void assign(BTaskId other) {
        setTaskId(other.getTaskId());
    }

    @Deprecated
    public void Assign(BTaskId other) {
        assign(other);
    }

    public BTaskId copyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BTaskId copy() {
        var copy = new BTaskId();
        copy.Assign(this);
        return copy;
    }

    @Deprecated
    public BTaskId Copy() {
        return copy();
    }

    public static void swap(BTaskId a, BTaskId b) {
        BTaskId save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public BTaskId copyBean() {
        return Copy();
    }

    public static final long TYPEID = -3646825359403112989L;

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__TaskId extends Zeze.Transaction.Logs.LogLong {
        public Log__TaskId(BTaskId bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTaskId)getBelong())._TaskId = Value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.RedoQueue.BTaskId: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("TaskId").append('=').append(getTaskId()).append(System.lineSeparator());
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
            long _x_ = getTaskId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setTaskId(_o_.ReadLong(_t_));
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
        if (getTaskId() < 0)
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
                case 1: _TaskId = ((Zeze.Transaction.Logs.LogLong)vlog).Value; break;
            }
        }
    }
}
