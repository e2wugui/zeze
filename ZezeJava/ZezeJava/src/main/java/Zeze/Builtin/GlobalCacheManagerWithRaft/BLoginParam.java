// auto-generated @formatter:off
package Zeze.Builtin.GlobalCacheManagerWithRaft;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BLoginParam extends Zeze.Transaction.Bean implements BLoginParamReadOnly {
    public static final long TYPEID = 9076855952725286109L;

    private int _ServerId;
    private int _GlobalCacheManagerHashIndex;
    private boolean _DebugMode; // 调试模式下不检查Release Timeout,方便单步调试

    @Override
    public int getServerId() {
        if (!isManaged())
            return _ServerId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ServerId;
        var log = (Log__ServerId)txn.getLog(objectId() + 1);
        return log != null ? log.value : _ServerId;
    }

    public void setServerId(int value) {
        if (!isManaged()) {
            _ServerId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ServerId(this, 1, value));
    }

    @Override
    public int getGlobalCacheManagerHashIndex() {
        if (!isManaged())
            return _GlobalCacheManagerHashIndex;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _GlobalCacheManagerHashIndex;
        var log = (Log__GlobalCacheManagerHashIndex)txn.getLog(objectId() + 2);
        return log != null ? log.value : _GlobalCacheManagerHashIndex;
    }

    public void setGlobalCacheManagerHashIndex(int value) {
        if (!isManaged()) {
            _GlobalCacheManagerHashIndex = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__GlobalCacheManagerHashIndex(this, 2, value));
    }

    @Override
    public boolean isDebugMode() {
        if (!isManaged())
            return _DebugMode;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _DebugMode;
        var log = (Log__DebugMode)txn.getLog(objectId() + 3);
        return log != null ? log.value : _DebugMode;
    }

    public void setDebugMode(boolean value) {
        if (!isManaged()) {
            _DebugMode = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__DebugMode(this, 3, value));
    }

    @SuppressWarnings("deprecation")
    public BLoginParam() {
    }

    @SuppressWarnings("deprecation")
    public BLoginParam(int _ServerId_, int _GlobalCacheManagerHashIndex_, boolean _DebugMode_) {
        _ServerId = _ServerId_;
        _GlobalCacheManagerHashIndex = _GlobalCacheManagerHashIndex_;
        _DebugMode = _DebugMode_;
    }

    @Override
    public void reset() {
        setServerId(0);
        setGlobalCacheManagerHashIndex(0);
        setDebugMode(false);
        _unknown_ = null;
    }

    public void assign(BLoginParam other) {
        setServerId(other.getServerId());
        setGlobalCacheManagerHashIndex(other.getGlobalCacheManagerHashIndex());
        setDebugMode(other.isDebugMode());
        _unknown_ = other._unknown_;
    }

    public BLoginParam copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BLoginParam copy() {
        var copy = new BLoginParam();
        copy.assign(this);
        return copy;
    }

    public static void swap(BLoginParam a, BLoginParam b) {
        BLoginParam save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ServerId extends Zeze.Transaction.Logs.LogInt {
        public Log__ServerId(BLoginParam bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BLoginParam)getBelong())._ServerId = value; }
    }

    private static final class Log__GlobalCacheManagerHashIndex extends Zeze.Transaction.Logs.LogInt {
        public Log__GlobalCacheManagerHashIndex(BLoginParam bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BLoginParam)getBelong())._GlobalCacheManagerHashIndex = value; }
    }

    private static final class Log__DebugMode extends Zeze.Transaction.Logs.LogBool {
        public Log__DebugMode(BLoginParam bean, int varId, boolean value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BLoginParam)getBelong())._DebugMode = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.GlobalCacheManagerWithRaft.BLoginParam: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ServerId=").append(getServerId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("GlobalCacheManagerHashIndex=").append(getGlobalCacheManagerHashIndex()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("DebugMode=").append(isDebugMode()).append(System.lineSeparator());
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
            int _x_ = getServerId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getGlobalCacheManagerHashIndex();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            boolean _x_ = isDebugMode();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
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
            setServerId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setGlobalCacheManagerHashIndex(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setDebugMode(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BLoginParam))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BLoginParam)_o_;
        if (getServerId() != _b_.getServerId())
            return false;
        if (getGlobalCacheManagerHashIndex() != _b_.getGlobalCacheManagerHashIndex())
            return false;
        if (isDebugMode() != _b_.isDebugMode())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getServerId() < 0)
            return true;
        if (getGlobalCacheManagerHashIndex() < 0)
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
                case 1: _ServerId = vlog.intValue(); break;
                case 2: _GlobalCacheManagerHashIndex = vlog.intValue(); break;
                case 3: _DebugMode = vlog.booleanValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setServerId(rs.getInt(_parents_name_ + "ServerId"));
        setGlobalCacheManagerHashIndex(rs.getInt(_parents_name_ + "GlobalCacheManagerHashIndex"));
        setDebugMode(rs.getBoolean(_parents_name_ + "DebugMode"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendInt(_parents_name_ + "ServerId", getServerId());
        st.appendInt(_parents_name_ + "GlobalCacheManagerHashIndex", getGlobalCacheManagerHashIndex());
        st.appendBoolean(_parents_name_ + "DebugMode", isDebugMode());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "ServerId", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "GlobalCacheManagerHashIndex", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "DebugMode", "bool", "", ""));
        return vars;
    }
}
