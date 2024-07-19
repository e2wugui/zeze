// auto-generated @formatter:off
package Zeze.Builtin.Threading;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BReadWriteLock extends Zeze.Transaction.Bean implements BReadWriteLockReadOnly {
    public static final long TYPEID = 5310988726582781550L;

    private Zeze.Builtin.Threading.BLockName _LockName;
    private int _OperateType; // see enum above
    private int _TimeoutMs; // 部分操作实际上没有使用这个参数

    @Override
    public Zeze.Builtin.Threading.BLockName getLockName() {
        if (!isManaged())
            return _LockName;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _LockName;
        var log = (Log__LockName)_t_.getLog(objectId() + 1);
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
        _t_.putLog(new Log__LockName(this, 1, _v_));
    }

    @Override
    public int getOperateType() {
        if (!isManaged())
            return _OperateType;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _OperateType;
        var log = (Log__OperateType)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _OperateType;
    }

    public void setOperateType(int _v_) {
        if (!isManaged()) {
            _OperateType = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__OperateType(this, 2, _v_));
    }

    @Override
    public int getTimeoutMs() {
        if (!isManaged())
            return _TimeoutMs;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _TimeoutMs;
        var log = (Log__TimeoutMs)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _TimeoutMs;
    }

    public void setTimeoutMs(int _v_) {
        if (!isManaged()) {
            _TimeoutMs = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__TimeoutMs(this, 3, _v_));
    }

    @SuppressWarnings("deprecation")
    public BReadWriteLock() {
        _LockName = new Zeze.Builtin.Threading.BLockName();
    }

    @SuppressWarnings("deprecation")
    public BReadWriteLock(Zeze.Builtin.Threading.BLockName _LockName_, int _OperateType_, int _TimeoutMs_) {
        if (_LockName_ == null)
            _LockName_ = new Zeze.Builtin.Threading.BLockName();
        _LockName = _LockName_;
        _OperateType = _OperateType_;
        _TimeoutMs = _TimeoutMs_;
    }

    @Override
    public void reset() {
        setLockName(new Zeze.Builtin.Threading.BLockName());
        setOperateType(0);
        setTimeoutMs(0);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Threading.BReadWriteLock.Data toData() {
        var _d_ = new Zeze.Builtin.Threading.BReadWriteLock.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Threading.BReadWriteLock.Data)_o_);
    }

    public void assign(BReadWriteLock.Data _o_) {
        setLockName(_o_._LockName);
        setOperateType(_o_._OperateType);
        setTimeoutMs(_o_._TimeoutMs);
        _unknown_ = null;
    }

    public void assign(BReadWriteLock _o_) {
        setLockName(_o_.getLockName());
        setOperateType(_o_.getOperateType());
        setTimeoutMs(_o_.getTimeoutMs());
        _unknown_ = _o_._unknown_;
    }

    public BReadWriteLock copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BReadWriteLock copy() {
        var _c_ = new BReadWriteLock();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BReadWriteLock _a_, BReadWriteLock _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__LockName extends Zeze.Transaction.Logs.LogBeanKey<Zeze.Builtin.Threading.BLockName> {
        public Log__LockName(BReadWriteLock _b_, int _i_, Zeze.Builtin.Threading.BLockName _v_) { super(Zeze.Builtin.Threading.BLockName.class, _b_, _i_, _v_); }

        @Override
        public void commit() { ((BReadWriteLock)getBelong())._LockName = value; }
    }

    private static final class Log__OperateType extends Zeze.Transaction.Logs.LogInt {
        public Log__OperateType(BReadWriteLock _b_, int _i_, int _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BReadWriteLock)getBelong())._OperateType = value; }
    }

    private static final class Log__TimeoutMs extends Zeze.Transaction.Logs.LogInt {
        public Log__TimeoutMs(BReadWriteLock _b_, int _i_, int _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BReadWriteLock)getBelong())._TimeoutMs = value; }
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Zeze.Builtin.Threading.BReadWriteLock: {").append(System.lineSeparator());
        _l_ += 4;
        _s_.append(Zeze.Util.Str.indent(_l_)).append("LockName=").append(System.lineSeparator());
        getLockName().buildString(_s_, _l_ + 4);
        _s_.append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("OperateType=").append(getOperateType()).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("TimeoutMs=").append(getTimeoutMs()).append(System.lineSeparator());
        _l_ -= 4;
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
            int _x_ = getOperateType();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getTimeoutMs();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
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
            setOperateType(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
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
        if (!(_o_ instanceof BReadWriteLock))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BReadWriteLock)_o_;
        if (!getLockName().equals(_b_.getLockName()))
            return false;
        if (getOperateType() != _b_.getOperateType())
            return false;
        if (getTimeoutMs() != _b_.getTimeoutMs())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getLockName().negativeCheck())
            return true;
        if (getOperateType() < 0)
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
                case 2: _OperateType = _v_.intValue(); break;
                case 3: _TimeoutMs = _v_.intValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        _p_.add("LockName");
        getLockName().decodeResultSet(_p_, _r_);
        _p_.remove(_p_.size() - 1);
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setOperateType(_r_.getInt(_pn_ + "OperateType"));
        setTimeoutMs(_r_.getInt(_pn_ + "TimeoutMs"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        _p_.add("LockName");
        getLockName().encodeSQLStatement(_p_, _s_);
        _p_.remove(_p_.size() - 1);
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendInt(_pn_ + "OperateType", getOperateType());
        _s_.appendInt(_pn_ + "TimeoutMs", getTimeoutMs());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "LockName", "Zeze.Builtin.Threading.BLockName", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "OperateType", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "TimeoutMs", "int", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 5310988726582781550L;

    private Zeze.Builtin.Threading.BLockName _LockName;
    private int _OperateType; // see enum above
    private int _TimeoutMs; // 部分操作实际上没有使用这个参数

    public Zeze.Builtin.Threading.BLockName getLockName() {
        return _LockName;
    }

    public void setLockName(Zeze.Builtin.Threading.BLockName _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _LockName = _v_;
    }

    public int getOperateType() {
        return _OperateType;
    }

    public void setOperateType(int _v_) {
        _OperateType = _v_;
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
    public Data(Zeze.Builtin.Threading.BLockName _LockName_, int _OperateType_, int _TimeoutMs_) {
        if (_LockName_ == null)
            _LockName_ = new Zeze.Builtin.Threading.BLockName();
        _LockName = _LockName_;
        _OperateType = _OperateType_;
        _TimeoutMs = _TimeoutMs_;
    }

    @Override
    public void reset() {
        _LockName = new Zeze.Builtin.Threading.BLockName();
        _OperateType = 0;
        _TimeoutMs = 0;
    }

    @Override
    public Zeze.Builtin.Threading.BReadWriteLock toBean() {
        var _b_ = new Zeze.Builtin.Threading.BReadWriteLock();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BReadWriteLock)_o_);
    }

    public void assign(BReadWriteLock _o_) {
        _LockName = _o_.getLockName();
        _OperateType = _o_.getOperateType();
        _TimeoutMs = _o_.getTimeoutMs();
    }

    public void assign(BReadWriteLock.Data _o_) {
        _LockName = _o_._LockName;
        _OperateType = _o_._OperateType;
        _TimeoutMs = _o_._TimeoutMs;
    }

    @Override
    public BReadWriteLock.Data copy() {
        var _c_ = new BReadWriteLock.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BReadWriteLock.Data _a_, BReadWriteLock.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BReadWriteLock.Data clone() {
        return (BReadWriteLock.Data)super.clone();
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Zeze.Builtin.Threading.BReadWriteLock: {").append(System.lineSeparator());
        _l_ += 4;
        _s_.append(Zeze.Util.Str.indent(_l_)).append("LockName=").append(System.lineSeparator());
        _LockName.buildString(_s_, _l_ + 4);
        _s_.append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("OperateType=").append(_OperateType).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("TimeoutMs=").append(_TimeoutMs).append(System.lineSeparator());
        _l_ -= 4;
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
            int _x_ = _OperateType;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = _TimeoutMs;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
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
            _OperateType = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
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
