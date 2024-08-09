// auto-generated @formatter:off
package Zeze.Builtin.Threading;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BMutex extends Zeze.Transaction.Bean implements BMutexReadOnly {
    public static final long TYPEID = 7224301299276482451L;

    private Zeze.Builtin.Threading.BLockName _LockName;
    private int _TimeoutMs;

    private static final java.lang.invoke.VarHandle vh_LockName;
    private static final java.lang.invoke.VarHandle vh_TimeoutMs;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_LockName = _l_.findVarHandle(BMutex.class, "_LockName", Zeze.Builtin.Threading.BLockName.class);
            vh_TimeoutMs = _l_.findVarHandle(BMutex.class, "_TimeoutMs", int.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public Zeze.Builtin.Threading.BLockName getLockName() {
        if (!isManaged())
            return _LockName;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _LockName;
        @SuppressWarnings("unchecked")
        var log = (Zeze.Transaction.Logs.LogBeanKey<Zeze.Builtin.Threading.BLockName>)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _LockName;
    }

    public void setLockName(Zeze.Builtin.Threading.BLockName _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _LockName = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBeanKey<>(this, 1, vh_LockName, _v_));
    }

    @Override
    public int getTimeoutMs() {
        if (!isManaged())
            return _TimeoutMs;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _TimeoutMs;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _TimeoutMs;
    }

    public void setTimeoutMs(int _v_) {
        if (!isManaged()) {
            _TimeoutMs = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 2, vh_TimeoutMs, _v_));
    }

    @SuppressWarnings("deprecation")
    public BMutex() {
        _LockName = new Zeze.Builtin.Threading.BLockName();
    }

    @SuppressWarnings("deprecation")
    public BMutex(Zeze.Builtin.Threading.BLockName _LockName_, int _TimeoutMs_) {
        if (_LockName_ == null)
            _LockName_ = new Zeze.Builtin.Threading.BLockName();
        _LockName = _LockName_;
        _TimeoutMs = _TimeoutMs_;
    }

    @Override
    public void reset() {
        setLockName(new Zeze.Builtin.Threading.BLockName());
        setTimeoutMs(0);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Threading.BMutex.Data toData() {
        var _d_ = new Zeze.Builtin.Threading.BMutex.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Threading.BMutex.Data)_o_);
    }

    public void assign(BMutex.Data _o_) {
        setLockName(_o_._LockName);
        setTimeoutMs(_o_._TimeoutMs);
        _unknown_ = null;
    }

    public void assign(BMutex _o_) {
        setLockName(_o_.getLockName());
        setTimeoutMs(_o_.getTimeoutMs());
        _unknown_ = _o_._unknown_;
    }

    public BMutex copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BMutex copy() {
        var _c_ = new BMutex();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BMutex _a_, BMutex _b_) {
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
        _s_.append("Zeze.Builtin.Threading.BMutex: {\n");
        _s_.append(_i1_).append("LockName=");
        getLockName().buildString(_s_, _l_ + 8);
        _s_.append(",\n");
        _s_.append(_i1_).append("TimeoutMs=").append(getTimeoutMs()).append('\n');
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
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 1, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            getLockName().encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        {
            int _x_ = getTimeoutMs();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
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
            _o_.ReadBean(getLockName(), _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setTimeoutMs(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BMutex))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BMutex)_o_;
        if (!getLockName().equals(_b_.getLockName()))
            return false;
        if (getTimeoutMs() != _b_.getTimeoutMs())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getLockName().negativeCheck())
            return true;
        if (getTimeoutMs() < 0)
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
                case 1: _LockName = ((Zeze.Transaction.Logs.LogBeanKey<Zeze.Builtin.Threading.BLockName>)_v_).value; break;
                case 2: _TimeoutMs = _v_.intValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        _p_.add("LockName");
        getLockName().decodeResultSet(_p_, _r_);
        _p_.remove(_p_.size() - 1);
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setTimeoutMs(_r_.getInt(_pn_ + "TimeoutMs"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        _p_.add("LockName");
        getLockName().encodeSQLStatement(_p_, _s_);
        _p_.remove(_p_.size() - 1);
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendInt(_pn_ + "TimeoutMs", getTimeoutMs());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "LockName", "Zeze.Builtin.Threading.BLockName", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "TimeoutMs", "int", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 7224301299276482451L;

    private Zeze.Builtin.Threading.BLockName _LockName;
    private int _TimeoutMs;

    public Zeze.Builtin.Threading.BLockName getLockName() {
        return _LockName;
    }

    public void setLockName(Zeze.Builtin.Threading.BLockName _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _LockName = _v_;
    }

    public int getTimeoutMs() {
        return _TimeoutMs;
    }

    public void setTimeoutMs(int _v_) {
        _TimeoutMs = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _LockName = new Zeze.Builtin.Threading.BLockName();
    }

    @SuppressWarnings("deprecation")
    public Data(Zeze.Builtin.Threading.BLockName _LockName_, int _TimeoutMs_) {
        if (_LockName_ == null)
            _LockName_ = new Zeze.Builtin.Threading.BLockName();
        _LockName = _LockName_;
        _TimeoutMs = _TimeoutMs_;
    }

    @Override
    public void reset() {
        _LockName = new Zeze.Builtin.Threading.BLockName();
        _TimeoutMs = 0;
    }

    @Override
    public Zeze.Builtin.Threading.BMutex toBean() {
        var _b_ = new Zeze.Builtin.Threading.BMutex();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BMutex)_o_);
    }

    public void assign(BMutex _o_) {
        _LockName = _o_.getLockName();
        _TimeoutMs = _o_.getTimeoutMs();
    }

    public void assign(BMutex.Data _o_) {
        _LockName = _o_._LockName;
        _TimeoutMs = _o_._TimeoutMs;
    }

    @Override
    public BMutex.Data copy() {
        var _c_ = new BMutex.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BMutex.Data _a_, BMutex.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BMutex.Data clone() {
        return (BMutex.Data)super.clone();
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
        _s_.append("Zeze.Builtin.Threading.BMutex: {\n");
        _s_.append(_i1_).append("LockName=");
        _LockName.buildString(_s_, _l_ + 8);
        _s_.append(",\n");
        _s_.append(_i1_).append("TimeoutMs=").append(_TimeoutMs).append('\n');
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
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 1, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _LockName.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        {
            int _x_ = _TimeoutMs;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
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
            _o_.ReadBean(_LockName, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _TimeoutMs = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
