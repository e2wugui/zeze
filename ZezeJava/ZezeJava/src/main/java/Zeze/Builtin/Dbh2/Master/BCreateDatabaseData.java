// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BCreateDatabaseData extends Zeze.Transaction.Data {
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
    public BCreateDatabaseData() {
        _Database = "";
    }

    @SuppressWarnings("deprecation")
    public BCreateDatabaseData(String _Database_) {
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

    public void assign(BCreateDatabaseData other) {
        setDatabase(other.getDatabase());
    }

    @Override
    public BCreateDatabaseData copy() {
        var copy = new BCreateDatabaseData();
        copy.assign(this);
        return copy;
    }

    public static void swap(BCreateDatabaseData a, BCreateDatabaseData b) {
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

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BCreateDatabase))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BCreateDatabase)_o_;
        if (!getDatabase().equals(_b_.getDatabase()))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int _p_ = 31;
        int _h_ = 0;
        _h_ = _h_ * _p_ + _Database.hashCode();
        return _h_;
    }

}
