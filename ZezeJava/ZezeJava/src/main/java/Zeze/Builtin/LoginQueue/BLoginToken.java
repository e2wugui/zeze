// auto-generated @formatter:off
package Zeze.Builtin.LoginQueue;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BLoginToken extends Zeze.Transaction.Bean implements BLoginTokenReadOnly {
    public static final long TYPEID = 4624437118588347002L;

    private Zeze.Net.Binary _Token;
    private String _LinkIp;
    private int _LinkPort;

    private static final java.lang.invoke.VarHandle vh_Token;
    private static final java.lang.invoke.VarHandle vh_LinkIp;
    private static final java.lang.invoke.VarHandle vh_LinkPort;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_Token = _l_.findVarHandle(BLoginToken.class, "_Token", Zeze.Net.Binary.class);
            vh_LinkIp = _l_.findVarHandle(BLoginToken.class, "_LinkIp", String.class);
            vh_LinkPort = _l_.findVarHandle(BLoginToken.class, "_LinkPort", int.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public Zeze.Net.Binary getToken() {
        if (!isManaged())
            return _Token;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Token;
        var log = (Zeze.Transaction.Logs.LogBinary)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _Token;
    }

    public void setToken(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Token = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBinary(this, 1, vh_Token, _v_));
    }

    @Override
    public String getLinkIp() {
        if (!isManaged())
            return _LinkIp;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _LinkIp;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 2);
        return log != null ? log.stringValue() : _LinkIp;
    }

    public void setLinkIp(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _LinkIp = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 2, vh_LinkIp, _v_));
    }

    @Override
    public int getLinkPort() {
        if (!isManaged())
            return _LinkPort;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _LinkPort;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _LinkPort;
    }

    public void setLinkPort(int _v_) {
        if (!isManaged()) {
            _LinkPort = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 3, vh_LinkPort, _v_));
    }

    @SuppressWarnings("deprecation")
    public BLoginToken() {
        _Token = Zeze.Net.Binary.Empty;
        _LinkIp = "";
    }

    @SuppressWarnings("deprecation")
    public BLoginToken(Zeze.Net.Binary _Token_, String _LinkIp_, int _LinkPort_) {
        if (_Token_ == null)
            _Token_ = Zeze.Net.Binary.Empty;
        _Token = _Token_;
        if (_LinkIp_ == null)
            _LinkIp_ = "";
        _LinkIp = _LinkIp_;
        _LinkPort = _LinkPort_;
    }

    @Override
    public void reset() {
        setToken(Zeze.Net.Binary.Empty);
        setLinkIp("");
        setLinkPort(0);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.LoginQueue.BLoginToken.Data toData() {
        var _d_ = new Zeze.Builtin.LoginQueue.BLoginToken.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.LoginQueue.BLoginToken.Data)_o_);
    }

    public void assign(BLoginToken.Data _o_) {
        setToken(_o_._Token);
        setLinkIp(_o_._LinkIp);
        setLinkPort(_o_._LinkPort);
        _unknown_ = null;
    }

    public void assign(BLoginToken _o_) {
        setToken(_o_.getToken());
        setLinkIp(_o_.getLinkIp());
        setLinkPort(_o_.getLinkPort());
        _unknown_ = _o_._unknown_;
    }

    public BLoginToken copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BLoginToken copy() {
        var _c_ = new BLoginToken();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BLoginToken _a_, BLoginToken _b_) {
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
        _s_.append("Zeze.Builtin.LoginQueue.BLoginToken: {\n");
        _s_.append(_i1_).append("Token=").append(getToken()).append(",\n");
        _s_.append(_i1_).append("LinkIp=").append(getLinkIp()).append(",\n");
        _s_.append(_i1_).append("LinkPort=").append(getLinkPort()).append('\n');
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
            var _x_ = getToken();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            String _x_ = getLinkIp();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            int _x_ = getLinkPort();
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
            setToken(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setLinkIp(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setLinkPort(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BLoginToken))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BLoginToken)_o_;
        if (!getToken().equals(_b_.getToken()))
            return false;
        if (!getLinkIp().equals(_b_.getLinkIp()))
            return false;
        if (getLinkPort() != _b_.getLinkPort())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getLinkPort() < 0)
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
                case 1: _Token = _v_.binaryValue(); break;
                case 2: _LinkIp = _v_.stringValue(); break;
                case 3: _LinkPort = _v_.intValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setToken(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "Token")));
        setLinkIp(_r_.getString(_pn_ + "LinkIp"));
        if (getLinkIp() == null)
            setLinkIp("");
        setLinkPort(_r_.getInt(_pn_ + "LinkPort"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendBinary(_pn_ + "Token", getToken());
        _s_.appendString(_pn_ + "LinkIp", getLinkIp());
        _s_.appendInt(_pn_ + "LinkPort", getLinkPort());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Token", "binary", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "LinkIp", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "LinkPort", "int", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 4624437118588347002L;

    private Zeze.Net.Binary _Token;
    private String _LinkIp;
    private int _LinkPort;

    public Zeze.Net.Binary getToken() {
        return _Token;
    }

    public void setToken(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Token = _v_;
    }

    public String getLinkIp() {
        return _LinkIp;
    }

    public void setLinkIp(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _LinkIp = _v_;
    }

    public int getLinkPort() {
        return _LinkPort;
    }

    public void setLinkPort(int _v_) {
        _LinkPort = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Token = Zeze.Net.Binary.Empty;
        _LinkIp = "";
    }

    @SuppressWarnings("deprecation")
    public Data(Zeze.Net.Binary _Token_, String _LinkIp_, int _LinkPort_) {
        if (_Token_ == null)
            _Token_ = Zeze.Net.Binary.Empty;
        _Token = _Token_;
        if (_LinkIp_ == null)
            _LinkIp_ = "";
        _LinkIp = _LinkIp_;
        _LinkPort = _LinkPort_;
    }

    @Override
    public void reset() {
        _Token = Zeze.Net.Binary.Empty;
        _LinkIp = "";
        _LinkPort = 0;
    }

    @Override
    public Zeze.Builtin.LoginQueue.BLoginToken toBean() {
        var _b_ = new Zeze.Builtin.LoginQueue.BLoginToken();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BLoginToken)_o_);
    }

    public void assign(BLoginToken _o_) {
        _Token = _o_.getToken();
        _LinkIp = _o_.getLinkIp();
        _LinkPort = _o_.getLinkPort();
    }

    public void assign(BLoginToken.Data _o_) {
        _Token = _o_._Token;
        _LinkIp = _o_._LinkIp;
        _LinkPort = _o_._LinkPort;
    }

    @Override
    public BLoginToken.Data copy() {
        var _c_ = new BLoginToken.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BLoginToken.Data _a_, BLoginToken.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BLoginToken.Data clone() {
        return (BLoginToken.Data)super.clone();
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
        _s_.append("Zeze.Builtin.LoginQueue.BLoginToken: {\n");
        _s_.append(_i1_).append("Token=").append(_Token).append(",\n");
        _s_.append(_i1_).append("LinkIp=").append(_LinkIp).append(",\n");
        _s_.append(_i1_).append("LinkPort=").append(_LinkPort).append('\n');
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
            var _x_ = _Token;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            String _x_ = _LinkIp;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            int _x_ = _LinkPort;
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
            _Token = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _LinkIp = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _LinkPort = _o_.ReadInt(_t_);
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
        if (!(_o_ instanceof BLoginToken.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BLoginToken.Data)_o_;
        if (!_Token.equals(_b_._Token))
            return false;
        if (!_LinkIp.equals(_b_._LinkIp))
            return false;
        if (_LinkPort != _b_._LinkPort)
            return false;
        return true;
    }
}
}
