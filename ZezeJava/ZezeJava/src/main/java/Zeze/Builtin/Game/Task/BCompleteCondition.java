// auto-generated @formatter:off
package Zeze.Builtin.Game.Task;

import Zeze.Serialize.ByteBuffer;

// Task rpc
@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BCompleteCondition extends Zeze.Transaction.Bean implements BCompleteConditionReadOnly {
    public static final long TYPEID = 5314708440903539225L;

    private String _ConditionId;

    @Override
    public String getConditionId() {
        if (!isManaged())
            return _ConditionId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ConditionId;
        var log = (Log__ConditionId)txn.getLog(objectId() + 1);
        return log != null ? log.value : _ConditionId;
    }

    public void setConditionId(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ConditionId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ConditionId(this, 1, value));
    }

    @SuppressWarnings("deprecation")
    public BCompleteCondition() {
        _ConditionId = "";
    }

    @SuppressWarnings("deprecation")
    public BCompleteCondition(String _ConditionId_) {
        if (_ConditionId_ == null)
            throw new IllegalArgumentException();
        _ConditionId = _ConditionId_;
    }

    public void assign(BCompleteCondition other) {
        setConditionId(other.getConditionId());
    }

    @Deprecated
    public void Assign(BCompleteCondition other) {
        assign(other);
    }

    public BCompleteCondition copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BCompleteCondition copy() {
        var copy = new BCompleteCondition();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BCompleteCondition Copy() {
        return copy();
    }

    public static void swap(BCompleteCondition a, BCompleteCondition b) {
        BCompleteCondition save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ConditionId extends Zeze.Transaction.Logs.LogString {
        public Log__ConditionId(BCompleteCondition bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BCompleteCondition)getBelong())._ConditionId = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.Task.BCompleteCondition: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ConditionId").append('=').append(getConditionId()).append(System.lineSeparator());
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
            String _x_ = getConditionId();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setConditionId(_o_.ReadString(_t_));
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
                case 1: _ConditionId = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
            }
        }
    }
}
