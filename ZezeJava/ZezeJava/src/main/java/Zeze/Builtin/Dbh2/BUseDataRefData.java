// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

import Zeze.Serialize.ByteBuffer;

// 用来生成Data数据结构，没有实际功能
@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BUseDataRefData extends Zeze.Transaction.Data {
    public static final long TYPEID = -8788076056876784776L;

    private Zeze.Builtin.Dbh2.BLogBeginTransactionData _Ref1;
    private Zeze.Builtin.Dbh2.BBucketMetaData _Ref2;

    public Zeze.Builtin.Dbh2.BLogBeginTransactionData getRef1() {
        return _Ref1;
    }

    public void setRef1(Zeze.Builtin.Dbh2.BLogBeginTransactionData value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Ref1 = value;
    }

    public Zeze.Builtin.Dbh2.BBucketMetaData getRef2() {
        return _Ref2;
    }

    public void setRef2(Zeze.Builtin.Dbh2.BBucketMetaData value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Ref2 = value;
    }

    @SuppressWarnings("deprecation")
    public BUseDataRefData() {
        _Ref1 = new Zeze.Builtin.Dbh2.BLogBeginTransactionData();
        _Ref1.variableId(1);
        _Ref2 = new Zeze.Builtin.Dbh2.BBucketMetaData();
        _Ref2.variableId(2);
    }

    @Override
    public Zeze.Builtin.Dbh2.BUseDataRef toBean() {
        var bean = new Zeze.Builtin.Dbh2.BUseDataRef();
        bean.assign(this);
        return bean;
    }

    public void assign(Zeze.Transaction.Bean other) {
        assign((BUseDataRef)other);
    }

    public void assign(BUseDataRef other) {
        _Ref1.assign(other.getRef1());
        _Ref2.assign(other.getRef2());
    }

    public void assign(BUseDataRefData other) {
        _Ref1.assign(other.getRef1());
        _Ref2.assign(other.getRef2());
    }

    @Override
    public BUseDataRefData copy() {
        var copy = new BUseDataRefData();
        copy.assign(this);
        return copy;
    }

    public static void swap(BUseDataRefData a, BUseDataRefData b) {
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.BUseDataRef: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Ref1=").append(System.lineSeparator());
        _Ref1.buildString(sb, level + 4);
        sb.append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Ref2=").append(System.lineSeparator());
        _Ref2.buildString(sb, level + 4);
        sb.append(System.lineSeparator());
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
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 1, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _Ref1.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 2, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _Ref2.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _o_.ReadBean(_Ref1, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _o_.ReadBean(_Ref2, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

}
