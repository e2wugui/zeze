// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskModule;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BTask extends Zeze.Transaction.Bean implements BTaskReadOnly {
    public static final long TYPEID = 4718982452353518468L;

    private int _TaskId;
    private final Zeze.Transaction.Collections.PList2<Zeze.Builtin.Game.TaskModule.BPhase> _Phases;
    private final Zeze.Transaction.Collections.PList2<Zeze.Builtin.Game.TaskModule.BCondition> _Conditions;
    private final Zeze.Transaction.Collections.PSet1<Integer> _IndexSet;
    private int _TaskState;
    private boolean _AutoCompleted;
    private int _RewardId;

    private transient Object __zeze_map_key__;

    @Override
    public Object mapKey() {
        return __zeze_map_key__;
    }

    @Override
    public void mapKey(Object value) {
        __zeze_map_key__ = value;
    }

    @Override
    public int getTaskId() {
        if (!isManaged())
            return _TaskId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _TaskId;
        var log = (Log__TaskId)txn.getLog(objectId() + 1);
        return log != null ? log.value : _TaskId;
    }

    public void setTaskId(int value) {
        if (!isManaged()) {
            _TaskId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__TaskId(this, 1, value));
    }

    public Zeze.Transaction.Collections.PList2<Zeze.Builtin.Game.TaskModule.BPhase> getPhases() {
        return _Phases;
    }

    @Override
    public Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.Game.TaskModule.BPhase, Zeze.Builtin.Game.TaskModule.BPhaseReadOnly> getPhasesReadOnly() {
        return new Zeze.Transaction.Collections.PList2ReadOnly<>(_Phases);
    }

    public Zeze.Transaction.Collections.PList2<Zeze.Builtin.Game.TaskModule.BCondition> getConditions() {
        return _Conditions;
    }

    @Override
    public Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.Game.TaskModule.BCondition, Zeze.Builtin.Game.TaskModule.BConditionReadOnly> getConditionsReadOnly() {
        return new Zeze.Transaction.Collections.PList2ReadOnly<>(_Conditions);
    }

    public Zeze.Transaction.Collections.PSet1<Integer> getIndexSet() {
        return _IndexSet;
    }

    @Override
    public Zeze.Transaction.Collections.PSet1ReadOnly<Integer> getIndexSetReadOnly() {
        return new Zeze.Transaction.Collections.PSet1ReadOnly<>(_IndexSet);
    }

    @Override
    public int getTaskState() {
        if (!isManaged())
            return _TaskState;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _TaskState;
        var log = (Log__TaskState)txn.getLog(objectId() + 5);
        return log != null ? log.value : _TaskState;
    }

    public void setTaskState(int value) {
        if (!isManaged()) {
            _TaskState = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__TaskState(this, 5, value));
    }

    @Override
    public boolean isAutoCompleted() {
        if (!isManaged())
            return _AutoCompleted;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _AutoCompleted;
        var log = (Log__AutoCompleted)txn.getLog(objectId() + 6);
        return log != null ? log.value : _AutoCompleted;
    }

    public void setAutoCompleted(boolean value) {
        if (!isManaged()) {
            _AutoCompleted = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__AutoCompleted(this, 6, value));
    }

    @Override
    public int getRewardId() {
        if (!isManaged())
            return _RewardId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _RewardId;
        var log = (Log__RewardId)txn.getLog(objectId() + 7);
        return log != null ? log.value : _RewardId;
    }

    public void setRewardId(int value) {
        if (!isManaged()) {
            _RewardId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__RewardId(this, 7, value));
    }

    @SuppressWarnings("deprecation")
    public BTask() {
        _Phases = new Zeze.Transaction.Collections.PList2<>(Zeze.Builtin.Game.TaskModule.BPhase.class);
        _Phases.variableId(2);
        _Conditions = new Zeze.Transaction.Collections.PList2<>(Zeze.Builtin.Game.TaskModule.BCondition.class);
        _Conditions.variableId(3);
        _IndexSet = new Zeze.Transaction.Collections.PSet1<>(Integer.class);
        _IndexSet.variableId(4);
    }

    @SuppressWarnings("deprecation")
    public BTask(int _TaskId_, int _TaskState_, boolean _AutoCompleted_, int _RewardId_) {
        _TaskId = _TaskId_;
        _Phases = new Zeze.Transaction.Collections.PList2<>(Zeze.Builtin.Game.TaskModule.BPhase.class);
        _Phases.variableId(2);
        _Conditions = new Zeze.Transaction.Collections.PList2<>(Zeze.Builtin.Game.TaskModule.BCondition.class);
        _Conditions.variableId(3);
        _IndexSet = new Zeze.Transaction.Collections.PSet1<>(Integer.class);
        _IndexSet.variableId(4);
        _TaskState = _TaskState_;
        _AutoCompleted = _AutoCompleted_;
        _RewardId = _RewardId_;
    }

    @Override
    public void reset() {
        setTaskId(0);
        _Phases.clear();
        _Conditions.clear();
        _IndexSet.clear();
        setTaskState(0);
        setAutoCompleted(false);
        setRewardId(0);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Game.TaskModule.BTask.Data toData() {
        var data = new Zeze.Builtin.Game.TaskModule.BTask.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Game.TaskModule.BTask.Data)other);
    }

    public void assign(BTask.Data other) {
        setTaskId(other._TaskId);
        _Phases.clear();
        for (var e : other._Phases) {
            Zeze.Builtin.Game.TaskModule.BPhase data = new Zeze.Builtin.Game.TaskModule.BPhase();
            data.assign(e);
            _Phases.add(data);
        }
        _Conditions.clear();
        for (var e : other._Conditions) {
            Zeze.Builtin.Game.TaskModule.BCondition data = new Zeze.Builtin.Game.TaskModule.BCondition();
            data.assign(e);
            _Conditions.add(data);
        }
        _IndexSet.clear();
        _IndexSet.addAll(other._IndexSet);
        setTaskState(other._TaskState);
        setAutoCompleted(other._AutoCompleted);
        setRewardId(other._RewardId);
        _unknown_ = null;
    }

    public void assign(BTask other) {
        setTaskId(other.getTaskId());
        _Phases.clear();
        for (var e : other._Phases)
            _Phases.add(e.copy());
        _Conditions.clear();
        for (var e : other._Conditions)
            _Conditions.add(e.copy());
        _IndexSet.clear();
        _IndexSet.addAll(other._IndexSet);
        setTaskState(other.getTaskState());
        setAutoCompleted(other.isAutoCompleted());
        setRewardId(other.getRewardId());
        _unknown_ = other._unknown_;
    }

    public BTask copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTask copy() {
        var copy = new BTask();
        copy.assign(this);
        return copy;
    }

    public static void swap(BTask a, BTask b) {
        BTask save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__TaskId extends Zeze.Transaction.Logs.LogInt {
        public Log__TaskId(BTask bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTask)getBelong())._TaskId = value; }
    }

    private static final class Log__TaskState extends Zeze.Transaction.Logs.LogInt {
        public Log__TaskState(BTask bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTask)getBelong())._TaskState = value; }
    }

    private static final class Log__AutoCompleted extends Zeze.Transaction.Logs.LogBool {
        public Log__AutoCompleted(BTask bean, int varId, boolean value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTask)getBelong())._AutoCompleted = value; }
    }

    private static final class Log__RewardId extends Zeze.Transaction.Logs.LogInt {
        public Log__RewardId(BTask bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTask)getBelong())._RewardId = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.TaskModule.BTask: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("TaskId=").append(getTaskId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Phases=[");
        if (!_Phases.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _Phases) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(System.lineSeparator());
                _item_.buildString(sb, level + 4);
                sb.append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Conditions=[");
        if (!_Conditions.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _Conditions) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(System.lineSeparator());
                _item_.buildString(sb, level + 4);
                sb.append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("IndexSet={");
        if (!_IndexSet.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _IndexSet) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(_item_).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append('}').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("TaskState=").append(getTaskState()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("AutoCompleted=").append(isAutoCompleted()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("RewardId=").append(getRewardId()).append(System.lineSeparator());
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
            int _x_ = getTaskId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = _Phases;
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
        {
            var _x_ = _Conditions;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BEAN);
                for (var _v_ : _x_) {
                    _v_.encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _IndexSet;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                for (var _v_ : _x_) {
                    _o_.WriteLong(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            int _x_ = getTaskState();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            boolean _x_ = isAutoCompleted();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        {
            int _x_ = getRewardId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 7, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
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
            setTaskId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = _Phases;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new Zeze.Builtin.Game.TaskModule.BPhase(), _t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            var _x_ = _Conditions;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new Zeze.Builtin.Game.TaskModule.BCondition(), _t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            var _x_ = _IndexSet;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadInt(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setTaskState(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            setAutoCompleted(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 7) {
            setRewardId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Phases.initRootInfo(root, this);
        _Conditions.initRootInfo(root, this);
        _IndexSet.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _Phases.initRootInfoWithRedo(root, this);
        _Conditions.initRootInfoWithRedo(root, this);
        _IndexSet.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getTaskId() < 0)
            return true;
        for (var _v_ : _Phases) {
            if (_v_.negativeCheck())
                return true;
        }
        for (var _v_ : _IndexSet) {
            if (_v_ < 0)
                return true;
        }
        if (getTaskState() < 0)
            return true;
        if (getRewardId() < 0)
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
                case 1: _TaskId = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 2: _Phases.followerApply(vlog); break;
                case 3: _Conditions.followerApply(vlog); break;
                case 4: _IndexSet.followerApply(vlog); break;
                case 5: _TaskState = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 6: _AutoCompleted = ((Zeze.Transaction.Logs.LogBool)vlog).value; break;
                case 7: _RewardId = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setTaskId(rs.getInt(_parents_name_ + "TaskId"));
        Zeze.Serialize.Helper.decodeJsonList(_Phases, Zeze.Builtin.Game.TaskModule.BPhase.class, rs.getString(_parents_name_ + "Phases"));
        Zeze.Serialize.Helper.decodeJsonList(_Conditions, Zeze.Builtin.Game.TaskModule.BCondition.class, rs.getString(_parents_name_ + "Conditions"));
        Zeze.Serialize.Helper.decodeJsonSet(_IndexSet, Integer.class, rs.getString(_parents_name_ + "IndexSet"));
        setTaskState(rs.getInt(_parents_name_ + "TaskState"));
        setAutoCompleted(rs.getBoolean(_parents_name_ + "AutoCompleted"));
        setRewardId(rs.getInt(_parents_name_ + "RewardId"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendInt(_parents_name_ + "TaskId", getTaskId());
        st.appendString(_parents_name_ + "Phases", Zeze.Serialize.Helper.encodeJson(_Phases));
        st.appendString(_parents_name_ + "Conditions", Zeze.Serialize.Helper.encodeJson(_Conditions));
        st.appendString(_parents_name_ + "IndexSet", Zeze.Serialize.Helper.encodeJson(_IndexSet));
        st.appendInt(_parents_name_ + "TaskState", getTaskState());
        st.appendBoolean(_parents_name_ + "AutoCompleted", isAutoCompleted());
        st.appendInt(_parents_name_ + "RewardId", getRewardId());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "TaskId", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Phases", "list", "", "BPhase"));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Conditions", "list", "", "BCondition"));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "IndexSet", "set", "", "int"));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "TaskState", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(6, "AutoCompleted", "bool", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(7, "RewardId", "int", "", ""));
        return vars;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 4718982452353518468L;

    private int _TaskId;
    private java.util.ArrayList<Zeze.Builtin.Game.TaskModule.BPhase.Data> _Phases;
    private java.util.ArrayList<Zeze.Builtin.Game.TaskModule.BCondition.Data> _Conditions;
    private java.util.HashSet<Integer> _IndexSet;
    private int _TaskState;
    private boolean _AutoCompleted;
    private int _RewardId;

    public int getTaskId() {
        return _TaskId;
    }

    public void setTaskId(int value) {
        _TaskId = value;
    }

    public java.util.ArrayList<Zeze.Builtin.Game.TaskModule.BPhase.Data> getPhases() {
        return _Phases;
    }

    public void setPhases(java.util.ArrayList<Zeze.Builtin.Game.TaskModule.BPhase.Data> value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Phases = value;
    }

    public java.util.ArrayList<Zeze.Builtin.Game.TaskModule.BCondition.Data> getConditions() {
        return _Conditions;
    }

    public void setConditions(java.util.ArrayList<Zeze.Builtin.Game.TaskModule.BCondition.Data> value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Conditions = value;
    }

    public java.util.HashSet<Integer> getIndexSet() {
        return _IndexSet;
    }

    public void setIndexSet(java.util.HashSet<Integer> value) {
        if (value == null)
            throw new IllegalArgumentException();
        _IndexSet = value;
    }

    public int getTaskState() {
        return _TaskState;
    }

    public void setTaskState(int value) {
        _TaskState = value;
    }

    public boolean isAutoCompleted() {
        return _AutoCompleted;
    }

    public void setAutoCompleted(boolean value) {
        _AutoCompleted = value;
    }

    public int getRewardId() {
        return _RewardId;
    }

    public void setRewardId(int value) {
        _RewardId = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Phases = new java.util.ArrayList<>();
        _Conditions = new java.util.ArrayList<>();
        _IndexSet = new java.util.HashSet<>();
    }

    @SuppressWarnings("deprecation")
    public Data(int _TaskId_, java.util.ArrayList<Zeze.Builtin.Game.TaskModule.BPhase.Data> _Phases_, java.util.ArrayList<Zeze.Builtin.Game.TaskModule.BCondition.Data> _Conditions_, java.util.HashSet<Integer> _IndexSet_, int _TaskState_, boolean _AutoCompleted_, int _RewardId_) {
        _TaskId = _TaskId_;
        if (_Phases_ == null)
            _Phases_ = new java.util.ArrayList<>();
        _Phases = _Phases_;
        if (_Conditions_ == null)
            _Conditions_ = new java.util.ArrayList<>();
        _Conditions = _Conditions_;
        if (_IndexSet_ == null)
            _IndexSet_ = new java.util.HashSet<>();
        _IndexSet = _IndexSet_;
        _TaskState = _TaskState_;
        _AutoCompleted = _AutoCompleted_;
        _RewardId = _RewardId_;
    }

    @Override
    public void reset() {
        _TaskId = 0;
        _Phases.clear();
        _Conditions.clear();
        _IndexSet.clear();
        _TaskState = 0;
        _AutoCompleted = false;
        _RewardId = 0;
    }

    @Override
    public Zeze.Builtin.Game.TaskModule.BTask toBean() {
        var bean = new Zeze.Builtin.Game.TaskModule.BTask();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BTask)other);
    }

    public void assign(BTask other) {
        _TaskId = other.getTaskId();
        _Phases.clear();
        for (var e : other._Phases) {
            Zeze.Builtin.Game.TaskModule.BPhase.Data data = new Zeze.Builtin.Game.TaskModule.BPhase.Data();
            data.assign(e);
            _Phases.add(data);
        }
        _Conditions.clear();
        for (var e : other._Conditions) {
            Zeze.Builtin.Game.TaskModule.BCondition.Data data = new Zeze.Builtin.Game.TaskModule.BCondition.Data();
            data.assign(e);
            _Conditions.add(data);
        }
        _IndexSet.clear();
        _IndexSet.addAll(other._IndexSet);
        _TaskState = other.getTaskState();
        _AutoCompleted = other.isAutoCompleted();
        _RewardId = other.getRewardId();
    }

    public void assign(BTask.Data other) {
        _TaskId = other._TaskId;
        _Phases.clear();
        for (var e : other._Phases)
            _Phases.add(e.copy());
        _Conditions.clear();
        for (var e : other._Conditions)
            _Conditions.add(e.copy());
        _IndexSet.clear();
        _IndexSet.addAll(other._IndexSet);
        _TaskState = other._TaskState;
        _AutoCompleted = other._AutoCompleted;
        _RewardId = other._RewardId;
    }

    @Override
    public BTask.Data copy() {
        var copy = new BTask.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BTask.Data a, BTask.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BTask.Data clone() {
        return (BTask.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.TaskModule.BTask: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("TaskId=").append(_TaskId).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Phases=[");
        if (!_Phases.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _Phases) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(System.lineSeparator());
                _item_.buildString(sb, level + 4);
                sb.append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Conditions=[");
        if (!_Conditions.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _Conditions) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(System.lineSeparator());
                _item_.buildString(sb, level + 4);
                sb.append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("IndexSet={");
        if (!_IndexSet.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _IndexSet) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(_item_).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append('}').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("TaskState=").append(_TaskState).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("AutoCompleted=").append(_AutoCompleted).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("RewardId=").append(_RewardId).append(System.lineSeparator());
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append('}');
    }

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
            int _x_ = _TaskId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = _Phases;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BEAN);
                for (int _j_ = 0, _c_ = _x_.size(); _j_ < _c_; _j_++) {
                    var _v_ = _x_.get(_j_);
                    _v_.encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _Conditions;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BEAN);
                for (int _j_ = 0, _c_ = _x_.size(); _j_ < _c_; _j_++) {
                    var _v_ = _x_.get(_j_);
                    _v_.encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _IndexSet;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                for (var _v_ : _x_) {
                    _o_.WriteLong(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            int _x_ = _TaskState;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            boolean _x_ = _AutoCompleted;
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        {
            int _x_ = _RewardId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 7, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _TaskId = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = _Phases;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new Zeze.Builtin.Game.TaskModule.BPhase.Data(), _t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            var _x_ = _Conditions;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new Zeze.Builtin.Game.TaskModule.BCondition.Data(), _t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            var _x_ = _IndexSet;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadInt(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            _TaskState = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            _AutoCompleted = _o_.ReadBool(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 7) {
            _RewardId = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
