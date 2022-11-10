// auto-generated @formatter:off
package Zeze.Builtin.Game.Task;

import Zeze.Serialize.ByteBuffer;

// 内置条件类型
@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BNamedCountCondition extends Zeze.Transaction.Bean implements BNamedCountConditionReadOnly {
    public static final long TYPEID = -6296950124860399786L;

    private long _CurrentCount; // 玩家吃到的金币数量
    private long _TargetCount; // 任务需要的金币数量

    @Override
    public long getCurrentCount() {
        if (!isManaged())
            return _CurrentCount;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _CurrentCount;
        var log = (Log__CurrentCount)txn.getLog(objectId() + 1);
        return log != null ? log.value : _CurrentCount;
    }

    public void setCurrentCount(long value) {
        if (!isManaged()) {
            _CurrentCount = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__CurrentCount(this, 1, value));
    }

    @Override
    public long getTargetCount() {
        if (!isManaged())
            return _TargetCount;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _TargetCount;
        var log = (Log__TargetCount)txn.getLog(objectId() + 2);
        return log != null ? log.value : _TargetCount;
    }

    public void setTargetCount(long value) {
        if (!isManaged()) {
            _TargetCount = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__TargetCount(this, 2, value));
    }

    @SuppressWarnings("deprecation")
    public BNamedCountCondition() {
    }

    @SuppressWarnings("deprecation")
    public BNamedCountCondition(long _CurrentCount_, long _TargetCount_) {
        _CurrentCount = _CurrentCount_;
        _TargetCount = _TargetCount_;
    }

    public void assign(BNamedCountCondition other) {
        setCurrentCount(other.getCurrentCount());
        setTargetCount(other.getTargetCount());
    }

    @Deprecated
    public void Assign(BNamedCountCondition other) {
        assign(other);
    }

    public BNamedCountCondition copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BNamedCountCondition copy() {
        var copy = new BNamedCountCondition();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BNamedCountCondition Copy() {
        return copy();
    }

    public static void swap(BNamedCountCondition a, BNamedCountCondition b) {
        BNamedCountCondition save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__CurrentCount extends Zeze.Transaction.Logs.LogLong {
        public Log__CurrentCount(BNamedCountCondition bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BNamedCountCondition)getBelong())._CurrentCount = value; }
    }

    private static final class Log__TargetCount extends Zeze.Transaction.Logs.LogLong {
        public Log__TargetCount(BNamedCountCondition bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BNamedCountCondition)getBelong())._TargetCount = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.Task.BNamedCountCondition: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("CurrentCount").append('=').append(getCurrentCount()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("TargetCount").append('=').append(getTargetCount()).append(System.lineSeparator());
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
            long _x_ = getCurrentCount();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getTargetCount();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
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
            setCurrentCount(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setTargetCount(_o_.ReadLong(_t_));
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
        if (getCurrentCount() < 0)
            return true;
        if (getTargetCount() < 0)
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
                case 1: _CurrentCount = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 2: _TargetCount = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
            }
        }
    }
}
