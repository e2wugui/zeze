// auto-generated @formatter:off
package Zeze.Builtin.Token;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BNewTokenArg extends Zeze.Transaction.Bean implements BNewTokenArgReadOnly {
    public static final long TYPEID = 2668590583481037382L;

    private Zeze.Net.Binary _context; // token绑定的自定义上下文
    private long _ttl; // 存活时长(毫秒). 超时会被自动清除token及绑定的所有状态

    @Override
    public Zeze.Net.Binary getContext() {
        if (!isManaged())
            return _context;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _context;
        var log = (Log__context)txn.getLog(objectId() + 1);
        return log != null ? log.value : _context;
    }

    public void setContext(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _context = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__context(this, 1, value));
    }

    @Override
    public long getTtl() {
        if (!isManaged())
            return _ttl;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ttl;
        var log = (Log__ttl)txn.getLog(objectId() + 2);
        return log != null ? log.value : _ttl;
    }

    public void setTtl(long value) {
        if (!isManaged()) {
            _ttl = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ttl(this, 2, value));
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
        var data = new Zeze.Builtin.Token.BNewTokenArg.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Token.BNewTokenArg.Data)other);
    }

    public void assign(BNewTokenArg.Data other) {
        setContext(other._context);
        setTtl(other._ttl);
        _unknown_ = null;
    }

    public void assign(BNewTokenArg other) {
        setContext(other.getContext());
        setTtl(other.getTtl());
        _unknown_ = other._unknown_;
    }

    public BNewTokenArg copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BNewTokenArg copy() {
        var copy = new BNewTokenArg();
        copy.assign(this);
        return copy;
    }

    public static void swap(BNewTokenArg a, BNewTokenArg b) {
        BNewTokenArg save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__context extends Zeze.Transaction.Logs.LogBinary {
        public Log__context(BNewTokenArg bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BNewTokenArg)getBelong())._context = value; }
    }

    private static final class Log__ttl extends Zeze.Transaction.Logs.LogLong {
        public Log__ttl(BNewTokenArg bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BNewTokenArg)getBelong())._ttl = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Token.BNewTokenArg: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("context=").append(getContext()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ttl=").append(getTtl()).append(System.lineSeparator());
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
    public void decode(ByteBuffer _o_) {
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
    public boolean negativeCheck() {
        if (getTtl() < 0)
            return true;
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
                case 1: _context = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
                case 2: _ttl = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setContext(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "context")));
        if (getContext() == null)
            setContext(Zeze.Net.Binary.Empty);
        setTtl(rs.getLong(_parents_name_ + "ttl"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendBinary(_parents_name_ + "context", getContext());
        st.appendLong(_parents_name_ + "ttl", getTtl());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "context", "binary", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "ttl", "long", "", ""));
        return vars;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 2668590583481037382L;

    private Zeze.Net.Binary _context; // token绑定的自定义上下文
    private long _ttl; // 存活时长(毫秒). 超时会被自动清除token及绑定的所有状态

    public Zeze.Net.Binary getContext() {
        return _context;
    }

    public void setContext(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        _context = value;
    }

    public long getTtl() {
        return _ttl;
    }

    public void setTtl(long value) {
        _ttl = value;
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
        var bean = new Zeze.Builtin.Token.BNewTokenArg();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BNewTokenArg)other);
    }

    public void assign(BNewTokenArg other) {
        _context = other.getContext();
        _ttl = other.getTtl();
    }

    public void assign(BNewTokenArg.Data other) {
        _context = other._context;
        _ttl = other._ttl;
    }

    @Override
    public BNewTokenArg.Data copy() {
        var copy = new BNewTokenArg.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BNewTokenArg.Data a, BNewTokenArg.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
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
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Token.BNewTokenArg: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("context=").append(_context).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ttl=").append(_ttl).append(System.lineSeparator());
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
    public void decode(ByteBuffer _o_) {
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
