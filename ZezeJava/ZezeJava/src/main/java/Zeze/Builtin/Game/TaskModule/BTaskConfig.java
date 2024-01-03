// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskModule;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BTaskConfig extends Zeze.Transaction.Bean implements BTaskConfigReadOnly {
    public static final long TYPEID = -2094822843741628318L;

    private final Zeze.Transaction.Collections.PSet1<Integer> _PreposeTasks;
    private final Zeze.Transaction.Collections.PSet1<Integer> _FollowTasks;
    private int _AcceptNpc;
    private int _FinishNpc;
    private final Zeze.Transaction.DynamicBean _CustomData;

    public static Zeze.Transaction.DynamicBean newDynamicBean_CustomData() {
        return new Zeze.Transaction.DynamicBean(5, Zeze.Game.TaskModule::getSpecialTypeIdFromBean, Zeze.Game.TaskModule::createBeanFromSpecialTypeId);
    }

    public static long getSpecialTypeIdFromBean_5(Zeze.Transaction.Bean bean) {
        return Zeze.Game.TaskModule.getSpecialTypeIdFromBean(bean);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_5(long typeId) {
        return Zeze.Game.TaskModule.createBeanFromSpecialTypeId(typeId);
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
        var log = (Log__AcceptNpc)txn.getLog(objectId() + 3);
        return log != null ? log.value : _AcceptNpc;
    }

    public void setAcceptNpc(int value) {
        if (!isManaged()) {
            _AcceptNpc = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__AcceptNpc(this, 3, value));
    }

    @Override
    public int getFinishNpc() {
        if (!isManaged())
            return _FinishNpc;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _FinishNpc;
        var log = (Log__FinishNpc)txn.getLog(objectId() + 4);
        return log != null ? log.value : _FinishNpc;
    }

    public void setFinishNpc(int value) {
        if (!isManaged()) {
            _FinishNpc = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__FinishNpc(this, 4, value));
    }

    public Zeze.Transaction.DynamicBean getCustomData() {
        return _CustomData;
    }

    @Override
    public Zeze.Transaction.DynamicBeanReadOnly getCustomDataReadOnly() {
        return _CustomData;
    }

    @SuppressWarnings("deprecation")
    public BTaskConfig() {
        _PreposeTasks = new Zeze.Transaction.Collections.PSet1<>(Integer.class);
        _PreposeTasks.variableId(1);
        _FollowTasks = new Zeze.Transaction.Collections.PSet1<>(Integer.class);
        _FollowTasks.variableId(2);
        _CustomData = newDynamicBean_CustomData();
    }

    @SuppressWarnings("deprecation")
    public BTaskConfig(int _AcceptNpc_, int _FinishNpc_) {
        _PreposeTasks = new Zeze.Transaction.Collections.PSet1<>(Integer.class);
        _PreposeTasks.variableId(1);
        _FollowTasks = new Zeze.Transaction.Collections.PSet1<>(Integer.class);
        _FollowTasks.variableId(2);
        _AcceptNpc = _AcceptNpc_;
        _FinishNpc = _FinishNpc_;
        _CustomData = newDynamicBean_CustomData();
    }

    @Override
    public void reset() {
        _PreposeTasks.clear();
        _FollowTasks.clear();
        setAcceptNpc(0);
        setFinishNpc(0);
        _CustomData.reset();
        _unknown_ = null;
    }

    public void assign(BTaskConfig other) {
        _PreposeTasks.clear();
        _PreposeTasks.addAll(other._PreposeTasks);
        _FollowTasks.clear();
        _FollowTasks.addAll(other._FollowTasks);
        setAcceptNpc(other.getAcceptNpc());
        setFinishNpc(other.getFinishNpc());
        _CustomData.assign(other._CustomData);
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
        sb.append(Zeze.Util.Str.indent(level)).append("CustomData=").append(System.lineSeparator());
        _CustomData.getBean().buildString(sb, level + 4);
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
            var _x_ = _PreposeTasks;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.LIST);
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
            int _x_ = getAcceptNpc();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getFinishNpc();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = _CustomData;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.DYNAMIC);
                _x_.encode(_o_);
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
            var _x_ = _PreposeTasks;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadInt(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = _FollowTasks;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadInt(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setAcceptNpc(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setFinishNpc(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            _o_.ReadDynamic(_CustomData, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _PreposeTasks.initRootInfo(root, this);
        _FollowTasks.initRootInfo(root, this);
        _CustomData.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _PreposeTasks.initRootInfoWithRedo(root, this);
        _FollowTasks.initRootInfoWithRedo(root, this);
        _CustomData.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
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
                case 1: _PreposeTasks.followerApply(vlog); break;
                case 2: _FollowTasks.followerApply(vlog); break;
                case 3: _AcceptNpc = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 4: _FinishNpc = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 5: _CustomData.followerApply(vlog); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        Zeze.Serialize.Helper.decodeJsonSet(_PreposeTasks, Integer.class, rs.getString(_parents_name_ + "PreposeTasks"));
        Zeze.Serialize.Helper.decodeJsonSet(_FollowTasks, Integer.class, rs.getString(_parents_name_ + "FollowTasks"));
        setAcceptNpc(rs.getInt(_parents_name_ + "AcceptNpc"));
        setFinishNpc(rs.getInt(_parents_name_ + "FinishNpc"));
        Zeze.Serialize.Helper.decodeJsonDynamic(_CustomData, rs.getString(_parents_name_ + "CustomData"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "PreposeTasks", Zeze.Serialize.Helper.encodeJson(_PreposeTasks));
        st.appendString(_parents_name_ + "FollowTasks", Zeze.Serialize.Helper.encodeJson(_FollowTasks));
        st.appendInt(_parents_name_ + "AcceptNpc", getAcceptNpc());
        st.appendInt(_parents_name_ + "FinishNpc", getFinishNpc());
        st.appendString(_parents_name_ + "CustomData", Zeze.Serialize.Helper.encodeJson(_CustomData));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "PreposeTasks", "set", "", "int"));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "FollowTasks", "set", "", "int"));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "AcceptNpc", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "FinishNpc", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "CustomData", "dynamic", "", ""));
        return vars;
    }
}
