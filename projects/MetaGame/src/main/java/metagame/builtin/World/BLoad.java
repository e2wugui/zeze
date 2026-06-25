// auto-generated @formatter:off
package metagame.builtin.World;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

// 地图实例（线）的负载
@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BLoad extends Zeze.Transaction.Bean implements BLoadReadOnly {
    public static final long TYPEID = -5243755928133160147L;

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
    public void mapKey(Object _v_) {
        __zeze_map_key__ = _v_;
    }

    private static final java.lang.invoke.VarHandle vh_PlayerCount;
    private static final java.lang.invoke.VarHandle vh_ComputeCount;
    private static final java.lang.invoke.VarHandle vh_ComputeCountPS;
    private static final java.lang.invoke.VarHandle vh_ComputeCountLast;
    private static final java.lang.invoke.VarHandle vh_ComputeCountTime;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_PlayerCount = _l_.findVarHandle(BLoad.class, "_PlayerCount", int.class);
            vh_ComputeCount = _l_.findVarHandle(BLoad.class, "_ComputeCount", long.class);
            vh_ComputeCountPS = _l_.findVarHandle(BLoad.class, "_ComputeCountPS", long.class);
            vh_ComputeCountLast = _l_.findVarHandle(BLoad.class, "_ComputeCountLast", long.class);
            vh_ComputeCountTime = _l_.findVarHandle(BLoad.class, "_ComputeCountTime", long.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public int getPlayerCount() {
        if (!isManaged())
            return _PlayerCount;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _PlayerCount;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _PlayerCount;
    }

    public void setPlayerCount(int _v_) {
        if (!isManaged()) {
            _PlayerCount = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 1, vh_PlayerCount, _v_));
    }

    @Override
    public long getComputeCount() {
        if (!isManaged())
            return _ComputeCount;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ComputeCount;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _ComputeCount;
    }

    public void setComputeCount(long _v_) {
        if (!isManaged()) {
            _ComputeCount = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 2, vh_ComputeCount, _v_));
    }

    @Override
    public long getComputeCountPS() {
        if (!isManaged())
            return _ComputeCountPS;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ComputeCountPS;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _ComputeCountPS;
    }

    public void setComputeCountPS(long _v_) {
        if (!isManaged()) {
            _ComputeCountPS = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 3, vh_ComputeCountPS, _v_));
    }

    @Override
    public long getComputeCountLast() {
        if (!isManaged())
            return _ComputeCountLast;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ComputeCountLast;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 4);
        return log != null ? log.value : _ComputeCountLast;
    }

    public void setComputeCountLast(long _v_) {
        if (!isManaged()) {
            _ComputeCountLast = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 4, vh_ComputeCountLast, _v_));
    }

    @Override
    public long getComputeCountTime() {
        if (!isManaged())
            return _ComputeCountTime;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ComputeCountTime;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 5);
        return log != null ? log.value : _ComputeCountTime;
    }

    public void setComputeCountTime(long _v_) {
        if (!isManaged()) {
            _ComputeCountTime = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 5, vh_ComputeCountTime, _v_));
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
    public metagame.builtin.World.BLoad.Data toData() {
        var _d_ = new metagame.builtin.World.BLoad.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((metagame.builtin.World.BLoad.Data)_o_);
    }

    public void assign(BLoad.Data _o_) {
        setPlayerCount(_o_._PlayerCount);
        setComputeCount(_o_._ComputeCount);
        setComputeCountPS(_o_._ComputeCountPS);
        setComputeCountLast(_o_._ComputeCountLast);
        setComputeCountTime(_o_._ComputeCountTime);
        _unknown_ = null;
    }

    public void assign(BLoad _o_) {
        setPlayerCount(_o_.getPlayerCount());
        setComputeCount(_o_.getComputeCount());
        setComputeCountPS(_o_.getComputeCountPS());
        setComputeCountLast(_o_.getComputeCountLast());
        setComputeCountTime(_o_.getComputeCountTime());
        _unknown_ = _o_._unknown_;
    }

    public BLoad copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BLoad copy() {
        var _c_ = new BLoad();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BLoad _a_, BLoad _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        var _i1_ = Zeze.Util.Str.indent(_l_ + 4);
        _s_.append("metagame.builtin.World.BLoad: {\n");
        _s_.append(_i1_).append("PlayerCount=").append(getPlayerCount()).append(",\n");
        _s_.append(_i1_).append("ComputeCount=").append(getComputeCount()).append(",\n");
        _s_.append(_i1_).append("ComputeCountPS=").append(getComputeCountPS()).append(",\n");
        _s_.append(_i1_).append("ComputeCountLast=").append(getComputeCountLast()).append(",\n");
        _s_.append(_i1_).append("ComputeCountTime=").append(getComputeCountTime()).append('\n');
        _s_.append(Zeze.Util.Str.indent(_l_)).append('}');
    }

    private static int _PRE_ALLOC_SIZE_ = 16;

    @Override
    public int preAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void preAllocSize(int _s_) {
        _PRE_ALLOC_SIZE_ = _s_;
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
    public void decode(IByteBuffer _o_) {
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
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BLoad))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BLoad)_o_;
        if (getPlayerCount() != _b_.getPlayerCount())
            return false;
        if (getComputeCount() != _b_.getComputeCount())
            return false;
        if (getComputeCountPS() != _b_.getComputeCountPS())
            return false;
        if (getComputeCountLast() != _b_.getComputeCountLast())
            return false;
        if (getComputeCountTime() != _b_.getComputeCountTime())
            return false;
        return true;
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
    public void followerApply(Zeze.Transaction.Log _l_) {
        var _vs_ = ((Zeze.Transaction.Collections.LogBean)_l_).getVariables();
        if (_vs_ == null)
            return;
        for (var _i_ = _vs_.iterator(); _i_.moveToNext(); ) {
            var _v_ = _i_.value();
            switch (_v_.getVariableId()) {
                case 1: _PlayerCount = _v_.intValue(); break;
                case 2: _ComputeCount = _v_.longValue(); break;
                case 3: _ComputeCountPS = _v_.longValue(); break;
                case 4: _ComputeCountLast = _v_.longValue(); break;
                case 5: _ComputeCountTime = _v_.longValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setPlayerCount(_r_.getInt(_pn_ + "PlayerCount"));
        setComputeCount(_r_.getLong(_pn_ + "ComputeCount"));
        setComputeCountPS(_r_.getLong(_pn_ + "ComputeCountPS"));
        setComputeCountLast(_r_.getLong(_pn_ + "ComputeCountLast"));
        setComputeCountTime(_r_.getLong(_pn_ + "ComputeCountTime"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendInt(_pn_ + "PlayerCount", getPlayerCount());
        _s_.appendLong(_pn_ + "ComputeCount", getComputeCount());
        _s_.appendLong(_pn_ + "ComputeCountPS", getComputeCountPS());
        _s_.appendLong(_pn_ + "ComputeCountLast", getComputeCountLast());
        _s_.appendLong(_pn_ + "ComputeCountTime", getComputeCountTime());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "PlayerCount", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "ComputeCount", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "ComputeCountPS", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "ComputeCountLast", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "ComputeCountTime", "long", "", ""));
        return _v_;
    }

// 地图实例（线）的负载
@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -5243755928133160147L;

    private int _PlayerCount; // 玩家数量（可用于简单规则）
    private long _ComputeCount; // 逻辑计算计数（衡量CPU）
    private long _ComputeCountPS;
    private long _ComputeCountLast;
    private long _ComputeCountTime;

    public int getPlayerCount() {
        return _PlayerCount;
    }

    public void setPlayerCount(int _v_) {
        _PlayerCount = _v_;
    }

    public long getComputeCount() {
        return _ComputeCount;
    }

    public void setComputeCount(long _v_) {
        _ComputeCount = _v_;
    }

    public long getComputeCountPS() {
        return _ComputeCountPS;
    }

    public void setComputeCountPS(long _v_) {
        _ComputeCountPS = _v_;
    }

    public long getComputeCountLast() {
        return _ComputeCountLast;
    }

    public void setComputeCountLast(long _v_) {
        _ComputeCountLast = _v_;
    }

    public long getComputeCountTime() {
        return _ComputeCountTime;
    }

    public void setComputeCountTime(long _v_) {
        _ComputeCountTime = _v_;
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
    public metagame.builtin.World.BLoad toBean() {
        var _b_ = new metagame.builtin.World.BLoad();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BLoad)_o_);
    }

    public void assign(BLoad _o_) {
        _PlayerCount = _o_.getPlayerCount();
        _ComputeCount = _o_.getComputeCount();
        _ComputeCountPS = _o_.getComputeCountPS();
        _ComputeCountLast = _o_.getComputeCountLast();
        _ComputeCountTime = _o_.getComputeCountTime();
    }

    public void assign(BLoad.Data _o_) {
        _PlayerCount = _o_._PlayerCount;
        _ComputeCount = _o_._ComputeCount;
        _ComputeCountPS = _o_._ComputeCountPS;
        _ComputeCountLast = _o_._ComputeCountLast;
        _ComputeCountTime = _o_._ComputeCountTime;
    }

    @Override
    public BLoad.Data copy() {
        var _c_ = new BLoad.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BLoad.Data _a_, BLoad.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
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
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        var _i1_ = Zeze.Util.Str.indent(_l_ + 4);
        _s_.append("metagame.builtin.World.BLoad: {\n");
        _s_.append(_i1_).append("PlayerCount=").append(_PlayerCount).append(",\n");
        _s_.append(_i1_).append("ComputeCount=").append(_ComputeCount).append(",\n");
        _s_.append(_i1_).append("ComputeCountPS=").append(_ComputeCountPS).append(",\n");
        _s_.append(_i1_).append("ComputeCountLast=").append(_ComputeCountLast).append(",\n");
        _s_.append(_i1_).append("ComputeCountTime=").append(_ComputeCountTime).append('\n');
        _s_.append(Zeze.Util.Str.indent(_l_)).append('}');
    }

    @Override
    public int preAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void preAllocSize(int _s_) {
        _PRE_ALLOC_SIZE_ = _s_;
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
    public void decode(IByteBuffer _o_) {
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

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BLoad.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BLoad.Data)_o_;
        if (_PlayerCount != _b_._PlayerCount)
            return false;
        if (_ComputeCount != _b_._ComputeCount)
            return false;
        if (_ComputeCountPS != _b_._ComputeCountPS)
            return false;
        if (_ComputeCountLast != _b_._ComputeCountLast)
            return false;
        if (_ComputeCountTime != _b_._ComputeCountTime)
            return false;
        return true;
    }
}
}
