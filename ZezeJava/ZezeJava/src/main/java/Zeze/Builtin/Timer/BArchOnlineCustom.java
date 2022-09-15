// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BArchOnlineCustom extends Zeze.Transaction.Bean {
    private String _Account;
    private String _ClientId;
<<<<<<< HEAD:ZezeJava/ZezeJava/src/main/java/Zeze/Builtin/Timer/BOnlineTimer.java
    private final Zeze.Transaction.DynamicBean _TimerObj;
    public static final long DynamicTypeId_TimerObj_Zeze_Builtin_Timer_BCronTimer = -6995089347718168392L;
    public static final long DynamicTypeId_TimerObj_Zeze_Builtin_Timer_BSimpleTimer = 1832177636612857692L;

    public static long getSpecialTypeIdFromBean_TimerObj(Zeze.Transaction.Bean bean) {
        var _typeId_ = bean.typeId();
        if (_typeId_ == Zeze.Transaction.EmptyBean.TYPEID)
            return Zeze.Transaction.EmptyBean.TYPEID;
        if (_typeId_ == -6995089347718168392L)
            return -6995089347718168392L; // Zeze.Builtin.Timer.BCronTimer
        if (_typeId_ == 1832177636612857692L)
            return 1832177636612857692L; // Zeze.Builtin.Timer.BSimpleTimer
        throw new RuntimeException("Unknown Bean! dynamic@Zeze.Builtin.Timer.BOnlineTimer:TimerObj");
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_TimerObj(long typeId) {
        if (typeId == -6995089347718168392L)
            return new Zeze.Builtin.Timer.BCronTimer();
        if (typeId == 1832177636612857692L)
            return new Zeze.Builtin.Timer.BSimpleTimer();
        return null;
=======
    private String _HandleName;
    private final Zeze.Transaction.DynamicBean _CustomData;

    public static long GetSpecialTypeIdFromBean_CustomData(Zeze.Transaction.Bean bean) {
        return Zeze.Component.Timer.GetSpecialTypeIdFromBean(bean);
    }

    public static Zeze.Transaction.Bean CreateBeanFromSpecialTypeId_CustomData(long typeId) {
        return Zeze.Component.Timer.CreateBeanFromSpecialTypeId(typeId);
>>>>>>> 59e80f2f (Timer Online 改成基于 Timer.Basic.):ZezeJava/ZezeJava/src/main/java/Zeze/Builtin/Timer/BArchOnlineCustom.java
    }

    public String getAccount() {
        if (!isManaged())
            return _Account;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Account;
        var log = (Log__Account)txn.GetLog(objectId() + 1);
        return log != null ? log.Value : _Account;
    }

    public void setAccount(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Account = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.PutLog(new Log__Account(this, 1, value));
    }

    public String getClientId() {
        if (!isManaged())
            return _ClientId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ClientId;
        var log = (Log__ClientId)txn.GetLog(objectId() + 2);
        return log != null ? log.Value : _ClientId;
    }

    public void setClientId(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ClientId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.PutLog(new Log__ClientId(this, 2, value));
    }

    public String getHandleName() {
        if (!isManaged())
            return _HandleName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _HandleName;
        var log = (Log__HandleName)txn.GetLog(objectId() + 3);
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
        txn.PutLog(new Log__HandleName(this, 3, value));
    }

    public Zeze.Transaction.DynamicBean getCustomData() {
        return _CustomData;
    }

    @SuppressWarnings("deprecation")
    public BArchOnlineCustom() {
        _Account = "";
        _ClientId = "";
<<<<<<< HEAD:ZezeJava/ZezeJava/src/main/java/Zeze/Builtin/Timer/BOnlineTimer.java
        _TimerObj = new Zeze.Transaction.DynamicBean(3, BOnlineTimer::getSpecialTypeIdFromBean_TimerObj, BOnlineTimer::createBeanFromSpecialTypeId_TimerObj);
=======
        _HandleName = "";
        _CustomData = new Zeze.Transaction.DynamicBean(4, Zeze.Component.Timer::GetSpecialTypeIdFromBean, Zeze.Component.Timer::CreateBeanFromSpecialTypeId);
>>>>>>> 59e80f2f (Timer Online 改成基于 Timer.Basic.):ZezeJava/ZezeJava/src/main/java/Zeze/Builtin/Timer/BArchOnlineCustom.java
    }

    @SuppressWarnings("deprecation")
    public BArchOnlineCustom(String _Account_, String _ClientId_, String _HandleName_) {
        if (_Account_ == null)
            throw new IllegalArgumentException();
        _Account = _Account_;
        if (_ClientId_ == null)
            throw new IllegalArgumentException();
        _ClientId = _ClientId_;
<<<<<<< HEAD:ZezeJava/ZezeJava/src/main/java/Zeze/Builtin/Timer/BOnlineTimer.java
        _TimerObj = new Zeze.Transaction.DynamicBean(3, BOnlineTimer::getSpecialTypeIdFromBean_TimerObj, BOnlineTimer::createBeanFromSpecialTypeId_TimerObj);
    }

    public void assign(BOnlineTimer other) {
=======
        if (_HandleName_ == null)
            throw new IllegalArgumentException();
        _HandleName = _HandleName_;
        _CustomData = new Zeze.Transaction.DynamicBean(4, Zeze.Component.Timer::GetSpecialTypeIdFromBean, Zeze.Component.Timer::CreateBeanFromSpecialTypeId);
    }

    public void Assign(BArchOnlineCustom other) {
>>>>>>> 59e80f2f (Timer Online 改成基于 Timer.Basic.):ZezeJava/ZezeJava/src/main/java/Zeze/Builtin/Timer/BArchOnlineCustom.java
        setAccount(other.getAccount());
        setClientId(other.getClientId());
        setHandleName(other.getHandleName());
        getCustomData().Assign(other.getCustomData());
    }

<<<<<<< HEAD:ZezeJava/ZezeJava/src/main/java/Zeze/Builtin/Timer/BOnlineTimer.java
    @Deprecated
    public void Assign(BOnlineTimer other) {
        assign(other);
    }

    public BOnlineTimer copyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BOnlineTimer copy() {
        var copy = new BOnlineTimer();
=======
    public BArchOnlineCustom CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BArchOnlineCustom Copy() {
        var copy = new BArchOnlineCustom();
>>>>>>> 59e80f2f (Timer Online 改成基于 Timer.Basic.):ZezeJava/ZezeJava/src/main/java/Zeze/Builtin/Timer/BArchOnlineCustom.java
        copy.Assign(this);
        return copy;
    }

<<<<<<< HEAD:ZezeJava/ZezeJava/src/main/java/Zeze/Builtin/Timer/BOnlineTimer.java
    @Deprecated
    public BOnlineTimer Copy() {
        return copy();
    }

    public static void swap(BOnlineTimer a, BOnlineTimer b) {
        BOnlineTimer save = a.Copy();
=======
    public static void Swap(BArchOnlineCustom a, BArchOnlineCustom b) {
        BArchOnlineCustom save = a.Copy();
>>>>>>> 59e80f2f (Timer Online 改成基于 Timer.Basic.):ZezeJava/ZezeJava/src/main/java/Zeze/Builtin/Timer/BArchOnlineCustom.java
        a.Assign(b);
        b.Assign(save);
    }

    @Override
<<<<<<< HEAD:ZezeJava/ZezeJava/src/main/java/Zeze/Builtin/Timer/BOnlineTimer.java
    public BOnlineTimer copyBean() {
=======
    public BArchOnlineCustom CopyBean() {
>>>>>>> 59e80f2f (Timer Online 改成基于 Timer.Basic.):ZezeJava/ZezeJava/src/main/java/Zeze/Builtin/Timer/BArchOnlineCustom.java
        return Copy();
    }

    public static final long TYPEID = 5751212207563675607L;

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Account extends Zeze.Transaction.Logs.LogString {
        public Log__Account(BArchOnlineCustom bean, int varId, String value) { super(bean, varId, value); }

        @Override
<<<<<<< HEAD:ZezeJava/ZezeJava/src/main/java/Zeze/Builtin/Timer/BOnlineTimer.java
        public void commit() { ((BOnlineTimer)getBelong())._Account = Value; }
=======
        public void Commit() { ((BArchOnlineCustom)getBelong())._Account = Value; }
>>>>>>> 59e80f2f (Timer Online 改成基于 Timer.Basic.):ZezeJava/ZezeJava/src/main/java/Zeze/Builtin/Timer/BArchOnlineCustom.java
    }

    private static final class Log__ClientId extends Zeze.Transaction.Logs.LogString {
        public Log__ClientId(BArchOnlineCustom bean, int varId, String value) { super(bean, varId, value); }

        @Override
<<<<<<< HEAD:ZezeJava/ZezeJava/src/main/java/Zeze/Builtin/Timer/BOnlineTimer.java
        public void commit() { ((BOnlineTimer)getBelong())._ClientId = Value; }
=======
        public void Commit() { ((BArchOnlineCustom)getBelong())._ClientId = Value; }
    }

    private static final class Log__HandleName extends Zeze.Transaction.Logs.LogString {
        public Log__HandleName(BArchOnlineCustom bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BArchOnlineCustom)getBelong())._HandleName = Value; }
>>>>>>> 59e80f2f (Timer Online 改成基于 Timer.Basic.):ZezeJava/ZezeJava/src/main/java/Zeze/Builtin/Timer/BArchOnlineCustom.java
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
<<<<<<< HEAD:ZezeJava/ZezeJava/src/main/java/Zeze/Builtin/Timer/BOnlineTimer.java
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Timer.BOnlineTimer: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Account").append('=').append(getAccount()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ClientId").append('=').append(getClientId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("TimerObj").append('=').append(System.lineSeparator());
        getTimerObj().getBean().buildString(sb, level + 4);
=======
    public void BuildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Timer.BArchOnlineCustom: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Account").append('=').append(getAccount()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ClientId").append('=').append(getClientId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("HandleName").append('=').append(getHandleName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("CustomData").append('=').append(System.lineSeparator());
        getCustomData().getBean().BuildString(sb, level + 4);
>>>>>>> 59e80f2f (Timer Online 改成基于 Timer.Basic.):ZezeJava/ZezeJava/src/main/java/Zeze/Builtin/Timer/BArchOnlineCustom.java
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
            String _x_ = getAccount();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getClientId();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getHandleName();
            if (!_x_.isEmpty()) {
<<<<<<< HEAD:ZezeJava/ZezeJava/src/main/java/Zeze/Builtin/Timer/BOnlineTimer.java
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.DYNAMIC);
                _x_.encode(_o_);
=======
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = getCustomData();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.DYNAMIC);
                _x_.Encode(_o_);
>>>>>>> 59e80f2f (Timer Online 改成基于 Timer.Basic.):ZezeJava/ZezeJava/src/main/java/Zeze/Builtin/Timer/BArchOnlineCustom.java
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setAccount(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setClientId(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setHandleName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _o_.ReadDynamic(getCustomData(), _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
<<<<<<< HEAD:ZezeJava/ZezeJava/src/main/java/Zeze/Builtin/Timer/BOnlineTimer.java
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _TimerObj.initRootInfo(root, this);
    }

    @Override
    protected void resetChildrenRootInfo() {
        _TimerObj.resetRootInfo();
    }

    @Override
    public boolean negativeCheck() {
        if (getTimerObj().negativeCheck())
            return true;
=======
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _CustomData.InitRootInfo(root, this);
    }

    @Override
    protected void ResetChildrenRootInfo() {
        _CustomData.ResetRootInfo();
    }

    @Override
    public boolean NegativeCheck() {
>>>>>>> 59e80f2f (Timer Online 改成基于 Timer.Basic.):ZezeJava/ZezeJava/src/main/java/Zeze/Builtin/Timer/BArchOnlineCustom.java
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
                case 1: _Account = ((Zeze.Transaction.Logs.LogString)vlog).Value; break;
                case 2: _ClientId = ((Zeze.Transaction.Logs.LogString)vlog).Value; break;
<<<<<<< HEAD:ZezeJava/ZezeJava/src/main/java/Zeze/Builtin/Timer/BOnlineTimer.java
                case 3: _TimerObj.followerApply(vlog); break;
=======
                case 3: _HandleName = ((Zeze.Transaction.Logs.LogString)vlog).Value; break;
                case 4: _CustomData.FollowerApply(vlog); break;
>>>>>>> 59e80f2f (Timer Online 改成基于 Timer.Basic.):ZezeJava/ZezeJava/src/main/java/Zeze/Builtin/Timer/BArchOnlineCustom.java
            }
        }
    }
}
