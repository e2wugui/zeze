// auto-generated @formatter:off
package Zeze.Builtin.Game.Task;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BTaskPhase extends Zeze.Transaction.Bean implements BTaskPhaseReadOnly {
    public static final long TYPEID = -2081342941887981883L;

    private String _PhaseId;

    @Override
    public String getPhaseId() {
        if (!isManaged())
            return _PhaseId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _PhaseId;
        var log = (Log__PhaseId)txn.getLog(objectId() + 1);
        return log != null ? log.value : _PhaseId;
    }

    public void setPhaseId(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _PhaseId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__PhaseId(this, 1, value));
    }

    @SuppressWarnings("deprecation")
    public BTaskPhase() {
        _PhaseId = "";
    }

    @SuppressWarnings("deprecation")
    public BTaskPhase(String _PhaseId_) {
        if (_PhaseId_ == null)
            throw new IllegalArgumentException();
        _PhaseId = _PhaseId_;
    }

    public void assign(BTaskPhase other) {
        setPhaseId(other.getPhaseId());
    }

    @Deprecated
    public void Assign(BTaskPhase other) {
        assign(other);
    }

    public BTaskPhase copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTaskPhase copy() {
        var copy = new BTaskPhase();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BTaskPhase Copy() {
        return copy();
    }

    public static void swap(BTaskPhase a, BTaskPhase b) {
        BTaskPhase save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__PhaseId extends Zeze.Transaction.Logs.LogString {
        public Log__PhaseId(BTaskPhase bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTaskPhase)getBelong())._PhaseId = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.Task.BTaskPhase: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("PhaseId").append('=').append(getPhaseId()).append(System.lineSeparator());
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
            String _x_ = getPhaseId();
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
            setPhaseId(_o_.ReadString(_t_));
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
                case 1: _PhaseId = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
            }
        }
    }
}
