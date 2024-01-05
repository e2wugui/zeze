// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskModule;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BTaskConfig extends Zeze.Transaction.Bean implements BTaskConfigReadOnly {
    public static final long TYPEID = -2094822843741628318L;

    private int _TaskId;
    private final Zeze.Transaction.Collections.PSet1<Integer> _PreposeTasks; // 前置任务
    private final Zeze.Transaction.Collections.PSet1<Integer> _FollowTasks; // 后续任务
    private int _AcceptNpc; // 接受任务的NpcId
    private int _FinishNpc; // 完成任务的NpcId
    private final Zeze.Transaction.DynamicBean _ExtendData;

    public static Zeze.Transaction.DynamicBean newDynamicBean_ExtendData() {
        return new Zeze.Transaction.DynamicBean(6, Zeze.Game.TaskModule::getSpecialTypeIdFromBean, Zeze.Game.TaskModule::createBeanFromSpecialTypeId);
    }

    public static long getSpecialTypeIdFromBean_6(Zeze.Transaction.Bean bean) {
        return Zeze.Game.TaskModule.getSpecialTypeIdFromBean(bean);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_6(long typeId) {
        return Zeze.Game.TaskModule.createBeanFromSpecialTypeId(typeId);
    }

    private final Zeze.Transaction.Collections.CollOne<Zeze.Builtin.Game.TaskModule.BTask> _TaskConditions; // 任务条件
    private int _PreposeRequired; // 需要的前置任务完成数量，0表示全部。

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
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _AcceptNpc;
        var log = (Log__AcceptNpc)txn.getLog(objectId() + 4);
        return log != null ? log.value : _AcceptNpc;
    }

    public void setAcceptNpc(int value) {
        if (!isManaged()) {
            _AcceptNpc = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__AcceptNpc(this, 4, value));
    }

    @Override
    public int getFinishNpc() {
        if (!isManaged())
            return _FinishNpc;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _FinishNpc;
        var log = (Log__FinishNpc)txn.getLog(objectId() + 5);
        return log != null ? log.value : _FinishNpc;
    }

    public void setFinishNpc(int value) {
        if (!isManaged()) {
            _FinishNpc = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__FinishNpc(this, 5, value));
    }

    public Zeze.Transaction.DynamicBean getExtendData() {
        return _ExtendData;
    }

    @Override
    public Zeze.Transaction.DynamicBeanReadOnly getExtendDataReadOnly() {
        return _ExtendData;
    }

    public Zeze.Builtin.Game.TaskModule.BTask getTaskConditions() {
        return _TaskConditions.getValue();
    }

    public void setTaskConditions(Zeze.Builtin.Game.TaskModule.BTask value) {
        _TaskConditions.setValue(value);
    }

    @Override
    public Zeze.Builtin.Game.TaskModule.BTaskReadOnly getTaskConditionsReadOnly() {
        return _TaskConditions.getValue();
    }

    @Override
    public int getPreposeRequired() {
        if (!isManaged())
            return _PreposeRequired;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _PreposeRequired;
        var log = (Log__PreposeRequired)txn.getLog(objectId() + 8);
        return log != null ? log.value : _PreposeRequired;
    }

    public void setPreposeRequired(int value) {
        if (!isManaged()) {
            _PreposeRequired = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__PreposeRequired(this, 8, value));
    }

    @SuppressWarnings("deprecation")
    public BTaskConfig() {
        _PreposeTasks = new Zeze.Transaction.Collections.PSet1<>(Integer.class);
        _PreposeTasks.variableId(2);
        _FollowTasks = new Zeze.Transaction.Collections.PSet1<>(Integer.class);
        _FollowTasks.variableId(3);
        _ExtendData = newDynamicBean_ExtendData();
        _TaskConditions = new Zeze.Transaction.Collections.CollOne<>(new Zeze.Builtin.Game.TaskModule.BTask(), Zeze.Builtin.Game.TaskModule.BTask.class);
        _TaskConditions.variableId(7);
    }

    @SuppressWarnings("deprecation")
    public BTaskConfig(int _TaskId_, int _AcceptNpc_, int _FinishNpc_, int _PreposeRequired_) {
        _TaskId = _TaskId_;
        _PreposeTasks = new Zeze.Transaction.Collections.PSet1<>(Integer.class);
        _PreposeTasks.variableId(2);
        _FollowTasks = new Zeze.Transaction.Collections.PSet1<>(Integer.class);
        _FollowTasks.variableId(3);
        _AcceptNpc = _AcceptNpc_;
        _FinishNpc = _FinishNpc_;
        _ExtendData = newDynamicBean_ExtendData();
        _TaskConditions = new Zeze.Transaction.Collections.CollOne<>(new Zeze.Builtin.Game.TaskModule.BTask(), Zeze.Builtin.Game.TaskModule.BTask.class);
        _TaskConditions.variableId(7);
        _PreposeRequired = _PreposeRequired_;
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
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Game.TaskModule.BTaskConfig.Data toData() {
        var data = new Zeze.Builtin.Game.TaskModule.BTaskConfig.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Game.TaskModule.BTaskConfig.Data)other);
    }

    public void assign(BTaskConfig.Data other) {
        setTaskId(other._TaskId);
        _PreposeTasks.clear();
        _PreposeTasks.addAll(other._PreposeTasks);
        _FollowTasks.clear();
        _FollowTasks.addAll(other._FollowTasks);
        setAcceptNpc(other._AcceptNpc);
        setFinishNpc(other._FinishNpc);
        _ExtendData.assign(other._ExtendData);
        Zeze.Builtin.Game.TaskModule.BTask data_TaskConditions = new Zeze.Builtin.Game.TaskModule.BTask();
        data_TaskConditions.assign(other._TaskConditions);
        _TaskConditions.setValue(data_TaskConditions);
        setPreposeRequired(other._PreposeRequired);
        _unknown_ = null;
    }

    public void assign(BTaskConfig other) {
        setTaskId(other.getTaskId());
        _PreposeTasks.clear();
        _PreposeTasks.addAll(other._PreposeTasks);
        _FollowTasks.clear();
        _FollowTasks.addAll(other._FollowTasks);
        setAcceptNpc(other.getAcceptNpc());
        setFinishNpc(other.getFinishNpc());
        _ExtendData.assign(other._ExtendData);
        _TaskConditions.assign(other._TaskConditions);
        setPreposeRequired(other.getPreposeRequired());
        _unknown_ = other._unknown_;
    }

    public BTaskConfig copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTaskConfig copy() {
        var copy = new BTaskConfig();
        copy.assign(this);
        return copy;
    }

    public static void swap(BTaskConfig a, BTaskConfig b) {
        BTaskConfig save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__TaskId extends Zeze.Transaction.Logs.LogInt {
        public Log__TaskId(BTaskConfig bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTaskConfig)getBelong())._TaskId = value; }
    }

    private static final class Log__AcceptNpc extends Zeze.Transaction.Logs.LogInt {
        public Log__AcceptNpc(BTaskConfig bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTaskConfig)getBelong())._AcceptNpc = value; }
    }

    private static final class Log__FinishNpc extends Zeze.Transaction.Logs.LogInt {
        public Log__FinishNpc(BTaskConfig bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTaskConfig)getBelong())._FinishNpc = value; }
    }

    private static final class Log__PreposeRequired extends Zeze.Transaction.Logs.LogInt {
        public Log__PreposeRequired(BTaskConfig bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTaskConfig)getBelong())._PreposeRequired = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.TaskModule.BTaskConfig: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("TaskId=").append(getTaskId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("PreposeTasks={");
        if (!_PreposeTasks.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _PreposeTasks) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(_item_).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append('}').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("FollowTasks={");
        if (!_FollowTasks.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _FollowTasks) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(_item_).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append('}').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("AcceptNpc=").append(getAcceptNpc()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("FinishNpc=").append(getFinishNpc()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ExtendData=").append(System.lineSeparator());
        _ExtendData.getBean().buildString(sb, level + 4);
        sb.append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("TaskConditions=").append(System.lineSeparator());
        _TaskConditions.buildString(sb, level + 4);
        sb.append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("PreposeRequired=").append(getPreposeRequired()).append(System.lineSeparator());
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
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _PreposeTasks.initRootInfo(root, this);
        _FollowTasks.initRootInfo(root, this);
        _ExtendData.initRootInfo(root, this);
        _TaskConditions.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _PreposeTasks.initRootInfoWithRedo(root, this);
        _FollowTasks.initRootInfoWithRedo(root, this);
        _ExtendData.initRootInfoWithRedo(root, this);
        _TaskConditions.initRootInfoWithRedo(root, this);
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
    public void followerApply(Zeze.Transaction.Log log) {
        var vars = ((Zeze.Transaction.Collections.LogBean)log).getVariables();
        if (vars == null)
            return;
        for (var it = vars.iterator(); it.moveToNext(); ) {
            var vlog = it.value();
            switch (vlog.getVariableId()) {
                case 1: _TaskId = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 2: _PreposeTasks.followerApply(vlog); break;
                case 3: _FollowTasks.followerApply(vlog); break;
                case 4: _AcceptNpc = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 5: _FinishNpc = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 6: _ExtendData.followerApply(vlog); break;
                case 7: _TaskConditions.followerApply(vlog); break;
                case 8: _PreposeRequired = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setTaskId(rs.getInt(_parents_name_ + "TaskId"));
        Zeze.Serialize.Helper.decodeJsonSet(_PreposeTasks, Integer.class, rs.getString(_parents_name_ + "PreposeTasks"));
        Zeze.Serialize.Helper.decodeJsonSet(_FollowTasks, Integer.class, rs.getString(_parents_name_ + "FollowTasks"));
        setAcceptNpc(rs.getInt(_parents_name_ + "AcceptNpc"));
        setFinishNpc(rs.getInt(_parents_name_ + "FinishNpc"));
        Zeze.Serialize.Helper.decodeJsonDynamic(_ExtendData, rs.getString(_parents_name_ + "ExtendData"));
        parents.add("TaskConditions");
        _TaskConditions.decodeResultSet(parents, rs);
        parents.remove(parents.size() - 1);
        setPreposeRequired(rs.getInt(_parents_name_ + "PreposeRequired"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendInt(_parents_name_ + "TaskId", getTaskId());
        st.appendString(_parents_name_ + "PreposeTasks", Zeze.Serialize.Helper.encodeJson(_PreposeTasks));
        st.appendString(_parents_name_ + "FollowTasks", Zeze.Serialize.Helper.encodeJson(_FollowTasks));
        st.appendInt(_parents_name_ + "AcceptNpc", getAcceptNpc());
        st.appendInt(_parents_name_ + "FinishNpc", getFinishNpc());
        st.appendString(_parents_name_ + "ExtendData", Zeze.Serialize.Helper.encodeJson(_ExtendData));
        parents.add("TaskConditions");
        _TaskConditions.encodeSQLStatement(parents, st);
        parents.remove(parents.size() - 1);
        st.appendInt(_parents_name_ + "PreposeRequired", getPreposeRequired());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "TaskId", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "PreposeTasks", "set", "", "int"));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "FollowTasks", "set", "", "int"));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "AcceptNpc", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "FinishNpc", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(6, "ExtendData", "dynamic", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(7, "TaskConditions", "BTask", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(8, "PreposeRequired", "int", "", ""));
        return vars;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -2094822843741628318L;

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
        public long toTypeId(Zeze.Transaction.Data data) {
            return Zeze.Game.TaskModule.getSpecialTypeIdFromBean(data);
        }

        @Override
        public Zeze.Transaction.Data toData(long typeId) {
            return Zeze.Game.TaskModule.createDataFromSpecialTypeId(typeId);
        }

        @Override
        public DynamicData_ExtendData copy() {
            return (DynamicData_ExtendData)super.copy();
        }
    }

    private Zeze.Builtin.Game.TaskModule.BTask.Data _TaskConditions; // 任务条件
    private int _PreposeRequired; // 需要的前置任务完成数量，0表示全部。

    public int getTaskId() {
        return _TaskId;
    }

    public void setTaskId(int value) {
        _TaskId = value;
    }

    public java.util.HashSet<Integer> getPreposeTasks() {
        return _PreposeTasks;
    }

    public void setPreposeTasks(java.util.HashSet<Integer> value) {
        if (value == null)
            throw new IllegalArgumentException();
        _PreposeTasks = value;
    }

    public java.util.HashSet<Integer> getFollowTasks() {
        return _FollowTasks;
    }

    public void setFollowTasks(java.util.HashSet<Integer> value) {
        if (value == null)
            throw new IllegalArgumentException();
        _FollowTasks = value;
    }

    public int getAcceptNpc() {
        return _AcceptNpc;
    }

    public void setAcceptNpc(int value) {
        _AcceptNpc = value;
    }

    public int getFinishNpc() {
        return _FinishNpc;
    }

    public void setFinishNpc(int value) {
        _FinishNpc = value;
    }

    public DynamicData_ExtendData getExtendData() {
        return _ExtendData;
    }

    public Zeze.Builtin.Game.TaskModule.BTask.Data getTaskConditions() {
        return _TaskConditions;
    }

    public void setTaskConditions(Zeze.Builtin.Game.TaskModule.BTask.Data value) {
        if (value == null)
            throw new IllegalArgumentException();
        _TaskConditions = value;
    }

    public int getPreposeRequired() {
        return _PreposeRequired;
    }

    public void setPreposeRequired(int value) {
        _PreposeRequired = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _PreposeTasks = new java.util.HashSet<>();
        _FollowTasks = new java.util.HashSet<>();
        _ExtendData = new DynamicData_ExtendData();
        _TaskConditions = new Zeze.Builtin.Game.TaskModule.BTask.Data();
    }

    @SuppressWarnings("deprecation")
    public Data(int _TaskId_, java.util.HashSet<Integer> _PreposeTasks_, java.util.HashSet<Integer> _FollowTasks_, int _AcceptNpc_, int _FinishNpc_, DynamicData_ExtendData _ExtendData_, Zeze.Builtin.Game.TaskModule.BTask.Data _TaskConditions_, int _PreposeRequired_) {
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
            _TaskConditions_ = new Zeze.Builtin.Game.TaskModule.BTask.Data();
        _TaskConditions = _TaskConditions_;
        _PreposeRequired = _PreposeRequired_;
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
    }

    @Override
    public Zeze.Builtin.Game.TaskModule.BTaskConfig toBean() {
        var bean = new Zeze.Builtin.Game.TaskModule.BTaskConfig();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BTaskConfig)other);
    }

    public void assign(BTaskConfig other) {
        _TaskId = other.getTaskId();
        _PreposeTasks.clear();
        _PreposeTasks.addAll(other._PreposeTasks);
        _FollowTasks.clear();
        _FollowTasks.addAll(other._FollowTasks);
        _AcceptNpc = other.getAcceptNpc();
        _FinishNpc = other.getFinishNpc();
        _ExtendData.assign(other._ExtendData);
        _TaskConditions.assign(other._TaskConditions.getValue());
        _PreposeRequired = other.getPreposeRequired();
    }

    public void assign(BTaskConfig.Data other) {
        _TaskId = other._TaskId;
        _PreposeTasks.clear();
        _PreposeTasks.addAll(other._PreposeTasks);
        _FollowTasks.clear();
        _FollowTasks.addAll(other._FollowTasks);
        _AcceptNpc = other._AcceptNpc;
        _FinishNpc = other._FinishNpc;
        _ExtendData.assign(other._ExtendData);
        _TaskConditions.assign(other._TaskConditions);
        _PreposeRequired = other._PreposeRequired;
    }

    @Override
    public BTaskConfig.Data copy() {
        var copy = new BTaskConfig.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BTaskConfig.Data a, BTaskConfig.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
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
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.TaskModule.BTaskConfig: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("TaskId=").append(_TaskId).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("PreposeTasks={");
        if (!_PreposeTasks.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _PreposeTasks) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(_item_).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append('}').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("FollowTasks={");
        if (!_FollowTasks.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _FollowTasks) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(_item_).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append('}').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("AcceptNpc=").append(_AcceptNpc).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("FinishNpc=").append(_FinishNpc).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ExtendData=").append(System.lineSeparator());
        _ExtendData.getData().buildString(sb, level + 4);
        sb.append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("TaskConditions=").append(System.lineSeparator());
        _TaskConditions.buildString(sb, level + 4);
        sb.append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("PreposeRequired=").append(_PreposeRequired).append(System.lineSeparator());
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
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
