// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BGameOnlineCustom extends Zeze.Transaction.Bean {
    private long _RoleId;
    private String _HandleName;
    private final Zeze.Transaction.DynamicBean _CustomData;

<<<<<<< HEAD:ZezeJava/ZezeJava/src/main/java/Zeze/Builtin/Timer/BGameOnlineTimer.java
    public static long getSpecialTypeIdFromBean_TimerObj(Zeze.Transaction.Bean bean) {
        var _typeId_ = bean.typeId();
        if (_typeId_ == Zeze.Transaction.EmptyBean.TYPEID)
            return Zeze.Transaction.EmptyBean.TYPEID;
        if (_typeId_ == -6995089347718168392L)
            return -6995089347718168392L; // Zeze.Builtin.Timer.BCronTimer
        if (_typeId_ == 1832177636612857692L)
            return 1832177636612857692L; // Zeze.Builtin.Timer.BSimpleTimer
        throw new RuntimeException("Unknown Bean! dynamic@Zeze.Builtin.Timer.BGameOnlineTimer:TimerObj");
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_TimerObj(long typeId) {
        if (typeId == -6995089347718168392L)
            return new Zeze.Builtin.Timer.BCronTimer();
        if (typeId == 1832177636612857692L)
            return new Zeze.Builtin.Timer.BSimpleTimer();
        return null;
=======
    public static long GetSpecialTypeIdFromBean_CustomData(Zeze.Transaction.Bean bean) {
        return Zeze.Component.Timer.GetSpecialTypeIdFromBean(bean);
    }

    public static Zeze.Transaction.Bean CreateBeanFromSpecialTypeId_CustomData(long typeId) {
        return Zeze.Component.Timer.CreateBeanFromSpecialTypeId(typeId);
>>>>>>> 59e80f2f (Timer Online 改成基于 Timer.Basic.):ZezeJava/ZezeJava/src/main/java/Zeze/Builtin/Timer/BGameOnlineCustom.java
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

    public String getHandleName() {
        if (!isManaged())
            return _HandleName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _HandleName;
        var log = (Log__HandleName)txn.GetLog(objectId() + 2);
        return log != null ? log.Value : _HandleName;
    }

    public void setHandleName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _HandleName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.PutLog(new Log__HandleName(this, 2, value));
    }

    public Zeze.Transaction.DynamicBean getCustomData() {
        return _CustomData;
    }

    @SuppressWarnings("deprecation")
<<<<<<< HEAD:ZezeJava/ZezeJava/src/main/java/Zeze/Builtin/Timer/BGameOnlineTimer.java
    public BGameOnlineTimer() {
        _TimerObj = new Zeze.Transaction.DynamicBean(2, BGameOnlineTimer::getSpecialTypeIdFromBean_TimerObj, BGameOnlineTimer::createBeanFromSpecialTypeId_TimerObj);
=======
    public BGameOnlineCustom() {
        _HandleName = "";
        _CustomData = new Zeze.Transaction.DynamicBean(3, Zeze.Component.Timer::GetSpecialTypeIdFromBean, Zeze.Component.Timer::CreateBeanFromSpecialTypeId);
>>>>>>> 59e80f2f (Timer Online 改成基于 Timer.Basic.):ZezeJava/ZezeJava/src/main/java/Zeze/Builtin/Timer/BGameOnlineCustom.java
    }

    @SuppressWarnings("deprecation")
    public BGameOnlineCustom(long _RoleId_, String _HandleName_) {
        _RoleId = _RoleId_;
<<<<<<< HEAD:ZezeJava/ZezeJava/src/main/java/Zeze/Builtin/Timer/BGameOnlineTimer.java
        _TimerObj = new Zeze.Transaction.DynamicBean(2, BGameOnlineTimer::getSpecialTypeIdFromBean_TimerObj, BGameOnlineTimer::createBeanFromSpecialTypeId_TimerObj);
    }

    public void assign(BGameOnlineTimer other) {
=======
        if (_HandleName_ == null)
            throw new IllegalArgumentException();
        _HandleName = _HandleName_;
        _CustomData = new Zeze.Transaction.DynamicBean(3, Zeze.Component.Timer::GetSpecialTypeIdFromBean, Zeze.Component.Timer::CreateBeanFromSpecialTypeId);
    }

    public void Assign(BGameOnlineCustom other) {
>>>>>>> 59e80f2f (Timer Online 改成基于 Timer.Basic.):ZezeJava/ZezeJava/src/main/java/Zeze/Builtin/Timer/BGameOnlineCustom.java
        setRoleId(other.getRoleId());
        setHandleName(other.getHandleName());
        getCustomData().Assign(other.getCustomData());
    }

<<<<<<< HEAD:ZezeJava/ZezeJava/src/main/java/Zeze/Builtin/Timer/BGameOnlineTimer.java
    @Deprecated
    public void Assign(BGameOnlineTimer other) {
        assign(other);
    }

    public BGameOnlineTimer copyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BGameOnlineTimer copy() {
        var copy = new BGameOnlineTimer();
=======
    public BGameOnlineCustom CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BGameOnlineCustom Copy() {
        var copy = new BGameOnlineCustom();
>>>>>>> 59e80f2f (Timer Online 改成基于 Timer.Basic.):ZezeJava/ZezeJava/src/main/java/Zeze/Builtin/Timer/BGameOnlineCustom.java
        copy.Assign(this);
        return copy;
    }

<<<<<<< HEAD:ZezeJava/ZezeJava/src/main/java/Zeze/Builtin/Timer/BGameOnlineTimer.java
    @Deprecated
    public BGameOnlineTimer Copy() {
        return copy();
    }

    public static void swap(BGameOnlineTimer a, BGameOnlineTimer b) {
        BGameOnlineTimer save = a.Copy();
=======
    public static void Swap(BGameOnlineCustom a, BGameOnlineCustom b) {
        BGameOnlineCustom save = a.Copy();
>>>>>>> 59e80f2f (Timer Online 改成基于 Timer.Basic.):ZezeJava/ZezeJava/src/main/java/Zeze/Builtin/Timer/BGameOnlineCustom.java
        a.Assign(b);
        b.Assign(save);
    }

    @Override
<<<<<<< HEAD:ZezeJava/ZezeJava/src/main/java/Zeze/Builtin/Timer/BGameOnlineTimer.java
    public BGameOnlineTimer copyBean() {
=======
    public BGameOnlineCustom CopyBean() {
>>>>>>> 59e80f2f (Timer Online 改成基于 Timer.Basic.):ZezeJava/ZezeJava/src/main/java/Zeze/Builtin/Timer/BGameOnlineCustom.java
        return Copy();
    }

    public static final long TYPEID = -8444232083929347301L;

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__RoleId extends Zeze.Transaction.Logs.LogLong {
        public Log__RoleId(BGameOnlineCustom bean, int varId, long value) { super(bean, varId, value); }

        @Override
<<<<<<< HEAD:ZezeJava/ZezeJava/src/main/java/Zeze/Builtin/Timer/BGameOnlineTimer.java
        public void commit() { ((BGameOnlineTimer)getBelong())._RoleId = Value; }
=======
        public void Commit() { ((BGameOnlineCustom)getBelong())._RoleId = Value; }
    }

    private static final class Log__HandleName extends Zeze.Transaction.Logs.LogString {
        public Log__HandleName(BGameOnlineCustom bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BGameOnlineCustom)getBelong())._HandleName = Value; }
>>>>>>> 59e80f2f (Timer Online 改成基于 Timer.Basic.):ZezeJava/ZezeJava/src/main/java/Zeze/Builtin/Timer/BGameOnlineCustom.java
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
<<<<<<< HEAD:ZezeJava/ZezeJava/src/main/java/Zeze/Builtin/Timer/BGameOnlineTimer.java
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Timer.BGameOnlineTimer: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("RoleId").append('=').append(getRoleId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("TimerObj").append('=').append(System.lineSeparator());
        getTimerObj().getBean().buildString(sb, level + 4);
=======
    public void BuildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Timer.BGameOnlineCustom: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("RoleId").append('=').append(getRoleId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("HandleName").append('=').append(getHandleName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("CustomData").append('=').append(System.lineSeparator());
        getCustomData().getBean().BuildString(sb, level + 4);
>>>>>>> 59e80f2f (Timer Online 改成基于 Timer.Basic.):ZezeJava/ZezeJava/src/main/java/Zeze/Builtin/Timer/BGameOnlineCustom.java
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
    public void encode(ByteBuffer _o_) {
        int _i_ = 0;
        {
            long _x_ = getRoleId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            String _x_ = getHandleName();
            if (!_x_.isEmpty()) {
<<<<<<< HEAD:ZezeJava/ZezeJava/src/main/java/Zeze/Builtin/Timer/BGameOnlineTimer.java
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.DYNAMIC);
                _x_.encode(_o_);
=======
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = getCustomData();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.DYNAMIC);
                _x_.Encode(_o_);
>>>>>>> 59e80f2f (Timer Online 改成基于 Timer.Basic.):ZezeJava/ZezeJava/src/main/java/Zeze/Builtin/Timer/BGameOnlineCustom.java
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setRoleId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setHandleName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _o_.ReadDynamic(getCustomData(), _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
<<<<<<< HEAD:ZezeJava/ZezeJava/src/main/java/Zeze/Builtin/Timer/BGameOnlineTimer.java
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _TimerObj.initRootInfo(root, this);
    }

    @Override
    protected void resetChildrenRootInfo() {
        _TimerObj.resetRootInfo();
=======
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _CustomData.InitRootInfo(root, this);
    }

    @Override
    protected void ResetChildrenRootInfo() {
        _CustomData.ResetRootInfo();
>>>>>>> 59e80f2f (Timer Online 改成基于 Timer.Basic.):ZezeJava/ZezeJava/src/main/java/Zeze/Builtin/Timer/BGameOnlineCustom.java
    }

    @Override
    public boolean negativeCheck() {
        if (getRoleId() < 0)
            return true;
<<<<<<< HEAD:ZezeJava/ZezeJava/src/main/java/Zeze/Builtin/Timer/BGameOnlineTimer.java
        if (getTimerObj().negativeCheck())
            return true;
=======
>>>>>>> 59e80f2f (Timer Online 改成基于 Timer.Basic.):ZezeJava/ZezeJava/src/main/java/Zeze/Builtin/Timer/BGameOnlineCustom.java
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
                case 1: _RoleId = ((Zeze.Transaction.Logs.LogLong)vlog).Value; break;
<<<<<<< HEAD:ZezeJava/ZezeJava/src/main/java/Zeze/Builtin/Timer/BGameOnlineTimer.java
                case 2: _TimerObj.followerApply(vlog); break;
=======
                case 2: _HandleName = ((Zeze.Transaction.Logs.LogString)vlog).Value; break;
                case 3: _CustomData.FollowerApply(vlog); break;
>>>>>>> 59e80f2f (Timer Online 改成基于 Timer.Basic.):ZezeJava/ZezeJava/src/main/java/Zeze/Builtin/Timer/BGameOnlineCustom.java
            }
        }
    }
}
