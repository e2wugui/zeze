// auto-generated @formatter:off
package Zeze.Builtin.ProviderDirect;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BModuleRedirectResult extends Zeze.Transaction.Bean implements BModuleRedirectResultReadOnly {
    public static final long TYPEID = 6325051164605397555L;

    private int _ModuleId;
    private int _ServerId; // 目标server的id。
    private Zeze.Net.Binary _Params;

    @Override
    public int getModuleId() {
        if (!isManaged())
            return _ModuleId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ModuleId;
        var log = (Log__ModuleId)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _ModuleId;
    }

    public void setModuleId(int _v_) {
        if (!isManaged()) {
            _ModuleId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__ModuleId(this, 1, _v_));
    }

    @Override
    public int getServerId() {
        if (!isManaged())
            return _ServerId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ServerId;
        var log = (Log__ServerId)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _ServerId;
    }

    public void setServerId(int _v_) {
        if (!isManaged()) {
            _ServerId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__ServerId(this, 2, _v_));
    }

    @Override
    public Zeze.Net.Binary getParams() {
        if (!isManaged())
            return _Params;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Params;
        var log = (Log__Params)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _Params;
    }

    public void setParams(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Params = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__Params(this, 3, _v_));
    }

    @SuppressWarnings("deprecation")
    public BModuleRedirectResult() {
        _Params = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BModuleRedirectResult(int _ModuleId_, int _ServerId_, Zeze.Net.Binary _Params_) {
        _ModuleId = _ModuleId_;
        _ServerId = _ServerId_;
        if (_Params_ == null)
            _Params_ = Zeze.Net.Binary.Empty;
        _Params = _Params_;
    }

    @Override
    public void reset() {
        setModuleId(0);
        setServerId(0);
        setParams(Zeze.Net.Binary.Empty);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.ProviderDirect.BModuleRedirectResult.Data toData() {
        var _d_ = new Zeze.Builtin.ProviderDirect.BModuleRedirectResult.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.ProviderDirect.BModuleRedirectResult.Data)_o_);
    }

    public void assign(BModuleRedirectResult.Data _o_) {
        setModuleId(_o_._ModuleId);
        setServerId(_o_._ServerId);
        setParams(_o_._Params);
        _unknown_ = null;
    }

    public void assign(BModuleRedirectResult _o_) {
        setModuleId(_o_.getModuleId());
        setServerId(_o_.getServerId());
        setParams(_o_.getParams());
        _unknown_ = _o_._unknown_;
    }

    public BModuleRedirectResult copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BModuleRedirectResult copy() {
        var _c_ = new BModuleRedirectResult();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BModuleRedirectResult _a_, BModuleRedirectResult _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ModuleId extends Zeze.Transaction.Logs.LogInt {
        public Log__ModuleId(BModuleRedirectResult _b_, int _i_, int _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BModuleRedirectResult)getBelong())._ModuleId = value; }
    }

    private static final class Log__ServerId extends Zeze.Transaction.Logs.LogInt {
        public Log__ServerId(BModuleRedirectResult _b_, int _i_, int _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BModuleRedirectResult)getBelong())._ServerId = value; }
    }

    private static final class Log__Params extends Zeze.Transaction.Logs.LogBinary {
        public Log__Params(BModuleRedirectResult _b_, int _i_, Zeze.Net.Binary _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BModuleRedirectResult)getBelong())._Params = value; }
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
        _s_.append("Zeze.Builtin.ProviderDirect.BModuleRedirectResult: {\n");
        _s_.append(_i1_).append("ModuleId=").append(getModuleId()).append(",\n");
        _s_.append(_i1_).append("ServerId=").append(getServerId()).append(",\n");
        _s_.append(_i1_).append("Params=").append(getParams()).append('\n');
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
            int _x_ = getModuleId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getServerId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = getParams();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
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
            setModuleId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setServerId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setParams(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BModuleRedirectResult))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BModuleRedirectResult)_o_;
        if (getModuleId() != _b_.getModuleId())
            return false;
        if (getServerId() != _b_.getServerId())
            return false;
        if (!getParams().equals(_b_.getParams()))
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getModuleId() < 0)
            return true;
        if (getServerId() < 0)
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
                case 1: _ModuleId = _v_.intValue(); break;
                case 2: _ServerId = _v_.intValue(); break;
                case 3: _Params = _v_.binaryValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setModuleId(_r_.getInt(_pn_ + "ModuleId"));
        setServerId(_r_.getInt(_pn_ + "ServerId"));
        setParams(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "Params")));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendInt(_pn_ + "ModuleId", getModuleId());
        _s_.appendInt(_pn_ + "ServerId", getServerId());
        _s_.appendBinary(_pn_ + "Params", getParams());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "ModuleId", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "ServerId", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Params", "binary", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 6325051164605397555L;

    private int _ModuleId;
    private int _ServerId; // 目标server的id。
    private Zeze.Net.Binary _Params;

    public int getModuleId() {
        return _ModuleId;
    }

    public void setModuleId(int _v_) {
        _ModuleId = _v_;
    }

    public int getServerId() {
        return _ServerId;
    }

    public void setServerId(int _v_) {
        _ServerId = _v_;
    }

    public Zeze.Net.Binary getParams() {
        return _Params;
    }

    public void setParams(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Params = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Params = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public Data(int _ModuleId_, int _ServerId_, Zeze.Net.Binary _Params_) {
        _ModuleId = _ModuleId_;
        _ServerId = _ServerId_;
        if (_Params_ == null)
            _Params_ = Zeze.Net.Binary.Empty;
        _Params = _Params_;
    }

    @Override
    public void reset() {
        _ModuleId = 0;
        _ServerId = 0;
        _Params = Zeze.Net.Binary.Empty;
    }

    @Override
    public Zeze.Builtin.ProviderDirect.BModuleRedirectResult toBean() {
        var _b_ = new Zeze.Builtin.ProviderDirect.BModuleRedirectResult();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BModuleRedirectResult)_o_);
    }

    public void assign(BModuleRedirectResult _o_) {
        _ModuleId = _o_.getModuleId();
        _ServerId = _o_.getServerId();
        _Params = _o_.getParams();
    }

    public void assign(BModuleRedirectResult.Data _o_) {
        _ModuleId = _o_._ModuleId;
        _ServerId = _o_._ServerId;
        _Params = _o_._Params;
    }

    @Override
    public BModuleRedirectResult.Data copy() {
        var _c_ = new BModuleRedirectResult.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BModuleRedirectResult.Data _a_, BModuleRedirectResult.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BModuleRedirectResult.Data clone() {
        return (BModuleRedirectResult.Data)super.clone();
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
        _s_.append("Zeze.Builtin.ProviderDirect.BModuleRedirectResult: {\n");
        _s_.append(_i1_).append("ModuleId=").append(_ModuleId).append(",\n");
        _s_.append(_i1_).append("ServerId=").append(_ServerId).append(",\n");
        _s_.append(_i1_).append("Params=").append(_Params).append('\n');
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
            int _x_ = _ModuleId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = _ServerId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = _Params;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _ModuleId = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _ServerId = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _Params = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
