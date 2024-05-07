// auto-generated @formatter:off
package Zeze.Builtin.Threading;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BMutex extends Zeze.Transaction.Bean implements BMutexReadOnly {
    public static final long TYPEID = 7224301299276482451L;

    private Zeze.Builtin.Threading.BLockName _LockName;
    private int _TimeoutMs;

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
    public int getTimeoutMs() {
        if (!isManaged())
            return _TimeoutMs;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _TimeoutMs;
        var log = (Log__TimeoutMs)txn.getLog(objectId() + 2);
        return log != null ? log.value : _TimeoutMs;
    }

    public void setTimeoutMs(int value) {
        if (!isManaged()) {
            _TimeoutMs = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__TimeoutMs(this, 2, value));
    }

    @SuppressWarnings("deprecation")
    public BMutex() {
        _LockName = new Zeze.Builtin.Threading.BLockName();
    }

    @SuppressWarnings("deprecation")
    public BMutex(Zeze.Builtin.Threading.BLockName _LockName_, int _TimeoutMs_) {
        if (_LockName_ == null)
            _LockName_ = new Zeze.Builtin.Threading.BLockName();
        _LockName = _LockName_;
        _TimeoutMs = _TimeoutMs_;
    }

    @Override
    public void reset() {
        setLockName(new Zeze.Builtin.Threading.BLockName());
        setTimeoutMs(0);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Threading.BMutex.Data toData() {
        var data = new Zeze.Builtin.Threading.BMutex.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Threading.BMutex.Data)other);
    }

    public void assign(BMutex.Data other) {
        setLockName(other._LockName);
        setTimeoutMs(other._TimeoutMs);
        _unknown_ = null;
    }

    public void assign(BMutex other) {
        setLockName(other.getLockName());
        setTimeoutMs(other.getTimeoutMs());
        _unknown_ = other._unknown_;
    }

    public BMutex copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BMutex copy() {
        var copy = new BMutex();
        copy.assign(this);
        return copy;
    }

    public static void swap(BMutex a, BMutex b) {
        BMutex save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__LockName extends Zeze.Transaction.Logs.LogBeanKey<Zeze.Builtin.Threading.BLockName> {
        public Log__LockName(BMutex bean, int varId, Zeze.Builtin.Threading.BLockName value) { super(Zeze.Builtin.Threading.BLockName.class, bean, varId, value); }

        @Override
        public void commit() { ((BMutex)getBelong())._LockName = value; }
    }

    private static final class Log__TimeoutMs extends Zeze.Transaction.Logs.LogInt {
        public Log__TimeoutMs(BMutex bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BMutex)getBelong())._TimeoutMs = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Threading.BMutex: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("LockName=").append(System.lineSeparator());
        getLockName().buildString(sb, level + 4);
        sb.append(',').append(System.lineSeparator());
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
            int _x_ = getTimeoutMs();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
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
                case 2: _TimeoutMs = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        parents.add("LockName");
        getLockName().decodeResultSet(parents, rs);
        parents.remove(parents.size() - 1);
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setTimeoutMs(rs.getInt(_parents_name_ + "TimeoutMs"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        parents.add("LockName");
        getLockName().encodeSQLStatement(parents, st);
        parents.remove(parents.size() - 1);
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendInt(_parents_name_ + "TimeoutMs", getTimeoutMs());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "LockName", "Zeze.Builtin.Threading.BLockName", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "TimeoutMs", "int", "", ""));
        return vars;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 7224301299276482451L;

    private Zeze.Builtin.Threading.BLockName _LockName;
    private int _TimeoutMs;

    public Zeze.Builtin.Threading.BLockName getLockName() {
        return _LockName;
    }

    public void setLockName(Zeze.Builtin.Threading.BLockName value) {
        if (value == null)
            throw new IllegalArgumentException();
        _LockName = value;
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
    public Data(Zeze.Builtin.Threading.BLockName _LockName_, int _TimeoutMs_) {
        if (_LockName_ == null)
            _LockName_ = new Zeze.Builtin.Threading.BLockName();
        _LockName = _LockName_;
        _TimeoutMs = _TimeoutMs_;
    }

    @Override
    public void reset() {
        _LockName = new Zeze.Builtin.Threading.BLockName();
        _TimeoutMs = 0;
    }

    @Override
    public Zeze.Builtin.Threading.BMutex toBean() {
        var bean = new Zeze.Builtin.Threading.BMutex();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BMutex)other);
    }

    public void assign(BMutex other) {
        _LockName = other.getLockName();
        _TimeoutMs = other.getTimeoutMs();
    }

    public void assign(BMutex.Data other) {
        _LockName = other._LockName;
        _TimeoutMs = other._TimeoutMs;
    }

    @Override
    public BMutex.Data copy() {
        var copy = new BMutex.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BMutex.Data a, BMutex.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BMutex.Data clone() {
        return (BMutex.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Threading.BMutex: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("LockName=").append(System.lineSeparator());
        _LockName.buildString(sb, level + 4);
        sb.append(',').append(System.lineSeparator());
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
            int _x_ = _TimeoutMs;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
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
