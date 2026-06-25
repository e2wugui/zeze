// auto-generated @formatter:off
package metagame.builtin.TaskModule;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BTask extends Zeze.Transaction.Bean implements BTaskReadOnly {
    public static final long TYPEID = -8612100103636319395L;

    private int _TaskId;
    private final Zeze.Transaction.Collections.PList2<metagame.builtin.TaskModule.BPhase> _Phases;
    private final Zeze.Transaction.Collections.PList2<metagame.builtin.TaskModule.BCondition> _Conditions;
    private final Zeze.Transaction.Collections.PSet1<Integer> _IndexSet;
    private int _TaskState;
    private boolean _AutoFinish;
    private int _RewardId;

    private transient Object __zeze_map_key__;

    @Override
    public Object mapKey() {
        return __zeze_map_key__;
    }

    @Override
    public void mapKey(Object _v_) {
        __zeze_map_key__ = _v_;
    }

    private static final java.lang.invoke.VarHandle vh_TaskId;
    private static final java.lang.invoke.VarHandle vh_TaskState;
    private static final java.lang.invoke.VarHandle vh_AutoFinish;
    private static final java.lang.invoke.VarHandle vh_RewardId;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_TaskId = _l_.findVarHandle(BTask.class, "_TaskId", int.class);
            vh_TaskState = _l_.findVarHandle(BTask.class, "_TaskState", int.class);
            vh_AutoFinish = _l_.findVarHandle(BTask.class, "_AutoFinish", boolean.class);
            vh_RewardId = _l_.findVarHandle(BTask.class, "_RewardId", int.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public int getTaskId() {
        if (!isManaged())
            return _TaskId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _TaskId;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _TaskId;
    }

    public void setTaskId(int _v_) {
        if (!isManaged()) {
            _TaskId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 1, vh_TaskId, _v_));
    }

    public Zeze.Transaction.Collections.PList2<metagame.builtin.TaskModule.BPhase> getPhases() {
        return _Phases;
    }

    @Override
    public Zeze.Transaction.Collections.PList2ReadOnly<metagame.builtin.TaskModule.BPhase, metagame.builtin.TaskModule.BPhaseReadOnly> getPhasesReadOnly() {
        return new Zeze.Transaction.Collections.PList2ReadOnly<>(_Phases);
    }

    public Zeze.Transaction.Collections.PList2<metagame.builtin.TaskModule.BCondition> getConditions() {
        return _Conditions;
    }

    @Override
    public Zeze.Transaction.Collections.PList2ReadOnly<metagame.builtin.TaskModule.BCondition, metagame.builtin.TaskModule.BConditionReadOnly> getConditionsReadOnly() {
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
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _TaskState;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 5);
        return log != null ? log.value : _TaskState;
    }

    public void setTaskState(int _v_) {
        if (!isManaged()) {
            _TaskState = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 5, vh_TaskState, _v_));
    }

    @Override
    public boolean isAutoFinish() {
        if (!isManaged())
            return _AutoFinish;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _AutoFinish;
        var log = (Zeze.Transaction.Logs.LogBool)_t_.getLog(objectId() + 6);
        return log != null ? log.value : _AutoFinish;
    }

    public void setAutoFinish(boolean _v_) {
        if (!isManaged()) {
            _AutoFinish = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBool(this, 6, vh_AutoFinish, _v_));
    }

    @Override
    public int getRewardId() {
        if (!isManaged())
            return _RewardId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _RewardId;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 7);
        return log != null ? log.value : _RewardId;
    }

    public void setRewardId(int _v_) {
        if (!isManaged()) {
            _RewardId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 7, vh_RewardId, _v_));
    }

    @SuppressWarnings("deprecation")
    public BTask() {
        _Phases = new Zeze.Transaction.Collections.PList2<>(metagame.builtin.TaskModule.BPhase.class);
        _Phases.variableId(2);
        _Conditions = new Zeze.Transaction.Collections.PList2<>(metagame.builtin.TaskModule.BCondition.class);
        _Conditions.variableId(3);
        _IndexSet = new Zeze.Transaction.Collections.PSet1<>(Integer.class);
        _IndexSet.variableId(4);
    }

    @SuppressWarnings("deprecation")
    public BTask(int _TaskId_, int _TaskState_, boolean _AutoFinish_, int _RewardId_) {
        _TaskId = _TaskId_;
        _Phases = new Zeze.Transaction.Collections.PList2<>(metagame.builtin.TaskModule.BPhase.class);
        _Phases.variableId(2);
        _Conditions = new Zeze.Transaction.Collections.PList2<>(metagame.builtin.TaskModule.BCondition.class);
        _Conditions.variableId(3);
        _IndexSet = new Zeze.Transaction.Collections.PSet1<>(Integer.class);
        _IndexSet.variableId(4);
        _TaskState = _TaskState_;
        _AutoFinish = _AutoFinish_;
        _RewardId = _RewardId_;
    }

    @Override
    public void reset() {
        setTaskId(0);
        _Phases.clear();
        _Conditions.clear();
        _IndexSet.clear();
        setTaskState(0);
        setAutoFinish(false);
        setRewardId(0);
        _unknown_ = null;
    }

    @Override
    public metagame.builtin.TaskModule.BTask.Data toData() {
        var _d_ = new metagame.builtin.TaskModule.BTask.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((metagame.builtin.TaskModule.BTask.Data)_o_);
    }

    public void assign(BTask.Data _o_) {
        setTaskId(_o_._TaskId);
        _Phases.clear();
        for (var _e_ : _o_._Phases) {
            var _v_ = new metagame.builtin.TaskModule.BPhase();
            _v_.assign(_e_);
            _Phases.add(_v_);
        }
        _Conditions.clear();
        for (var _e_ : _o_._Conditions) {
            var _v_ = new metagame.builtin.TaskModule.BCondition();
            _v_.assign(_e_);
            _Conditions.add(_v_);
        }
        _IndexSet.clear();
        _IndexSet.addAll(_o_._IndexSet);
        setTaskState(_o_._TaskState);
        setAutoFinish(_o_._AutoFinish);
        setRewardId(_o_._RewardId);
        _unknown_ = null;
    }

    public void assign(BTask _o_) {
        setTaskId(_o_.getTaskId());
        _Phases.clear();
        for (var _e_ : _o_._Phases)
            _Phases.add(_e_.copy());
        _Conditions.clear();
        for (var _e_ : _o_._Conditions)
            _Conditions.add(_e_.copy());
        _IndexSet.assign(_o_._IndexSet);
        setTaskState(_o_.getTaskState());
        setAutoFinish(_o_.isAutoFinish());
        setRewardId(_o_.getRewardId());
        _unknown_ = _o_._unknown_;
    }

    public BTask copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTask copy() {
        var _c_ = new BTask();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BTask _a_, BTask _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        var _i1_ = Zeze.Util.Str.indent(_l_ + 4);
        var _i2_ = Zeze.Util.Str.indent(_l_ + 8);
        _s_.append("metagame.builtin.TaskModule.BTask: {\n");
        _s_.append(_i1_).append("TaskId=").append(getTaskId()).append(",\n");
        _s_.append(_i1_).append("Phases=[");
        if (!_Phases.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _Phases) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_Phases.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=");
                _v_.buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("],\n");
        _s_.append(_i1_).append("Conditions=[");
        if (!_Conditions.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _Conditions) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_Conditions.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=");
                _v_.buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("],\n");
        _s_.append(_i1_).append("IndexSet={");
        if (!_IndexSet.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _IndexSet) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_IndexSet.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("TaskState=").append(getTaskState()).append(",\n");
        _s_.append(_i1_).append("AutoFinish=").append(isAutoFinish()).append(",\n");
        _s_.append(_i1_).append("RewardId=").append(getRewardId()).append('\n');
        _s_.append(Zeze.Util.Str.indent(_l_)).append('}');
    }

    private static int _PRE_ALLOC_SIZE_ = 16;

    @Override
    public int preAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void preAllocSize(int _s_) {
        _PRE_ALLOC_SIZE_ = _s_;
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
            boolean _x_ = isAutoFinish();
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
                    _x_.add(_o_.ReadBean(new metagame.builtin.TaskModule.BPhase(), _t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            var _x_ = _Conditions;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new metagame.builtin.TaskModule.BCondition(), _t_));
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
            setAutoFinish(_o_.ReadBool(_t_));
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
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BTask))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BTask)_o_;
        if (getTaskId() != _b_.getTaskId())
            return false;
        if (!_Phases.equals(_b_._Phases))
            return false;
        if (!_Conditions.equals(_b_._Conditions))
            return false;
        if (!_IndexSet.equals(_b_._IndexSet))
            return false;
        if (getTaskState() != _b_.getTaskState())
            return false;
        if (isAutoFinish() != _b_.isAutoFinish())
            return false;
        if (getRewardId() != _b_.getRewardId())
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _Phases.initRootInfo(_r_, this);
        _Conditions.initRootInfo(_r_, this);
        _IndexSet.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _Phases.initRootInfoWithRedo(_r_, this);
        _Conditions.initRootInfoWithRedo(_r_, this);
        _IndexSet.initRootInfoWithRedo(_r_, this);
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
    public void followerApply(Zeze.Transaction.Log _l_) {
        var _vs_ = ((Zeze.Transaction.Collections.LogBean)_l_).getVariables();
        if (_vs_ == null)
            return;
        for (var _i_ = _vs_.iterator(); _i_.moveToNext(); ) {
            var _v_ = _i_.value();
            switch (_v_.getVariableId()) {
                case 1: _TaskId = _v_.intValue(); break;
                case 2: _Phases.followerApply(_v_); break;
                case 3: _Conditions.followerApply(_v_); break;
                case 4: _IndexSet.followerApply(_v_); break;
                case 5: _TaskState = _v_.intValue(); break;
                case 6: _AutoFinish = _v_.booleanValue(); break;
                case 7: _RewardId = _v_.intValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setTaskId(_r_.getInt(_pn_ + "TaskId"));
        Zeze.Serialize.Helper.decodeJsonList(_Phases, metagame.builtin.TaskModule.BPhase.class, _r_.getString(_pn_ + "Phases"));
        Zeze.Serialize.Helper.decodeJsonList(_Conditions, metagame.builtin.TaskModule.BCondition.class, _r_.getString(_pn_ + "Conditions"));
        Zeze.Serialize.Helper.decodeJsonSet(_IndexSet, Integer.class, _r_.getString(_pn_ + "IndexSet"));
        setTaskState(_r_.getInt(_pn_ + "TaskState"));
        setAutoFinish(_r_.getBoolean(_pn_ + "AutoFinish"));
        setRewardId(_r_.getInt(_pn_ + "RewardId"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendInt(_pn_ + "TaskId", getTaskId());
        _s_.appendString(_pn_ + "Phases", Zeze.Serialize.Helper.encodeJson(_Phases));
        _s_.appendString(_pn_ + "Conditions", Zeze.Serialize.Helper.encodeJson(_Conditions));
        _s_.appendString(_pn_ + "IndexSet", Zeze.Serialize.Helper.encodeJson(_IndexSet));
        _s_.appendInt(_pn_ + "TaskState", getTaskState());
        _s_.appendBoolean(_pn_ + "AutoFinish", isAutoFinish());
        _s_.appendInt(_pn_ + "RewardId", getRewardId());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "TaskId", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Phases", "list", "", "metagame.builtin.TaskModule.BPhase"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Conditions", "list", "", "metagame.builtin.TaskModule.BCondition"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "IndexSet", "set", "", "int"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "TaskState", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(6, "AutoFinish", "bool", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(7, "RewardId", "int", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -8612100103636319395L;

    private int _TaskId;
    private java.util.ArrayList<metagame.builtin.TaskModule.BPhase.Data> _Phases;
    private java.util.ArrayList<metagame.builtin.TaskModule.BCondition.Data> _Conditions;
    private java.util.HashSet<Integer> _IndexSet;
    private int _TaskState;
    private boolean _AutoFinish;
    private int _RewardId;

    public int getTaskId() {
        return _TaskId;
    }

    public void setTaskId(int _v_) {
        _TaskId = _v_;
    }

    public java.util.ArrayList<metagame.builtin.TaskModule.BPhase.Data> getPhases() {
        return _Phases;
    }

    public void setPhases(java.util.ArrayList<metagame.builtin.TaskModule.BPhase.Data> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Phases = _v_;
    }

    public java.util.ArrayList<metagame.builtin.TaskModule.BCondition.Data> getConditions() {
        return _Conditions;
    }

    public void setConditions(java.util.ArrayList<metagame.builtin.TaskModule.BCondition.Data> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Conditions = _v_;
    }

    public java.util.HashSet<Integer> getIndexSet() {
        return _IndexSet;
    }

    public void setIndexSet(java.util.HashSet<Integer> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _IndexSet = _v_;
    }

    public int getTaskState() {
        return _TaskState;
    }

    public void setTaskState(int _v_) {
        _TaskState = _v_;
    }

    public boolean isAutoFinish() {
        return _AutoFinish;
    }

    public void setAutoFinish(boolean _v_) {
        _AutoFinish = _v_;
    }

    public int getRewardId() {
        return _RewardId;
    }

    public void setRewardId(int _v_) {
        _RewardId = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Phases = new java.util.ArrayList<>();
        _Conditions = new java.util.ArrayList<>();
        _IndexSet = new java.util.HashSet<>();
    }

    @SuppressWarnings("deprecation")
    public Data(int _TaskId_, java.util.ArrayList<metagame.builtin.TaskModule.BPhase.Data> _Phases_, java.util.ArrayList<metagame.builtin.TaskModule.BCondition.Data> _Conditions_, java.util.HashSet<Integer> _IndexSet_, int _TaskState_, boolean _AutoFinish_, int _RewardId_) {
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
        _AutoFinish = _AutoFinish_;
        _RewardId = _RewardId_;
    }

    @Override
    public void reset() {
        _TaskId = 0;
        _Phases.clear();
        _Conditions.clear();
        _IndexSet.clear();
        _TaskState = 0;
        _AutoFinish = false;
        _RewardId = 0;
    }

    @Override
    public metagame.builtin.TaskModule.BTask toBean() {
        var _b_ = new metagame.builtin.TaskModule.BTask();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BTask)_o_);
    }

    public void assign(BTask _o_) {
        _TaskId = _o_.getTaskId();
        _Phases.clear();
        for (var _e_ : _o_._Phases) {
            var _v_ = new metagame.builtin.TaskModule.BPhase.Data();
            _v_.assign(_e_);
            _Phases.add(_v_);
        }
        _Conditions.clear();
        for (var _e_ : _o_._Conditions) {
            var _v_ = new metagame.builtin.TaskModule.BCondition.Data();
            _v_.assign(_e_);
            _Conditions.add(_v_);
        }
        _IndexSet.clear();
        _IndexSet.addAll(_o_._IndexSet);
        _TaskState = _o_.getTaskState();
        _AutoFinish = _o_.isAutoFinish();
        _RewardId = _o_.getRewardId();
    }

    public void assign(BTask.Data _o_) {
        _TaskId = _o_._TaskId;
        _Phases.clear();
        for (var _e_ : _o_._Phases)
            _Phases.add(_e_.copy());
        _Conditions.clear();
        for (var _e_ : _o_._Conditions)
            _Conditions.add(_e_.copy());
        _IndexSet.clear();
        _IndexSet.addAll(_o_._IndexSet);
        _TaskState = _o_._TaskState;
        _AutoFinish = _o_._AutoFinish;
        _RewardId = _o_._RewardId;
    }

    @Override
    public BTask.Data copy() {
        var _c_ = new BTask.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BTask.Data _a_, BTask.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
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
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        var _i1_ = Zeze.Util.Str.indent(_l_ + 4);
        var _i2_ = Zeze.Util.Str.indent(_l_ + 8);
        _s_.append("metagame.builtin.TaskModule.BTask: {\n");
        _s_.append(_i1_).append("TaskId=").append(_TaskId).append(",\n");
        _s_.append(_i1_).append("Phases=[");
        if (!_Phases.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _Phases) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_Phases.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=");
                _v_.buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("],\n");
        _s_.append(_i1_).append("Conditions=[");
        if (!_Conditions.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _Conditions) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_Conditions.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=");
                _v_.buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("],\n");
        _s_.append(_i1_).append("IndexSet={");
        if (!_IndexSet.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _IndexSet) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_IndexSet.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("TaskState=").append(_TaskState).append(",\n");
        _s_.append(_i1_).append("AutoFinish=").append(_AutoFinish).append(",\n");
        _s_.append(_i1_).append("RewardId=").append(_RewardId).append('\n');
        _s_.append(Zeze.Util.Str.indent(_l_)).append('}');
    }

    @Override
    public int preAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void preAllocSize(int _s_) {
        _PRE_ALLOC_SIZE_ = _s_;
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
            boolean _x_ = _AutoFinish;
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
                    _x_.add(_o_.ReadBean(new metagame.builtin.TaskModule.BPhase.Data(), _t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            var _x_ = _Conditions;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new metagame.builtin.TaskModule.BCondition.Data(), _t_));
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
            _AutoFinish = _o_.ReadBool(_t_);
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

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BTask.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BTask.Data)_o_;
        if (_TaskId != _b_._TaskId)
            return false;
        if (!_Phases.equals(_b_._Phases))
            return false;
        if (!_Conditions.equals(_b_._Conditions))
            return false;
        if (!_IndexSet.equals(_b_._IndexSet))
            return false;
        if (_TaskState != _b_._TaskState)
            return false;
        if (_AutoFinish != _b_._AutoFinish)
            return false;
        if (_RewardId != _b_._RewardId)
            return false;
        return true;
    }
}
}
