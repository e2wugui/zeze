// auto-generated @formatter:off
package Zeze.Builtin.Web;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BHttpResponse extends Zeze.Transaction.Bean {
    private String _ContentType;
    private Zeze.Net.Binary _Body;
    private final Zeze.Transaction.Collections.PList1<String> _Cookie; // TODO ... more http data

    public String getContentType() {
        if (!isManaged())
            return _ContentType;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _ContentType;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ContentType)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.Value : _ContentType;
    }

    public void setContentType(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ContentType = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__ContentType(this, 1, value));
    }

    public Zeze.Net.Binary getBody() {
        if (!isManaged())
            return _Body;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _Body;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__Body)txn.GetLog(this.getObjectId() + 2);
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
        txn.PutLog(new Log__Body(this, 2, value));
    }

    public Zeze.Transaction.Collections.PList1<String> getCookie() {
        return _Cookie;
    }

    public BHttpResponse() {
         this(0);
    }

    public BHttpResponse(int _varId_) {
        super(_varId_);
        _ContentType = "";
        _Body = Zeze.Net.Binary.Empty;
        _Cookie = new Zeze.Transaction.Collections.PList1<>(String.class);
        _Cookie.VariableId = 3;
    }

    public void Assign(BHttpResponse other) {
        setContentType(other.getContentType());
        setBody(other.getBody());
        getCookie().clear();
        for (var e : other.getCookie())
            getCookie().add(e);
    }

    public BHttpResponse CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BHttpResponse Copy() {
        var copy = new BHttpResponse();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BHttpResponse a, BHttpResponse b) {
        BHttpResponse save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public static final long TYPEID = -6610665185330746407L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private static final class Log__ContentType extends Zeze.Transaction.Logs.LogString {
        public Log__ContentType(BHttpResponse bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BHttpResponse)getBelong())._ContentType = Value; }
    }

    private static final class Log__Body extends Zeze.Transaction.Logs.LogBinary {
        public Log__Body(BHttpResponse bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BHttpResponse)getBelong())._Body = Value; }
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Web.BHttpResponse: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ContentType").append('=').append(getContentType()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Body").append('=').append(getBody()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Cookie").append("=[").append(System.lineSeparator());
        level += 4;
        for (var _item_ : getCookie()) {
            sb.append(Zeze.Util.Str.indent(level)).append("Item").append('=').append(_item_).append(',').append(System.lineSeparator());
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
            String _x_ = getContentType();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = getBody();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            var _x_ = getCookie();
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BYTES);
                for (var _v_ : _x_)
                    _o_.WriteString(_v_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void Decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setContentType(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setBody(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            var _x_ = getCookie();
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadString(_t_));
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
        _Cookie.InitRootInfo(root, this);
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
                case 1: _ContentType = ((Zeze.Transaction.Logs.LogString)vlog).Value; break;
                case 2: _Body = ((Zeze.Transaction.Logs.LogBinary)vlog).Value; break;
                case 3: _Cookie.FollowerApply(vlog); break;
            }
        }
    }
}
