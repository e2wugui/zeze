// auto-generated @formatter:off
package Zeze.Builtin.HistoryModule;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BLogChanges extends Zeze.Transaction.Bean implements BLogChangesReadOnly {
    public static final long TYPEID = 395935719895809559L;

    private long _GlobalSerialId;
    private Zeze.Net.Binary _Encoded;
    private Zeze.Net.Binary _ProtocolArgument;

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
    public Zeze.Net.Binary getEncoded() {
        if (!isManaged())
            return _Encoded;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Encoded;
        var log = (Log__Encoded)txn.getLog(objectId() + 2);
        return log != null ? log.value : _Encoded;
    }

    public void setEncoded(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Encoded = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Encoded(this, 2, value));
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

    @SuppressWarnings("deprecation")
    public BLogChanges() {
        _Encoded = Zeze.Net.Binary.Empty;
        _ProtocolArgument = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BLogChanges(long _GlobalSerialId_, Zeze.Net.Binary _Encoded_, Zeze.Net.Binary _ProtocolArgument_) {
        _GlobalSerialId = _GlobalSerialId_;
        if (_Encoded_ == null)
            _Encoded_ = Zeze.Net.Binary.Empty;
        _Encoded = _Encoded_;
        if (_ProtocolArgument_ == null)
            _ProtocolArgument_ = Zeze.Net.Binary.Empty;
        _ProtocolArgument = _ProtocolArgument_;
    }

    @Override
    public void reset() {
        setGlobalSerialId(0);
        setEncoded(Zeze.Net.Binary.Empty);
        setProtocolArgument(Zeze.Net.Binary.Empty);
        _unknown_ = null;
    }

    public void assign(BLogChanges other) {
        setGlobalSerialId(other.getGlobalSerialId());
        setEncoded(other.getEncoded());
        setProtocolArgument(other.getProtocolArgument());
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

    private static final class Log__Encoded extends Zeze.Transaction.Logs.LogBinary {
        public Log__Encoded(BLogChanges bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BLogChanges)getBelong())._Encoded = value; }
    }

    private static final class Log__ProtocolArgument extends Zeze.Transaction.Logs.LogBinary {
        public Log__ProtocolArgument(BLogChanges bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BLogChanges)getBelong())._ProtocolArgument = value; }
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
        sb.append(Zeze.Util.Str.indent(level)).append("Encoded=").append(getEncoded()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ProtocolArgument=").append(getProtocolArgument()).append(System.lineSeparator());
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
        while (_ui_ < 3) {
            _i_ = _o_.writeUnknownField(_i_, _ui_, _u_);
            _ui_ = _u_.readUnknownIndex();
        }
        {
            var _x_ = getProtocolArgument();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
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
        while ((_t_ & 0xff) > 1 && _i_ < 3) {
            _u_ = _o_.readUnknownField(_i_, _t_, _u_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setProtocolArgument(_o_.ReadBinary(_t_));
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
                case 3: _ProtocolArgument = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setGlobalSerialId(rs.getLong(_parents_name_ + "GlobalSerialId"));
        setProtocolArgument(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "ProtocolArgument")));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendLong(_parents_name_ + "GlobalSerialId", getGlobalSerialId());
        st.appendBinary(_parents_name_ + "ProtocolArgument", getProtocolArgument());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "GlobalSerialId", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Encoded", "binary", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "ProtocolArgument", "binary", "", ""));
        return vars;
    }
}
