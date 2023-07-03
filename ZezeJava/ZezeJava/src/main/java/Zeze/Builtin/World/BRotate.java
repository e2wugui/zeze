// auto-generated @formatter:off
package Zeze.Builtin.World;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BRotate extends Zeze.Transaction.Bean implements BRotateReadOnly {
    public static final long TYPEID = -7391596098548970887L;

    private Zeze.Serialize.Vector3 _Current; // 转身命令时客户端的真实位置。
    private Zeze.Serialize.Vector3 _Direct; // 转身命令时的面向方向。
    private boolean _TurnDirect; // 转身方向。
    private long _Timestamp; // 命令发起时刻的时戳。

    @Override
    public Zeze.Serialize.Vector3 getCurrent() {
        if (!isManaged())
            return _Current;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Current;
        var log = (Log__Current)txn.getLog(objectId() + 1);
        return log != null ? log.value : _Current;
    }

    public void setCurrent(Zeze.Serialize.Vector3 value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Current = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Current(this, 1, value));
    }

    @Override
    public Zeze.Serialize.Vector3 getDirect() {
        if (!isManaged())
            return _Direct;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Direct;
        var log = (Log__Direct)txn.getLog(objectId() + 2);
        return log != null ? log.value : _Direct;
    }

    public void setDirect(Zeze.Serialize.Vector3 value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Direct = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Direct(this, 2, value));
    }

    @Override
    public boolean isTurnDirect() {
        if (!isManaged())
            return _TurnDirect;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _TurnDirect;
        var log = (Log__TurnDirect)txn.getLog(objectId() + 3);
        return log != null ? log.value : _TurnDirect;
    }

    public void setTurnDirect(boolean value) {
        if (!isManaged()) {
            _TurnDirect = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__TurnDirect(this, 3, value));
    }

    @Override
    public long getTimestamp() {
        if (!isManaged())
            return _Timestamp;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Timestamp;
        var log = (Log__Timestamp)txn.getLog(objectId() + 4);
        return log != null ? log.value : _Timestamp;
    }

    public void setTimestamp(long value) {
        if (!isManaged()) {
            _Timestamp = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Timestamp(this, 4, value));
    }

    @SuppressWarnings("deprecation")
    public BRotate() {
        _Current = Zeze.Serialize.Vector3.ZERO;
        _Direct = Zeze.Serialize.Vector3.ZERO;
    }

    @SuppressWarnings("deprecation")
    public BRotate(Zeze.Serialize.Vector3 _Current_, Zeze.Serialize.Vector3 _Direct_, boolean _TurnDirect_, long _Timestamp_) {
        if (_Current_ == null)
            _Current_ = Zeze.Serialize.Vector3.ZERO;
        _Current = _Current_;
        if (_Direct_ == null)
            _Direct_ = Zeze.Serialize.Vector3.ZERO;
        _Direct = _Direct_;
        _TurnDirect = _TurnDirect_;
        _Timestamp = _Timestamp_;
    }

    @Override
    public Zeze.Builtin.World.BRotate.Data toData() {
        var data = new Zeze.Builtin.World.BRotate.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.World.BRotate.Data)other);
    }

    public void assign(BRotate.Data other) {
        setCurrent(other._Current);
        setDirect(other._Direct);
        setTurnDirect(other._TurnDirect);
        setTimestamp(other._Timestamp);
    }

    public void assign(BRotate other) {
        setCurrent(other.getCurrent());
        setDirect(other.getDirect());
        setTurnDirect(other.isTurnDirect());
        setTimestamp(other.getTimestamp());
    }

    public BRotate copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BRotate copy() {
        var copy = new BRotate();
        copy.assign(this);
        return copy;
    }

    public static void swap(BRotate a, BRotate b) {
        BRotate save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Current extends Zeze.Transaction.Logs.LogVector3 {
        public Log__Current(BRotate bean, int varId, Zeze.Serialize.Vector3 value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BRotate)getBelong())._Current = value; }
    }

    private static final class Log__Direct extends Zeze.Transaction.Logs.LogVector3 {
        public Log__Direct(BRotate bean, int varId, Zeze.Serialize.Vector3 value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BRotate)getBelong())._Direct = value; }
    }

    private static final class Log__TurnDirect extends Zeze.Transaction.Logs.LogBool {
        public Log__TurnDirect(BRotate bean, int varId, boolean value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BRotate)getBelong())._TurnDirect = value; }
    }

    private static final class Log__Timestamp extends Zeze.Transaction.Logs.LogLong {
        public Log__Timestamp(BRotate bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BRotate)getBelong())._Timestamp = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.World.BRotate: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Current=").append(getCurrent()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Direct=").append(getDirect()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("TurnDirect=").append(isTurnDirect()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Timestamp=").append(getTimestamp()).append(System.lineSeparator());
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

    @Override
    public void encode(ByteBuffer _o_) {
        int _i_ = 0;
        {
            var _x_ = getCurrent();
            if (_x_ != null && !_x_.isZero()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.VECTOR3);
                _o_.WriteVector3(_x_);
            }
        }
        {
            var _x_ = getDirect();
            if (_x_ != null && !_x_.isZero()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.VECTOR3);
                _o_.WriteVector3(_x_);
            }
        }
        {
            boolean _x_ = isTurnDirect();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        {
            long _x_ = getTimestamp();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setCurrent(_o_.ReadVector3(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setDirect(_o_.ReadVector3(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setTurnDirect(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setTimestamp(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    public boolean negativeCheck() {
        if (getTimestamp() < 0)
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
                case 1: _Current = ((Zeze.Transaction.Logs.LogVector3)vlog).value; break;
                case 2: _Direct = ((Zeze.Transaction.Logs.LogVector3)vlog).value; break;
                case 3: _TurnDirect = ((Zeze.Transaction.Logs.LogBool)vlog).value; break;
                case 4: _Timestamp = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        parents.add("Current");
        setCurrent(Zeze.Serialize.Helper.decodeVector3(parents, rs));
        parents.remove(parents.size() - 1);
        parents.add("Direct");
        setDirect(Zeze.Serialize.Helper.decodeVector3(parents, rs));
        parents.remove(parents.size() - 1);
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setTurnDirect(rs.getBoolean(_parents_name_ + "TurnDirect"));
        setTimestamp(rs.getLong(_parents_name_ + "Timestamp"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        parents.add("Current");
        Zeze.Serialize.Helper.encodeVector3(getCurrent(), parents, st);
        parents.remove(parents.size() - 1);
        parents.add("Direct");
        Zeze.Serialize.Helper.encodeVector3(getDirect(), parents, st);
        parents.remove(parents.size() - 1);
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendBoolean(_parents_name_ + "TurnDirect", isTurnDirect());
        st.appendLong(_parents_name_ + "Timestamp", getTimestamp());
    }

public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -7391596098548970887L;

    private Zeze.Serialize.Vector3 _Current; // 转身命令时客户端的真实位置。
    private Zeze.Serialize.Vector3 _Direct; // 转身命令时的面向方向。
    private boolean _TurnDirect; // 转身方向。
    private long _Timestamp; // 命令发起时刻的时戳。

    public Zeze.Serialize.Vector3 getCurrent() {
        return _Current;
    }

    public void setCurrent(Zeze.Serialize.Vector3 value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Current = value;
    }

    public Zeze.Serialize.Vector3 getDirect() {
        return _Direct;
    }

    public void setDirect(Zeze.Serialize.Vector3 value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Direct = value;
    }

    public boolean isTurnDirect() {
        return _TurnDirect;
    }

    public void setTurnDirect(boolean value) {
        _TurnDirect = value;
    }

    public long getTimestamp() {
        return _Timestamp;
    }

    public void setTimestamp(long value) {
        _Timestamp = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Current = Zeze.Serialize.Vector3.ZERO;
        _Direct = Zeze.Serialize.Vector3.ZERO;
    }

    @SuppressWarnings("deprecation")
    public Data(Zeze.Serialize.Vector3 _Current_, Zeze.Serialize.Vector3 _Direct_, boolean _TurnDirect_, long _Timestamp_) {
        if (_Current_ == null)
            _Current_ = Zeze.Serialize.Vector3.ZERO;
        _Current = _Current_;
        if (_Direct_ == null)
            _Direct_ = Zeze.Serialize.Vector3.ZERO;
        _Direct = _Direct_;
        _TurnDirect = _TurnDirect_;
        _Timestamp = _Timestamp_;
    }

    @Override
    public Zeze.Builtin.World.BRotate toBean() {
        var bean = new Zeze.Builtin.World.BRotate();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BRotate)other);
    }

    public void assign(BRotate other) {
        _Current = other.getCurrent();
        _Direct = other.getDirect();
        _TurnDirect = other.isTurnDirect();
        _Timestamp = other.getTimestamp();
    }

    public void assign(BRotate.Data other) {
        _Current = other._Current;
        _Direct = other._Direct;
        _TurnDirect = other._TurnDirect;
        _Timestamp = other._Timestamp;
    }

    @Override
    public BRotate.Data copy() {
        var copy = new BRotate.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BRotate.Data a, BRotate.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BRotate.Data clone() {
        return (BRotate.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.World.BRotate: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Current=").append(_Current).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Direct=").append(_Direct).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("TurnDirect=").append(_TurnDirect).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Timestamp=").append(_Timestamp).append(System.lineSeparator());
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

    @Override
    public void encode(ByteBuffer _o_) {
        int _i_ = 0;
        {
            var _x_ = _Current;
            if (_x_ != null && !_x_.isZero()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.VECTOR3);
                _o_.WriteVector3(_x_);
            }
        }
        {
            var _x_ = _Direct;
            if (_x_ != null && !_x_.isZero()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.VECTOR3);
                _o_.WriteVector3(_x_);
            }
        }
        {
            boolean _x_ = _TurnDirect;
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        {
            long _x_ = _Timestamp;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _Current = _o_.ReadVector3(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _Direct = _o_.ReadVector3(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _TurnDirect = _o_.ReadBool(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _Timestamp = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
