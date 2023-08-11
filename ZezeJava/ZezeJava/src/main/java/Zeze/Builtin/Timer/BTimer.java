// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BTimer extends Zeze.Transaction.Bean implements BTimerReadOnly {
    public static final long TYPEID = -3755541261968580150L;

    private String _TimerName;
    private String _HandleName;
    private final Zeze.Transaction.DynamicBean _TimerObj;
    public static final long DynamicTypeId_TimerObj_Zeze_Builtin_Timer_BCronTimer = -6995089347718168392L;
    public static final long DynamicTypeId_TimerObj_Zeze_Builtin_Timer_BSimpleTimer = 1832177636612857692L;

    public static Zeze.Transaction.DynamicBean newDynamicBean_TimerObj() {
        return new Zeze.Transaction.DynamicBean(3, BTimer::getSpecialTypeIdFromBean_3, BTimer::createBeanFromSpecialTypeId_3);
    }

    public static long getSpecialTypeIdFromBean_3(Zeze.Transaction.Bean bean) {
        var _typeId_ = bean.typeId();
        if (_typeId_ == Zeze.Transaction.EmptyBean.TYPEID)
            return Zeze.Transaction.EmptyBean.TYPEID;
        if (_typeId_ == -6995089347718168392L)
            return -6995089347718168392L; // Zeze.Builtin.Timer.BCronTimer
        if (_typeId_ == 1832177636612857692L)
            return 1832177636612857692L; // Zeze.Builtin.Timer.BSimpleTimer
        throw new UnsupportedOperationException("Unknown Bean! dynamic@Zeze.Builtin.Timer.BTimer:TimerObj");
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_3(long typeId) {
        if (typeId == -6995089347718168392L)
            return new Zeze.Builtin.Timer.BCronTimer();
        if (typeId == 1832177636612857692L)
            return new Zeze.Builtin.Timer.BSimpleTimer();
        if (typeId == Zeze.Transaction.EmptyBean.TYPEID)
            return new Zeze.Transaction.EmptyBean();
        return null;
    }

    private final Zeze.Transaction.DynamicBean _CustomData;

    public static Zeze.Transaction.DynamicBean newDynamicBean_CustomData() {
        return new Zeze.Transaction.DynamicBean(4, Zeze.Component.Timer::getSpecialTypeIdFromBean, Zeze.Component.Timer::createBeanFromSpecialTypeId);
    }

    public static long getSpecialTypeIdFromBean_4(Zeze.Transaction.Bean bean) {
        return Zeze.Component.Timer.getSpecialTypeIdFromBean(bean);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_4(long typeId) {
        return Zeze.Component.Timer.createBeanFromSpecialTypeId(typeId);
    }

    private long _ConcurrentFireSerialNo;

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
    public String getTimerName() {
        if (!isManaged())
            return _TimerName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _TimerName;
        var log = (Log__TimerName)txn.getLog(objectId() + 1);
        return log != null ? log.value : _TimerName;
    }

    public void setTimerName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _TimerName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__TimerName(this, 1, value));
    }

    @Override
    public String getHandleName() {
        if (!isManaged())
            return _HandleName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _HandleName;
        var log = (Log__HandleName)txn.getLog(objectId() + 2);
        return log != null ? log.value : _HandleName;
    }

    public void setHandleName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _HandleName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__HandleName(this, 2, value));
    }

    public Zeze.Transaction.DynamicBean getTimerObj() {
        return _TimerObj;
    }

    public Zeze.Builtin.Timer.BCronTimer getTimerObj_Zeze_Builtin_Timer_BCronTimer() {
        return (Zeze.Builtin.Timer.BCronTimer)_TimerObj.getBean();
    }

    public void setTimerObj(Zeze.Builtin.Timer.BCronTimer value) {
        _TimerObj.setBean(value);
    }

    public Zeze.Builtin.Timer.BSimpleTimer getTimerObj_Zeze_Builtin_Timer_BSimpleTimer() {
        return (Zeze.Builtin.Timer.BSimpleTimer)_TimerObj.getBean();
    }

    public void setTimerObj(Zeze.Builtin.Timer.BSimpleTimer value) {
        _TimerObj.setBean(value);
    }

    @Override
    public Zeze.Transaction.DynamicBeanReadOnly getTimerObjReadOnly() {
        return _TimerObj;
    }

    @Override
    public Zeze.Builtin.Timer.BCronTimerReadOnly getTimerObj_Zeze_Builtin_Timer_BCronTimerReadOnly() {
        return (Zeze.Builtin.Timer.BCronTimer)_TimerObj.getBean();
    }

    @Override
    public Zeze.Builtin.Timer.BSimpleTimerReadOnly getTimerObj_Zeze_Builtin_Timer_BSimpleTimerReadOnly() {
        return (Zeze.Builtin.Timer.BSimpleTimer)_TimerObj.getBean();
    }

    public Zeze.Transaction.DynamicBean getCustomData() {
        return _CustomData;
    }

    @Override
    public Zeze.Transaction.DynamicBeanReadOnly getCustomDataReadOnly() {
        return _CustomData;
    }

    @Override
    public long getConcurrentFireSerialNo() {
        if (!isManaged())
            return _ConcurrentFireSerialNo;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ConcurrentFireSerialNo;
        var log = (Log__ConcurrentFireSerialNo)txn.getLog(objectId() + 5);
        return log != null ? log.value : _ConcurrentFireSerialNo;
    }

    public void setConcurrentFireSerialNo(long value) {
        if (!isManaged()) {
            _ConcurrentFireSerialNo = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ConcurrentFireSerialNo(this, 5, value));
    }

    @SuppressWarnings("deprecation")
    public BTimer() {
        _TimerName = "";
        _HandleName = "";
        _TimerObj = newDynamicBean_TimerObj();
        _CustomData = newDynamicBean_CustomData();
    }

    @SuppressWarnings("deprecation")
    public BTimer(String _TimerName_, String _HandleName_, long _ConcurrentFireSerialNo_) {
        if (_TimerName_ == null)
            _TimerName_ = "";
        _TimerName = _TimerName_;
        if (_HandleName_ == null)
            _HandleName_ = "";
        _HandleName = _HandleName_;
        _TimerObj = newDynamicBean_TimerObj();
        _CustomData = newDynamicBean_CustomData();
        _ConcurrentFireSerialNo = _ConcurrentFireSerialNo_;
    }

    @Override
    public void reset() {
        setTimerName("");
        setHandleName("");
        _TimerObj.reset();
        _CustomData.reset();
        setConcurrentFireSerialNo(0);
        _unknown_ = null;
    }

    public void assign(BTimer other) {
        setTimerName(other.getTimerName());
        setHandleName(other.getHandleName());
        _TimerObj.assign(other._TimerObj);
        _CustomData.assign(other._CustomData);
        setConcurrentFireSerialNo(other.getConcurrentFireSerialNo());
        _unknown_ = other._unknown_;
    }

    public BTimer copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTimer copy() {
        var copy = new BTimer();
        copy.assign(this);
        return copy;
    }

    public static void swap(BTimer a, BTimer b) {
        BTimer save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__TimerName extends Zeze.Transaction.Logs.LogString {
        public Log__TimerName(BTimer bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTimer)getBelong())._TimerName = value; }
    }

    private static final class Log__HandleName extends Zeze.Transaction.Logs.LogString {
        public Log__HandleName(BTimer bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTimer)getBelong())._HandleName = value; }
    }

    private static final class Log__ConcurrentFireSerialNo extends Zeze.Transaction.Logs.LogLong {
        public Log__ConcurrentFireSerialNo(BTimer bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTimer)getBelong())._ConcurrentFireSerialNo = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Timer.BTimer: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("TimerName=").append(getTimerName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("HandleName=").append(getHandleName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("TimerObj=").append(System.lineSeparator());
        _TimerObj.getBean().buildString(sb, level + 4);
        sb.append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("CustomData=").append(System.lineSeparator());
        _CustomData.getBean().buildString(sb, level + 4);
        sb.append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ConcurrentFireSerialNo=").append(getConcurrentFireSerialNo()).append(System.lineSeparator());
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
            String _x_ = getTimerName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getHandleName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = _TimerObj;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.DYNAMIC);
                _x_.encode(_o_);
            }
        }
        {
            var _x_ = _CustomData;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.DYNAMIC);
                _x_.encode(_o_);
            }
        }
        {
            long _x_ = getConcurrentFireSerialNo();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
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
            setTimerName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setHandleName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _o_.ReadDynamic(_TimerObj, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _o_.ReadDynamic(_CustomData, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setConcurrentFireSerialNo(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _TimerObj.initRootInfo(root, this);
        _CustomData.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _TimerObj.initRootInfoWithRedo(root, this);
        _CustomData.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
        if (_TimerObj.negativeCheck())
            return true;
        if (getConcurrentFireSerialNo() < 0)
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
                case 1: _TimerName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 2: _HandleName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 3: _TimerObj.followerApply(vlog); break;
                case 4: _CustomData.followerApply(vlog); break;
                case 5: _ConcurrentFireSerialNo = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setTimerName(rs.getString(_parents_name_ + "TimerName"));
        if (getTimerName() == null)
            setTimerName("");
        setHandleName(rs.getString(_parents_name_ + "HandleName"));
        if (getHandleName() == null)
            setHandleName("");
        Zeze.Serialize.Helper.decodeJsonDynamic(_TimerObj, rs.getString(_parents_name_ + "TimerObj"));
        Zeze.Serialize.Helper.decodeJsonDynamic(_CustomData, rs.getString(_parents_name_ + "CustomData"));
        setConcurrentFireSerialNo(rs.getLong(_parents_name_ + "ConcurrentFireSerialNo"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "TimerName", getTimerName());
        st.appendString(_parents_name_ + "HandleName", getHandleName());
        st.appendString(_parents_name_ + "TimerObj", Zeze.Serialize.Helper.encodeJson(_TimerObj));
        st.appendString(_parents_name_ + "CustomData", Zeze.Serialize.Helper.encodeJson(_CustomData));
        st.appendLong(_parents_name_ + "ConcurrentFireSerialNo", getConcurrentFireSerialNo());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "TimerName", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "HandleName", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "TimerObj", "dynamic", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "CustomData", "dynamic", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "ConcurrentFireSerialNo", "long", "", ""));
        return vars;
    }
}
