// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BRegisterData extends Zeze.Transaction.Data {
    public static final long TYPEID = -3200018963971290421L;

    private String _Dbh2RaftAcceptorName;

    public String getDbh2RaftAcceptorName() {
        return _Dbh2RaftAcceptorName;
    }

    public void setDbh2RaftAcceptorName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Dbh2RaftAcceptorName = value;
    }

    @SuppressWarnings("deprecation")
    public BRegisterData() {
        _Dbh2RaftAcceptorName = "";
    }

    @SuppressWarnings("deprecation")
    public BRegisterData(String _Dbh2RaftAcceptorName_) {
        if (_Dbh2RaftAcceptorName_ == null)
            throw new IllegalArgumentException();
        _Dbh2RaftAcceptorName = _Dbh2RaftAcceptorName_;
    }

    @Override
    public Zeze.Builtin.Dbh2.Master.BRegister toBean() {
        var bean = new Zeze.Builtin.Dbh2.Master.BRegister();
        bean.assign(this);
        return bean;
    }

    public void assign(Zeze.Transaction.Bean other) {
        assign((BRegister)other);
    }

    public void assign(BRegister other) {
        setDbh2RaftAcceptorName(other.getDbh2RaftAcceptorName());
    }

    public void assign(BRegisterData other) {
        setDbh2RaftAcceptorName(other.getDbh2RaftAcceptorName());
    }

    @Override
    public BRegisterData copy() {
        var copy = new BRegisterData();
        copy.assign(this);
        return copy;
    }

    public static void swap(BRegisterData a, BRegisterData b) {
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.Master.BRegister: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Dbh2RaftAcceptorName=").append(getDbh2RaftAcceptorName()).append(System.lineSeparator());
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
            String _x_ = getDbh2RaftAcceptorName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setDbh2RaftAcceptorName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BRegister))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BRegister)_o_;
        if (!getDbh2RaftAcceptorName().equals(_b_.getDbh2RaftAcceptorName()))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int _p_ = 31;
        int _h_ = 0;
        _h_ = _h_ * _p_ + _Dbh2RaftAcceptorName.hashCode();
        return _h_;
    }

}
