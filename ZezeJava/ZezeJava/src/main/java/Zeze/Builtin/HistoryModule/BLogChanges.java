// auto-generated @formatter:off
package Zeze.Builtin.HistoryModule;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BLogChanges extends Zeze.Transaction.Bean implements BLogChangesReadOnly {
    public static final long TYPEID = 395935719895809559L;

    private long _GlobalSerialId;
    private String _ProtocolClassName;
    private Zeze.Net.Binary _ProtocolArgument;
    private Zeze.Net.Binary _Changes;

    @Override
    public long getGlobalSerialId() {
        if (!isManaged())
            return _GlobalSerialId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _GlobalSerialId;
        var log = (Log__GlobalSerialId)txn.getLog(objectId() + 1);
        return log != null ? log.value : _GlobalSerialId;
    }

    public void setGlobalSerialId(long value) {
        if (!isManaged()) {
            _GlobalSerialId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__GlobalSerialId(this, 1, value));
    }

    @Override
    public String getProtocolClassName() {
        if (!isManaged())
            return _ProtocolClassName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ProtocolClassName;
        var log = (Log__ProtocolClassName)txn.getLog(objectId() + 2);
        return log != null ? log.value : _ProtocolClassName;
    }

    public void setProtocolClassName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ProtocolClassName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ProtocolClassName(this, 2, value));
    }

    @Override
    public Zeze.Net.Binary getProtocolArgument() {
        if (!isManaged())
            return _ProtocolArgument;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ProtocolArgument;
        var log = (Log__ProtocolArgument)txn.getLog(objectId() + 3);
        return log != null ? log.value : _ProtocolArgument;
    }

    public void setProtocolArgument(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ProtocolArgument = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ProtocolArgument(this, 3, value));
    }

    @Override
    public Zeze.Net.Binary getChanges() {
        if (!isManaged())
            return _Changes;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Changes;
        var log = (Log__Changes)txn.getLog(objectId() + 4);
        return log != null ? log.value : _Changes;
    }

    public void setChanges(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Changes = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Changes(this, 4, value));
    }

    @SuppressWarnings("deprecation")
    public BLogChanges() {
        _ProtocolClassName = "";
        _ProtocolArgument = Zeze.Net.Binary.Empty;
        _Changes = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BLogChanges(long _GlobalSerialId_, String _ProtocolClassName_, Zeze.Net.Binary _ProtocolArgument_, Zeze.Net.Binary _Changes_) {
        _GlobalSerialId = _GlobalSerialId_;
        if (_ProtocolClassName_ == null)
            _ProtocolClassName_ = "";
        _ProtocolClassName = _ProtocolClassName_;
        if (_ProtocolArgument_ == null)
            _ProtocolArgument_ = Zeze.Net.Binary.Empty;
        _ProtocolArgument = _ProtocolArgument_;
        if (_Changes_ == null)
            _Changes_ = Zeze.Net.Binary.Empty;
        _Changes = _Changes_;
    }

    @Override
    public void reset() {
        setGlobalSerialId(0);
        setProtocolClassName("");
        setProtocolArgument(Zeze.Net.Binary.Empty);
        setChanges(Zeze.Net.Binary.Empty);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.HistoryModule.BLogChanges.Data toData() {
        var data = new Zeze.Builtin.HistoryModule.BLogChanges.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.HistoryModule.BLogChanges.Data)other);
    }

    public void assign(BLogChanges.Data other) {
        setGlobalSerialId(other._GlobalSerialId);
        setProtocolClassName(other._ProtocolClassName);
        setProtocolArgument(other._ProtocolArgument);
        setChanges(other._Changes);
        _unknown_ = null;
    }

    public void assign(BLogChanges other) {
        setGlobalSerialId(other.getGlobalSerialId());
        setProtocolClassName(other.getProtocolClassName());
        setProtocolArgument(other.getProtocolArgument());
        setChanges(other.getChanges());
        _unknown_ = other._unknown_;
    }

    public BLogChanges copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BLogChanges copy() {
        var copy = new BLogChanges();
        copy.assign(this);
        return copy;
    }

    public static void swap(BLogChanges a, BLogChanges b) {
        BLogChanges save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__GlobalSerialId extends Zeze.Transaction.Logs.LogLong {
        public Log__GlobalSerialId(BLogChanges bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BLogChanges)getBelong())._GlobalSerialId = value; }
    }

    private static final class Log__ProtocolClassName extends Zeze.Transaction.Logs.LogString {
        public Log__ProtocolClassName(BLogChanges bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BLogChanges)getBelong())._ProtocolClassName = value; }
    }

    private static final class Log__ProtocolArgument extends Zeze.Transaction.Logs.LogBinary {
        public Log__ProtocolArgument(BLogChanges bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BLogChanges)getBelong())._ProtocolArgument = value; }
    }

    private static final class Log__Changes extends Zeze.Transaction.Logs.LogBinary {
        public Log__Changes(BLogChanges bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BLogChanges)getBelong())._Changes = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.HistoryModule.BLogChanges: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("GlobalSerialId=").append(getGlobalSerialId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ProtocolClassName=").append(getProtocolClassName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ProtocolArgument=").append(getProtocolArgument()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Changes=").append(getChanges()).append(System.lineSeparator());
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
            long _x_ = getGlobalSerialId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            String _x_ = getProtocolClassName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = getProtocolArgument();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            var _x_ = getChanges();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
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
            setGlobalSerialId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setProtocolClassName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setProtocolArgument(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setChanges(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean negativeCheck() {
        if (getGlobalSerialId() < 0)
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
                case 1: _GlobalSerialId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 2: _ProtocolClassName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 3: _ProtocolArgument = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
                case 4: _Changes = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setGlobalSerialId(rs.getLong(_parents_name_ + "GlobalSerialId"));
        setProtocolClassName(rs.getString(_parents_name_ + "ProtocolClassName"));
        if (getProtocolClassName() == null)
            setProtocolClassName("");
        setProtocolArgument(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "ProtocolArgument")));
        setChanges(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "Changes")));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendLong(_parents_name_ + "GlobalSerialId", getGlobalSerialId());
        st.appendString(_parents_name_ + "ProtocolClassName", getProtocolClassName());
        st.appendBinary(_parents_name_ + "ProtocolArgument", getProtocolArgument());
        st.appendBinary(_parents_name_ + "Changes", getChanges());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "GlobalSerialId", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "ProtocolClassName", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "ProtocolArgument", "binary", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "Changes", "binary", "", ""));
        return vars;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 395935719895809559L;

    private long _GlobalSerialId;
    private String _ProtocolClassName;
    private Zeze.Net.Binary _ProtocolArgument;
    private Zeze.Net.Binary _Changes;

    public long getGlobalSerialId() {
        return _GlobalSerialId;
    }

    public void setGlobalSerialId(long value) {
        _GlobalSerialId = value;
    }

    public String getProtocolClassName() {
        return _ProtocolClassName;
    }

    public void setProtocolClassName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _ProtocolClassName = value;
    }

    public Zeze.Net.Binary getProtocolArgument() {
        return _ProtocolArgument;
    }

    public void setProtocolArgument(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        _ProtocolArgument = value;
    }

    public Zeze.Net.Binary getChanges() {
        return _Changes;
    }

    public void setChanges(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Changes = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _ProtocolClassName = "";
        _ProtocolArgument = Zeze.Net.Binary.Empty;
        _Changes = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public Data(long _GlobalSerialId_, String _ProtocolClassName_, Zeze.Net.Binary _ProtocolArgument_, Zeze.Net.Binary _Changes_) {
        _GlobalSerialId = _GlobalSerialId_;
        if (_ProtocolClassName_ == null)
            _ProtocolClassName_ = "";
        _ProtocolClassName = _ProtocolClassName_;
        if (_ProtocolArgument_ == null)
            _ProtocolArgument_ = Zeze.Net.Binary.Empty;
        _ProtocolArgument = _ProtocolArgument_;
        if (_Changes_ == null)
            _Changes_ = Zeze.Net.Binary.Empty;
        _Changes = _Changes_;
    }

    @Override
    public void reset() {
        _GlobalSerialId = 0;
        _ProtocolClassName = "";
        _ProtocolArgument = Zeze.Net.Binary.Empty;
        _Changes = Zeze.Net.Binary.Empty;
    }

    @Override
    public Zeze.Builtin.HistoryModule.BLogChanges toBean() {
        var bean = new Zeze.Builtin.HistoryModule.BLogChanges();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BLogChanges)other);
    }

    public void assign(BLogChanges other) {
        _GlobalSerialId = other.getGlobalSerialId();
        _ProtocolClassName = other.getProtocolClassName();
        _ProtocolArgument = other.getProtocolArgument();
        _Changes = other.getChanges();
    }

    public void assign(BLogChanges.Data other) {
        _GlobalSerialId = other._GlobalSerialId;
        _ProtocolClassName = other._ProtocolClassName;
        _ProtocolArgument = other._ProtocolArgument;
        _Changes = other._Changes;
    }

    @Override
    public BLogChanges.Data copy() {
        var copy = new BLogChanges.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BLogChanges.Data a, BLogChanges.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BLogChanges.Data clone() {
        return (BLogChanges.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.HistoryModule.BLogChanges: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("GlobalSerialId=").append(_GlobalSerialId).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ProtocolClassName=").append(_ProtocolClassName).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ProtocolArgument=").append(_ProtocolArgument).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Changes=").append(_Changes).append(System.lineSeparator());
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
            long _x_ = _GlobalSerialId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            String _x_ = _ProtocolClassName;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = _ProtocolArgument;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            var _x_ = _Changes;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _GlobalSerialId = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _ProtocolClassName = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _ProtocolArgument = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _Changes = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
