// auto-generated @formatter:off
package Zeze.Builtin.World;

import Zeze.Serialize.ByteBuffer;

// 地图实例（线）的负载
@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BLoad extends Zeze.Transaction.Bean implements BLoadReadOnly {
    public static final long TYPEID = -3855690854900982322L;

    private int _PlayerCount; // 玩家数量（可用于简单规则）
    private long _ComputeCount; // 逻辑计算计数（衡量CPU）
    private long _ComputeCountPS;
    private long _ComputeCountLast;
    private long _ComputeCountTime;

    private transient Object __zeze_map_key__;

    @Override
    public Object mapKey() {
        return __zeze_map_key__;
    }

    @Override
    public void mapKey(Object value) {
        __zeze_map_key__ = value;
    }

    @Override
    public int getPlayerCount() {
        if (!isManaged())
            return _PlayerCount;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _PlayerCount;
        var log = (Log__PlayerCount)txn.getLog(objectId() + 1);
        return log != null ? log.value : _PlayerCount;
    }

    public void setPlayerCount(int value) {
        if (!isManaged()) {
            _PlayerCount = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__PlayerCount(this, 1, value));
    }

    @Override
    public long getComputeCount() {
        if (!isManaged())
            return _ComputeCount;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ComputeCount;
        var log = (Log__ComputeCount)txn.getLog(objectId() + 2);
        return log != null ? log.value : _ComputeCount;
    }

    public void setComputeCount(long value) {
        if (!isManaged()) {
            _ComputeCount = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ComputeCount(this, 2, value));
    }

    @Override
    public long getComputeCountPS() {
        if (!isManaged())
            return _ComputeCountPS;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ComputeCountPS;
        var log = (Log__ComputeCountPS)txn.getLog(objectId() + 3);
        return log != null ? log.value : _ComputeCountPS;
    }

    public void setComputeCountPS(long value) {
        if (!isManaged()) {
            _ComputeCountPS = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ComputeCountPS(this, 3, value));
    }

    @Override
    public long getComputeCountLast() {
        if (!isManaged())
            return _ComputeCountLast;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ComputeCountLast;
        var log = (Log__ComputeCountLast)txn.getLog(objectId() + 4);
        return log != null ? log.value : _ComputeCountLast;
    }

    public void setComputeCountLast(long value) {
        if (!isManaged()) {
            _ComputeCountLast = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ComputeCountLast(this, 4, value));
    }

    @Override
    public long getComputeCountTime() {
        if (!isManaged())
            return _ComputeCountTime;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ComputeCountTime;
        var log = (Log__ComputeCountTime)txn.getLog(objectId() + 5);
        return log != null ? log.value : _ComputeCountTime;
    }

    public void setComputeCountTime(long value) {
        if (!isManaged()) {
            _ComputeCountTime = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ComputeCountTime(this, 5, value));
    }

    @SuppressWarnings("deprecation")
    public BLoad() {
    }

    @SuppressWarnings("deprecation")
    public BLoad(int _PlayerCount_, long _ComputeCount_, long _ComputeCountPS_, long _ComputeCountLast_, long _ComputeCountTime_) {
        _PlayerCount = _PlayerCount_;
        _ComputeCount = _ComputeCount_;
        _ComputeCountPS = _ComputeCountPS_;
        _ComputeCountLast = _ComputeCountLast_;
        _ComputeCountTime = _ComputeCountTime_;
    }

    @Override
    public void reset() {
        setPlayerCount(0);
        setComputeCount(0);
        setComputeCountPS(0);
        setComputeCountLast(0);
        setComputeCountTime(0);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.World.BLoad.Data toData() {
        var data = new Zeze.Builtin.World.BLoad.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.World.BLoad.Data)other);
    }

    public void assign(BLoad.Data other) {
        setPlayerCount(other._PlayerCount);
        setComputeCount(other._ComputeCount);
        setComputeCountPS(other._ComputeCountPS);
        setComputeCountLast(other._ComputeCountLast);
        setComputeCountTime(other._ComputeCountTime);
        _unknown_ = null;
    }

    public void assign(BLoad other) {
        setPlayerCount(other.getPlayerCount());
        setComputeCount(other.getComputeCount());
        setComputeCountPS(other.getComputeCountPS());
        setComputeCountLast(other.getComputeCountLast());
        setComputeCountTime(other.getComputeCountTime());
        _unknown_ = other._unknown_;
    }

    public BLoad copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BLoad copy() {
        var copy = new BLoad();
        copy.assign(this);
        return copy;
    }

    public static void swap(BLoad a, BLoad b) {
        BLoad save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__PlayerCount extends Zeze.Transaction.Logs.LogInt {
        public Log__PlayerCount(BLoad bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BLoad)getBelong())._PlayerCount = value; }
    }

    private static final class Log__ComputeCount extends Zeze.Transaction.Logs.LogLong {
        public Log__ComputeCount(BLoad bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BLoad)getBelong())._ComputeCount = value; }
    }

    private static final class Log__ComputeCountPS extends Zeze.Transaction.Logs.LogLong {
        public Log__ComputeCountPS(BLoad bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BLoad)getBelong())._ComputeCountPS = value; }
    }

    private static final class Log__ComputeCountLast extends Zeze.Transaction.Logs.LogLong {
        public Log__ComputeCountLast(BLoad bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BLoad)getBelong())._ComputeCountLast = value; }
    }

    private static final class Log__ComputeCountTime extends Zeze.Transaction.Logs.LogLong {
        public Log__ComputeCountTime(BLoad bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BLoad)getBelong())._ComputeCountTime = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.World.BLoad: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("PlayerCount=").append(getPlayerCount()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ComputeCount=").append(getComputeCount()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ComputeCountPS=").append(getComputeCountPS()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ComputeCountLast=").append(getComputeCountLast()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ComputeCountTime=").append(getComputeCountTime()).append(System.lineSeparator());
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
            int _x_ = getPlayerCount();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            long _x_ = getComputeCount();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getComputeCountPS();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getComputeCountLast();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getComputeCountTime();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
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
            setPlayerCount(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setComputeCount(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setComputeCountPS(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setComputeCountLast(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setComputeCountTime(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean negativeCheck() {
        if (getPlayerCount() < 0)
            return true;
        if (getComputeCount() < 0)
            return true;
        if (getComputeCountPS() < 0)
            return true;
        if (getComputeCountLast() < 0)
            return true;
        if (getComputeCountTime() < 0)
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
                case 1: _PlayerCount = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 2: _ComputeCount = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 3: _ComputeCountPS = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 4: _ComputeCountLast = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 5: _ComputeCountTime = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setPlayerCount(rs.getInt(_parents_name_ + "PlayerCount"));
        setComputeCount(rs.getLong(_parents_name_ + "ComputeCount"));
        setComputeCountPS(rs.getLong(_parents_name_ + "ComputeCountPS"));
        setComputeCountLast(rs.getLong(_parents_name_ + "ComputeCountLast"));
        setComputeCountTime(rs.getLong(_parents_name_ + "ComputeCountTime"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendInt(_parents_name_ + "PlayerCount", getPlayerCount());
        st.appendLong(_parents_name_ + "ComputeCount", getComputeCount());
        st.appendLong(_parents_name_ + "ComputeCountPS", getComputeCountPS());
        st.appendLong(_parents_name_ + "ComputeCountLast", getComputeCountLast());
        st.appendLong(_parents_name_ + "ComputeCountTime", getComputeCountTime());
    }

    @Override
    public java.util.List<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "PlayerCount", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "ComputeCount", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "ComputeCountPS", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "ComputeCountLast", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "ComputeCountTime", "long", "", ""));
        return vars;
    }

    @Override
    public Zeze.Transaction.Bean toPrevious() {
        return null; // todo BLoad
    }

// 地图实例（线）的负载
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -3855690854900982322L;

    private int _PlayerCount; // 玩家数量（可用于简单规则）
    private long _ComputeCount; // 逻辑计算计数（衡量CPU）
    private long _ComputeCountPS;
    private long _ComputeCountLast;
    private long _ComputeCountTime;

    public int getPlayerCount() {
        return _PlayerCount;
    }

    public void setPlayerCount(int value) {
        _PlayerCount = value;
    }

    public long getComputeCount() {
        return _ComputeCount;
    }

    public void setComputeCount(long value) {
        _ComputeCount = value;
    }

    public long getComputeCountPS() {
        return _ComputeCountPS;
    }

    public void setComputeCountPS(long value) {
        _ComputeCountPS = value;
    }

    public long getComputeCountLast() {
        return _ComputeCountLast;
    }

    public void setComputeCountLast(long value) {
        _ComputeCountLast = value;
    }

    public long getComputeCountTime() {
        return _ComputeCountTime;
    }

    public void setComputeCountTime(long value) {
        _ComputeCountTime = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
    }

    @SuppressWarnings("deprecation")
    public Data(int _PlayerCount_, long _ComputeCount_, long _ComputeCountPS_, long _ComputeCountLast_, long _ComputeCountTime_) {
        _PlayerCount = _PlayerCount_;
        _ComputeCount = _ComputeCount_;
        _ComputeCountPS = _ComputeCountPS_;
        _ComputeCountLast = _ComputeCountLast_;
        _ComputeCountTime = _ComputeCountTime_;
    }

    @Override
    public void reset() {
        _PlayerCount = 0;
        _ComputeCount = 0;
        _ComputeCountPS = 0;
        _ComputeCountLast = 0;
        _ComputeCountTime = 0;
    }

    @Override
    public Zeze.Builtin.World.BLoad toBean() {
        var bean = new Zeze.Builtin.World.BLoad();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BLoad)other);
    }

    public void assign(BLoad other) {
        _PlayerCount = other.getPlayerCount();
        _ComputeCount = other.getComputeCount();
        _ComputeCountPS = other.getComputeCountPS();
        _ComputeCountLast = other.getComputeCountLast();
        _ComputeCountTime = other.getComputeCountTime();
    }

    public void assign(BLoad.Data other) {
        _PlayerCount = other._PlayerCount;
        _ComputeCount = other._ComputeCount;
        _ComputeCountPS = other._ComputeCountPS;
        _ComputeCountLast = other._ComputeCountLast;
        _ComputeCountTime = other._ComputeCountTime;
    }

    @Override
    public BLoad.Data copy() {
        var copy = new BLoad.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BLoad.Data a, BLoad.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BLoad.Data clone() {
        return (BLoad.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.World.BLoad: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("PlayerCount=").append(_PlayerCount).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ComputeCount=").append(_ComputeCount).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ComputeCountPS=").append(_ComputeCountPS).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ComputeCountLast=").append(_ComputeCountLast).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ComputeCountTime=").append(_ComputeCountTime).append(System.lineSeparator());
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
            int _x_ = _PlayerCount;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            long _x_ = _ComputeCount;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = _ComputeCountPS;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = _ComputeCountLast;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = _ComputeCountTime;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
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
            _PlayerCount = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _ComputeCount = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _ComputeCountPS = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _ComputeCountLast = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            _ComputeCountTime = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
