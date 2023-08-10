// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BLoad extends Zeze.Transaction.Bean implements BLoadReadOnly {
    public static final long TYPEID = -4353513939405897032L;

    private double _Load;

    @Override
    public double getLoad() {
        if (!isManaged())
            return _Load;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Load;
        var log = (Log__Load)txn.getLog(objectId() + 1);
        return log != null ? log.value : _Load;
    }

    public void setLoad(double value) {
        if (!isManaged()) {
            _Load = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Load(this, 1, value));
    }

    @SuppressWarnings("deprecation")
    public BLoad() {
    }

    @SuppressWarnings("deprecation")
    public BLoad(double _Load_) {
        _Load = _Load_;
    }

    @Override
    public void reset() {
        setLoad(0);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Dbh2.Master.BLoad.Data toData() {
        var data = new Zeze.Builtin.Dbh2.Master.BLoad.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Dbh2.Master.BLoad.Data)other);
    }

    public void assign(BLoad.Data other) {
        setLoad(other._Load);
        _unknown_ = null;
    }

    public void assign(BLoad other) {
        setLoad(other.getLoad());
        _unknown_ = other._unknown_;
    }

    public BLoad copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BLoad copy() {
        var copy = new BLoad();
        copy.assign(this);
        return copy;
    }

    public static void swap(BLoad a, BLoad b) {
        BLoad save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Load extends Zeze.Transaction.Logs.LogDouble {
        public Log__Load(BLoad bean, int varId, double value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BLoad)getBelong())._Load = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.Master.BLoad: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Load=").append(getLoad()).append(System.lineSeparator());
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
            double _x_ = getLoad();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.DOUBLE);
                _o_.WriteDouble(_x_);
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
            setLoad(_o_.ReadDouble(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
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
                case 1: _Load = ((Zeze.Transaction.Logs.LogDouble)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setLoad(rs.getDouble(_parents_name_ + "Load"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendDouble(_parents_name_ + "Load", getLoad());
    }

    @Override
    public Zeze.Transaction.Bean toPrevious() {
        return null; // todo BLoad
    }

public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -4353513939405897032L;

    private double _Load;

    public double getLoad() {
        return _Load;
    }

    public void setLoad(double value) {
        _Load = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
    }

    @SuppressWarnings("deprecation")
    public Data(double _Load_) {
        _Load = _Load_;
    }

    @Override
    public void reset() {
        _Load = 0;
    }

    @Override
    public Zeze.Builtin.Dbh2.Master.BLoad toBean() {
        var bean = new Zeze.Builtin.Dbh2.Master.BLoad();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BLoad)other);
    }

    public void assign(BLoad other) {
        _Load = other.getLoad();
    }

    public void assign(BLoad.Data other) {
        _Load = other._Load;
    }

    @Override
    public BLoad.Data copy() {
        var copy = new BLoad.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BLoad.Data a, BLoad.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BLoad.Data clone() {
        return (BLoad.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.Master.BLoad: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Load=").append(_Load).append(System.lineSeparator());
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
            double _x_ = _Load;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.DOUBLE);
                _o_.WriteDouble(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _Load = _o_.ReadDouble(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
