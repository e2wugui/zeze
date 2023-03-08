// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BBeginTransactionArgumentData extends Zeze.Transaction.Data {
    public static final long TYPEID = -7619569472530558952L;

    private String _Database;
    private String _Table;

    public String getDatabase() {
        return _Database;
    }

    public void setDatabase(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Database = value;
    }

    public String getTable() {
        return _Table;
    }

    public void setTable(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Table = value;
    }

    @SuppressWarnings("deprecation")
    public BBeginTransactionArgumentData() {
        _Database = "";
        _Table = "";
    }

    @SuppressWarnings("deprecation")
    public BBeginTransactionArgumentData(String _Database_, String _Table_) {
        if (_Database_ == null)
            throw new IllegalArgumentException();
        _Database = _Database_;
        if (_Table_ == null)
            throw new IllegalArgumentException();
        _Table = _Table_;
    }

    @Override
    public Zeze.Builtin.Dbh2.BBeginTransactionArgument toBean() {
        var bean = new Zeze.Builtin.Dbh2.BBeginTransactionArgument();
        bean.assign(this);
        return bean;
    }

    public void assign(Zeze.Transaction.Bean other) {
        assign((BBeginTransactionArgument)other);
    }

    public void assign(BBeginTransactionArgument other) {
        setDatabase(other.getDatabase());
        setTable(other.getTable());
    }

    public void assign(BBeginTransactionArgumentData other) {
        setDatabase(other.getDatabase());
        setTable(other.getTable());
    }

    @Override
    public BBeginTransactionArgumentData copy() {
        var copy = new BBeginTransactionArgumentData();
        copy.assign(this);
        return copy;
    }

    public static void swap(BBeginTransactionArgumentData a, BBeginTransactionArgumentData b) {
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.BBeginTransactionArgument: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Database=").append(getDatabase()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Table=").append(getTable()).append(System.lineSeparator());
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
        {
            String _x_ = getTable();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
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
        if (_i_ == 2) {
            setTable(_o_.ReadString(_t_));
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
        if (!(_o_ instanceof BBeginTransactionArgument))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BBeginTransactionArgument)_o_;
        if (!getDatabase().equals(_b_.getDatabase()))
            return false;
        if (!getTable().equals(_b_.getTable()))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int _p_ = 31;
        int _h_ = 0;
        _h_ = _h_ * _p_ + _Database.hashCode();
        _h_ = _h_ * _p_ + _Table.hashCode();
        return _h_;
    }

}
