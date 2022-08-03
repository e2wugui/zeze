// auto-generated @formatter:off
package Zeze.Builtin.Web;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BCloseExchange extends Zeze.Transaction.Bean {
    private long _ExchangeId;

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

    public BCloseExchange() {
         this(0);
    }

    public BCloseExchange(int _varId_) {
        super(_varId_);
    }

    public void Assign(BCloseExchange other) {
        setExchangeId(other.getExchangeId());
    }

    public BCloseExchange CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BCloseExchange Copy() {
        var copy = new BCloseExchange();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BCloseExchange a, BCloseExchange b) {
        BCloseExchange save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public static final long TYPEID = 2158529094627834211L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private static final class Log__ExchangeId extends Zeze.Transaction.Logs.LogLong {
        public Log__ExchangeId(BCloseExchange bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BCloseExchange)getBelong())._ExchangeId = Value; }
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Web.BCloseExchange: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ExchangeId").append('=').append(getExchangeId()).append(System.lineSeparator());
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
            }
        }
    }
}
