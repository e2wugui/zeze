// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BCreateDatabaseDaTa extends Zeze.Transaction.Data {
    public static final long TYPEID = -4068258744708449065L;

    private String _Database;

    public String getDatabase() {
        return _Database;
    }

    public void setDatabase(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Database = value;
    }

    @SuppressWarnings("deprecation")
    public BCreateDatabaseDaTa() {
        _Database = "";
    }

    @SuppressWarnings("deprecation")
    public BCreateDatabaseDaTa(String _Database_) {
        if (_Database_ == null)
            throw new IllegalArgumentException();
        _Database = _Database_;
    }

    @Override
    public Zeze.Builtin.Dbh2.Master.BCreateDatabase toBean() {
        var bean = new Zeze.Builtin.Dbh2.Master.BCreateDatabase();
        bean.assign(this);
        return bean;
    }

    public void assign(Zeze.Transaction.Bean other) {
        assign((BCreateDatabase)other);
    }

    public void assign(BCreateDatabase other) {
        setDatabase(other.getDatabase());
    }

    public void assign(BCreateDatabaseDaTa other) {
        setDatabase(other.getDatabase());
    }

    @Override
    public BCreateDatabaseDaTa copy() {
        var copy = new BCreateDatabaseDaTa();
        copy.assign(this);
        return copy;
    }

    public static void swap(BCreateDatabaseDaTa a, BCreateDatabaseDaTa b) {
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.Master.BCreateDatabase: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Database=").append(getDatabase()).append(System.lineSeparator());
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
            String _x_ = getDatabase();
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
            setDatabase(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

}
