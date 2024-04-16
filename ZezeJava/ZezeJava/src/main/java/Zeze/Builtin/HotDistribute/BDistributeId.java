// auto-generated @formatter:off
package Zeze.Builtin.HotDistribute;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BDistributeId extends Zeze.Transaction.Bean implements BDistributeIdReadOnly {
    public static final long TYPEID = 2872490458717325084L;

    private long _DistributeId;

    @Override
    public long getDistributeId() {
        if (!isManaged())
            return _DistributeId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _DistributeId;
        var log = (Log__DistributeId)txn.getLog(objectId() + 1);
        return log != null ? log.value : _DistributeId;
    }

    public void setDistributeId(long value) {
        if (!isManaged()) {
            _DistributeId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__DistributeId(this, 1, value));
    }

    @SuppressWarnings("deprecation")
    public BDistributeId() {
    }

    @SuppressWarnings("deprecation")
    public BDistributeId(long _DistributeId_) {
        _DistributeId = _DistributeId_;
    }

    @Override
    public void reset() {
        setDistributeId(0);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.HotDistribute.BDistributeId.Data toData() {
        var data = new Zeze.Builtin.HotDistribute.BDistributeId.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.HotDistribute.BDistributeId.Data)other);
    }

    public void assign(BDistributeId.Data other) {
        setDistributeId(other._DistributeId);
        _unknown_ = null;
    }

    public void assign(BDistributeId other) {
        setDistributeId(other.getDistributeId());
        _unknown_ = other._unknown_;
    }

    public BDistributeId copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BDistributeId copy() {
        var copy = new BDistributeId();
        copy.assign(this);
        return copy;
    }

    public static void swap(BDistributeId a, BDistributeId b) {
        BDistributeId save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__DistributeId extends Zeze.Transaction.Logs.LogLong {
        public Log__DistributeId(BDistributeId bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BDistributeId)getBelong())._DistributeId = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.HotDistribute.BDistributeId: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("DistributeId=").append(getDistributeId()).append(System.lineSeparator());
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
            long _x_ = getDistributeId();
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
            setDistributeId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean negativeCheck() {
        if (getDistributeId() < 0)
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
                case 1: _DistributeId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setDistributeId(rs.getLong(_parents_name_ + "DistributeId"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendLong(_parents_name_ + "DistributeId", getDistributeId());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "DistributeId", "long", "", ""));
        return vars;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 2872490458717325084L;

    private long _DistributeId;

    public long getDistributeId() {
        return _DistributeId;
    }

    public void setDistributeId(long value) {
        _DistributeId = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
    }

    @SuppressWarnings("deprecation")
    public Data(long _DistributeId_) {
        _DistributeId = _DistributeId_;
    }

    @Override
    public void reset() {
        _DistributeId = 0;
    }

    @Override
    public Zeze.Builtin.HotDistribute.BDistributeId toBean() {
        var bean = new Zeze.Builtin.HotDistribute.BDistributeId();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BDistributeId)other);
    }

    public void assign(BDistributeId other) {
        _DistributeId = other.getDistributeId();
    }

    public void assign(BDistributeId.Data other) {
        _DistributeId = other._DistributeId;
    }

    @Override
    public BDistributeId.Data copy() {
        var copy = new BDistributeId.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BDistributeId.Data a, BDistributeId.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BDistributeId.Data clone() {
        return (BDistributeId.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.HotDistribute.BDistributeId: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("DistributeId=").append(_DistributeId).append(System.lineSeparator());
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
            long _x_ = _DistributeId;
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
            _DistributeId = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
