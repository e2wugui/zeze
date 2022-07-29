// auto-generated @formatter:off
package Zeze.Builtin.Web;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BRequest extends Zeze.Transaction.Bean {
    private long _ExchangeId;
    private String _Method;
    private String _Path;
    private String _Query;
    private final Zeze.Transaction.Collections.PMap2<String, Zeze.Builtin.Web.BHeader> _Headers;
    private Zeze.Net.Binary _Body;
    private boolean _RemainInputStream;

    public long getExchangeId() {
        if (!isManaged())
            return _ExchangeId;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _ExchangeId;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ExchangeId)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.Value : _ExchangeId;
    }

    public void setExchangeId(long value) {
        if (!isManaged()) {
            _ExchangeId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__ExchangeId(this, 1, value));
    }

    public String getMethod() {
        if (!isManaged())
            return _Method;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _Method;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__Method)txn.GetLog(this.getObjectId() + 2);
        return log != null ? log.Value : _Method;
    }

    public void setMethod(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Method = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__Method(this, 2, value));
    }

    public String getPath() {
        if (!isManaged())
            return _Path;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _Path;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__Path)txn.GetLog(this.getObjectId() + 3);
        return log != null ? log.Value : _Path;
    }

    public void setPath(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Path = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__Path(this, 3, value));
    }

    public String getQuery() {
        if (!isManaged())
            return _Query;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _Query;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__Query)txn.GetLog(this.getObjectId() + 4);
        return log != null ? log.Value : _Query;
    }

    public void setQuery(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Query = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__Query(this, 4, value));
    }

    public Zeze.Transaction.Collections.PMap2<String, Zeze.Builtin.Web.BHeader> getHeaders() {
        return _Headers;
    }

    public Zeze.Net.Binary getBody() {
        if (!isManaged())
            return _Body;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _Body;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__Body)txn.GetLog(this.getObjectId() + 6);
        return log != null ? log.Value : _Body;
    }

    public void setBody(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Body = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__Body(this, 6, value));
    }

    public boolean isRemainInputStream() {
        if (!isManaged())
            return _RemainInputStream;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _RemainInputStream;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__RemainInputStream)txn.GetLog(this.getObjectId() + 7);
        return log != null ? log.Value : _RemainInputStream;
    }

    public void setRemainInputStream(boolean value) {
        if (!isManaged()) {
            _RemainInputStream = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__RemainInputStream(this, 7, value));
    }

    public BRequest() {
         this(0);
    }

    public BRequest(int _varId_) {
        super(_varId_);
        _Method = "";
        _Path = "";
        _Query = "";
        _Headers = new Zeze.Transaction.Collections.PMap2<>(String.class, Zeze.Builtin.Web.BHeader.class);
        _Headers.VariableId = 5;
        _Body = Zeze.Net.Binary.Empty;
    }

    public void Assign(BRequest other) {
        setExchangeId(other.getExchangeId());
        setMethod(other.getMethod());
        setPath(other.getPath());
        setQuery(other.getQuery());
        getHeaders().clear();
        for (var e : other.getHeaders().entrySet())
            getHeaders().put(e.getKey(), e.getValue().Copy());
        setBody(other.getBody());
        setRemainInputStream(other.isRemainInputStream());
    }

    public BRequest CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BRequest Copy() {
        var copy = new BRequest();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BRequest a, BRequest b) {
        BRequest save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public static final long TYPEID = -8704897348167290545L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private static final class Log__ExchangeId extends Zeze.Transaction.Logs.LogLong {
        public Log__ExchangeId(BRequest bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BRequest)getBelong())._ExchangeId = Value; }
    }

    private static final class Log__Method extends Zeze.Transaction.Logs.LogString {
        public Log__Method(BRequest bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BRequest)getBelong())._Method = Value; }
    }

    private static final class Log__Path extends Zeze.Transaction.Logs.LogString {
        public Log__Path(BRequest bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BRequest)getBelong())._Path = Value; }
    }

    private static final class Log__Query extends Zeze.Transaction.Logs.LogString {
        public Log__Query(BRequest bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BRequest)getBelong())._Query = Value; }
    }

    private static final class Log__Body extends Zeze.Transaction.Logs.LogBinary {
        public Log__Body(BRequest bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BRequest)getBelong())._Body = Value; }
    }

    private static final class Log__RemainInputStream extends Zeze.Transaction.Logs.LogBool {
        public Log__RemainInputStream(BRequest bean, int varId, boolean value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BRequest)getBelong())._RemainInputStream = Value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        BuildString(sb, 0);
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    @Override
    public void BuildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Web.BRequest: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ExchangeId").append('=').append(getExchangeId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Method").append('=').append(getMethod()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Path").append('=').append(getPath()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Query").append('=').append(getQuery()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Headers").append("=[").append(System.lineSeparator());
        level += 4;
        for (var _kv_ : getHeaders().entrySet()) {
            sb.append(Zeze.Util.Str.indent(level)).append('(').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append("Key").append('=').append(_kv_.getKey()).append(',').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append("Value").append('=').append(System.lineSeparator());
            _kv_.getValue().BuildString(sb, level + 4);
            sb.append(',').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append(')').append(System.lineSeparator());
        }
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Body").append('=').append(getBody()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("RemainInputStream").append('=').append(isRemainInputStream()).append(System.lineSeparator());
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append('}');
    }

    private static int _PRE_ALLOC_SIZE_ = 16;

    @Override
    public int getPreAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void setPreAllocSize(int size) {
        _PRE_ALLOC_SIZE_ = size;
    }

    @Override
    public void Encode(ByteBuffer _o_) {
        int _i_ = 0;
        {
            long _x_ = getExchangeId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            String _x_ = getMethod();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getPath();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getQuery();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = getHeaders();
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BYTES, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteString(_e_.getKey());
                    _e_.getValue().Encode(_o_);
                }
            }
        }
        {
            var _x_ = getBody();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            boolean _x_ = isRemainInputStream();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 7, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void Decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setExchangeId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setMethod(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setPath(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setQuery(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            var _x_ = getHeaders();
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadString(_s_);
                    var _v_ = _o_.ReadBean(new Zeze.Builtin.Web.BHeader(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownField(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            setBody(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 7) {
            setRemainInputStream(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Headers.InitRootInfo(root, this);
    }

    @Override
    public boolean NegativeCheck() {
        if (getExchangeId() < 0)
            return true;
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void FollowerApply(Zeze.Transaction.Log log) {
        var vars = ((Zeze.Transaction.Collections.LogBean)log).getVariables();
        if (vars == null)
            return;
        for (var it = vars.iterator(); it.moveToNext(); ) {
            var vlog = it.value();
            switch (vlog.getVariableId()) {
                case 1: _ExchangeId = ((Zeze.Transaction.Logs.LogLong)vlog).Value; break;
                case 2: _Method = ((Zeze.Transaction.Logs.LogString)vlog).Value; break;
                case 3: _Path = ((Zeze.Transaction.Logs.LogString)vlog).Value; break;
                case 4: _Query = ((Zeze.Transaction.Logs.LogString)vlog).Value; break;
                case 5: _Headers.FollowerApply(vlog); break;
                case 6: _Body = ((Zeze.Transaction.Logs.LogBinary)vlog).Value; break;
                case 7: _RemainInputStream = ((Zeze.Transaction.Logs.LogBool)vlog).Value; break;
            }
        }
    }
}
