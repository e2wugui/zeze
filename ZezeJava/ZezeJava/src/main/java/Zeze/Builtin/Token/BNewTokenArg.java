// auto-generated @formatter:off
package Zeze.Builtin.Token;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BNewTokenArg extends Zeze.Transaction.Bean implements BNewTokenArgReadOnly {
    public static final long TYPEID = 2668590583481037382L;

    private Zeze.Net.Binary _context; // token绑定的自定义上下文
    private long _ttl; // 存活时长(毫秒). 超时会被自动清除token及绑定的所有状态

    @Override
    public Zeze.Net.Binary getContext() {
        if (!isManaged())
            return _context;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _context;
        var log = (Log__context)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _context;
    }

    public void setContext(Zeze.Net.Binary _v_) {
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
    public long getTtl() {
        if (!isManaged())
            return _ttl;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ttl;
        var log = (Log__ttl)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _ttl;
    }

    public void setTtl(long _v_) {
        if (!isManaged()) {
            _ttl = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__ttl(this, 2, _v_));
    }

    @SuppressWarnings("deprecation")
    public BNewTokenArg() {
        _context = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BNewTokenArg(Zeze.Net.Binary _context_, long _ttl_) {
        if (_context_ == null)
            _context_ = Zeze.Net.Binary.Empty;
        _context = _context_;
        _ttl = _ttl_;
    }

    @Override
    public void reset() {
        setContext(Zeze.Net.Binary.Empty);
        setTtl(0);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Token.BNewTokenArg.Data toData() {
        var _d_ = new Zeze.Builtin.Token.BNewTokenArg.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Token.BNewTokenArg.Data)_o_);
    }

    public void assign(BNewTokenArg.Data _o_) {
        setContext(_o_._context);
        setTtl(_o_._ttl);
        _unknown_ = null;
    }

    public void assign(BNewTokenArg _o_) {
        setContext(_o_.getContext());
        setTtl(_o_.getTtl());
        _unknown_ = _o_._unknown_;
    }

    public BNewTokenArg copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BNewTokenArg copy() {
        var _c_ = new BNewTokenArg();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BNewTokenArg _a_, BNewTokenArg _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__context extends Zeze.Transaction.Logs.LogBinary {
        public Log__context(BNewTokenArg _b_, int _i_, Zeze.Net.Binary _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BNewTokenArg)getBelong())._context = value; }
    }

    private static final class Log__ttl extends Zeze.Transaction.Logs.LogLong {
        public Log__ttl(BNewTokenArg _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BNewTokenArg)getBelong())._ttl = value; }
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
        _s_.append("Zeze.Builtin.Token.BNewTokenArg: {\n");
        _s_.append(_i1_).append("context=").append(getContext()).append(",\n");
        _s_.append(_i1_).append("ttl=").append(getTtl()).append('\n');
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
            var _x_ = getContext();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            long _x_ = getTtl();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
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
            setContext(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setTtl(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BNewTokenArg))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BNewTokenArg)_o_;
        if (!getContext().equals(_b_.getContext()))
            return false;
        if (getTtl() != _b_.getTtl())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getTtl() < 0)
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
                case 1: _context = _v_.binaryValue(); break;
                case 2: _ttl = _v_.longValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setContext(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "context")));
        setTtl(_r_.getLong(_pn_ + "ttl"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendBinary(_pn_ + "context", getContext());
        _s_.appendLong(_pn_ + "ttl", getTtl());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "context", "binary", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "ttl", "long", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 2668590583481037382L;

    private Zeze.Net.Binary _context; // token绑定的自定义上下文
    private long _ttl; // 存活时长(毫秒). 超时会被自动清除token及绑定的所有状态

    public Zeze.Net.Binary getContext() {
        return _context;
    }

    public void setContext(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _context = _v_;
    }

    public long getTtl() {
        return _ttl;
    }

    public void setTtl(long _v_) {
        _ttl = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _context = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public Data(Zeze.Net.Binary _context_, long _ttl_) {
        if (_context_ == null)
            _context_ = Zeze.Net.Binary.Empty;
        _context = _context_;
        _ttl = _ttl_;
    }

    @Override
    public void reset() {
        _context = Zeze.Net.Binary.Empty;
        _ttl = 0;
    }

    @Override
    public Zeze.Builtin.Token.BNewTokenArg toBean() {
        var _b_ = new Zeze.Builtin.Token.BNewTokenArg();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BNewTokenArg)_o_);
    }

    public void assign(BNewTokenArg _o_) {
        _context = _o_.getContext();
        _ttl = _o_.getTtl();
    }

    public void assign(BNewTokenArg.Data _o_) {
        _context = _o_._context;
        _ttl = _o_._ttl;
    }

    @Override
    public BNewTokenArg.Data copy() {
        var _c_ = new BNewTokenArg.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BNewTokenArg.Data _a_, BNewTokenArg.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BNewTokenArg.Data clone() {
        return (BNewTokenArg.Data)super.clone();
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
        _s_.append("Zeze.Builtin.Token.BNewTokenArg: {\n");
        _s_.append(_i1_).append("context=").append(_context).append(",\n");
        _s_.append(_i1_).append("ttl=").append(_ttl).append('\n');
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
            var _x_ = _context;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            long _x_ = _ttl;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _context = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _ttl = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
