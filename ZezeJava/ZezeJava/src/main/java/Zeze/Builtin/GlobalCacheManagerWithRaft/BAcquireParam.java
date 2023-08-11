// auto-generated @formatter:off
package Zeze.Builtin.GlobalCacheManagerWithRaft;

import Zeze.Serialize.ByteBuffer;

// rpc
@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BAcquireParam extends Zeze.Transaction.Bean implements BAcquireParamReadOnly {
    public static final long TYPEID = -8330630345134214646L;

    private Zeze.Net.Binary _GlobalKey;
    private int _State;

    @Override
    public Zeze.Net.Binary getGlobalKey() {
        if (!isManaged())
            return _GlobalKey;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _GlobalKey;
        var log = (Log__GlobalKey)txn.getLog(objectId() + 1);
        return log != null ? log.value : _GlobalKey;
    }

    public void setGlobalKey(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _GlobalKey = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__GlobalKey(this, 1, value));
    }

    @Override
    public int getState() {
        if (!isManaged())
            return _State;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _State;
        var log = (Log__State)txn.getLog(objectId() + 2);
        return log != null ? log.value : _State;
    }

    public void setState(int value) {
        if (!isManaged()) {
            _State = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__State(this, 2, value));
    }

    @SuppressWarnings("deprecation")
    public BAcquireParam() {
        _GlobalKey = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BAcquireParam(Zeze.Net.Binary _GlobalKey_, int _State_) {
        if (_GlobalKey_ == null)
            _GlobalKey_ = Zeze.Net.Binary.Empty;
        _GlobalKey = _GlobalKey_;
        _State = _State_;
    }

    @Override
    public void reset() {
        setGlobalKey(Zeze.Net.Binary.Empty);
        setState(0);
        _unknown_ = null;
    }

    public void assign(BAcquireParam other) {
        setGlobalKey(other.getGlobalKey());
        setState(other.getState());
        _unknown_ = other._unknown_;
    }

    public BAcquireParam copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BAcquireParam copy() {
        var copy = new BAcquireParam();
        copy.assign(this);
        return copy;
    }

    public static void swap(BAcquireParam a, BAcquireParam b) {
        BAcquireParam save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__GlobalKey extends Zeze.Transaction.Logs.LogBinary {
        public Log__GlobalKey(BAcquireParam bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BAcquireParam)getBelong())._GlobalKey = value; }
    }

    private static final class Log__State extends Zeze.Transaction.Logs.LogInt {
        public Log__State(BAcquireParam bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BAcquireParam)getBelong())._State = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.GlobalCacheManagerWithRaft.BAcquireParam: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("GlobalKey=").append(getGlobalKey()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("State=").append(getState()).append(System.lineSeparator());
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
            var _x_ = getGlobalKey();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            int _x_ = getState();
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
            setGlobalKey(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setState(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean negativeCheck() {
        if (getState() < 0)
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
                case 1: _GlobalKey = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
                case 2: _State = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setGlobalKey(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "GlobalKey")));
        if (getGlobalKey() == null)
            setGlobalKey(Zeze.Net.Binary.Empty);
        setState(rs.getInt(_parents_name_ + "State"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendBinary(_parents_name_ + "GlobalKey", getGlobalKey());
        st.appendInt(_parents_name_ + "State", getState());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "GlobalKey", "binary", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "State", "int", "", ""));
        return vars;
    }
}
