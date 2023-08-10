// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BTask extends Zeze.Transaction.Bean implements BTaskReadOnly {
    public static final long TYPEID = -4882366066662057969L;

    private long _roleId; // 角色的Id
    private long _taskId; // 任务的Id
    private String _taskType; // 任务的类型：每日任务、隐藏任务等
    private int _taskState; // 任务的状态：可接取、已接取、已完成等
    private String _taskName; // 任务的名字
    private String _taskDescription; // 任务的描述。
    private final Zeze.Transaction.Collections.PList1<Long> _preTaskIds; // 前置任务Id（只需要存储前置任务就可以建立起整个TaskGraphics，不需要存储后置任务，这样也方便扩展）
    private long _currentPhaseId; // 当前的PhaseId
    private final Zeze.Transaction.Collections.PMap2<Long, Zeze.Builtin.Game.TaskBase.BTaskPhase> _taskPhases; // 该任务所有的Phase
    private final Zeze.Transaction.DynamicBean _extendedData;

    public static Zeze.Transaction.DynamicBean newDynamicBean_ExtendedData() {
        return new Zeze.Transaction.DynamicBean(10, Zeze.Game.TaskBase::getSpecialTypeIdFromBean, Zeze.Game.TaskBase::createBeanFromSpecialTypeId);
    }

    public static long getSpecialTypeIdFromBean_10(Zeze.Transaction.Bean bean) {
        return Zeze.Game.TaskBase.getSpecialTypeIdFromBean(bean);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_10(long typeId) {
        return Zeze.Game.TaskBase.createBeanFromSpecialTypeId(typeId);
    }

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
    public long getRoleId() {
        if (!isManaged())
            return _roleId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _roleId;
        var log = (Log__roleId)txn.getLog(objectId() + 1);
        return log != null ? log.value : _roleId;
    }

    public void setRoleId(long value) {
        if (!isManaged()) {
            _roleId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__roleId(this, 1, value));
    }

    @Override
    public long getTaskId() {
        if (!isManaged())
            return _taskId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _taskId;
        var log = (Log__taskId)txn.getLog(objectId() + 2);
        return log != null ? log.value : _taskId;
    }

    public void setTaskId(long value) {
        if (!isManaged()) {
            _taskId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__taskId(this, 2, value));
    }

    @Override
    public String getTaskType() {
        if (!isManaged())
            return _taskType;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _taskType;
        var log = (Log__taskType)txn.getLog(objectId() + 3);
        return log != null ? log.value : _taskType;
    }

    public void setTaskType(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _taskType = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__taskType(this, 3, value));
    }

    @Override
    public int getTaskState() {
        if (!isManaged())
            return _taskState;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _taskState;
        var log = (Log__taskState)txn.getLog(objectId() + 4);
        return log != null ? log.value : _taskState;
    }

    public void setTaskState(int value) {
        if (!isManaged()) {
            _taskState = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__taskState(this, 4, value));
    }

    @Override
    public String getTaskName() {
        if (!isManaged())
            return _taskName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _taskName;
        var log = (Log__taskName)txn.getLog(objectId() + 5);
        return log != null ? log.value : _taskName;
    }

    public void setTaskName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _taskName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__taskName(this, 5, value));
    }

    @Override
    public String getTaskDescription() {
        if (!isManaged())
            return _taskDescription;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _taskDescription;
        var log = (Log__taskDescription)txn.getLog(objectId() + 6);
        return log != null ? log.value : _taskDescription;
    }

    public void setTaskDescription(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _taskDescription = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__taskDescription(this, 6, value));
    }

    public Zeze.Transaction.Collections.PList1<Long> getPreTaskIds() {
        return _preTaskIds;
    }

    @Override
    public Zeze.Transaction.Collections.PList1ReadOnly<Long> getPreTaskIdsReadOnly() {
        return new Zeze.Transaction.Collections.PList1ReadOnly<>(_preTaskIds);
    }

    @Override
    public long getCurrentPhaseId() {
        if (!isManaged())
            return _currentPhaseId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _currentPhaseId;
        var log = (Log__currentPhaseId)txn.getLog(objectId() + 8);
        return log != null ? log.value : _currentPhaseId;
    }

    public void setCurrentPhaseId(long value) {
        if (!isManaged()) {
            _currentPhaseId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__currentPhaseId(this, 8, value));
    }

    public Zeze.Transaction.Collections.PMap2<Long, Zeze.Builtin.Game.TaskBase.BTaskPhase> getTaskPhases() {
        return _taskPhases;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<Long, Zeze.Builtin.Game.TaskBase.BTaskPhase, Zeze.Builtin.Game.TaskBase.BTaskPhaseReadOnly> getTaskPhasesReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_taskPhases);
    }

    public Zeze.Transaction.DynamicBean getExtendedData() {
        return _extendedData;
    }

    @Override
    public Zeze.Transaction.DynamicBeanReadOnly getExtendedDataReadOnly() {
        return _extendedData;
    }

    @SuppressWarnings("deprecation")
    public BTask() {
        _taskType = "";
        _taskName = "";
        _taskDescription = "";
        _preTaskIds = new Zeze.Transaction.Collections.PList1<>(Long.class);
        _preTaskIds.variableId(7);
        _taskPhases = new Zeze.Transaction.Collections.PMap2<>(Long.class, Zeze.Builtin.Game.TaskBase.BTaskPhase.class);
        _taskPhases.variableId(9);
        _extendedData = newDynamicBean_ExtendedData();
    }

    @SuppressWarnings("deprecation")
    public BTask(long _roleId_, long _taskId_, String _taskType_, int _taskState_, String _taskName_, String _taskDescription_, long _currentPhaseId_) {
        _roleId = _roleId_;
        _taskId = _taskId_;
        if (_taskType_ == null)
            _taskType_ = "";
        _taskType = _taskType_;
        _taskState = _taskState_;
        if (_taskName_ == null)
            _taskName_ = "";
        _taskName = _taskName_;
        if (_taskDescription_ == null)
            _taskDescription_ = "";
        _taskDescription = _taskDescription_;
        _preTaskIds = new Zeze.Transaction.Collections.PList1<>(Long.class);
        _preTaskIds.variableId(7);
        _currentPhaseId = _currentPhaseId_;
        _taskPhases = new Zeze.Transaction.Collections.PMap2<>(Long.class, Zeze.Builtin.Game.TaskBase.BTaskPhase.class);
        _taskPhases.variableId(9);
        _extendedData = newDynamicBean_ExtendedData();
    }

    @Override
    public void reset() {
        setRoleId(0);
        setTaskId(0);
        setTaskType("");
        setTaskState(0);
        setTaskName("");
        setTaskDescription("");
        _preTaskIds.clear();
        setCurrentPhaseId(0);
        _taskPhases.clear();
        _extendedData.reset();
        _unknown_ = null;
    }

    public void assign(BTask other) {
        setRoleId(other.getRoleId());
        setTaskId(other.getTaskId());
        setTaskType(other.getTaskType());
        setTaskState(other.getTaskState());
        setTaskName(other.getTaskName());
        setTaskDescription(other.getTaskDescription());
        _preTaskIds.clear();
        _preTaskIds.addAll(other._preTaskIds);
        setCurrentPhaseId(other.getCurrentPhaseId());
        _taskPhases.clear();
        for (var e : other._taskPhases.entrySet())
            _taskPhases.put(e.getKey(), e.getValue().copy());
        _extendedData.assign(other._extendedData);
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

    private static final class Log__roleId extends Zeze.Transaction.Logs.LogLong {
        public Log__roleId(BTask bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTask)getBelong())._roleId = value; }
    }

    private static final class Log__taskId extends Zeze.Transaction.Logs.LogLong {
        public Log__taskId(BTask bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTask)getBelong())._taskId = value; }
    }

    private static final class Log__taskType extends Zeze.Transaction.Logs.LogString {
        public Log__taskType(BTask bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTask)getBelong())._taskType = value; }
    }

    private static final class Log__taskState extends Zeze.Transaction.Logs.LogInt {
        public Log__taskState(BTask bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTask)getBelong())._taskState = value; }
    }

    private static final class Log__taskName extends Zeze.Transaction.Logs.LogString {
        public Log__taskName(BTask bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTask)getBelong())._taskName = value; }
    }

    private static final class Log__taskDescription extends Zeze.Transaction.Logs.LogString {
        public Log__taskDescription(BTask bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTask)getBelong())._taskDescription = value; }
    }

    private static final class Log__currentPhaseId extends Zeze.Transaction.Logs.LogLong {
        public Log__currentPhaseId(BTask bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTask)getBelong())._currentPhaseId = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.TaskBase.BTask: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("roleId=").append(getRoleId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("taskId=").append(getTaskId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("taskType=").append(getTaskType()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("taskState=").append(getTaskState()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("taskName=").append(getTaskName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("taskDescription=").append(getTaskDescription()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("preTaskIds=[");
        if (!_preTaskIds.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _preTaskIds) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(_item_).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("currentPhaseId=").append(getCurrentPhaseId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("taskPhases={");
        if (!_taskPhases.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _kv_ : _taskPhases.entrySet()) {
                sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(_kv_.getKey()).append(',').append(System.lineSeparator());
                sb.append(Zeze.Util.Str.indent(level)).append("Value=").append(System.lineSeparator());
                _kv_.getValue().buildString(sb, level + 4);
                sb.append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append('}').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("extendedData=").append(System.lineSeparator());
        _extendedData.getBean().buildString(sb, level + 4);
        sb.append(System.lineSeparator());
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
            long _x_ = getRoleId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getTaskId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            String _x_ = getTaskType();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            int _x_ = getTaskState();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            String _x_ = getTaskName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getTaskDescription();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = _preTaskIds;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 7, ByteBuffer.LIST);
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
            long _x_ = getCurrentPhaseId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 8, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = _taskPhases;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 9, ByteBuffer.MAP);
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
        {
            var _x_ = _extendedData;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 10, ByteBuffer.DYNAMIC);
                _x_.encode(_o_);
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
            setRoleId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setTaskId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setTaskType(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setTaskState(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setTaskName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            setTaskDescription(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 7) {
            var _x_ = _preTaskIds;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadLong(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 8) {
            setCurrentPhaseId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 9) {
            var _x_ = _taskPhases;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadLong(_s_);
                    var _v_ = _o_.ReadBean(new Zeze.Builtin.Game.TaskBase.BTaskPhase(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 10) {
            _o_.ReadDynamic(_extendedData, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _preTaskIds.initRootInfo(root, this);
        _taskPhases.initRootInfo(root, this);
        _extendedData.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _preTaskIds.initRootInfoWithRedo(root, this);
        _taskPhases.initRootInfoWithRedo(root, this);
        _extendedData.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getRoleId() < 0)
            return true;
        if (getTaskId() < 0)
            return true;
        if (getTaskState() < 0)
            return true;
        for (var _v_ : _preTaskIds) {
            if (_v_ < 0)
                return true;
        }
        if (getCurrentPhaseId() < 0)
            return true;
        for (var _v_ : _taskPhases.values()) {
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
                case 1: _roleId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 2: _taskId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 3: _taskType = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 4: _taskState = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 5: _taskName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 6: _taskDescription = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 7: _preTaskIds.followerApply(vlog); break;
                case 8: _currentPhaseId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 9: _taskPhases.followerApply(vlog); break;
                case 10: _extendedData.followerApply(vlog); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setRoleId(rs.getLong(_parents_name_ + "roleId"));
        setTaskId(rs.getLong(_parents_name_ + "taskId"));
        setTaskType(rs.getString(_parents_name_ + "taskType"));
        if (getTaskType() == null)
            setTaskType("");
        setTaskState(rs.getInt(_parents_name_ + "taskState"));
        setTaskName(rs.getString(_parents_name_ + "taskName"));
        if (getTaskName() == null)
            setTaskName("");
        setTaskDescription(rs.getString(_parents_name_ + "taskDescription"));
        if (getTaskDescription() == null)
            setTaskDescription("");
        Zeze.Serialize.Helper.decodeJsonList(_preTaskIds, Long.class, rs.getString(_parents_name_ + "preTaskIds"));
        setCurrentPhaseId(rs.getLong(_parents_name_ + "currentPhaseId"));
        Zeze.Serialize.Helper.decodeJsonMap(this, "taskPhases", _taskPhases, rs.getString(_parents_name_ + "taskPhases"));
        Zeze.Serialize.Helper.decodeJsonDynamic(_extendedData, rs.getString(_parents_name_ + "extendedData"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendLong(_parents_name_ + "roleId", getRoleId());
        st.appendLong(_parents_name_ + "taskId", getTaskId());
        st.appendString(_parents_name_ + "taskType", getTaskType());
        st.appendInt(_parents_name_ + "taskState", getTaskState());
        st.appendString(_parents_name_ + "taskName", getTaskName());
        st.appendString(_parents_name_ + "taskDescription", getTaskDescription());
        st.appendString(_parents_name_ + "preTaskIds", Zeze.Serialize.Helper.encodeJson(_preTaskIds));
        st.appendLong(_parents_name_ + "currentPhaseId", getCurrentPhaseId());
        st.appendString(_parents_name_ + "taskPhases", Zeze.Serialize.Helper.encodeJson(_taskPhases));
        st.appendString(_parents_name_ + "extendedData", Zeze.Serialize.Helper.encodeJson(_extendedData));
    }

    @Override
    public java.util.List<Zeze.Transaction.Bean.Variable> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Transaction.Bean.Variable(1, "roleId", "long", "", ""));
        vars.add(new Zeze.Transaction.Bean.Variable(2, "taskId", "long", "", ""));
        vars.add(new Zeze.Transaction.Bean.Variable(3, "taskType", "string", "", ""));
        vars.add(new Zeze.Transaction.Bean.Variable(4, "taskState", "int", "", ""));
        vars.add(new Zeze.Transaction.Bean.Variable(5, "taskName", "string", "", ""));
        vars.add(new Zeze.Transaction.Bean.Variable(6, "taskDescription", "string", "", ""));
        vars.add(new Zeze.Transaction.Bean.Variable(7, "preTaskIds", "list", "", "long"));
        vars.add(new Zeze.Transaction.Bean.Variable(8, "currentPhaseId", "long", "", ""));
        vars.add(new Zeze.Transaction.Bean.Variable(9, "taskPhases", "map", "long", "BTaskPhase"));
        vars.add(new Zeze.Transaction.Bean.Variable(10, "extendedData", "dynamic", "", ""));
        return vars;
    }

    @Override
    public Zeze.Transaction.Bean toPrevious() {
        return null; // todo BTask
    }
}
