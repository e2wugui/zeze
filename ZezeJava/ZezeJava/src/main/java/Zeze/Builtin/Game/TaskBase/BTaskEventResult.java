// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BTaskEventResult extends Zeze.Transaction.Bean implements BTaskEventResultReadOnly {
    public static final long TYPEID = 7412512539470816714L;

    private long _resultCode; // 返回码
    private final Zeze.Transaction.Collections.PList2<Zeze.Builtin.Game.TaskBase.BTask> _changedTasks; // 变化的任务

    @Override
    public long getResultCode() {
        if (!isManaged())
            return _resultCode;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _resultCode;
        var log = (Log__resultCode)txn.getLog(objectId() + 1);
        return log != null ? log.value : _resultCode;
    }

    public void setResultCode(long value) {
        if (!isManaged()) {
            _resultCode = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__resultCode(this, 1, value));
    }

    public Zeze.Transaction.Collections.PList2<Zeze.Builtin.Game.TaskBase.BTask> getChangedTasks() {
        return _changedTasks;
    }

    @Override
    public Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.Game.TaskBase.BTask, Zeze.Builtin.Game.TaskBase.BTaskReadOnly> getChangedTasksReadOnly() {
        return new Zeze.Transaction.Collections.PList2ReadOnly<>(_changedTasks);
    }

    @SuppressWarnings("deprecation")
    public BTaskEventResult() {
        _changedTasks = new Zeze.Transaction.Collections.PList2<>(Zeze.Builtin.Game.TaskBase.BTask.class);
        _changedTasks.variableId(2);
    }

    @SuppressWarnings("deprecation")
    public BTaskEventResult(long _resultCode_) {
        _resultCode = _resultCode_;
        _changedTasks = new Zeze.Transaction.Collections.PList2<>(Zeze.Builtin.Game.TaskBase.BTask.class);
        _changedTasks.variableId(2);
    }

    @Override
    public void reset() {
        setResultCode(0);
        _changedTasks.clear();
        _unknown_ = null;
    }

    public void assign(BTaskEventResult other) {
        setResultCode(other.getResultCode());
        _changedTasks.clear();
        for (var e : other._changedTasks)
            _changedTasks.add(e.copy());
        _unknown_ = other._unknown_;
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

    public static void swap(BTaskEventResult a, BTaskEventResult b) {
        BTaskEventResult save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__resultCode extends Zeze.Transaction.Logs.LogLong {
        public Log__resultCode(BTaskEventResult bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTaskEventResult)getBelong())._resultCode = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.TaskBase.BTaskEventResult: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("resultCode=").append(getResultCode()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("changedTasks=[");
        if (!_changedTasks.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _changedTasks) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(System.lineSeparator());
                _item_.buildString(sb, level + 4);
                sb.append(',').append(System.lineSeparator());
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

    private byte[] _unknown_;

    public byte[] unknown() {
        return _unknown_;
    }

    public void clearUnknown() {
        _unknown_ = null;
    }

    @Override
    public void encode(ByteBuffer _o_) {
        ByteBuffer _u_ = null;
        var _ua_ = _unknown_;
        var _ui_ = _ua_ != null ? (_u_ = ByteBuffer.Wrap(_ua_)).readUnknownIndex() : Long.MAX_VALUE;
        int _i_ = 0;
        {
            long _x_ = getResultCode();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = _changedTasks;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BEAN);
                for (var _v_ : _x_) {
                    _v_.encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        _o_.writeAllUnknownFields(_i_, _ui_, _u_);
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        ByteBuffer _u_ = null;
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setResultCode(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = _changedTasks;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new Zeze.Builtin.Game.TaskBase.BTask(), _t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _changedTasks.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _changedTasks.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getResultCode() < 0)
            return true;
        for (var _v_ : _changedTasks) {
            if (_v_.negativeCheck())
                return true;
        }
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
                case 1: _resultCode = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 2: _changedTasks.followerApply(vlog); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setResultCode(rs.getLong(_parents_name_ + "resultCode"));
        Zeze.Serialize.Helper.decodeJsonList(_changedTasks, Zeze.Builtin.Game.TaskBase.BTask.class, rs.getString(_parents_name_ + "changedTasks"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendLong(_parents_name_ + "resultCode", getResultCode());
        st.appendString(_parents_name_ + "changedTasks", Zeze.Serialize.Helper.encodeJson(_changedTasks));
    }

    @Override
    public java.util.List<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "resultCode", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "changedTasks", "list", "", "BTask"));
        return vars;
    }

    @Override
    public Zeze.Transaction.Bean toPrevious() {
        return null; // todo BTaskEventResult
    }
}
