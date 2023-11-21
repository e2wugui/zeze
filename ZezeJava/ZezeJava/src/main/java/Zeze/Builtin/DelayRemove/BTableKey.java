// auto-generated @formatter:off
package Zeze.Builtin.DelayRemove;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BTableKey extends Zeze.Transaction.Bean implements BTableKeyReadOnly {
    public static final long TYPEID = 6060766480176216446L;

    private String _TableName;
    private Zeze.Net.Binary _EncodedKey;
    private long _EnqueueTime;

    @Override
    public String getTableName() {
        if (!isManaged())
            return _TableName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _TableName;
        var log = (Log__TableName)txn.getLog(objectId() + 1);
        return log != null ? log.value : _TableName;
    }

    public void setTableName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _TableName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__TableName(this, 1, value));
    }

    @Override
    public Zeze.Net.Binary getEncodedKey() {
        if (!isManaged())
            return _EncodedKey;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _EncodedKey;
        var log = (Log__EncodedKey)txn.getLog(objectId() + 2);
        return log != null ? log.value : _EncodedKey;
    }

    public void setEncodedKey(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _EncodedKey = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__EncodedKey(this, 2, value));
    }

    @Override
    public long getEnqueueTime() {
        if (!isManaged())
            return _EnqueueTime;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _EnqueueTime;
        var log = (Log__EnqueueTime)txn.getLog(objectId() + 3);
        return log != null ? log.value : _EnqueueTime;
    }

    public void setEnqueueTime(long value) {
        if (!isManaged()) {
            _EnqueueTime = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__EnqueueTime(this, 3, value));
    }

    @SuppressWarnings("deprecation")
    public BTableKey() {
        _TableName = "";
        _EncodedKey = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BTableKey(String _TableName_, Zeze.Net.Binary _EncodedKey_, long _EnqueueTime_) {
        if (_TableName_ == null)
            _TableName_ = "";
        _TableName = _TableName_;
        if (_EncodedKey_ == null)
            _EncodedKey_ = Zeze.Net.Binary.Empty;
        _EncodedKey = _EncodedKey_;
        _EnqueueTime = _EnqueueTime_;
    }

    @Override
    public void reset() {
        setTableName("");
        setEncodedKey(Zeze.Net.Binary.Empty);
        setEnqueueTime(0);
        _unknown_ = null;
    }

    public void assign(BTableKey other) {
        setTableName(other.getTableName());
        setEncodedKey(other.getEncodedKey());
        setEnqueueTime(other.getEnqueueTime());
        _unknown_ = other._unknown_;
    }

    public BTableKey copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTableKey copy() {
        var copy = new BTableKey();
        copy.assign(this);
        return copy;
    }

    public static void swap(BTableKey a, BTableKey b) {
        BTableKey save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__TableName extends Zeze.Transaction.Logs.LogString {
        public Log__TableName(BTableKey bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTableKey)getBelong())._TableName = value; }
    }

    private static final class Log__EncodedKey extends Zeze.Transaction.Logs.LogBinary {
        public Log__EncodedKey(BTableKey bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTableKey)getBelong())._EncodedKey = value; }
    }

    private static final class Log__EnqueueTime extends Zeze.Transaction.Logs.LogLong {
        public Log__EnqueueTime(BTableKey bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTableKey)getBelong())._EnqueueTime = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.DelayRemove.BTableKey: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("TableName=").append(getTableName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("EncodedKey=").append(getEncodedKey()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("EnqueueTime=").append(getEnqueueTime()).append(System.lineSeparator());
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
            String _x_ = getTableName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = getEncodedKey();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            long _x_ = getEnqueueTime();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
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
            setTableName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setEncodedKey(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setEnqueueTime(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean negativeCheck() {
        if (getEnqueueTime() < 0)
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
                case 1: _TableName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 2: _EncodedKey = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
                case 3: _EnqueueTime = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setTableName(rs.getString(_parents_name_ + "TableName"));
        if (getTableName() == null)
            setTableName("");
        setEncodedKey(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "EncodedKey")));
        if (getEncodedKey() == null)
            setEncodedKey(Zeze.Net.Binary.Empty);
        setEnqueueTime(rs.getLong(_parents_name_ + "EnqueueTime"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "TableName", getTableName());
        st.appendBinary(_parents_name_ + "EncodedKey", getEncodedKey());
        st.appendLong(_parents_name_ + "EnqueueTime", getEnqueueTime());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "TableName", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "EncodedKey", "binary", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "EnqueueTime", "long", "", ""));
        return vars;
    }
}
