// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

import Zeze.Serialize.ByteBuffer;

/*
					Task rpc
					所有的TaskEvent均由这个rpc驱动（仿照现serverdev的结构）
					这个rpc的参数是BTaskEvent，内部的DynamicData是各个不同的任务的不同Event数据
*/
@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BTaskEvent extends Zeze.Transaction.Bean implements BTaskEventReadOnly {
    public static final long TYPEID = -5362050394114558969L;

    private long _roleId; // 当新角色创建时直接发roleId，不附带任何下面这两个dynamics，能够创建整个角色的初始任务表。
    private final Zeze.Transaction.DynamicBean _eventType;
    public static final long DynamicTypeId_EventType_Zeze_Builtin_Game_TaskBase_BSubmitTaskEvent = -2631835383026852161L;
    public static final long DynamicTypeId_EventType_Zeze_Builtin_Game_TaskBase_BAcceptTaskEvent = -2289231209469223633L;
    public static final long DynamicTypeId_EventType_Zeze_Builtin_Game_TaskBase_BSpecificTaskEvent = 2442000638159095225L;
    public static final long DynamicTypeId_EventType_Zeze_Builtin_Game_TaskBase_BBroadcastTaskEvent = 2627115510834301728L;

    public static Zeze.Transaction.DynamicBean newDynamicBean_EventType() {
        return new Zeze.Transaction.DynamicBean(2, BTaskEvent::getSpecialTypeIdFromBean_2, BTaskEvent::createBeanFromSpecialTypeId_2);
    }

    public static long getSpecialTypeIdFromBean_2(Zeze.Transaction.Bean bean) {
        var _typeId_ = bean.typeId();
        if (_typeId_ == Zeze.Transaction.EmptyBean.TYPEID)
            return Zeze.Transaction.EmptyBean.TYPEID;
        if (_typeId_ == -2631835383026852161L)
            return -2631835383026852161L; // Zeze.Builtin.Game.TaskBase.BSubmitTaskEvent
        if (_typeId_ == -2289231209469223633L)
            return -2289231209469223633L; // Zeze.Builtin.Game.TaskBase.BAcceptTaskEvent
        if (_typeId_ == 2442000638159095225L)
            return 2442000638159095225L; // Zeze.Builtin.Game.TaskBase.BSpecificTaskEvent
        if (_typeId_ == 2627115510834301728L)
            return 2627115510834301728L; // Zeze.Builtin.Game.TaskBase.BBroadcastTaskEvent
        throw new UnsupportedOperationException("Unknown Bean! dynamic@Zeze.Builtin.Game.TaskBase.BTaskEvent:eventType");
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_2(long typeId) {
        if (typeId == -2631835383026852161L)
            return new Zeze.Builtin.Game.TaskBase.BSubmitTaskEvent();
        if (typeId == -2289231209469223633L)
            return new Zeze.Builtin.Game.TaskBase.BAcceptTaskEvent();
        if (typeId == 2442000638159095225L)
            return new Zeze.Builtin.Game.TaskBase.BSpecificTaskEvent();
        if (typeId == 2627115510834301728L)
            return new Zeze.Builtin.Game.TaskBase.BBroadcastTaskEvent();
        if (typeId == Zeze.Transaction.EmptyBean.TYPEID)
            return new Zeze.Transaction.EmptyBean();
        return null;
    }

    private final Zeze.Transaction.DynamicBean _eventBean;

    public static Zeze.Transaction.DynamicBean newDynamicBean_EventBean() {
        return new Zeze.Transaction.DynamicBean(3, Zeze.Game.TaskBase::getSpecialTypeIdFromBean, Zeze.Game.TaskBase::createBeanFromSpecialTypeId);
    }

    public static long getSpecialTypeIdFromBean_3(Zeze.Transaction.Bean bean) {
        return Zeze.Game.TaskBase.getSpecialTypeIdFromBean(bean);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_3(long typeId) {
        return Zeze.Game.TaskBase.createBeanFromSpecialTypeId(typeId);
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

    public Zeze.Transaction.DynamicBean getEventType() {
        return _eventType;
    }

    public Zeze.Builtin.Game.TaskBase.BSubmitTaskEvent getEventType_Zeze_Builtin_Game_TaskBase_BSubmitTaskEvent() {
        return (Zeze.Builtin.Game.TaskBase.BSubmitTaskEvent)_eventType.getBean();
    }

    public void setEventType(Zeze.Builtin.Game.TaskBase.BSubmitTaskEvent value) {
        _eventType.setBean(value);
    }

    public Zeze.Builtin.Game.TaskBase.BAcceptTaskEvent getEventType_Zeze_Builtin_Game_TaskBase_BAcceptTaskEvent() {
        return (Zeze.Builtin.Game.TaskBase.BAcceptTaskEvent)_eventType.getBean();
    }

    public void setEventType(Zeze.Builtin.Game.TaskBase.BAcceptTaskEvent value) {
        _eventType.setBean(value);
    }

    public Zeze.Builtin.Game.TaskBase.BSpecificTaskEvent getEventType_Zeze_Builtin_Game_TaskBase_BSpecificTaskEvent() {
        return (Zeze.Builtin.Game.TaskBase.BSpecificTaskEvent)_eventType.getBean();
    }

    public void setEventType(Zeze.Builtin.Game.TaskBase.BSpecificTaskEvent value) {
        _eventType.setBean(value);
    }

    public Zeze.Builtin.Game.TaskBase.BBroadcastTaskEvent getEventType_Zeze_Builtin_Game_TaskBase_BBroadcastTaskEvent() {
        return (Zeze.Builtin.Game.TaskBase.BBroadcastTaskEvent)_eventType.getBean();
    }

    public void setEventType(Zeze.Builtin.Game.TaskBase.BBroadcastTaskEvent value) {
        _eventType.setBean(value);
    }

    @Override
    public Zeze.Transaction.DynamicBeanReadOnly getEventTypeReadOnly() {
        return _eventType;
    }

    @Override
    public Zeze.Builtin.Game.TaskBase.BSubmitTaskEventReadOnly getEventType_Zeze_Builtin_Game_TaskBase_BSubmitTaskEventReadOnly() {
        return (Zeze.Builtin.Game.TaskBase.BSubmitTaskEvent)_eventType.getBean();
    }

    @Override
    public Zeze.Builtin.Game.TaskBase.BAcceptTaskEventReadOnly getEventType_Zeze_Builtin_Game_TaskBase_BAcceptTaskEventReadOnly() {
        return (Zeze.Builtin.Game.TaskBase.BAcceptTaskEvent)_eventType.getBean();
    }

    @Override
    public Zeze.Builtin.Game.TaskBase.BSpecificTaskEventReadOnly getEventType_Zeze_Builtin_Game_TaskBase_BSpecificTaskEventReadOnly() {
        return (Zeze.Builtin.Game.TaskBase.BSpecificTaskEvent)_eventType.getBean();
    }

    @Override
    public Zeze.Builtin.Game.TaskBase.BBroadcastTaskEventReadOnly getEventType_Zeze_Builtin_Game_TaskBase_BBroadcastTaskEventReadOnly() {
        return (Zeze.Builtin.Game.TaskBase.BBroadcastTaskEvent)_eventType.getBean();
    }

    public Zeze.Transaction.DynamicBean getEventBean() {
        return _eventBean;
    }

    @Override
    public Zeze.Transaction.DynamicBeanReadOnly getEventBeanReadOnly() {
        return _eventBean;
    }

    @SuppressWarnings("deprecation")
    public BTaskEvent() {
        _eventType = newDynamicBean_EventType();
        _eventBean = newDynamicBean_EventBean();
    }

    @SuppressWarnings("deprecation")
    public BTaskEvent(long _roleId_) {
        _roleId = _roleId_;
        _eventType = newDynamicBean_EventType();
        _eventBean = newDynamicBean_EventBean();
    }

    @Override
    public void reset() {
        setRoleId(0);
        _eventType.reset();
        _eventBean.reset();
        _unknown_ = null;
    }

    public void assign(BTaskEvent other) {
        setRoleId(other.getRoleId());
        _eventType.assign(other._eventType);
        _eventBean.assign(other._eventBean);
        _unknown_ = other._unknown_;
    }

    public BTaskEvent copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTaskEvent copy() {
        var copy = new BTaskEvent();
        copy.assign(this);
        return copy;
    }

    public static void swap(BTaskEvent a, BTaskEvent b) {
        BTaskEvent save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__roleId extends Zeze.Transaction.Logs.LogLong {
        public Log__roleId(BTaskEvent bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTaskEvent)getBelong())._roleId = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.TaskBase.BTaskEvent: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("roleId=").append(getRoleId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("eventType=").append(System.lineSeparator());
        _eventType.getBean().buildString(sb, level + 4);
        sb.append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("eventBean=").append(System.lineSeparator());
        _eventBean.getBean().buildString(sb, level + 4);
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
            var _x_ = _eventType;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.DYNAMIC);
                _x_.encode(_o_);
            }
        }
        {
            var _x_ = _eventBean;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.DYNAMIC);
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
            _o_.ReadDynamic(_eventType, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _o_.ReadDynamic(_eventBean, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _eventType.initRootInfo(root, this);
        _eventBean.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _eventType.initRootInfoWithRedo(root, this);
        _eventBean.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getRoleId() < 0)
            return true;
        if (_eventType.negativeCheck())
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
                case 1: _roleId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 2: _eventType.followerApply(vlog); break;
                case 3: _eventBean.followerApply(vlog); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setRoleId(rs.getLong(_parents_name_ + "roleId"));
        Zeze.Serialize.Helper.decodeJsonDynamic(_eventType, rs.getString(_parents_name_ + "eventType"));
        Zeze.Serialize.Helper.decodeJsonDynamic(_eventBean, rs.getString(_parents_name_ + "eventBean"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendLong(_parents_name_ + "roleId", getRoleId());
        st.appendString(_parents_name_ + "eventType", Zeze.Serialize.Helper.encodeJson(_eventType));
        st.appendString(_parents_name_ + "eventBean", Zeze.Serialize.Helper.encodeJson(_eventBean));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "roleId", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "eventType", "dynamic", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "eventBean", "dynamic", "", ""));
        return vars;
    }

    @Override
    public Zeze.Transaction.Bean toPrevious() {
        return null; // todo BTaskEvent
    }
}
