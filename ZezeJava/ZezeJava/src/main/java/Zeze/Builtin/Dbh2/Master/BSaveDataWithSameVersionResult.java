// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BSaveDataWithSameVersionResult extends Zeze.Transaction.Bean implements BSaveDataWithSameVersionResultReadOnly {
    public static final long TYPEID = 1346251189599769952L;

    private long _Version;

    @Override
    public long getVersion() {
        if (!isManaged())
            return _Version;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Version;
        var log = (Log__Version)txn.getLog(objectId() + 1);
        return log != null ? log.value : _Version;
    }

    public void setVersion(long value) {
        if (!isManaged()) {
            _Version = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Version(this, 1, value));
    }

    @SuppressWarnings("deprecation")
    public BSaveDataWithSameVersionResult() {
    }

    @SuppressWarnings("deprecation")
    public BSaveDataWithSameVersionResult(long _Version_) {
        _Version = _Version_;
    }

    @Override
    public void reset() {
        setVersion(0);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Dbh2.Master.BSaveDataWithSameVersionResult.Data toData() {
        var data = new Zeze.Builtin.Dbh2.Master.BSaveDataWithSameVersionResult.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Dbh2.Master.BSaveDataWithSameVersionResult.Data)other);
    }

    public void assign(BSaveDataWithSameVersionResult.Data other) {
        setVersion(other._Version);
        _unknown_ = null;
    }

    public void assign(BSaveDataWithSameVersionResult other) {
        setVersion(other.getVersion());
        _unknown_ = other._unknown_;
    }

    public BSaveDataWithSameVersionResult copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BSaveDataWithSameVersionResult copy() {
        var copy = new BSaveDataWithSameVersionResult();
        copy.assign(this);
        return copy;
    }

    public static void swap(BSaveDataWithSameVersionResult a, BSaveDataWithSameVersionResult b) {
        BSaveDataWithSameVersionResult save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Version extends Zeze.Transaction.Logs.LogLong {
        public Log__Version(BSaveDataWithSameVersionResult bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BSaveDataWithSameVersionResult)getBelong())._Version = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.Master.BSaveDataWithSameVersionResult: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Version=").append(getVersion()).append(System.lineSeparator());
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
            long _x_ = getVersion();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        _o_.writeAllUnknownFields(_i_, _ui_, _u_);
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        ByteBuffer _u_ = null;
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setVersion(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean negativeCheck() {
        if (getVersion() < 0)
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
                case 1: _Version = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setVersion(rs.getLong(_parents_name_ + "Version"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendLong(_parents_name_ + "Version", getVersion());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Version", "long", "", ""));
        return vars;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 1346251189599769952L;

    private long _Version;

    public long getVersion() {
        return _Version;
    }

    public void setVersion(long value) {
        _Version = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
    }

    @SuppressWarnings("deprecation")
    public Data(long _Version_) {
        _Version = _Version_;
    }

    @Override
    public void reset() {
        _Version = 0;
    }

    @Override
    public Zeze.Builtin.Dbh2.Master.BSaveDataWithSameVersionResult toBean() {
        var bean = new Zeze.Builtin.Dbh2.Master.BSaveDataWithSameVersionResult();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BSaveDataWithSameVersionResult)other);
    }

    public void assign(BSaveDataWithSameVersionResult other) {
        _Version = other.getVersion();
    }

    public void assign(BSaveDataWithSameVersionResult.Data other) {
        _Version = other._Version;
    }

    @Override
    public BSaveDataWithSameVersionResult.Data copy() {
        var copy = new BSaveDataWithSameVersionResult.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BSaveDataWithSameVersionResult.Data a, BSaveDataWithSameVersionResult.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BSaveDataWithSameVersionResult.Data clone() {
        return (BSaveDataWithSameVersionResult.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.Master.BSaveDataWithSameVersionResult: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Version=").append(_Version).append(System.lineSeparator());
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append('}');
    }

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
            long _x_ = _Version;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _Version = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
