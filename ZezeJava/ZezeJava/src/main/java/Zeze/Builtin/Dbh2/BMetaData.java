// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BMetaData extends Zeze.Transaction.Data {
    public static final long TYPEID = 7699603239757108357L;

    private String _DatabaseName;
    private String _TableName;
    private Zeze.Net.Binary _KeyFirst;
    private Zeze.Net.Binary _KeyLast;

    public String getDatabaseName() {
        return _DatabaseName;
    }

    public void setDatabaseName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _DatabaseName = value;
    }

    public String getTableName() {
        return _TableName;
    }

    public void setTableName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _TableName = value;
    }

    public Zeze.Net.Binary getKeyFirst() {
        return _KeyFirst;
    }

    public void setKeyFirst(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        _KeyFirst = value;
    }

    public Zeze.Net.Binary getKeyLast() {
        return _KeyLast;
    }

    public void setKeyLast(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        _KeyLast = value;
    }

    @SuppressWarnings("deprecation")
    public BMetaData() {
        _DatabaseName = "";
        _TableName = "";
        _KeyFirst = Zeze.Net.Binary.Empty;
        _KeyLast = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BMetaData(String _DatabaseName_, String _TableName_, Zeze.Net.Binary _KeyFirst_, Zeze.Net.Binary _KeyLast_) {
        if (_DatabaseName_ == null)
            throw new IllegalArgumentException();
        _DatabaseName = _DatabaseName_;
        if (_TableName_ == null)
            throw new IllegalArgumentException();
        _TableName = _TableName_;
        if (_KeyFirst_ == null)
            throw new IllegalArgumentException();
        _KeyFirst = _KeyFirst_;
        if (_KeyLast_ == null)
            throw new IllegalArgumentException();
        _KeyLast = _KeyLast_;
    }

    @Override
    public Zeze.Builtin.Dbh2.BMeta toBean() {
        var bean = new Zeze.Builtin.Dbh2.BMeta();
        bean.assign(this);
        return bean;
    }

    public void assign(Zeze.Transaction.Bean other) {
        assign((BMeta)other);
    }

    public void assign(BMeta other) {
        setDatabaseName(other.getDatabaseName());
        setTableName(other.getTableName());
        setKeyFirst(other.getKeyFirst());
        setKeyLast(other.getKeyLast());
    }

    public void assign(BMetaData other) {
        setDatabaseName(other.getDatabaseName());
        setTableName(other.getTableName());
        setKeyFirst(other.getKeyFirst());
        setKeyLast(other.getKeyLast());
    }

    @Override
    public BMetaData copy() {
        var copy = new BMetaData();
        copy.assign(this);
        return copy;
    }

    public static void swap(BMetaData a, BMetaData b) {
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.BMeta: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("DatabaseName=").append(getDatabaseName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("TableName=").append(getTableName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("KeyFirst=").append(getKeyFirst()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("KeyLast=").append(getKeyLast()).append(System.lineSeparator());
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
            String _x_ = getDatabaseName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getTableName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = getKeyFirst();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            var _x_ = getKeyLast();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
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
            setDatabaseName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setTableName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setKeyFirst(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setKeyLast(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

}
