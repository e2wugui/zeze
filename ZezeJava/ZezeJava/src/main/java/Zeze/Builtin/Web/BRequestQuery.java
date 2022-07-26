// auto-generated @formatter:off
package Zeze.Builtin.Web;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BRequestQuery extends Zeze.Transaction.Bean {
    private String _Cookie;
    private final Zeze.Transaction.Collections.PMap1<String, String> _Query;

    public String getCookie() {
        if (!isManaged())
            return _Cookie;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _Cookie;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__Cookie)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.Value : _Cookie;
    }

    public void setCookie(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Cookie = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__Cookie(this, 1, value));
    }

    public Zeze.Transaction.Collections.PMap1<String, String> getQuery() {
        return _Query;
    }

    public BRequestQuery() {
         this(0);
    }

    public BRequestQuery(int _varId_) {
        super(_varId_);
        _Cookie = "";
        _Query = new Zeze.Transaction.Collections.PMap1<>(String.class, String.class);
        _Query.VariableId = 2;
    }

    public void Assign(BRequestQuery other) {
        setCookie(other.getCookie());
        getQuery().clear();
        for (var e : other.getQuery().entrySet())
            getQuery().put(e.getKey(), e.getValue());
    }

    public BRequestQuery CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BRequestQuery Copy() {
        var copy = new BRequestQuery();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BRequestQuery a, BRequestQuery b) {
        BRequestQuery save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public static final long TYPEID = 2717056765608793705L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private static final class Log__Cookie extends Zeze.Transaction.Logs.LogString {
        public Log__Cookie(BRequestQuery bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BRequestQuery)getBelong())._Cookie = Value; }
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Web.BRequestQuery: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Cookie").append('=').append(getCookie()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Query").append("=[").append(System.lineSeparator());
        level += 4;
        for (var _kv_ : getQuery().entrySet()) {
            sb.append(Zeze.Util.Str.indent(level)).append('(').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append("Key").append('=').append(_kv_.getKey()).append(',').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append("Value").append('=').append(_kv_.getValue()).append(',').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append(')').append(System.lineSeparator());
        }
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append(']').append(System.lineSeparator());
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
            String _x_ = getCookie();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = getQuery();
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BYTES, ByteBuffer.BYTES);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteString(_e_.getKey());
                    _o_.WriteString(_e_.getValue());
                }
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void Decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setCookie(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = getQuery();
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadString(_s_);
                    var _v_ = _o_.ReadString(_t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownField(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Query.InitRootInfo(root, this);
    }

    @Override
    public boolean NegativeCheck() {
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
                case 1: _Cookie = ((Zeze.Transaction.Logs.LogString)vlog).Value; break;
                case 2: _Query.FollowerApply(vlog); break;
            }
        }
    }
}
