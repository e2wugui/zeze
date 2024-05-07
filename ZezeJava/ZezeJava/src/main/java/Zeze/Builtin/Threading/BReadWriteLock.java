// auto-generated @formatter:off
package Zeze.Builtin.Threading;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BReadWriteLock extends Zeze.Transaction.Bean implements BReadWriteLockReadOnly {
    public static final long TYPEID = 5310988726582781550L;

    private Zeze.Builtin.Threading.BLockName _LockName;
    private int _OperateType; // see enum above
    private int _TimeoutMs; // 部分操作实际上没有使用这个参数

    @Override
    public Zeze.Builtin.Threading.BLockName getLockName() {
        if (!isManaged())
            return _LockName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _LockName;
        var log = (Log__LockName)txn.getLog(objectId() + 1);
        return log != null ? log.value : _LockName;
    }

    public void setLockName(Zeze.Builtin.Threading.BLockName value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _LockName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__LockName(this, 1, value));
    }

    @Override
    public int getOperateType() {
        if (!isManaged())
            return _OperateType;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _OperateType;
        var log = (Log__OperateType)txn.getLog(objectId() + 2);
        return log != null ? log.value : _OperateType;
    }

    public void setOperateType(int value) {
        if (!isManaged()) {
            _OperateType = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__OperateType(this, 2, value));
    }

    @Override
    public int getTimeoutMs() {
        if (!isManaged())
            return _TimeoutMs;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _TimeoutMs;
        var log = (Log__TimeoutMs)txn.getLog(objectId() + 3);
        return log != null ? log.value : _TimeoutMs;
    }

    public void setTimeoutMs(int value) {
        if (!isManaged()) {
            _TimeoutMs = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__TimeoutMs(this, 3, value));
    }

    @SuppressWarnings("deprecation")
    public BReadWriteLock() {
        _LockName = new Zeze.Builtin.Threading.BLockName();
    }

    @SuppressWarnings("deprecation")
    public BReadWriteLock(Zeze.Builtin.Threading.BLockName _LockName_, int _OperateType_, int _TimeoutMs_) {
        if (_LockName_ == null)
            _LockName_ = new Zeze.Builtin.Threading.BLockName();
        _LockName = _LockName_;
        _OperateType = _OperateType_;
        _TimeoutMs = _TimeoutMs_;
    }

    @Override
    public void reset() {
        setLockName(new Zeze.Builtin.Threading.BLockName());
        setOperateType(0);
        setTimeoutMs(0);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Threading.BReadWriteLock.Data toData() {
        var data = new Zeze.Builtin.Threading.BReadWriteLock.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Threading.BReadWriteLock.Data)other);
    }

    public void assign(BReadWriteLock.Data other) {
        setLockName(other._LockName);
        setOperateType(other._OperateType);
        setTimeoutMs(other._TimeoutMs);
        _unknown_ = null;
    }

    public void assign(BReadWriteLock other) {
        setLockName(other.getLockName());
        setOperateType(other.getOperateType());
        setTimeoutMs(other.getTimeoutMs());
        _unknown_ = other._unknown_;
    }

    public BReadWriteLock copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BReadWriteLock copy() {
        var copy = new BReadWriteLock();
        copy.assign(this);
        return copy;
    }

    public static void swap(BReadWriteLock a, BReadWriteLock b) {
        BReadWriteLock save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__LockName extends Zeze.Transaction.Logs.LogBeanKey<Zeze.Builtin.Threading.BLockName> {
        public Log__LockName(BReadWriteLock bean, int varId, Zeze.Builtin.Threading.BLockName value) { super(Zeze.Builtin.Threading.BLockName.class, bean, varId, value); }

        @Override
        public void commit() { ((BReadWriteLock)getBelong())._LockName = value; }
    }

    private static final class Log__OperateType extends Zeze.Transaction.Logs.LogInt {
        public Log__OperateType(BReadWriteLock bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BReadWriteLock)getBelong())._OperateType = value; }
    }

    private static final class Log__TimeoutMs extends Zeze.Transaction.Logs.LogInt {
        public Log__TimeoutMs(BReadWriteLock bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BReadWriteLock)getBelong())._TimeoutMs = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Threading.BReadWriteLock: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("LockName=").append(System.lineSeparator());
        getLockName().buildString(sb, level + 4);
        sb.append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("OperateType=").append(getOperateType()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("TimeoutMs=").append(getTimeoutMs()).append(System.lineSeparator());
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
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 1, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            getLockName().encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        {
            int _x_ = getOperateType();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getTimeoutMs();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
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
            _o_.ReadBean(getLockName(), _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setOperateType(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setTimeoutMs(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean negativeCheck() {
        if (getLockName().negativeCheck())
            return true;
        if (getOperateType() < 0)
            return true;
        if (getTimeoutMs() < 0)
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
                case 1: _LockName = ((Zeze.Transaction.Logs.LogBeanKey<Zeze.Builtin.Threading.BLockName>)vlog).value; break;
                case 2: _OperateType = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 3: _TimeoutMs = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        parents.add("LockName");
        getLockName().decodeResultSet(parents, rs);
        parents.remove(parents.size() - 1);
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setOperateType(rs.getInt(_parents_name_ + "OperateType"));
        setTimeoutMs(rs.getInt(_parents_name_ + "TimeoutMs"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        parents.add("LockName");
        getLockName().encodeSQLStatement(parents, st);
        parents.remove(parents.size() - 1);
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendInt(_parents_name_ + "OperateType", getOperateType());
        st.appendInt(_parents_name_ + "TimeoutMs", getTimeoutMs());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "LockName", "Zeze.Builtin.Threading.BLockName", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "OperateType", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "TimeoutMs", "int", "", ""));
        return vars;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 5310988726582781550L;

    private Zeze.Builtin.Threading.BLockName _LockName;
    private int _OperateType; // see enum above
    private int _TimeoutMs; // 部分操作实际上没有使用这个参数

    public Zeze.Builtin.Threading.BLockName getLockName() {
        return _LockName;
    }

    public void setLockName(Zeze.Builtin.Threading.BLockName value) {
        if (value == null)
            throw new IllegalArgumentException();
        _LockName = value;
    }

    public int getOperateType() {
        return _OperateType;
    }

    public void setOperateType(int value) {
        _OperateType = value;
    }

    public int getTimeoutMs() {
        return _TimeoutMs;
    }

    public void setTimeoutMs(int value) {
        _TimeoutMs = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _LockName = new Zeze.Builtin.Threading.BLockName();
    }

    @SuppressWarnings("deprecation")
    public Data(Zeze.Builtin.Threading.BLockName _LockName_, int _OperateType_, int _TimeoutMs_) {
        if (_LockName_ == null)
            _LockName_ = new Zeze.Builtin.Threading.BLockName();
        _LockName = _LockName_;
        _OperateType = _OperateType_;
        _TimeoutMs = _TimeoutMs_;
    }

    @Override
    public void reset() {
        _LockName = new Zeze.Builtin.Threading.BLockName();
        _OperateType = 0;
        _TimeoutMs = 0;
    }

    @Override
    public Zeze.Builtin.Threading.BReadWriteLock toBean() {
        var bean = new Zeze.Builtin.Threading.BReadWriteLock();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BReadWriteLock)other);
    }

    public void assign(BReadWriteLock other) {
        _LockName = other.getLockName();
        _OperateType = other.getOperateType();
        _TimeoutMs = other.getTimeoutMs();
    }

    public void assign(BReadWriteLock.Data other) {
        _LockName = other._LockName;
        _OperateType = other._OperateType;
        _TimeoutMs = other._TimeoutMs;
    }

    @Override
    public BReadWriteLock.Data copy() {
        var copy = new BReadWriteLock.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BReadWriteLock.Data a, BReadWriteLock.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BReadWriteLock.Data clone() {
        return (BReadWriteLock.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Threading.BReadWriteLock: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("LockName=").append(System.lineSeparator());
        _LockName.buildString(sb, level + 4);
        sb.append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("OperateType=").append(_OperateType).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("TimeoutMs=").append(_TimeoutMs).append(System.lineSeparator());
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
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 1, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _LockName.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        {
            int _x_ = _OperateType;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = _TimeoutMs;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _o_.ReadBean(_LockName, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _OperateType = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _TimeoutMs = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
