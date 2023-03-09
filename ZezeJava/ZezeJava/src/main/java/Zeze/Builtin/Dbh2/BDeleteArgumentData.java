// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BDeleteArgumentDaTa extends Zeze.Transaction.Data {
    public static final long TYPEID = 3113412576231487346L;

    private long _TransactionId;
    private Zeze.Net.Binary _Key;

    public long getTransactionId() {
        return _TransactionId;
    }

    public void setTransactionId(long value) {
        _TransactionId = value;
    }

    public Zeze.Net.Binary getKey() {
        return _Key;
    }

    public void setKey(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Key = value;
    }

    @SuppressWarnings("deprecation")
    public BDeleteArgumentDaTa() {
        _Key = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BDeleteArgumentDaTa(long _TransactionId_, Zeze.Net.Binary _Key_) {
        _TransactionId = _TransactionId_;
        if (_Key_ == null)
            throw new IllegalArgumentException();
        _Key = _Key_;
    }

    @Override
    public Zeze.Builtin.Dbh2.BDeleteArgument toBean() {
        var bean = new Zeze.Builtin.Dbh2.BDeleteArgument();
        bean.assign(this);
        return bean;
    }

    public void assign(Zeze.Transaction.Bean other) {
        assign((BDeleteArgument)other);
    }

    public void assign(BDeleteArgument other) {
        setTransactionId(other.getTransactionId());
        setKey(other.getKey());
    }

    public void assign(BDeleteArgumentDaTa other) {
        setTransactionId(other.getTransactionId());
        setKey(other.getKey());
    }

    @Override
    public BDeleteArgumentDaTa copy() {
        var copy = new BDeleteArgumentDaTa();
        copy.assign(this);
        return copy;
    }

    public static void swap(BDeleteArgumentDaTa a, BDeleteArgumentDaTa b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
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

}
