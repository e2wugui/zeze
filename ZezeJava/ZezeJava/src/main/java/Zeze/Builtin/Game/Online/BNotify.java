// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BNotify extends Zeze.Transaction.Bean implements BNotifyReadOnly {
    public static final long TYPEID = 663625160021568926L;

    private Zeze.Net.Binary _FullEncodedProtocol;

    @Override
    public Zeze.Net.Binary getFullEncodedProtocol() {
        if (!isManaged())
            return _FullEncodedProtocol;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _FullEncodedProtocol;
        var log = (Log__FullEncodedProtocol)txn.getLog(objectId() + 1);
        return log != null ? log.value : _FullEncodedProtocol;
    }

    public void setFullEncodedProtocol(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _FullEncodedProtocol = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__FullEncodedProtocol(this, 1, value));
    }

    @SuppressWarnings("deprecation")
    public BNotify() {
        _FullEncodedProtocol = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BNotify(Zeze.Net.Binary _FullEncodedProtocol_) {
        if (_FullEncodedProtocol_ == null)
            _FullEncodedProtocol_ = Zeze.Net.Binary.Empty;
        _FullEncodedProtocol = _FullEncodedProtocol_;
    }

    @Override
    public void reset() {
        setFullEncodedProtocol(Zeze.Net.Binary.Empty);
        _unknown_ = null;
    }

    public void assign(BNotify other) {
        setFullEncodedProtocol(other.getFullEncodedProtocol());
        _unknown_ = other._unknown_;
    }

    public BNotify copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BNotify copy() {
        var copy = new BNotify();
        copy.assign(this);
        return copy;
    }

    public static void swap(BNotify a, BNotify b) {
        BNotify save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__FullEncodedProtocol extends Zeze.Transaction.Logs.LogBinary {
        public Log__FullEncodedProtocol(BNotify bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BNotify)getBelong())._FullEncodedProtocol = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.Online.BNotify: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("FullEncodedProtocol=").append(getFullEncodedProtocol()).append(System.lineSeparator());
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
            var _x_ = getFullEncodedProtocol();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
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
            setFullEncodedProtocol(_o_.ReadBinary(_t_));
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
                case 1: _FullEncodedProtocol = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setFullEncodedProtocol(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "FullEncodedProtocol")));
        if (getFullEncodedProtocol() == null)
            setFullEncodedProtocol(Zeze.Net.Binary.Empty);
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendBinary(_parents_name_ + "FullEncodedProtocol", getFullEncodedProtocol());
    }

    @Override
    public java.util.List<Zeze.Transaction.Bean.Variable> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Transaction.Bean.Variable(1, "FullEncodedProtocol", "binary", "", ""));
        return vars;
    }

    @Override
    public Zeze.Transaction.Bean toPrevious() {
        return null; // todo BNotify
    }
}
