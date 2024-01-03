// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskModule;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BRoleTasks extends Zeze.Transaction.Bean implements BRoleTasksReadOnly {
    public static final long TYPEID = 3175977186051967471L;

    private final Zeze.Transaction.Collections.PMap2<Integer, Zeze.Builtin.Game.TaskModule.BTask> _Tasks; // key是taskId，这种描述意味着相同的任务不能同时接多次。

    public Zeze.Transaction.Collections.PMap2<Integer, Zeze.Builtin.Game.TaskModule.BTask> getTasks() {
        return _Tasks;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<Integer, Zeze.Builtin.Game.TaskModule.BTask, Zeze.Builtin.Game.TaskModule.BTaskReadOnly> getTasksReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_Tasks);
    }

    @SuppressWarnings("deprecation")
    public BRoleTasks() {
        _Tasks = new Zeze.Transaction.Collections.PMap2<>(Integer.class, Zeze.Builtin.Game.TaskModule.BTask.class);
        _Tasks.variableId(1);
    }

    @Override
    public void reset() {
        _Tasks.clear();
        _unknown_ = null;
    }

    public void assign(BRoleTasks other) {
        _Tasks.clear();
        for (var e : other._Tasks.entrySet())
            _Tasks.put(e.getKey(), e.getValue().copy());
        _unknown_ = other._unknown_;
    }

    public BRoleTasks copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BRoleTasks copy() {
        var copy = new BRoleTasks();
        copy.assign(this);
        return copy;
    }

    public static void swap(BRoleTasks a, BRoleTasks b) {
        BRoleTasks save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.TaskModule.BRoleTasks: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Tasks={");
        if (!_Tasks.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _kv_ : _Tasks.entrySet()) {
                sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(_kv_.getKey()).append(',').append(System.lineSeparator());
                sb.append(Zeze.Util.Str.indent(level)).append("Value=").append(System.lineSeparator());
                _kv_.getValue().buildString(sb, level + 4);
                sb.append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append('}').append(System.lineSeparator());
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
            var _x_ = _Tasks;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _e_.getValue().encode(_o_);
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
    public void decode(IByteBuffer _o_) {
        ByteBuffer _u_ = null;
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            var _x_ = _Tasks;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadInt(_s_);
                    var _v_ = _o_.ReadBean(new Zeze.Builtin.Game.TaskModule.BTask(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Tasks.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _Tasks.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
        for (var _v_ : _Tasks.values()) {
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
                case 1: _Tasks.followerApply(vlog); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        Zeze.Serialize.Helper.decodeJsonMap(this, "Tasks", _Tasks, rs.getString(_parents_name_ + "Tasks"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "Tasks", Zeze.Serialize.Helper.encodeJson(_Tasks));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Tasks", "map", "int", "BTask"));
        return vars;
    }
}
