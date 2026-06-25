// auto-generated @formatter:off
package metagame.builtin.TaskModule;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BTaskConfig extends Zeze.Transaction.Bean implements BTaskConfigReadOnly {
    public static final long TYPEID = -4064201956807660645L;

    private int _TaskId;
    private final Zeze.Transaction.Collections.PSet1<Integer> _PreposeTasks; // 前置任务
    private final Zeze.Transaction.Collections.PSet1<Integer> _FollowTasks; // 后续任务
    private int _AcceptNpc; // 接受任务的NpcId
    private int _FinishNpc; // 完成任务的NpcId
    private final Zeze.Transaction.DynamicBean _ExtendData;

    public static Zeze.Transaction.DynamicBean newDynamicBean_ExtendData() {
        return new Zeze.Transaction.DynamicBean(6, metagame.Task.TaskModule::getSpecialTypeIdFromBean, metagame.Task.TaskModule::createBeanFromSpecialTypeId);
    }

    public static long getSpecialTypeIdFromBean_6(Zeze.Transaction.Bean _b_) {
        return metagame.Task.TaskModule.getSpecialTypeIdFromBean(_b_);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_6(long _t_) {
        return metagame.Task.TaskModule.createBeanFromSpecialTypeId(_t_);
    }

    private final Zeze.Transaction.Collections.CollOne<metagame.builtin.TaskModule.BTask> _TaskConditions; // 任务条件
    private int _PreposeRequired; // 需要的前置任务完成数量，0表示全部。
    private boolean _Repeatable; // 任务是否可重复完成

    private static final java.lang.invoke.VarHandle vh_TaskId;
    private static final java.lang.invoke.VarHandle vh_AcceptNpc;
    private static final java.lang.invoke.VarHandle vh_FinishNpc;
    private static final java.lang.invoke.VarHandle vh_PreposeRequired;
    private static final java.lang.invoke.VarHandle vh_Repeatable;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_TaskId = _l_.findVarHandle(BTaskConfig.class, "_TaskId", int.class);
            vh_AcceptNpc = _l_.findVarHandle(BTaskConfig.class, "_AcceptNpc", int.class);
            vh_FinishNpc = _l_.findVarHandle(BTaskConfig.class, "_FinishNpc", int.class);
            vh_PreposeRequired = _l_.findVarHandle(BTaskConfig.class, "_PreposeRequired", int.class);
            vh_Repeatable = _l_.findVarHandle(BTaskConfig.class, "_Repeatable", boolean.class);
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

    public Zeze.Transaction.Collections.PSet1<Integer> getPreposeTasks() {
        return _PreposeTasks;
    }

    @Override
    public Zeze.Transaction.Collections.PSet1ReadOnly<Integer> getPreposeTasksReadOnly() {
        return new Zeze.Transaction.Collections.PSet1ReadOnly<>(_PreposeTasks);
    }

    public Zeze.Transaction.Collections.PSet1<Integer> getFollowTasks() {
        return _FollowTasks;
    }

    @Override
    public Zeze.Transaction.Collections.PSet1ReadOnly<Integer> getFollowTasksReadOnly() {
        return new Zeze.Transaction.Collections.PSet1ReadOnly<>(_FollowTasks);
    }

    @Override
    public int getAcceptNpc() {
        if (!isManaged())
            return _AcceptNpc;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _AcceptNpc;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 4);
        return log != null ? log.value : _AcceptNpc;
    }

    public void setAcceptNpc(int _v_) {
        if (!isManaged()) {
            _AcceptNpc = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 4, vh_AcceptNpc, _v_));
    }

    @Override
    public int getFinishNpc() {
        if (!isManaged())
            return _FinishNpc;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _FinishNpc;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 5);
        return log != null ? log.value : _FinishNpc;
    }

    public void setFinishNpc(int _v_) {
        if (!isManaged()) {
            _FinishNpc = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 5, vh_FinishNpc, _v_));
    }

    public Zeze.Transaction.DynamicBean getExtendData() {
        return _ExtendData;
    }

    @Override
    public Zeze.Transaction.DynamicBeanReadOnly getExtendDataReadOnly() {
        return _ExtendData;
    }

    public metagame.builtin.TaskModule.BTask getTaskConditions() {
        return _TaskConditions.getValue();
    }

    public void setTaskConditions(metagame.builtin.TaskModule.BTask _v_) {
        _TaskConditions.setValue(_v_);
    }

    @Override
    public metagame.builtin.TaskModule.BTaskReadOnly getTaskConditionsReadOnly() {
        return _TaskConditions.getValue();
    }

    @Override
    public int getPreposeRequired() {
        if (!isManaged())
            return _PreposeRequired;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _PreposeRequired;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 8);
        return log != null ? log.value : _PreposeRequired;
    }

    public void setPreposeRequired(int _v_) {
        if (!isManaged()) {
            _PreposeRequired = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 8, vh_PreposeRequired, _v_));
    }

    @Override
    public boolean isRepeatable() {
        if (!isManaged())
            return _Repeatable;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Repeatable;
        var log = (Zeze.Transaction.Logs.LogBool)_t_.getLog(objectId() + 9);
        return log != null ? log.value : _Repeatable;
    }

    public void setRepeatable(boolean _v_) {
        if (!isManaged()) {
            _Repeatable = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBool(this, 9, vh_Repeatable, _v_));
    }

    @SuppressWarnings("deprecation")
    public BTaskConfig() {
        _PreposeTasks = new Zeze.Transaction.Collections.PSet1<>(Integer.class);
        _PreposeTasks.variableId(2);
        _FollowTasks = new Zeze.Transaction.Collections.PSet1<>(Integer.class);
        _FollowTasks.variableId(3);
        _ExtendData = newDynamicBean_ExtendData();
        _TaskConditions = new Zeze.Transaction.Collections.CollOne<>(new metagame.builtin.TaskModule.BTask(), metagame.builtin.TaskModule.BTask.class);
        _TaskConditions.variableId(7);
    }

    @SuppressWarnings("deprecation")
    public BTaskConfig(int _TaskId_, int _AcceptNpc_, int _FinishNpc_, int _PreposeRequired_, boolean _Repeatable_) {
        _TaskId = _TaskId_;
        _PreposeTasks = new Zeze.Transaction.Collections.PSet1<>(Integer.class);
        _PreposeTasks.variableId(2);
        _FollowTasks = new Zeze.Transaction.Collections.PSet1<>(Integer.class);
        _FollowTasks.variableId(3);
        _AcceptNpc = _AcceptNpc_;
        _FinishNpc = _FinishNpc_;
        _ExtendData = newDynamicBean_ExtendData();
        _TaskConditions = new Zeze.Transaction.Collections.CollOne<>(new metagame.builtin.TaskModule.BTask(), metagame.builtin.TaskModule.BTask.class);
        _TaskConditions.variableId(7);
        _PreposeRequired = _PreposeRequired_;
        _Repeatable = _Repeatable_;
    }

    @Override
    public void reset() {
        setTaskId(0);
        _PreposeTasks.clear();
        _FollowTasks.clear();
        setAcceptNpc(0);
        setFinishNpc(0);
        _ExtendData.reset();
        _TaskConditions.reset();
        setPreposeRequired(0);
        setRepeatable(false);
        _unknown_ = null;
    }

    @Override
    public metagame.builtin.TaskModule.BTaskConfig.Data toData() {
        var _d_ = new metagame.builtin.TaskModule.BTaskConfig.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((metagame.builtin.TaskModule.BTaskConfig.Data)_o_);
    }

    public void assign(BTaskConfig.Data _o_) {
        setTaskId(_o_._TaskId);
        _PreposeTasks.clear();
        _PreposeTasks.addAll(_o_._PreposeTasks);
        _FollowTasks.clear();
        _FollowTasks.addAll(_o_._FollowTasks);
        setAcceptNpc(_o_._AcceptNpc);
        setFinishNpc(_o_._FinishNpc);
        _ExtendData.assign(_o_._ExtendData);
        var _d__TaskConditions = new metagame.builtin.TaskModule.BTask();
        _d__TaskConditions.assign(_o_._TaskConditions);
        _TaskConditions.setValue(_d__TaskConditions);
        setPreposeRequired(_o_._PreposeRequired);
        setRepeatable(_o_._Repeatable);
        _unknown_ = null;
    }

    public void assign(BTaskConfig _o_) {
        setTaskId(_o_.getTaskId());
        _PreposeTasks.assign(_o_._PreposeTasks);
        _FollowTasks.assign(_o_._FollowTasks);
        setAcceptNpc(_o_.getAcceptNpc());
        setFinishNpc(_o_.getFinishNpc());
        _ExtendData.assign(_o_._ExtendData);
        _TaskConditions.assign(_o_._TaskConditions);
        setPreposeRequired(_o_.getPreposeRequired());
        setRepeatable(_o_.isRepeatable());
        _unknown_ = _o_._unknown_;
    }

    public BTaskConfig copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTaskConfig copy() {
        var _c_ = new BTaskConfig();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BTaskConfig _a_, BTaskConfig _b_) {
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
        _s_.append("metagame.builtin.TaskModule.BTaskConfig: {\n");
        _s_.append(_i1_).append("TaskId=").append(getTaskId()).append(",\n");
        _s_.append(_i1_).append("PreposeTasks={");
        if (!_PreposeTasks.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _PreposeTasks) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_PreposeTasks.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("FollowTasks={");
        if (!_FollowTasks.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _FollowTasks) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_FollowTasks.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("AcceptNpc=").append(getAcceptNpc()).append(",\n");
        _s_.append(_i1_).append("FinishNpc=").append(getFinishNpc()).append(",\n");
        _s_.append(_i1_).append("ExtendData=");
        _ExtendData.getBean().buildString(_s_, _l_ + 8);
        _s_.append(",\n");
        _s_.append(_i1_).append("TaskConditions=");
        _TaskConditions.buildString(_s_, _l_ + 8);
        _s_.append(",\n");
        _s_.append(_i1_).append("PreposeRequired=").append(getPreposeRequired()).append(",\n");
        _s_.append(_i1_).append("Repeatable=").append(isRepeatable()).append('\n');
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
            var _x_ = _PreposeTasks;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
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
            var _x_ = _FollowTasks;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.LIST);
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
            int _x_ = getAcceptNpc();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getFinishNpc();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = _ExtendData;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.DYNAMIC);
                _x_.encode(_o_);
            }
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 7, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _TaskConditions.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        {
            int _x_ = getPreposeRequired();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 8, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            boolean _x_ = isRepeatable();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 9, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
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
            var _x_ = _PreposeTasks;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadInt(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            var _x_ = _FollowTasks;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadInt(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setAcceptNpc(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setFinishNpc(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            _o_.ReadDynamic(_ExtendData, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 7) {
            _o_.ReadBean(_TaskConditions, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 8) {
            setPreposeRequired(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 9) {
            setRepeatable(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BTaskConfig))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BTaskConfig)_o_;
        if (getTaskId() != _b_.getTaskId())
            return false;
        if (!_PreposeTasks.equals(_b_._PreposeTasks))
            return false;
        if (!_FollowTasks.equals(_b_._FollowTasks))
            return false;
        if (getAcceptNpc() != _b_.getAcceptNpc())
            return false;
        if (getFinishNpc() != _b_.getFinishNpc())
            return false;
        if (!_ExtendData.equals(_b_._ExtendData))
            return false;
        if (!_TaskConditions.equals(_b_._TaskConditions))
            return false;
        if (getPreposeRequired() != _b_.getPreposeRequired())
            return false;
        if (isRepeatable() != _b_.isRepeatable())
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _PreposeTasks.initRootInfo(_r_, this);
        _FollowTasks.initRootInfo(_r_, this);
        _ExtendData.initRootInfo(_r_, this);
        _TaskConditions.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _PreposeTasks.initRootInfoWithRedo(_r_, this);
        _FollowTasks.initRootInfoWithRedo(_r_, this);
        _ExtendData.initRootInfoWithRedo(_r_, this);
        _TaskConditions.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getTaskId() < 0)
            return true;
        for (var _v_ : _PreposeTasks) {
            if (_v_ < 0)
                return true;
        }
        for (var _v_ : _FollowTasks) {
            if (_v_ < 0)
                return true;
        }
        if (getAcceptNpc() < 0)
            return true;
        if (getFinishNpc() < 0)
            return true;
        if (_TaskConditions.negativeCheck())
            return true;
        if (getPreposeRequired() < 0)
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
                case 2: _PreposeTasks.followerApply(_v_); break;
                case 3: _FollowTasks.followerApply(_v_); break;
                case 4: _AcceptNpc = _v_.intValue(); break;
                case 5: _FinishNpc = _v_.intValue(); break;
                case 6: _ExtendData.followerApply(_v_); break;
                case 7: _TaskConditions.followerApply(_v_); break;
                case 8: _PreposeRequired = _v_.intValue(); break;
                case 9: _Repeatable = _v_.booleanValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setTaskId(_r_.getInt(_pn_ + "TaskId"));
        Zeze.Serialize.Helper.decodeJsonSet(_PreposeTasks, Integer.class, _r_.getString(_pn_ + "PreposeTasks"));
        Zeze.Serialize.Helper.decodeJsonSet(_FollowTasks, Integer.class, _r_.getString(_pn_ + "FollowTasks"));
        setAcceptNpc(_r_.getInt(_pn_ + "AcceptNpc"));
        setFinishNpc(_r_.getInt(_pn_ + "FinishNpc"));
        Zeze.Serialize.Helper.decodeJsonDynamic(_ExtendData, _r_.getString(_pn_ + "ExtendData"));
        _p_.add("TaskConditions");
        _TaskConditions.decodeResultSet(_p_, _r_);
        _p_.remove(_p_.size() - 1);
        setPreposeRequired(_r_.getInt(_pn_ + "PreposeRequired"));
        setRepeatable(_r_.getBoolean(_pn_ + "Repeatable"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendInt(_pn_ + "TaskId", getTaskId());
        _s_.appendString(_pn_ + "PreposeTasks", Zeze.Serialize.Helper.encodeJson(_PreposeTasks));
        _s_.appendString(_pn_ + "FollowTasks", Zeze.Serialize.Helper.encodeJson(_FollowTasks));
        _s_.appendInt(_pn_ + "AcceptNpc", getAcceptNpc());
        _s_.appendInt(_pn_ + "FinishNpc", getFinishNpc());
        _s_.appendString(_pn_ + "ExtendData", Zeze.Serialize.Helper.encodeJson(_ExtendData));
        _p_.add("TaskConditions");
        _TaskConditions.encodeSQLStatement(_p_, _s_);
        _p_.remove(_p_.size() - 1);
        _s_.appendInt(_pn_ + "PreposeRequired", getPreposeRequired());
        _s_.appendBoolean(_pn_ + "Repeatable", isRepeatable());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "TaskId", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "PreposeTasks", "set", "", "int"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "FollowTasks", "set", "", "int"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "AcceptNpc", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "FinishNpc", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(6, "ExtendData", "dynamic", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(7, "TaskConditions", "metagame.builtin.TaskModule.BTask", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(8, "PreposeRequired", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(9, "Repeatable", "bool", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -4064201956807660645L;

    private int _TaskId;
    private java.util.HashSet<Integer> _PreposeTasks; // 前置任务
    private java.util.HashSet<Integer> _FollowTasks; // 后续任务
    private int _AcceptNpc; // 接受任务的NpcId
    private int _FinishNpc; // 完成任务的NpcId
    private final DynamicData_ExtendData _ExtendData;

    public static final class DynamicData_ExtendData extends Zeze.Transaction.DynamicData {
        static {
            registerJsonParser(DynamicData_ExtendData.class);
        }

        @Override
        public long toTypeId(Zeze.Transaction.Data _d_) {
            return metagame.Task.TaskModule.getSpecialTypeIdFromBean(_d_);
        }

        @Override
        public Zeze.Transaction.Data toData(long _t_) {
            return metagame.Task.TaskModule.createDataFromSpecialTypeId(_t_);
        }

        @Override
        public DynamicData_ExtendData copy() {
            return (DynamicData_ExtendData)super.copy();
        }
    }

    private metagame.builtin.TaskModule.BTask.Data _TaskConditions; // 任务条件
    private int _PreposeRequired; // 需要的前置任务完成数量，0表示全部。
    private boolean _Repeatable; // 任务是否可重复完成

    public int getTaskId() {
        return _TaskId;
    }

    public void setTaskId(int _v_) {
        _TaskId = _v_;
    }

    public java.util.HashSet<Integer> getPreposeTasks() {
        return _PreposeTasks;
    }

    public void setPreposeTasks(java.util.HashSet<Integer> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _PreposeTasks = _v_;
    }

    public java.util.HashSet<Integer> getFollowTasks() {
        return _FollowTasks;
    }

    public void setFollowTasks(java.util.HashSet<Integer> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _FollowTasks = _v_;
    }

    public int getAcceptNpc() {
        return _AcceptNpc;
    }

    public void setAcceptNpc(int _v_) {
        _AcceptNpc = _v_;
    }

    public int getFinishNpc() {
        return _FinishNpc;
    }

    public void setFinishNpc(int _v_) {
        _FinishNpc = _v_;
    }

    public DynamicData_ExtendData getExtendData() {
        return _ExtendData;
    }

    public metagame.builtin.TaskModule.BTask.Data getTaskConditions() {
        return _TaskConditions;
    }

    public void setTaskConditions(metagame.builtin.TaskModule.BTask.Data _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _TaskConditions = _v_;
    }

    public int getPreposeRequired() {
        return _PreposeRequired;
    }

    public void setPreposeRequired(int _v_) {
        _PreposeRequired = _v_;
    }

    public boolean isRepeatable() {
        return _Repeatable;
    }

    public void setRepeatable(boolean _v_) {
        _Repeatable = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _PreposeTasks = new java.util.HashSet<>();
        _FollowTasks = new java.util.HashSet<>();
        _ExtendData = new DynamicData_ExtendData();
        _TaskConditions = new metagame.builtin.TaskModule.BTask.Data();
    }

    @SuppressWarnings("deprecation")
    public Data(int _TaskId_, java.util.HashSet<Integer> _PreposeTasks_, java.util.HashSet<Integer> _FollowTasks_, int _AcceptNpc_, int _FinishNpc_, DynamicData_ExtendData _ExtendData_, metagame.builtin.TaskModule.BTask.Data _TaskConditions_, int _PreposeRequired_, boolean _Repeatable_) {
        _TaskId = _TaskId_;
        if (_PreposeTasks_ == null)
            _PreposeTasks_ = new java.util.HashSet<>();
        _PreposeTasks = _PreposeTasks_;
        if (_FollowTasks_ == null)
            _FollowTasks_ = new java.util.HashSet<>();
        _FollowTasks = _FollowTasks_;
        _AcceptNpc = _AcceptNpc_;
        _FinishNpc = _FinishNpc_;
        if (_ExtendData_ == null)
            _ExtendData_ = new DynamicData_ExtendData();
        _ExtendData = _ExtendData_;
        if (_TaskConditions_ == null)
            _TaskConditions_ = new metagame.builtin.TaskModule.BTask.Data();
        _TaskConditions = _TaskConditions_;
        _PreposeRequired = _PreposeRequired_;
        _Repeatable = _Repeatable_;
    }

    @Override
    public void reset() {
        _TaskId = 0;
        _PreposeTasks.clear();
        _FollowTasks.clear();
        _AcceptNpc = 0;
        _FinishNpc = 0;
        _ExtendData.reset();
        _TaskConditions.reset();
        _PreposeRequired = 0;
        _Repeatable = false;
    }

    @Override
    public metagame.builtin.TaskModule.BTaskConfig toBean() {
        var _b_ = new metagame.builtin.TaskModule.BTaskConfig();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BTaskConfig)_o_);
    }

    public void assign(BTaskConfig _o_) {
        _TaskId = _o_.getTaskId();
        _PreposeTasks.clear();
        _PreposeTasks.addAll(_o_._PreposeTasks);
        _FollowTasks.clear();
        _FollowTasks.addAll(_o_._FollowTasks);
        _AcceptNpc = _o_.getAcceptNpc();
        _FinishNpc = _o_.getFinishNpc();
        _ExtendData.assign(_o_._ExtendData);
        _TaskConditions.assign(_o_._TaskConditions.getValue());
        _PreposeRequired = _o_.getPreposeRequired();
        _Repeatable = _o_.isRepeatable();
    }

    public void assign(BTaskConfig.Data _o_) {
        _TaskId = _o_._TaskId;
        _PreposeTasks.clear();
        _PreposeTasks.addAll(_o_._PreposeTasks);
        _FollowTasks.clear();
        _FollowTasks.addAll(_o_._FollowTasks);
        _AcceptNpc = _o_._AcceptNpc;
        _FinishNpc = _o_._FinishNpc;
        _ExtendData.assign(_o_._ExtendData);
        _TaskConditions.assign(_o_._TaskConditions);
        _PreposeRequired = _o_._PreposeRequired;
        _Repeatable = _o_._Repeatable;
    }

    @Override
    public BTaskConfig.Data copy() {
        var _c_ = new BTaskConfig.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BTaskConfig.Data _a_, BTaskConfig.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BTaskConfig.Data clone() {
        return (BTaskConfig.Data)super.clone();
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
        _s_.append("metagame.builtin.TaskModule.BTaskConfig: {\n");
        _s_.append(_i1_).append("TaskId=").append(_TaskId).append(",\n");
        _s_.append(_i1_).append("PreposeTasks={");
        if (!_PreposeTasks.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _PreposeTasks) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_PreposeTasks.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("FollowTasks={");
        if (!_FollowTasks.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _FollowTasks) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_FollowTasks.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("AcceptNpc=").append(_AcceptNpc).append(",\n");
        _s_.append(_i1_).append("FinishNpc=").append(_FinishNpc).append(",\n");
        _s_.append(_i1_).append("ExtendData=");
        _ExtendData.getData().buildString(_s_, _l_ + 8);
        _s_.append(",\n");
        _s_.append(_i1_).append("TaskConditions=");
        _TaskConditions.buildString(_s_, _l_ + 8);
        _s_.append(",\n");
        _s_.append(_i1_).append("PreposeRequired=").append(_PreposeRequired).append(",\n");
        _s_.append(_i1_).append("Repeatable=").append(_Repeatable).append('\n');
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
            var _x_ = _PreposeTasks;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
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
            var _x_ = _FollowTasks;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.LIST);
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
            int _x_ = _AcceptNpc;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = _FinishNpc;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = _ExtendData;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.DYNAMIC);
                _x_.encode(_o_);
            }
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 7, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _TaskConditions.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        {
            int _x_ = _PreposeRequired;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 8, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            boolean _x_ = _Repeatable;
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 9, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
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
            var _x_ = _PreposeTasks;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadInt(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            var _x_ = _FollowTasks;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadInt(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _AcceptNpc = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            _FinishNpc = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            _o_.ReadDynamic(_ExtendData, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 7) {
            _o_.ReadBean(_TaskConditions, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 8) {
            _PreposeRequired = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 9) {
            _Repeatable = _o_.ReadBool(_t_);
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
        if (!(_o_ instanceof BTaskConfig.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BTaskConfig.Data)_o_;
        if (_TaskId != _b_._TaskId)
            return false;
        if (!_PreposeTasks.equals(_b_._PreposeTasks))
            return false;
        if (!_FollowTasks.equals(_b_._FollowTasks))
            return false;
        if (_AcceptNpc != _b_._AcceptNpc)
            return false;
        if (_FinishNpc != _b_._FinishNpc)
            return false;
        if (!_ExtendData.equals(_b_._ExtendData))
            return false;
        if (!_TaskConditions.equals(_b_._TaskConditions))
            return false;
        if (_PreposeRequired != _b_._PreposeRequired)
            return false;
        if (_Repeatable != _b_._Repeatable)
            return false;
        return true;
    }
}
}
