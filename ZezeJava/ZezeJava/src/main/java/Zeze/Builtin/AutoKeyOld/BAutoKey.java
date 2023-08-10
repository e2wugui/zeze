// auto-generated @formatter:off
package Zeze.Builtin.AutoKeyOld;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BAutoKey extends Zeze.Transaction.Bean implements BAutoKeyReadOnly {
    public static final long TYPEID = 1874176782463244405L;

    private long _NextId;

    @Override
    public long getNextId() {
        if (!isManaged())
            return _NextId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _NextId;
        var log = (Log__NextId)txn.getLog(objectId() + 1);
        return log != null ? log.value : _NextId;
    }

    public void setNextId(long value) {
        if (!isManaged()) {
            _NextId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__NextId(this, 1, value));
    }

    @SuppressWarnings("deprecation")
    public BAutoKey() {
    }

    @SuppressWarnings("deprecation")
    public BAutoKey(long _NextId_) {
        _NextId = _NextId_;
    }

    @Override
    public void reset() {
        setNextId(0);
        _unknown_ = null;
    }

    public void assign(BAutoKey other) {
        setNextId(other.getNextId());
        _unknown_ = other._unknown_;
    }

    public BAutoKey copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BAutoKey copy() {
        var copy = new BAutoKey();
        copy.assign(this);
        return copy;
    }

    public static void swap(BAutoKey a, BAutoKey b) {
        BAutoKey save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__NextId extends Zeze.Transaction.Logs.LogLong {
        public Log__NextId(BAutoKey bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BAutoKey)getBelong())._NextId = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.AutoKeyOld.BAutoKey: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("NextId=").append(getNextId()).append(System.lineSeparator());
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
            long _x_ = getNextId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
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
            setNextId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean negativeCheck() {
        if (getNextId() < 0)
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
                case 1: _NextId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setNextId(rs.getLong(_parents_name_ + "NextId"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendLong(_parents_name_ + "NextId", getNextId());
    }

    @Override
    public Zeze.Transaction.Bean toPrevious() {
        return null; // todo BAutoKey
    }
}
