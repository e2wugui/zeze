// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskModule;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BTaskDescription extends Zeze.Transaction.Bean implements BTaskDescriptionReadOnly {
    public static final long TYPEID = 1814889450078372192L;

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

    @Override
    public int getTaskState() {
        if (!isManaged())
            return _TaskState;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _TaskState;
        var log = (Log__TaskState)txn.getLog(objectId() + 2);
        return log != null ? log.value : _TaskState;
    }

    public void setTaskState(int value) {
        if (!isManaged()) {
            _TaskState = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__TaskState(this, 2, value));
    }

    @Override
    public String getPhaseDescription() {
        if (!isManaged())
            return _PhaseDescription;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _PhaseDescription;
        var log = (Log__PhaseDescription)txn.getLog(objectId() + 3);
        return log != null ? log.value : _PhaseDescription;
    }

    public void setPhaseDescription(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _PhaseDescription = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__PhaseDescription(this, 3, value));
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
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _RewardId;
        var log = (Log__RewardId)txn.getLog(objectId() + 6);
        return log != null ? log.value : _RewardId;
    }

    public void setRewardId(int value) {
        if (!isManaged()) {
            _RewardId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__RewardId(this, 6, value));
    }

    @Override
    public int getRewardType() {
        if (!isManaged())
            return _RewardType;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _RewardType;
        var log = (Log__RewardType)txn.getLog(objectId() + 7);
        return log != null ? log.value : _RewardType;
    }

    public void setRewardType(int value) {
        if (!isManaged()) {
            _RewardType = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__RewardType(this, 7, value));
    }

    @Override
    public Zeze.Net.Binary getRewardParam() {
        if (!isManaged())
            return _RewardParam;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _RewardParam;
        var log = (Log__RewardParam)txn.getLog(objectId() + 8);
        return log != null ? log.value : _RewardParam;
    }

    public void setRewardParam(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _RewardParam = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__RewardParam(this, 8, value));
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

    public void assign(BTaskDescription other) {
        setTaskId(other.getTaskId());
        setTaskState(other.getTaskState());
        setPhaseDescription(other.getPhaseDescription());
        _PhaseConditions.clear();
        _PhaseConditions.addAll(other._PhaseConditions);
        _Conditions.clear();
        _Conditions.addAll(other._Conditions);
        setRewardId(other.getRewardId());
        setRewardType(other.getRewardType());
        setRewardParam(other.getRewardParam());
        _unknown_ = other._unknown_;
    }

    public BTaskDescription copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTaskDescription copy() {
        var copy = new BTaskDescription();
        copy.assign(this);
        return copy;
    }

    public static void swap(BTaskDescription a, BTaskDescription b) {
        BTaskDescription save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__TaskId extends Zeze.Transaction.Logs.LogInt {
        public Log__TaskId(BTaskDescription bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTaskDescription)getBelong())._TaskId = value; }
    }

    private static final class Log__TaskState extends Zeze.Transaction.Logs.LogInt {
        public Log__TaskState(BTaskDescription bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTaskDescription)getBelong())._TaskState = value; }
    }

    private static final class Log__PhaseDescription extends Zeze.Transaction.Logs.LogString {
        public Log__PhaseDescription(BTaskDescription bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTaskDescription)getBelong())._PhaseDescription = value; }
    }

    private static final class Log__RewardId extends Zeze.Transaction.Logs.LogInt {
        public Log__RewardId(BTaskDescription bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTaskDescription)getBelong())._RewardId = value; }
    }

    private static final class Log__RewardType extends Zeze.Transaction.Logs.LogInt {
        public Log__RewardType(BTaskDescription bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTaskDescription)getBelong())._RewardType = value; }
    }

    private static final class Log__RewardParam extends Zeze.Transaction.Logs.LogBinary {
        public Log__RewardParam(BTaskDescription bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTaskDescription)getBelong())._RewardParam = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.TaskModule.BTaskDescription: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("TaskId=").append(getTaskId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("TaskState=").append(getTaskState()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("PhaseDescription=").append(getPhaseDescription()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("PhaseConditions=[");
        if (!_PhaseConditions.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _PhaseConditions) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(_item_).append(',').append(System.lineSeparator());
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
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(_item_).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("RewardId=").append(getRewardId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("RewardType=").append(getRewardType()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("RewardParam=").append(getRewardParam()).append(System.lineSeparator());
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
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _PhaseConditions.initRootInfo(root, this);
        _Conditions.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _PhaseConditions.initRootInfoWithRedo(root, this);
        _Conditions.initRootInfoWithRedo(root, this);
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
    public void followerApply(Zeze.Transaction.Log log) {
        var vars = ((Zeze.Transaction.Collections.LogBean)log).getVariables();
        if (vars == null)
            return;
        for (var it = vars.iterator(); it.moveToNext(); ) {
            var vlog = it.value();
            switch (vlog.getVariableId()) {
                case 1: _TaskId = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 2: _TaskState = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 3: _PhaseDescription = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 4: _PhaseConditions.followerApply(vlog); break;
                case 5: _Conditions.followerApply(vlog); break;
                case 6: _RewardId = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 7: _RewardType = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 8: _RewardParam = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setTaskId(rs.getInt(_parents_name_ + "TaskId"));
        setTaskState(rs.getInt(_parents_name_ + "TaskState"));
        setPhaseDescription(rs.getString(_parents_name_ + "PhaseDescription"));
        if (getPhaseDescription() == null)
            setPhaseDescription("");
        Zeze.Serialize.Helper.decodeJsonList(_PhaseConditions, String.class, rs.getString(_parents_name_ + "PhaseConditions"));
        Zeze.Serialize.Helper.decodeJsonList(_Conditions, String.class, rs.getString(_parents_name_ + "Conditions"));
        setRewardId(rs.getInt(_parents_name_ + "RewardId"));
        setRewardType(rs.getInt(_parents_name_ + "RewardType"));
        setRewardParam(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "RewardParam")));
        if (getRewardParam() == null)
            setRewardParam(Zeze.Net.Binary.Empty);
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendInt(_parents_name_ + "TaskId", getTaskId());
        st.appendInt(_parents_name_ + "TaskState", getTaskState());
        st.appendString(_parents_name_ + "PhaseDescription", getPhaseDescription());
        st.appendString(_parents_name_ + "PhaseConditions", Zeze.Serialize.Helper.encodeJson(_PhaseConditions));
        st.appendString(_parents_name_ + "Conditions", Zeze.Serialize.Helper.encodeJson(_Conditions));
        st.appendInt(_parents_name_ + "RewardId", getRewardId());
        st.appendInt(_parents_name_ + "RewardType", getRewardType());
        st.appendBinary(_parents_name_ + "RewardParam", getRewardParam());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "TaskId", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "TaskState", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "PhaseDescription", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "PhaseConditions", "list", "", "string"));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "Conditions", "list", "", "string"));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(6, "RewardId", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(7, "RewardType", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(8, "RewardParam", "binary", "", ""));
        return vars;
    }
}
