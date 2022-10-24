// auto-generated @formatter:off
package Zeze.Builtin.Web;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BStream extends Zeze.Transaction.Bean implements BStreamReadOnly {
    public static final long TYPEID = 6767831806810414082L;

    private long _ExchangeId;
    private Zeze.Net.Binary _Body;
    private boolean _Finish;

    @Override
    public long getExchangeId() {
        if (!isManaged())
            return _ExchangeId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ExchangeId;
        var log = (Log__ExchangeId)txn.getLog(objectId() + 1);
        return log != null ? log.value : _ExchangeId;
    }

    public void setExchangeId(long value) {
        if (!isManaged()) {
            _ExchangeId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ExchangeId(this, 1, value));
    }

    @Override
    public Zeze.Net.Binary getBody() {
        if (!isManaged())
            return _Body;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Body;
        var log = (Log__Body)txn.getLog(objectId() + 2);
        return log != null ? log.value : _Body;
    }

    public void setBody(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Body = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Body(this, 2, value));
    }

    @Override
    public boolean isFinish() {
        if (!isManaged())
            return _Finish;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Finish;
        var log = (Log__Finish)txn.getLog(objectId() + 3);
        return log != null ? log.value : _Finish;
    }

    public void setFinish(boolean value) {
        if (!isManaged()) {
            _Finish = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Finish(this, 3, value));
    }

    @SuppressWarnings("deprecation")
    public BStream() {
        _Body = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BStream(long _ExchangeId_, Zeze.Net.Binary _Body_, boolean _Finish_) {
        _ExchangeId = _ExchangeId_;
        if (_Body_ == null)
            throw new IllegalArgumentException();
        _Body = _Body_;
        _Finish = _Finish_;
    }

    public void assign(BStream other) {
        setExchangeId(other.getExchangeId());
        setBody(other.getBody());
        setFinish(other.isFinish());
    }

    @Deprecated
    public void Assign(BStream other) {
        assign(other);
    }

    public BStream copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BStream copy() {
        var copy = new BStream();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BStream Copy() {
        return copy();
    }

    public static void swap(BStream a, BStream b) {
        BStream save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ExchangeId extends Zeze.Transaction.Logs.LogLong {
        public Log__ExchangeId(BStream bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BStream)getBelong())._ExchangeId = value; }
    }

    private static final class Log__Body extends Zeze.Transaction.Logs.LogBinary {
        public Log__Body(BStream bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BStream)getBelong())._Body = value; }
    }

    private static final class Log__Finish extends Zeze.Transaction.Logs.LogBool {
        public Log__Finish(BStream bean, int varId, boolean value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BStream)getBelong())._Finish = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
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
    public void decode(ByteBuffer _o_) {
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
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
    }

    @Override
    protected void resetChildrenRootInfo() {
    }

    @Override
    public boolean negativeCheck() {
        if (getExchangeId() < 0)
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
                case 1: _ExchangeId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 2: _Body = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
                case 3: _Finish = ((Zeze.Transaction.Logs.LogBool)vlog).value; break;
            }
        }
    }
}
