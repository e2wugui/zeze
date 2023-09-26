// auto-generated @formatter:off
package Zeze.Builtin.LogService;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BSearch extends Zeze.Transaction.Bean implements BSearchReadOnly {
    public static final long TYPEID = 7436194280707275049L;

    private long _Id;
    private int _Limit;

    @Override
    public long getId() {
        if (!isManaged())
            return _Id;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Id;
        var log = (Log__Id)txn.getLog(objectId() + 1);
        return log != null ? log.value : _Id;
    }

    public void setId(long value) {
        if (!isManaged()) {
            _Id = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Id(this, 1, value));
    }

    @Override
    public int getLimit() {
        if (!isManaged())
            return _Limit;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Limit;
        var log = (Log__Limit)txn.getLog(objectId() + 2);
        return log != null ? log.value : _Limit;
    }

    public void setLimit(int value) {
        if (!isManaged()) {
            _Limit = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Limit(this, 2, value));
    }

    @SuppressWarnings("deprecation")
    public BSearch() {
    }

    @SuppressWarnings("deprecation")
    public BSearch(long _Id_, int _Limit_) {
        _Id = _Id_;
        _Limit = _Limit_;
    }

    @Override
    public void reset() {
        setId(0);
        setLimit(0);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.LogService.BSearch.Data toData() {
        var data = new Zeze.Builtin.LogService.BSearch.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.LogService.BSearch.Data)other);
    }

    public void assign(BSearch.Data other) {
        setId(other._Id);
        setLimit(other._Limit);
        _unknown_ = null;
    }

    public void assign(BSearch other) {
        setId(other.getId());
        setLimit(other.getLimit());
        _unknown_ = other._unknown_;
    }

    public BSearch copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BSearch copy() {
        var copy = new BSearch();
        copy.assign(this);
        return copy;
    }

    public static void swap(BSearch a, BSearch b) {
        BSearch save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Id extends Zeze.Transaction.Logs.LogLong {
        public Log__Id(BSearch bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BSearch)getBelong())._Id = value; }
    }

    private static final class Log__Limit extends Zeze.Transaction.Logs.LogInt {
        public Log__Limit(BSearch bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BSearch)getBelong())._Limit = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.LogService.BSearch: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Id=").append(getId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Limit=").append(getLimit()).append(System.lineSeparator());
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

    private byte[] _unknown_;

    public byte[] unknown() {
        return _unknown_;
    }

    public void clearUnknown() {
        _unknown_ = null;
    }

    @Override
    public void encode(ByteBuffer _o_) {
        ByteBuffer _u_ = null;
        var _ua_ = _unknown_;
        var _ui_ = _ua_ != null ? (_u_ = ByteBuffer.Wrap(_ua_)).readUnknownIndex() : Long.MAX_VALUE;
        int _i_ = 0;
        {
            long _x_ = getId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            int _x_ = getLimit();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        _o_.writeAllUnknownFields(_i_, _ui_, _u_);
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        ByteBuffer _u_ = null;
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setLimit(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean negativeCheck() {
        if (getId() < 0)
            return true;
        if (getLimit() < 0)
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
                case 1: _Id = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 2: _Limit = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setId(rs.getLong(_parents_name_ + "Id"));
        setLimit(rs.getInt(_parents_name_ + "Limit"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendLong(_parents_name_ + "Id", getId());
        st.appendInt(_parents_name_ + "Limit", getLimit());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Id", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Limit", "int", "", ""));
        return vars;
    }

public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 7436194280707275049L;

    private long _Id;
    private int _Limit;

    public long getId() {
        return _Id;
    }

    public void setId(long value) {
        _Id = value;
    }

    public int getLimit() {
        return _Limit;
    }

    public void setLimit(int value) {
        _Limit = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
    }

    @SuppressWarnings("deprecation")
    public Data(long _Id_, int _Limit_) {
        _Id = _Id_;
        _Limit = _Limit_;
    }

    @Override
    public void reset() {
        _Id = 0;
        _Limit = 0;
    }

    @Override
    public Zeze.Builtin.LogService.BSearch toBean() {
        var bean = new Zeze.Builtin.LogService.BSearch();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BSearch)other);
    }

    public void assign(BSearch other) {
        _Id = other.getId();
        _Limit = other.getLimit();
    }

    public void assign(BSearch.Data other) {
        _Id = other._Id;
        _Limit = other._Limit;
    }

    @Override
    public BSearch.Data copy() {
        var copy = new BSearch.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BSearch.Data a, BSearch.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BSearch.Data clone() {
        return (BSearch.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.LogService.BSearch: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Id=").append(_Id).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Limit=").append(_Limit).append(System.lineSeparator());
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
            long _x_ = _Id;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            int _x_ = _Limit;
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
            _Id = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _Limit = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
