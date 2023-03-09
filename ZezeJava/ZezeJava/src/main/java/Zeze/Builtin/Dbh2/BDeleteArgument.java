// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BDeleteArgument extends Zeze.Transaction.Bean implements BDeleteArgumentReadOnly {
    public static final long TYPEID = 3113412576231487346L;

    private long _TransactionId;
    private Zeze.Net.Binary _Key;

    @Override
    public long getTransactionId() {
        if (!isManaged())
            return _TransactionId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _TransactionId;
        var log = (Log__TransactionId)txn.getLog(objectId() + 1);
        return log != null ? log.value : _TransactionId;
    }

    public void setTransactionId(long value) {
        if (!isManaged()) {
            _TransactionId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__TransactionId(this, 1, value));
    }

    @Override
    public Zeze.Net.Binary getKey() {
        if (!isManaged())
            return _Key;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Key;
        var log = (Log__Key)txn.getLog(objectId() + 2);
        return log != null ? log.value : _Key;
    }

    public void setKey(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Key = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Key(this, 2, value));
    }

    @SuppressWarnings("deprecation")
    public BDeleteArgument() {
        _Key = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BDeleteArgument(long _TransactionId_, Zeze.Net.Binary _Key_) {
        _TransactionId = _TransactionId_;
        if (_Key_ == null)
            throw new IllegalArgumentException();
        _Key = _Key_;
    }

    @Override
    public Zeze.Builtin.Dbh2.BDeleteArgumentDaTa toData() {
        var data = new Zeze.Builtin.Dbh2.BDeleteArgumentDaTa();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Dbh2.BDeleteArgumentDaTa)other);
    }

    public void assign(BDeleteArgumentDaTa other) {
        setTransactionId(other.getTransactionId());
        setKey(other.getKey());
    }

    public void assign(BDeleteArgument other) {
        setTransactionId(other.getTransactionId());
        setKey(other.getKey());
    }

    @Deprecated
    public void Assign(BDeleteArgument other) {
        assign(other);
    }

    public BDeleteArgument copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BDeleteArgument copy() {
        var copy = new BDeleteArgument();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BDeleteArgument Copy() {
        return copy();
    }

    public static void swap(BDeleteArgument a, BDeleteArgument b) {
        BDeleteArgument save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__TransactionId extends Zeze.Transaction.Logs.LogLong {
        public Log__TransactionId(BDeleteArgument bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BDeleteArgument)getBelong())._TransactionId = value; }
    }

    private static final class Log__Key extends Zeze.Transaction.Logs.LogBinary {
        public Log__Key(BDeleteArgument bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BDeleteArgument)getBelong())._Key = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.BDeleteArgument: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("TransactionId=").append(getTransactionId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(getKey()).append(System.lineSeparator());
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
            long _x_ = getTransactionId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = getKey();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setTransactionId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setKey(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    public boolean negativeCheck() {
        if (getTransactionId() < 0)
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
                case 1: _TransactionId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 2: _Key = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
            }
        }
    }
}
