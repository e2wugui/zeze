// auto-generated @formatter:off
package Zeze.Builtin.Provider;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BUserState extends Zeze.Transaction.Bean implements BUserStateReadOnly {
    public static final long TYPEID = 5802054934505091577L;

    private String _context;
    private Zeze.Net.Binary _contextx;
    private String _onlineSetName;

    @Override
    public String getContext() {
        if (!isManaged())
            return _context;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _context;
        var log = (Log__context)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _context;
    }

    public void setContext(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _context = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__context(this, 1, _v_));
    }

    @Override
    public Zeze.Net.Binary getContextx() {
        if (!isManaged())
            return _contextx;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _contextx;
        var log = (Log__contextx)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _contextx;
    }

    public void setContextx(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _contextx = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__contextx(this, 2, _v_));
    }

    @Override
    public String getOnlineSetName() {
        if (!isManaged())
            return _onlineSetName;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _onlineSetName;
        var log = (Log__onlineSetName)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _onlineSetName;
    }

    public void setOnlineSetName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _onlineSetName = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__onlineSetName(this, 3, _v_));
    }

    @SuppressWarnings("deprecation")
    public BUserState() {
        _context = "";
        _contextx = Zeze.Net.Binary.Empty;
        _onlineSetName = "";
    }

    @SuppressWarnings("deprecation")
    public BUserState(String _context_, Zeze.Net.Binary _contextx_, String _onlineSetName_) {
        if (_context_ == null)
            _context_ = "";
        _context = _context_;
        if (_contextx_ == null)
            _contextx_ = Zeze.Net.Binary.Empty;
        _contextx = _contextx_;
        if (_onlineSetName_ == null)
            _onlineSetName_ = "";
        _onlineSetName = _onlineSetName_;
    }

    @Override
    public void reset() {
        setContext("");
        setContextx(Zeze.Net.Binary.Empty);
        setOnlineSetName("");
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Provider.BUserState.Data toData() {
        var _d_ = new Zeze.Builtin.Provider.BUserState.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Provider.BUserState.Data)_o_);
    }

    public void assign(BUserState.Data _o_) {
        setContext(_o_._context);
        setContextx(_o_._contextx);
        setOnlineSetName(_o_._onlineSetName);
        _unknown_ = null;
    }

    public void assign(BUserState _o_) {
        setContext(_o_.getContext());
        setContextx(_o_.getContextx());
        setOnlineSetName(_o_.getOnlineSetName());
        _unknown_ = _o_._unknown_;
    }

    public BUserState copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BUserState copy() {
        var _c_ = new BUserState();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BUserState _a_, BUserState _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__context extends Zeze.Transaction.Logs.LogString {
        public Log__context(BUserState _b_, int _i_, String _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BUserState)getBelong())._context = value; }
    }

    private static final class Log__contextx extends Zeze.Transaction.Logs.LogBinary {
        public Log__contextx(BUserState _b_, int _i_, Zeze.Net.Binary _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BUserState)getBelong())._contextx = value; }
    }

    private static final class Log__onlineSetName extends Zeze.Transaction.Logs.LogString {
        public Log__onlineSetName(BUserState _b_, int _i_, String _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BUserState)getBelong())._onlineSetName = value; }
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
        _s_.append("Zeze.Builtin.Provider.BUserState: {\n");
        _s_.append(_i1_).append("context=").append(getContext()).append(",\n");
        _s_.append(_i1_).append("contextx=").append(getContextx()).append(",\n");
        _s_.append(_i1_).append("onlineSetName=").append(getOnlineSetName()).append('\n');
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
            String _x_ = getContext();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = getContextx();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            String _x_ = getOnlineSetName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
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
            setContext(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setContextx(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setOnlineSetName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BUserState))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BUserState)_o_;
        if (!getContext().equals(_b_.getContext()))
            return false;
        if (!getContextx().equals(_b_.getContextx()))
            return false;
        if (!getOnlineSetName().equals(_b_.getOnlineSetName()))
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
                case 1: _context = _v_.stringValue(); break;
                case 2: _contextx = _v_.binaryValue(); break;
                case 3: _onlineSetName = _v_.stringValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setContext(_r_.getString(_pn_ + "context"));
        if (getContext() == null)
            setContext("");
        setContextx(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "contextx")));
        setOnlineSetName(_r_.getString(_pn_ + "onlineSetName"));
        if (getOnlineSetName() == null)
            setOnlineSetName("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "context", getContext());
        _s_.appendBinary(_pn_ + "contextx", getContextx());
        _s_.appendString(_pn_ + "onlineSetName", getOnlineSetName());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "context", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "contextx", "binary", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "onlineSetName", "string", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 5802054934505091577L;

    private String _context;
    private Zeze.Net.Binary _contextx;
    private String _onlineSetName;

    public String getContext() {
        return _context;
    }

    public void setContext(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _context = _v_;
    }

    public Zeze.Net.Binary getContextx() {
        return _contextx;
    }

    public void setContextx(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _contextx = _v_;
    }

    public String getOnlineSetName() {
        return _onlineSetName;
    }

    public void setOnlineSetName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _onlineSetName = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _context = "";
        _contextx = Zeze.Net.Binary.Empty;
        _onlineSetName = "";
    }

    @SuppressWarnings("deprecation")
    public Data(String _context_, Zeze.Net.Binary _contextx_, String _onlineSetName_) {
        if (_context_ == null)
            _context_ = "";
        _context = _context_;
        if (_contextx_ == null)
            _contextx_ = Zeze.Net.Binary.Empty;
        _contextx = _contextx_;
        if (_onlineSetName_ == null)
            _onlineSetName_ = "";
        _onlineSetName = _onlineSetName_;
    }

    @Override
    public void reset() {
        _context = "";
        _contextx = Zeze.Net.Binary.Empty;
        _onlineSetName = "";
    }

    @Override
    public Zeze.Builtin.Provider.BUserState toBean() {
        var _b_ = new Zeze.Builtin.Provider.BUserState();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BUserState)_o_);
    }

    public void assign(BUserState _o_) {
        _context = _o_.getContext();
        _contextx = _o_.getContextx();
        _onlineSetName = _o_.getOnlineSetName();
    }

    public void assign(BUserState.Data _o_) {
        _context = _o_._context;
        _contextx = _o_._contextx;
        _onlineSetName = _o_._onlineSetName;
    }

    @Override
    public BUserState.Data copy() {
        var _c_ = new BUserState.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BUserState.Data _a_, BUserState.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BUserState.Data clone() {
        return (BUserState.Data)super.clone();
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
        _s_.append("Zeze.Builtin.Provider.BUserState: {\n");
        _s_.append(_i1_).append("context=").append(_context).append(",\n");
        _s_.append(_i1_).append("contextx=").append(_contextx).append(",\n");
        _s_.append(_i1_).append("onlineSetName=").append(_onlineSetName).append('\n');
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
            String _x_ = _context;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = _contextx;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            String _x_ = _onlineSetName;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _context = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _contextx = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _onlineSetName = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
