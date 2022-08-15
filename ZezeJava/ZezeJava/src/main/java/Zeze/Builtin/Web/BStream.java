// auto-generated @formatter:off
package Zeze.Builtin.Web;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BStream extends Zeze.Transaction.Bean {
    private long _ExchangeId;
    private Zeze.Net.Binary _Body;
    private boolean _Finish;

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

    public boolean isFinish() {
        if (!isManaged())
            return _Finish;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _Finish;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__Finish)txn.GetLog(this.getObjectId() + 3);
        return log != null ? log.Value : _Finish;
    }

    public void setFinish(boolean value) {
        if (!isManaged()) {
            _Finish = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__Finish(this, 3, value));
    }

    public BStream() {
        _Body = Zeze.Net.Binary.Empty;
    }

    public BStream(long _ExchangeId_, Zeze.Net.Binary _Body_, boolean _Finish_) {
        _ExchangeId = _ExchangeId_;
        _Body = _Body_;
        _Finish = _Finish_;
    }

    public void Assign(BStream other) {
        setExchangeId(other.getExchangeId());
        setBody(other.getBody());
        setFinish(other.isFinish());
    }

    public BStream CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BStream Copy() {
        var copy = new BStream();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BStream a, BStream b) {
        BStream save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public static final long TYPEID = 6767831806810414082L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private static final class Log__ExchangeId extends Zeze.Transaction.Logs.LogLong {
        public Log__ExchangeId(BStream bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BStream)getBelong())._ExchangeId = Value; }
    }

    private static final class Log__Body extends Zeze.Transaction.Logs.LogBinary {
        public Log__Body(BStream bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BStream)getBelong())._Body = Value; }
    }

    private static final class Log__Finish extends Zeze.Transaction.Logs.LogBool {
        public Log__Finish(BStream bean, int varId, boolean value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BStream)getBelong())._Finish = Value; }
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Web.BStream: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ExchangeId").append('=').append(getExchangeId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Body").append('=').append(getBody()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Finish").append('=').append(isFinish()).append(System.lineSeparator());
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
            var _x_ = getBody();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            boolean _x_ = isFinish();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
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
            setBody(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setFinish(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
    }

    @Override
    protected void ResetChildrenRootInfo() {
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
                case 2: _Body = ((Zeze.Transaction.Logs.LogBinary)vlog).Value; break;
                case 3: _Finish = ((Zeze.Transaction.Logs.LogBool)vlog).Value; break;
            }
        }
    }
}
