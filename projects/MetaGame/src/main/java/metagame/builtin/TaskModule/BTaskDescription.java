// auto-generated @formatter:off
package metagame.builtin.TaskModule;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BTaskDescription extends Zeze.Transaction.Bean implements BTaskDescriptionReadOnly {
    public static final long TYPEID = -7573750133683986473L;

    private int _TaskId;
    private int _TaskState;
    private String _PhaseDescription;
    private final Zeze.Transaction.Collections.PList1<String> _PhaseConditions;
    private final Zeze.Transaction.Collections.PList1<String> _Conditions;
    private int _RewardId; // 调试目的，客户端未用。
    private int _RewardType;
    private Zeze.Net.Binary _RewardParam;

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
    private static final java.lang.invoke.VarHandle vh_PhaseDescription;
    private static final java.lang.invoke.VarHandle vh_RewardId;
    private static final java.lang.invoke.VarHandle vh_RewardType;
    private static final java.lang.invoke.VarHandle vh_RewardParam;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_TaskId = _l_.findVarHandle(BTaskDescription.class, "_TaskId", int.class);
            vh_TaskState = _l_.findVarHandle(BTaskDescription.class, "_TaskState", int.class);
            vh_PhaseDescription = _l_.findVarHandle(BTaskDescription.class, "_PhaseDescription", String.class);
            vh_RewardId = _l_.findVarHandle(BTaskDescription.class, "_RewardId", int.class);
            vh_RewardType = _l_.findVarHandle(BTaskDescription.class, "_RewardType", int.class);
            vh_RewardParam = _l_.findVarHandle(BTaskDescription.class, "_RewardParam", Zeze.Net.Binary.class);
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

    @Override
    public int getTaskState() {
        if (!isManaged())
            return _TaskState;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _TaskState;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _TaskState;
    }

    public void setTaskState(int _v_) {
        if (!isManaged()) {
            _TaskState = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 2, vh_TaskState, _v_));
    }

    @Override
    public String getPhaseDescription() {
        if (!isManaged())
            return _PhaseDescription;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _PhaseDescription;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 3);
        return log != null ? log.stringValue() : _PhaseDescription;
    }

    public void setPhaseDescription(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _PhaseDescription = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 3, vh_PhaseDescription, _v_));
    }

    public Zeze.Transaction.Collections.PList1<String> getPhaseConditions() {
        return _PhaseConditions;
    }

    @Override
    public Zeze.Transaction.Collections.PList1ReadOnly<String> getPhaseConditionsReadOnly() {
        return new Zeze.Transaction.Collections.PList1ReadOnly<>(_PhaseConditions);
    }

    public Zeze.Transaction.Collections.PList1<String> getConditions() {
        return _Conditions;
    }

    @Override
    public Zeze.Transaction.Collections.PList1ReadOnly<String> getConditionsReadOnly() {
        return new Zeze.Transaction.Collections.PList1ReadOnly<>(_Conditions);
    }

    @Override
    public int getRewardId() {
        if (!isManaged())
            return _RewardId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _RewardId;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 6);
        return log != null ? log.value : _RewardId;
    }

    public void setRewardId(int _v_) {
        if (!isManaged()) {
            _RewardId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 6, vh_RewardId, _v_));
    }

    @Override
    public int getRewardType() {
        if (!isManaged())
            return _RewardType;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _RewardType;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 7);
        return log != null ? log.value : _RewardType;
    }

    public void setRewardType(int _v_) {
        if (!isManaged()) {
            _RewardType = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 7, vh_RewardType, _v_));
    }

    @Override
    public Zeze.Net.Binary getRewardParam() {
        if (!isManaged())
            return _RewardParam;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _RewardParam;
        var log = (Zeze.Transaction.Logs.LogBinary)_t_.getLog(objectId() + 8);
        return log != null ? log.value : _RewardParam;
    }

    public void setRewardParam(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _RewardParam = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBinary(this, 8, vh_RewardParam, _v_));
    }

    @SuppressWarnings("deprecation")
    public BTaskDescription() {
        _PhaseDescription = "";
        _PhaseConditions = new Zeze.Transaction.Collections.PList1<>(String.class);
        _PhaseConditions.variableId(4);
        _Conditions = new Zeze.Transaction.Collections.PList1<>(String.class);
        _Conditions.variableId(5);
        _RewardParam = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BTaskDescription(int _TaskId_, int _TaskState_, String _PhaseDescription_, int _RewardId_, int _RewardType_, Zeze.Net.Binary _RewardParam_) {
        _TaskId = _TaskId_;
        _TaskState = _TaskState_;
        if (_PhaseDescription_ == null)
            _PhaseDescription_ = "";
        _PhaseDescription = _PhaseDescription_;
        _PhaseConditions = new Zeze.Transaction.Collections.PList1<>(String.class);
        _PhaseConditions.variableId(4);
        _Conditions = new Zeze.Transaction.Collections.PList1<>(String.class);
        _Conditions.variableId(5);
        _RewardId = _RewardId_;
        _RewardType = _RewardType_;
        if (_RewardParam_ == null)
            _RewardParam_ = Zeze.Net.Binary.Empty;
        _RewardParam = _RewardParam_;
    }

    @Override
    public void reset() {
        setTaskId(0);
        setTaskState(0);
        setPhaseDescription("");
        _PhaseConditions.clear();
        _Conditions.clear();
        setRewardId(0);
        setRewardType(0);
        setRewardParam(Zeze.Net.Binary.Empty);
        _unknown_ = null;
    }

    public void assign(BTaskDescription _o_) {
        setTaskId(_o_.getTaskId());
        setTaskState(_o_.getTaskState());
        setPhaseDescription(_o_.getPhaseDescription());
        _PhaseConditions.assign(_o_._PhaseConditions);
        _Conditions.assign(_o_._Conditions);
        setRewardId(_o_.getRewardId());
        setRewardType(_o_.getRewardType());
        setRewardParam(_o_.getRewardParam());
        _unknown_ = _o_._unknown_;
    }

    public BTaskDescription copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTaskDescription copy() {
        var _c_ = new BTaskDescription();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BTaskDescription _a_, BTaskDescription _b_) {
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
        _s_.append("metagame.builtin.TaskModule.BTaskDescription: {\n");
        _s_.append(_i1_).append("TaskId=").append(getTaskId()).append(",\n");
        _s_.append(_i1_).append("TaskState=").append(getTaskState()).append(",\n");
        _s_.append(_i1_).append("PhaseDescription=").append(getPhaseDescription()).append(",\n");
        _s_.append(_i1_).append("PhaseConditions=[");
        if (!_PhaseConditions.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _PhaseConditions) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_PhaseConditions.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
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
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("],\n");
        _s_.append(_i1_).append("RewardId=").append(getRewardId()).append(",\n");
        _s_.append(_i1_).append("RewardType=").append(getRewardType()).append(",\n");
        _s_.append(_i1_).append("RewardParam=").append(getRewardParam()).append('\n');
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
            int _x_ = getTaskState();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            String _x_ = getPhaseDescription();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = _PhaseConditions;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BYTES);
                for (var _v_ : _x_) {
                    _o_.WriteString(_v_);
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
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BYTES);
                for (var _v_ : _x_) {
                    _o_.WriteString(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            int _x_ = getRewardId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getRewardType();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 7, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = getRewardParam();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 8, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
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
            setTaskState(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setPhaseDescription(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            var _x_ = _PhaseConditions;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadString(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            var _x_ = _Conditions;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadString(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            setRewardId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 7) {
            setRewardType(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 8) {
            setRewardParam(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BTaskDescription))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BTaskDescription)_o_;
        if (getTaskId() != _b_.getTaskId())
            return false;
        if (getTaskState() != _b_.getTaskState())
            return false;
        if (!getPhaseDescription().equals(_b_.getPhaseDescription()))
            return false;
        if (!_PhaseConditions.equals(_b_._PhaseConditions))
            return false;
        if (!_Conditions.equals(_b_._Conditions))
            return false;
        if (getRewardId() != _b_.getRewardId())
            return false;
        if (getRewardType() != _b_.getRewardType())
            return false;
        if (!getRewardParam().equals(_b_.getRewardParam()))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _PhaseConditions.initRootInfo(_r_, this);
        _Conditions.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _PhaseConditions.initRootInfoWithRedo(_r_, this);
        _Conditions.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getTaskId() < 0)
            return true;
        if (getTaskState() < 0)
            return true;
        if (getRewardId() < 0)
            return true;
        if (getRewardType() < 0)
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
                case 2: _TaskState = _v_.intValue(); break;
                case 3: _PhaseDescription = _v_.stringValue(); break;
                case 4: _PhaseConditions.followerApply(_v_); break;
                case 5: _Conditions.followerApply(_v_); break;
                case 6: _RewardId = _v_.intValue(); break;
                case 7: _RewardType = _v_.intValue(); break;
                case 8: _RewardParam = _v_.binaryValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setTaskId(_r_.getInt(_pn_ + "TaskId"));
        setTaskState(_r_.getInt(_pn_ + "TaskState"));
        setPhaseDescription(_r_.getString(_pn_ + "PhaseDescription"));
        if (getPhaseDescription() == null)
            setPhaseDescription("");
        Zeze.Serialize.Helper.decodeJsonList(_PhaseConditions, String.class, _r_.getString(_pn_ + "PhaseConditions"));
        Zeze.Serialize.Helper.decodeJsonList(_Conditions, String.class, _r_.getString(_pn_ + "Conditions"));
        setRewardId(_r_.getInt(_pn_ + "RewardId"));
        setRewardType(_r_.getInt(_pn_ + "RewardType"));
        setRewardParam(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "RewardParam")));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendInt(_pn_ + "TaskId", getTaskId());
        _s_.appendInt(_pn_ + "TaskState", getTaskState());
        _s_.appendString(_pn_ + "PhaseDescription", getPhaseDescription());
        _s_.appendString(_pn_ + "PhaseConditions", Zeze.Serialize.Helper.encodeJson(_PhaseConditions));
        _s_.appendString(_pn_ + "Conditions", Zeze.Serialize.Helper.encodeJson(_Conditions));
        _s_.appendInt(_pn_ + "RewardId", getRewardId());
        _s_.appendInt(_pn_ + "RewardType", getRewardType());
        _s_.appendBinary(_pn_ + "RewardParam", getRewardParam());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "TaskId", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "TaskState", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "PhaseDescription", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "PhaseConditions", "list", "", "string"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "Conditions", "list", "", "string"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(6, "RewardId", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(7, "RewardType", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(8, "RewardParam", "binary", "", ""));
        return _v_;
    }
}
