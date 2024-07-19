// auto-generated @formatter:off
package Zeze.Builtin.GlobalCacheManagerWithRaft;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BLoginParam extends Zeze.Transaction.Bean implements BLoginParamReadOnly {
    public static final long TYPEID = 9076855952725286109L;

    private int _ServerId;
    private int _GlobalCacheManagerHashIndex;
    private boolean _DebugMode; // 调试模式下不检查Release Timeout,方便单步调试

    @Override
    public int getServerId() {
        if (!isManaged())
            return _ServerId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ServerId;
        var log = (Log__ServerId)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _ServerId;
    }

    public void setServerId(int _v_) {
        if (!isManaged()) {
            _ServerId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__ServerId(this, 1, _v_));
    }

    @Override
    public int getGlobalCacheManagerHashIndex() {
        if (!isManaged())
            return _GlobalCacheManagerHashIndex;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _GlobalCacheManagerHashIndex;
        var log = (Log__GlobalCacheManagerHashIndex)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _GlobalCacheManagerHashIndex;
    }

    public void setGlobalCacheManagerHashIndex(int _v_) {
        if (!isManaged()) {
            _GlobalCacheManagerHashIndex = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__GlobalCacheManagerHashIndex(this, 2, _v_));
    }

    @Override
    public boolean isDebugMode() {
        if (!isManaged())
            return _DebugMode;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _DebugMode;
        var log = (Log__DebugMode)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _DebugMode;
    }

    public void setDebugMode(boolean _v_) {
        if (!isManaged()) {
            _DebugMode = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__DebugMode(this, 3, _v_));
    }

    @SuppressWarnings("deprecation")
    public BLoginParam() {
    }

    @SuppressWarnings("deprecation")
    public BLoginParam(int _ServerId_, int _GlobalCacheManagerHashIndex_, boolean _DebugMode_) {
        _ServerId = _ServerId_;
        _GlobalCacheManagerHashIndex = _GlobalCacheManagerHashIndex_;
        _DebugMode = _DebugMode_;
    }

    @Override
    public void reset() {
        setServerId(0);
        setGlobalCacheManagerHashIndex(0);
        setDebugMode(false);
        _unknown_ = null;
    }

    public void assign(BLoginParam _o_) {
        setServerId(_o_.getServerId());
        setGlobalCacheManagerHashIndex(_o_.getGlobalCacheManagerHashIndex());
        setDebugMode(_o_.isDebugMode());
        _unknown_ = _o_._unknown_;
    }

    public BLoginParam copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BLoginParam copy() {
        var _c_ = new BLoginParam();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BLoginParam _a_, BLoginParam _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ServerId extends Zeze.Transaction.Logs.LogInt {
        public Log__ServerId(BLoginParam _b_, int _i_, int _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BLoginParam)getBelong())._ServerId = value; }
    }

    private static final class Log__GlobalCacheManagerHashIndex extends Zeze.Transaction.Logs.LogInt {
        public Log__GlobalCacheManagerHashIndex(BLoginParam _b_, int _i_, int _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BLoginParam)getBelong())._GlobalCacheManagerHashIndex = value; }
    }

    private static final class Log__DebugMode extends Zeze.Transaction.Logs.LogBool {
        public Log__DebugMode(BLoginParam _b_, int _i_, boolean _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BLoginParam)getBelong())._DebugMode = value; }
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Zeze.Builtin.GlobalCacheManagerWithRaft.BLoginParam: {").append(System.lineSeparator());
        _l_ += 4;
        _s_.append(Zeze.Util.Str.indent(_l_)).append("ServerId=").append(getServerId()).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("GlobalCacheManagerHashIndex=").append(getGlobalCacheManagerHashIndex()).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("DebugMode=").append(isDebugMode()).append(System.lineSeparator());
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
            int _x_ = getServerId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getGlobalCacheManagerHashIndex();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            boolean _x_ = isDebugMode();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
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
            setServerId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setGlobalCacheManagerHashIndex(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setDebugMode(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BLoginParam))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BLoginParam)_o_;
        if (getServerId() != _b_.getServerId())
            return false;
        if (getGlobalCacheManagerHashIndex() != _b_.getGlobalCacheManagerHashIndex())
            return false;
        if (isDebugMode() != _b_.isDebugMode())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getServerId() < 0)
            return true;
        if (getGlobalCacheManagerHashIndex() < 0)
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
                case 1: _ServerId = _v_.intValue(); break;
                case 2: _GlobalCacheManagerHashIndex = _v_.intValue(); break;
                case 3: _DebugMode = _v_.booleanValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setServerId(_r_.getInt(_pn_ + "ServerId"));
        setGlobalCacheManagerHashIndex(_r_.getInt(_pn_ + "GlobalCacheManagerHashIndex"));
        setDebugMode(_r_.getBoolean(_pn_ + "DebugMode"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendInt(_pn_ + "ServerId", getServerId());
        _s_.appendInt(_pn_ + "GlobalCacheManagerHashIndex", getGlobalCacheManagerHashIndex());
        _s_.appendBoolean(_pn_ + "DebugMode", isDebugMode());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "ServerId", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "GlobalCacheManagerHashIndex", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "DebugMode", "bool", "", ""));
        return _v_;
    }
}
