// auto-generated @formatter:off
package Zeze.Builtin.Web;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BResponse extends Zeze.Transaction.Bean {
    private int _Code;
    private final Zeze.Transaction.Collections.PMap2<String, Zeze.Builtin.Web.BHeader> _Headers;
    private Zeze.Net.Binary _Body;
    private boolean _Finish;
    private String _Message;
    private String _Stacktrace;

    public int getCode() {
        if (!isManaged())
            return _Code;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Code;
        var log = (Log__Code)txn.GetLog(objectId() + 1);
        return log != null ? log.Value : _Code;
    }

    public void setCode(int value) {
        if (!isManaged()) {
            _Code = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.PutLog(new Log__Code(this, 1, value));
    }

    public Zeze.Transaction.Collections.PMap2<String, Zeze.Builtin.Web.BHeader> getHeaders() {
        return _Headers;
    }

    public Zeze.Net.Binary getBody() {
        if (!isManaged())
            return _Body;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Body;
        var log = (Log__Body)txn.GetLog(objectId() + 3);
        return log != null ? log.Value : _Body;
    }

    public void setBody(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Body = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.PutLog(new Log__Body(this, 3, value));
    }

    public boolean isFinish() {
        if (!isManaged())
            return _Finish;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Finish;
        var log = (Log__Finish)txn.GetLog(objectId() + 4);
        return log != null ? log.Value : _Finish;
    }

    public void setFinish(boolean value) {
        if (!isManaged()) {
            _Finish = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.PutLog(new Log__Finish(this, 4, value));
    }

    public String getMessage() {
        if (!isManaged())
            return _Message;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Message;
        var log = (Log__Message)txn.GetLog(objectId() + 5);
        return log != null ? log.Value : _Message;
    }

    public void setMessage(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Message = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.PutLog(new Log__Message(this, 5, value));
    }

    public String getStacktrace() {
        if (!isManaged())
            return _Stacktrace;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Stacktrace;
        var log = (Log__Stacktrace)txn.GetLog(objectId() + 6);
        return log != null ? log.Value : _Stacktrace;
    }

    public void setStacktrace(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Stacktrace = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.PutLog(new Log__Stacktrace(this, 6, value));
    }

    @SuppressWarnings("deprecation")
    public BResponse() {
        _Headers = new Zeze.Transaction.Collections.PMap2<>(String.class, Zeze.Builtin.Web.BHeader.class);
        _Headers.variableId(2);
        _Body = Zeze.Net.Binary.Empty;
        _Message = "";
        _Stacktrace = "";
    }

    @SuppressWarnings("deprecation")
    public BResponse(int _Code_, Zeze.Net.Binary _Body_, boolean _Finish_, String _Message_, String _Stacktrace_) {
        _Code = _Code_;
        _Headers = new Zeze.Transaction.Collections.PMap2<>(String.class, Zeze.Builtin.Web.BHeader.class);
        _Headers.variableId(2);
        if (_Body_ == null)
            throw new IllegalArgumentException();
        _Body = _Body_;
        _Finish = _Finish_;
        if (_Message_ == null)
            throw new IllegalArgumentException();
        _Message = _Message_;
        if (_Stacktrace_ == null)
            throw new IllegalArgumentException();
        _Stacktrace = _Stacktrace_;
    }

    public void Assign(BResponse other) {
        setCode(other.getCode());
        getHeaders().clear();
        for (var e : other.getHeaders().entrySet())
            getHeaders().put(e.getKey(), e.getValue().Copy());
        setBody(other.getBody());
        setFinish(other.isFinish());
        setMessage(other.getMessage());
        setStacktrace(other.getStacktrace());
    }

    public BResponse CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BResponse Copy() {
        var copy = new BResponse();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BResponse a, BResponse b) {
        BResponse save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public BResponse CopyBean() {
        return Copy();
    }

    public static final long TYPEID = -5862638463049880127L;

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Code extends Zeze.Transaction.Logs.LogInt {
        public Log__Code(BResponse bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BResponse)getBelong())._Code = Value; }
    }

    private static final class Log__Body extends Zeze.Transaction.Logs.LogBinary {
        public Log__Body(BResponse bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BResponse)getBelong())._Body = Value; }
    }

    private static final class Log__Finish extends Zeze.Transaction.Logs.LogBool {
        public Log__Finish(BResponse bean, int varId, boolean value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BResponse)getBelong())._Finish = Value; }
    }

    private static final class Log__Message extends Zeze.Transaction.Logs.LogString {
        public Log__Message(BResponse bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BResponse)getBelong())._Message = Value; }
    }

    private static final class Log__Stacktrace extends Zeze.Transaction.Logs.LogString {
        public Log__Stacktrace(BResponse bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BResponse)getBelong())._Stacktrace = Value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        BuildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void BuildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Web.BResponse: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Code").append('=').append(getCode()).append(',').append(System.lineSeparator());
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
        sb.append(Zeze.Util.Str.indent(level)).append("Finish").append('=').append(isFinish()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Message").append('=').append(getMessage()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Stacktrace").append('=').append(getStacktrace()).append(System.lineSeparator());
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
    public void Encode(ByteBuffer _o_) {
        int _i_ = 0;
        {
            int _x_ = getCode();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = getHeaders();
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.MAP);
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
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            boolean _x_ = isFinish();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        {
            String _x_ = getMessage();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getStacktrace();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void Decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setCode(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
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
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setBody(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setFinish(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setMessage(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            setStacktrace(_o_.ReadString(_t_));
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
    protected void ResetChildrenRootInfo() {
        _Headers.ResetRootInfo();
    }

    @Override
    public boolean NegativeCheck() {
        if (getCode() < 0)
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
                case 1: _Code = ((Zeze.Transaction.Logs.LogInt)vlog).Value; break;
                case 2: _Headers.FollowerApply(vlog); break;
                case 3: _Body = ((Zeze.Transaction.Logs.LogBinary)vlog).Value; break;
                case 4: _Finish = ((Zeze.Transaction.Logs.LogBool)vlog).Value; break;
                case 5: _Message = ((Zeze.Transaction.Logs.LogString)vlog).Value; break;
                case 6: _Stacktrace = ((Zeze.Transaction.Logs.LogString)vlog).Value; break;
            }
        }
    }
}
