// auto-generated @formatter:off
package metagame.builtin.World.Static;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BSwitchWorld extends Zeze.Transaction.Bean implements BSwitchWorldReadOnly {
    public static final long TYPEID = -2291144489014983879L;

    private int _MapId;
    private int _FromMapId;
    private int _FromGateId;

    private static final java.lang.invoke.VarHandle vh_MapId;
    private static final java.lang.invoke.VarHandle vh_FromMapId;
    private static final java.lang.invoke.VarHandle vh_FromGateId;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_MapId = _l_.findVarHandle(BSwitchWorld.class, "_MapId", int.class);
            vh_FromMapId = _l_.findVarHandle(BSwitchWorld.class, "_FromMapId", int.class);
            vh_FromGateId = _l_.findVarHandle(BSwitchWorld.class, "_FromGateId", int.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public int getMapId() {
        if (!isManaged())
            return _MapId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _MapId;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _MapId;
    }

    public void setMapId(int _v_) {
        if (!isManaged()) {
            _MapId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 1, vh_MapId, _v_));
    }

    @Override
    public int getFromMapId() {
        if (!isManaged())
            return _FromMapId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _FromMapId;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _FromMapId;
    }

    public void setFromMapId(int _v_) {
        if (!isManaged()) {
            _FromMapId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 2, vh_FromMapId, _v_));
    }

    @Override
    public int getFromGateId() {
        if (!isManaged())
            return _FromGateId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _FromGateId;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _FromGateId;
    }

    public void setFromGateId(int _v_) {
        if (!isManaged()) {
            _FromGateId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 3, vh_FromGateId, _v_));
    }

    @SuppressWarnings("deprecation")
    public BSwitchWorld() {
    }

    @SuppressWarnings("deprecation")
    public BSwitchWorld(int _MapId_, int _FromMapId_, int _FromGateId_) {
        _MapId = _MapId_;
        _FromMapId = _FromMapId_;
        _FromGateId = _FromGateId_;
    }

    @Override
    public void reset() {
        setMapId(0);
        setFromMapId(0);
        setFromGateId(0);
        _unknown_ = null;
    }

    @Override
    public metagame.builtin.World.Static.BSwitchWorld.Data toData() {
        var _d_ = new metagame.builtin.World.Static.BSwitchWorld.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((metagame.builtin.World.Static.BSwitchWorld.Data)_o_);
    }

    public void assign(BSwitchWorld.Data _o_) {
        setMapId(_o_._MapId);
        setFromMapId(_o_._FromMapId);
        setFromGateId(_o_._FromGateId);
        _unknown_ = null;
    }

    public void assign(BSwitchWorld _o_) {
        setMapId(_o_.getMapId());
        setFromMapId(_o_.getFromMapId());
        setFromGateId(_o_.getFromGateId());
        _unknown_ = _o_._unknown_;
    }

    public BSwitchWorld copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BSwitchWorld copy() {
        var _c_ = new BSwitchWorld();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BSwitchWorld _a_, BSwitchWorld _b_) {
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
        _s_.append("metagame.builtin.World.Static.BSwitchWorld: {\n");
        _s_.append(_i1_).append("MapId=").append(getMapId()).append(",\n");
        _s_.append(_i1_).append("FromMapId=").append(getFromMapId()).append(",\n");
        _s_.append(_i1_).append("FromGateId=").append(getFromGateId()).append('\n');
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
            int _x_ = getMapId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getFromMapId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getFromGateId();
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
            setMapId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setFromMapId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setFromGateId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BSwitchWorld))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BSwitchWorld)_o_;
        if (getMapId() != _b_.getMapId())
            return false;
        if (getFromMapId() != _b_.getFromMapId())
            return false;
        if (getFromGateId() != _b_.getFromGateId())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getMapId() < 0)
            return true;
        if (getFromMapId() < 0)
            return true;
        if (getFromGateId() < 0)
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
                case 1: _MapId = _v_.intValue(); break;
                case 2: _FromMapId = _v_.intValue(); break;
                case 3: _FromGateId = _v_.intValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setMapId(_r_.getInt(_pn_ + "MapId"));
        setFromMapId(_r_.getInt(_pn_ + "FromMapId"));
        setFromGateId(_r_.getInt(_pn_ + "FromGateId"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendInt(_pn_ + "MapId", getMapId());
        _s_.appendInt(_pn_ + "FromMapId", getFromMapId());
        _s_.appendInt(_pn_ + "FromGateId", getFromGateId());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "MapId", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "FromMapId", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "FromGateId", "int", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -2291144489014983879L;

    private int _MapId;
    private int _FromMapId;
    private int _FromGateId;

    public int getMapId() {
        return _MapId;
    }

    public void setMapId(int _v_) {
        _MapId = _v_;
    }

    public int getFromMapId() {
        return _FromMapId;
    }

    public void setFromMapId(int _v_) {
        _FromMapId = _v_;
    }

    public int getFromGateId() {
        return _FromGateId;
    }

    public void setFromGateId(int _v_) {
        _FromGateId = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
    }

    @SuppressWarnings("deprecation")
    public Data(int _MapId_, int _FromMapId_, int _FromGateId_) {
        _MapId = _MapId_;
        _FromMapId = _FromMapId_;
        _FromGateId = _FromGateId_;
    }

    @Override
    public void reset() {
        _MapId = 0;
        _FromMapId = 0;
        _FromGateId = 0;
    }

    @Override
    public metagame.builtin.World.Static.BSwitchWorld toBean() {
        var _b_ = new metagame.builtin.World.Static.BSwitchWorld();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BSwitchWorld)_o_);
    }

    public void assign(BSwitchWorld _o_) {
        _MapId = _o_.getMapId();
        _FromMapId = _o_.getFromMapId();
        _FromGateId = _o_.getFromGateId();
    }

    public void assign(BSwitchWorld.Data _o_) {
        _MapId = _o_._MapId;
        _FromMapId = _o_._FromMapId;
        _FromGateId = _o_._FromGateId;
    }

    @Override
    public BSwitchWorld.Data copy() {
        var _c_ = new BSwitchWorld.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BSwitchWorld.Data _a_, BSwitchWorld.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BSwitchWorld.Data clone() {
        return (BSwitchWorld.Data)super.clone();
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
        _s_.append("metagame.builtin.World.Static.BSwitchWorld: {\n");
        _s_.append(_i1_).append("MapId=").append(_MapId).append(",\n");
        _s_.append(_i1_).append("FromMapId=").append(_FromMapId).append(",\n");
        _s_.append(_i1_).append("FromGateId=").append(_FromGateId).append('\n');
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
            int _x_ = _MapId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = _FromMapId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = _FromGateId;
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
            _MapId = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _FromMapId = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _FromGateId = _o_.ReadInt(_t_);
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
        if (!(_o_ instanceof BSwitchWorld.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BSwitchWorld.Data)_o_;
        if (_MapId != _b_._MapId)
            return false;
        if (_FromMapId != _b_._FromMapId)
            return false;
        if (_FromGateId != _b_._FromGateId)
            return false;
        return true;
    }
}
}
