// auto-generated @formatter:off
package Zeze.Builtin.LoginQueueServer;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BSecret extends Zeze.Transaction.Bean implements BSecretReadOnly {
    public static final long TYPEID = -1086547097778874201L;

    private Zeze.Net.Binary _SecretKey; // 16bytes
    private Zeze.Net.Binary _SecretIv; // 16bytes

    private static final java.lang.invoke.VarHandle vh_SecretKey;
    private static final java.lang.invoke.VarHandle vh_SecretIv;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_SecretKey = _l_.findVarHandle(BSecret.class, "_SecretKey", Zeze.Net.Binary.class);
            vh_SecretIv = _l_.findVarHandle(BSecret.class, "_SecretIv", Zeze.Net.Binary.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public Zeze.Net.Binary getSecretKey() {
        if (!isManaged())
            return _SecretKey;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _SecretKey;
        var log = (Zeze.Transaction.Logs.LogBinary)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _SecretKey;
    }

    public void setSecretKey(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _SecretKey = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBinary(this, 1, vh_SecretKey, _v_));
    }

    @Override
    public Zeze.Net.Binary getSecretIv() {
        if (!isManaged())
            return _SecretIv;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _SecretIv;
        var log = (Zeze.Transaction.Logs.LogBinary)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _SecretIv;
    }

    public void setSecretIv(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _SecretIv = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBinary(this, 2, vh_SecretIv, _v_));
    }

    @SuppressWarnings("deprecation")
    public BSecret() {
        _SecretKey = Zeze.Net.Binary.Empty;
        _SecretIv = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BSecret(Zeze.Net.Binary _SecretKey_, Zeze.Net.Binary _SecretIv_) {
        if (_SecretKey_ == null)
            _SecretKey_ = Zeze.Net.Binary.Empty;
        _SecretKey = _SecretKey_;
        if (_SecretIv_ == null)
            _SecretIv_ = Zeze.Net.Binary.Empty;
        _SecretIv = _SecretIv_;
    }

    @Override
    public void reset() {
        setSecretKey(Zeze.Net.Binary.Empty);
        setSecretIv(Zeze.Net.Binary.Empty);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.LoginQueueServer.BSecret.Data toData() {
        var _d_ = new Zeze.Builtin.LoginQueueServer.BSecret.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.LoginQueueServer.BSecret.Data)_o_);
    }

    public void assign(BSecret.Data _o_) {
        setSecretKey(_o_._SecretKey);
        setSecretIv(_o_._SecretIv);
        _unknown_ = null;
    }

    public void assign(BSecret _o_) {
        setSecretKey(_o_.getSecretKey());
        setSecretIv(_o_.getSecretIv());
        _unknown_ = _o_._unknown_;
    }

    public BSecret copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BSecret copy() {
        var _c_ = new BSecret();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BSecret _a_, BSecret _b_) {
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
        _s_.append("Zeze.Builtin.LoginQueueServer.BSecret: {\n");
        _s_.append(_i1_).append("SecretKey=").append(getSecretKey()).append(",\n");
        _s_.append(_i1_).append("SecretIv=").append(getSecretIv()).append('\n');
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
            var _x_ = getSecretKey();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            var _x_ = getSecretIv();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
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
            setSecretKey(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setSecretIv(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BSecret))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BSecret)_o_;
        if (!getSecretKey().equals(_b_.getSecretKey()))
            return false;
        if (!getSecretIv().equals(_b_.getSecretIv()))
            return false;
        return true;
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
                case 1: _SecretKey = _v_.binaryValue(); break;
                case 2: _SecretIv = _v_.binaryValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setSecretKey(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "SecretKey")));
        setSecretIv(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "SecretIv")));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendBinary(_pn_ + "SecretKey", getSecretKey());
        _s_.appendBinary(_pn_ + "SecretIv", getSecretIv());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "SecretKey", "binary", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "SecretIv", "binary", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -1086547097778874201L;

    private Zeze.Net.Binary _SecretKey; // 16bytes
    private Zeze.Net.Binary _SecretIv; // 16bytes

    public Zeze.Net.Binary getSecretKey() {
        return _SecretKey;
    }

    public void setSecretKey(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _SecretKey = _v_;
    }

    public Zeze.Net.Binary getSecretIv() {
        return _SecretIv;
    }

    public void setSecretIv(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _SecretIv = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _SecretKey = Zeze.Net.Binary.Empty;
        _SecretIv = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public Data(Zeze.Net.Binary _SecretKey_, Zeze.Net.Binary _SecretIv_) {
        if (_SecretKey_ == null)
            _SecretKey_ = Zeze.Net.Binary.Empty;
        _SecretKey = _SecretKey_;
        if (_SecretIv_ == null)
            _SecretIv_ = Zeze.Net.Binary.Empty;
        _SecretIv = _SecretIv_;
    }

    @Override
    public void reset() {
        _SecretKey = Zeze.Net.Binary.Empty;
        _SecretIv = Zeze.Net.Binary.Empty;
    }

    @Override
    public Zeze.Builtin.LoginQueueServer.BSecret toBean() {
        var _b_ = new Zeze.Builtin.LoginQueueServer.BSecret();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BSecret)_o_);
    }

    public void assign(BSecret _o_) {
        _SecretKey = _o_.getSecretKey();
        _SecretIv = _o_.getSecretIv();
    }

    public void assign(BSecret.Data _o_) {
        _SecretKey = _o_._SecretKey;
        _SecretIv = _o_._SecretIv;
    }

    @Override
    public BSecret.Data copy() {
        var _c_ = new BSecret.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BSecret.Data _a_, BSecret.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BSecret.Data clone() {
        return (BSecret.Data)super.clone();
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
        _s_.append("Zeze.Builtin.LoginQueueServer.BSecret: {\n");
        _s_.append(_i1_).append("SecretKey=").append(_SecretKey).append(",\n");
        _s_.append(_i1_).append("SecretIv=").append(_SecretIv).append('\n');
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
            var _x_ = _SecretKey;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            var _x_ = _SecretIv;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
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
            _SecretKey = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _SecretIv = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BSecret.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BSecret.Data)_o_;
        if (!_SecretKey.equals(_b_._SecretKey))
            return false;
        if (!_SecretIv.equals(_b_._SecretIv))
            return false;
        return true;
    }
}
}
