// auto-generated @formatter:off
package Zeze.Builtin.Token;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BGetTokenRes extends Zeze.Transaction.Bean implements BGetTokenResReadOnly {
    public static final long TYPEID = 4780430105301681046L;

    private Zeze.Net.Binary _context; // token绑定的自定义上下文. token已失效(状态已清除)时为空
    private long _count; // 此token已被GetToken访问的次数(包括当前访问). token已失效(状态已清除)时为0
    private long _time; // 此token已存活时间(毫秒). token已失效(状态已清除)时为负值
    private String _addr; // 请求分配此token的IP地址

    private static final java.lang.invoke.VarHandle vh_context;
    private static final java.lang.invoke.VarHandle vh_count;
    private static final java.lang.invoke.VarHandle vh_time;
    private static final java.lang.invoke.VarHandle vh_addr;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_context = _l_.findVarHandle(BGetTokenRes.class, "_context", Zeze.Net.Binary.class);
            vh_count = _l_.findVarHandle(BGetTokenRes.class, "_count", long.class);
            vh_time = _l_.findVarHandle(BGetTokenRes.class, "_time", long.class);
            vh_addr = _l_.findVarHandle(BGetTokenRes.class, "_addr", String.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public Zeze.Net.Binary getContext() {
        if (!isManaged())
            return _context;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _context;
        var log = (Zeze.Transaction.Logs.LogBinary)_t_.getLog(objectId() + 1);
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
        _t_.putLog(new Zeze.Transaction.Logs.LogBinary(this, 1, vh_context, _v_));
    }

    @Override
    public long getCount() {
        if (!isManaged())
            return _count;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _count;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _count;
    }

    public void setCount(long _v_) {
        if (!isManaged()) {
            _count = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 2, vh_count, _v_));
    }

    @Override
    public long getTime() {
        if (!isManaged())
            return _time;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _time;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _time;
    }

    public void setTime(long _v_) {
        if (!isManaged()) {
            _time = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 3, vh_time, _v_));
    }

    @Override
    public String getAddr() {
        if (!isManaged())
            return _addr;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _addr;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 4);
        return log != null ? log.value : _addr;
    }

    public void setAddr(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _addr = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 4, vh_addr, _v_));
    }

    @SuppressWarnings("deprecation")
    public BGetTokenRes() {
        _context = Zeze.Net.Binary.Empty;
        _addr = "";
    }

    @SuppressWarnings("deprecation")
    public BGetTokenRes(Zeze.Net.Binary _context_, long _count_, long _time_, String _addr_) {
        if (_context_ == null)
            _context_ = Zeze.Net.Binary.Empty;
        _context = _context_;
        _count = _count_;
        _time = _time_;
        if (_addr_ == null)
            _addr_ = "";
        _addr = _addr_;
    }

    @Override
    public void reset() {
        setContext(Zeze.Net.Binary.Empty);
        setCount(0);
        setTime(0);
        setAddr("");
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Token.BGetTokenRes.Data toData() {
        var _d_ = new Zeze.Builtin.Token.BGetTokenRes.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Token.BGetTokenRes.Data)_o_);
    }

    public void assign(BGetTokenRes.Data _o_) {
        setContext(_o_._context);
        setCount(_o_._count);
        setTime(_o_._time);
        setAddr(_o_._addr);
        _unknown_ = null;
    }

    public void assign(BGetTokenRes _o_) {
        setContext(_o_.getContext());
        setCount(_o_.getCount());
        setTime(_o_.getTime());
        setAddr(_o_.getAddr());
        _unknown_ = _o_._unknown_;
    }

    public BGetTokenRes copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BGetTokenRes copy() {
        var _c_ = new BGetTokenRes();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BGetTokenRes _a_, BGetTokenRes _b_) {
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
        _s_.append("Zeze.Builtin.Token.BGetTokenRes: {\n");
        _s_.append(_i1_).append("context=").append(getContext()).append(",\n");
        _s_.append(_i1_).append("count=").append(getCount()).append(",\n");
        _s_.append(_i1_).append("time=").append(getTime()).append(",\n");
        _s_.append(_i1_).append("addr=").append(getAddr()).append('\n');
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
            long _x_ = getCount();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getTime();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            String _x_ = getAddr();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
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
            setContext(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setCount(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setTime(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setAddr(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BGetTokenRes))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BGetTokenRes)_o_;
        if (!getContext().equals(_b_.getContext()))
            return false;
        if (getCount() != _b_.getCount())
            return false;
        if (getTime() != _b_.getTime())
            return false;
        if (!getAddr().equals(_b_.getAddr()))
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getCount() < 0)
            return true;
        if (getTime() < 0)
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
                case 2: _count = _v_.longValue(); break;
                case 3: _time = _v_.longValue(); break;
                case 4: _addr = _v_.stringValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setContext(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "context")));
        setCount(_r_.getLong(_pn_ + "count"));
        setTime(_r_.getLong(_pn_ + "time"));
        setAddr(_r_.getString(_pn_ + "addr"));
        if (getAddr() == null)
            setAddr("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendBinary(_pn_ + "context", getContext());
        _s_.appendLong(_pn_ + "count", getCount());
        _s_.appendLong(_pn_ + "time", getTime());
        _s_.appendString(_pn_ + "addr", getAddr());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "context", "binary", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "count", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "time", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "addr", "string", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 4780430105301681046L;

    private Zeze.Net.Binary _context; // token绑定的自定义上下文. token已失效(状态已清除)时为空
    private long _count; // 此token已被GetToken访问的次数(包括当前访问). token已失效(状态已清除)时为0
    private long _time; // 此token已存活时间(毫秒). token已失效(状态已清除)时为负值
    private String _addr; // 请求分配此token的IP地址

    public Zeze.Net.Binary getContext() {
        return _context;
    }

    public void setContext(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _context = _v_;
    }

    public long getCount() {
        return _count;
    }

    public void setCount(long _v_) {
        _count = _v_;
    }

    public long getTime() {
        return _time;
    }

    public void setTime(long _v_) {
        _time = _v_;
    }

    public String getAddr() {
        return _addr;
    }

    public void setAddr(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _addr = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _context = Zeze.Net.Binary.Empty;
        _addr = "";
    }

    @SuppressWarnings("deprecation")
    public Data(Zeze.Net.Binary _context_, long _count_, long _time_, String _addr_) {
        if (_context_ == null)
            _context_ = Zeze.Net.Binary.Empty;
        _context = _context_;
        _count = _count_;
        _time = _time_;
        if (_addr_ == null)
            _addr_ = "";
        _addr = _addr_;
    }

    @Override
    public void reset() {
        _context = Zeze.Net.Binary.Empty;
        _count = 0;
        _time = 0;
        _addr = "";
    }

    @Override
    public Zeze.Builtin.Token.BGetTokenRes toBean() {
        var _b_ = new Zeze.Builtin.Token.BGetTokenRes();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BGetTokenRes)_o_);
    }

    public void assign(BGetTokenRes _o_) {
        _context = _o_.getContext();
        _count = _o_.getCount();
        _time = _o_.getTime();
        _addr = _o_.getAddr();
    }

    public void assign(BGetTokenRes.Data _o_) {
        _context = _o_._context;
        _count = _o_._count;
        _time = _o_._time;
        _addr = _o_._addr;
    }

    @Override
    public BGetTokenRes.Data copy() {
        var _c_ = new BGetTokenRes.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BGetTokenRes.Data _a_, BGetTokenRes.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BGetTokenRes.Data clone() {
        return (BGetTokenRes.Data)super.clone();
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
        _s_.append("Zeze.Builtin.Token.BGetTokenRes: {\n");
        _s_.append(_i1_).append("context=").append(_context).append(",\n");
        _s_.append(_i1_).append("count=").append(_count).append(",\n");
        _s_.append(_i1_).append("time=").append(_time).append(",\n");
        _s_.append(_i1_).append("addr=").append(_addr).append('\n');
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
            long _x_ = _count;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = _time;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            String _x_ = _addr;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
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
            _context = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _count = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _time = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _addr = _o_.ReadString(_t_);
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
        if (!(_o_ instanceof BGetTokenRes.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BGetTokenRes.Data)_o_;
        if (!_context.equals(_b_._context))
            return false;
        if (_count != _b_._count)
            return false;
        if (_time != _b_._time)
            return false;
        if (!_addr.equals(_b_._addr))
            return false;
        return true;
    }
}
}
