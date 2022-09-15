// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BGameOnlineTimer extends Zeze.Transaction.Bean {
    private long _RoleId;
    private final Zeze.Transaction.DynamicBean _TimerObj;
    public static final long DynamicTypeId_TimerObj_Zeze_Builtin_Timer_BCronTimer = -6995089347718168392L;
    public static final long DynamicTypeId_TimerObj_Zeze_Builtin_Timer_BSimpleTimer = 1832177636612857692L;

    public static long GetSpecialTypeIdFromBean_TimerObj(Zeze.Transaction.Bean bean) {
        var _typeId_ = bean.typeId();
        if (_typeId_ == Zeze.Transaction.EmptyBean.TYPEID)
            return Zeze.Transaction.EmptyBean.TYPEID;
        if (_typeId_ == -6995089347718168392L)
            return -6995089347718168392L; // Zeze.Builtin.Timer.BCronTimer
        if (_typeId_ == 1832177636612857692L)
            return 1832177636612857692L; // Zeze.Builtin.Timer.BSimpleTimer
        throw new RuntimeException("Unknown Bean! dynamic@Zeze.Builtin.Timer.BGameOnlineTimer:TimerObj");
    }

    public static Zeze.Transaction.Bean CreateBeanFromSpecialTypeId_TimerObj(long typeId) {
        if (typeId == -6995089347718168392L)
            return new Zeze.Builtin.Timer.BCronTimer();
        if (typeId == 1832177636612857692L)
            return new Zeze.Builtin.Timer.BSimpleTimer();
        return null;
    }

    public long getRoleId() {
        if (!isManaged())
            return _RoleId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _RoleId;
        var log = (Log__RoleId)txn.GetLog(objectId() + 1);
        return log != null ? log.Value : _RoleId;
    }

    public void setRoleId(long value) {
        if (!isManaged()) {
            _RoleId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.PutLog(new Log__RoleId(this, 1, value));
    }

    public Zeze.Transaction.DynamicBean getTimerObj() {
        return _TimerObj;
    }

    public Zeze.Builtin.Timer.BCronTimer getTimerObj_Zeze_Builtin_Timer_BCronTimer(){
        return (Zeze.Builtin.Timer.BCronTimer)getTimerObj().getBean();
    }

    public void setTimerObj(Zeze.Builtin.Timer.BCronTimer value) {
        getTimerObj().setBean(value);
    }

    public Zeze.Builtin.Timer.BSimpleTimer getTimerObj_Zeze_Builtin_Timer_BSimpleTimer(){
        return (Zeze.Builtin.Timer.BSimpleTimer)getTimerObj().getBean();
    }

    public void setTimerObj(Zeze.Builtin.Timer.BSimpleTimer value) {
        getTimerObj().setBean(value);
    }

    @SuppressWarnings("deprecation")
    public BGameOnlineTimer() {
        _TimerObj = new Zeze.Transaction.DynamicBean(2, BGameOnlineTimer::GetSpecialTypeIdFromBean_TimerObj, BGameOnlineTimer::CreateBeanFromSpecialTypeId_TimerObj);
    }

    @SuppressWarnings("deprecation")
    public BGameOnlineTimer(long _RoleId_) {
        _RoleId = _RoleId_;
        _TimerObj = new Zeze.Transaction.DynamicBean(2, BGameOnlineTimer::GetSpecialTypeIdFromBean_TimerObj, BGameOnlineTimer::CreateBeanFromSpecialTypeId_TimerObj);
    }

    public void Assign(BGameOnlineTimer other) {
        setRoleId(other.getRoleId());
        getTimerObj().Assign(other.getTimerObj());
    }

    public BGameOnlineTimer CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BGameOnlineTimer Copy() {
        var copy = new BGameOnlineTimer();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BGameOnlineTimer a, BGameOnlineTimer b) {
        BGameOnlineTimer save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public BGameOnlineTimer CopyBean() {
        return Copy();
    }

    public static final long TYPEID = -3455653027701280193L;

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__RoleId extends Zeze.Transaction.Logs.LogLong {
        public Log__RoleId(BGameOnlineTimer bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BGameOnlineTimer)getBelong())._RoleId = Value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        BuildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void BuildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Timer.BGameOnlineTimer: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("RoleId").append('=').append(getRoleId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("TimerObj").append('=').append(System.lineSeparator());
        getTimerObj().getBean().BuildString(sb, level + 4);
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

    @Override
    public void Encode(ByteBuffer _o_) {
        int _i_ = 0;
        {
            long _x_ = getRoleId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = getTimerObj();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.DYNAMIC);
                _x_.Encode(_o_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void Decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setRoleId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _o_.ReadDynamic(getTimerObj(), _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _TimerObj.InitRootInfo(root, this);
    }

    @Override
    protected void ResetChildrenRootInfo() {
        _TimerObj.ResetRootInfo();
    }

    @Override
    public boolean NegativeCheck() {
        if (getRoleId() < 0)
            return true;
        if (getTimerObj().NegativeCheck())
            return true;
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void FollowerApply(Zeze.Transaction.Log log) {
        var vars = ((Zeze.Transaction.Collections.LogBean)log).getVariables();
        if (vars == null)
            return;
        for (var it = vars.iterator(); it.moveToNext(); ) {
            var vlog = it.value();
            switch (vlog.getVariableId()) {
                case 1: _RoleId = ((Zeze.Transaction.Logs.LogLong)vlog).Value; break;
                case 2: _TimerObj.FollowerApply(vlog); break;
            }
        }
    }
}
