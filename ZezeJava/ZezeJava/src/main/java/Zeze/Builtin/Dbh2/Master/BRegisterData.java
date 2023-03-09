// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BRegisterDaTa extends Zeze.Transaction.Data {
    public static final long TYPEID = -3200018963971290421L;

    private String _Dbh2RaftAcceptorName;
    private int _NextPort; // Master用于维护已分配的端口，固定从10000开始，manager不用填写

    public String getDbh2RaftAcceptorName() {
        return _Dbh2RaftAcceptorName;
    }

    public void setDbh2RaftAcceptorName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Dbh2RaftAcceptorName = value;
    }

    public int getNextPort() {
        return _NextPort;
    }

    public void setNextPort(int value) {
        _NextPort = value;
    }

    @SuppressWarnings("deprecation")
    public BRegisterDaTa() {
        _Dbh2RaftAcceptorName = "";
    }

    @SuppressWarnings("deprecation")
    public BRegisterDaTa(String _Dbh2RaftAcceptorName_, int _NextPort_) {
        if (_Dbh2RaftAcceptorName_ == null)
            throw new IllegalArgumentException();
        _Dbh2RaftAcceptorName = _Dbh2RaftAcceptorName_;
        _NextPort = _NextPort_;
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
        setNextPort(other.getNextPort());
    }

    public void assign(BRegisterDaTa other) {
        setDbh2RaftAcceptorName(other.getDbh2RaftAcceptorName());
        setNextPort(other.getNextPort());
    }

    @Override
    public BRegisterDaTa copy() {
        var copy = new BRegisterDaTa();
        copy.assign(this);
        return copy;
    }

    public static void swap(BRegisterDaTa a, BRegisterDaTa b) {
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
        sb.append(Zeze.Util.Str.indent(level)).append("Dbh2RaftAcceptorName=").append(getDbh2RaftAcceptorName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("NextPort=").append(getNextPort()).append(System.lineSeparator());
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
        {
            int _x_ = getNextPort();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
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
        if (_i_ == 2) {
            setNextPort(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

}
