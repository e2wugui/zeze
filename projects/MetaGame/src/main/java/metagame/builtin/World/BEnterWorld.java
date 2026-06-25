// auto-generated @formatter:off
package metagame.builtin.World;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

/*
为了不污染根空间，改成Command了。
			<protocol name="SwitchWorld" argument="BSwitchWorld" handle="server"/> mapId==-1，进入地图由服务器控制，此时仅仅表示客户端准备好进入地图了。
			<protocol name="EnterWorld" argument="BEnterWorld" handle="client"/>
			<protocol name="EnterConfirm" argument="BEnterConfirm" handle="server"/>

			Aoi-Notify
			<protocol name="AoiEnter" argument="BAoiEnter" handle="client"/>
			<protocol name="AoiOperate" argument="BAoiOperate" handle="client"/>
			<protocol name="AoiLeave" argument="BAoiLeave" handle="client"/>
*/
@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BEnterWorld extends Zeze.Transaction.Bean implements BEnterWorldReadOnly {
    public static final long TYPEID = 5049345430341027881L;

    private int _MapId;
    private long _MapInstanceId;
    private Zeze.Serialize.Vector3 _Position;
    private Zeze.Serialize.Vector3 _Direct;
    private final Zeze.Transaction.Collections.PList2<metagame.builtin.World.BAoiOperates> _PriorityData; // 优先数据，服务器第一次进入的时候就跟随EnterWorld发送给客户端。

    private static final java.lang.invoke.VarHandle vh_MapId;
    private static final java.lang.invoke.VarHandle vh_MapInstanceId;
    private static final java.lang.invoke.VarHandle vh_Position;
    private static final java.lang.invoke.VarHandle vh_Direct;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_MapId = _l_.findVarHandle(BEnterWorld.class, "_MapId", int.class);
            vh_MapInstanceId = _l_.findVarHandle(BEnterWorld.class, "_MapInstanceId", long.class);
            vh_Position = _l_.findVarHandle(BEnterWorld.class, "_Position", Zeze.Serialize.Vector3.class);
            vh_Direct = _l_.findVarHandle(BEnterWorld.class, "_Direct", Zeze.Serialize.Vector3.class);
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
    public long getMapInstanceId() {
        if (!isManaged())
            return _MapInstanceId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _MapInstanceId;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _MapInstanceId;
    }

    public void setMapInstanceId(long _v_) {
        if (!isManaged()) {
            _MapInstanceId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 2, vh_MapInstanceId, _v_));
    }

    @Override
    public Zeze.Serialize.Vector3 getPosition() {
        if (!isManaged())
            return _Position;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Position;
        var log = (Zeze.Transaction.Logs.LogVector3)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _Position;
    }

    public void setPosition(Zeze.Serialize.Vector3 _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Position = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogVector3(this, 3, vh_Position, _v_));
    }

    @Override
    public Zeze.Serialize.Vector3 getDirect() {
        if (!isManaged())
            return _Direct;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Direct;
        var log = (Zeze.Transaction.Logs.LogVector3)_t_.getLog(objectId() + 4);
        return log != null ? log.value : _Direct;
    }

    public void setDirect(Zeze.Serialize.Vector3 _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Direct = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogVector3(this, 4, vh_Direct, _v_));
    }

    public Zeze.Transaction.Collections.PList2<metagame.builtin.World.BAoiOperates> getPriorityData() {
        return _PriorityData;
    }

    @Override
    public Zeze.Transaction.Collections.PList2ReadOnly<metagame.builtin.World.BAoiOperates, metagame.builtin.World.BAoiOperatesReadOnly> getPriorityDataReadOnly() {
        return new Zeze.Transaction.Collections.PList2ReadOnly<>(_PriorityData);
    }

    @SuppressWarnings("deprecation")
    public BEnterWorld() {
        _Position = Zeze.Serialize.Vector3.ZERO;
        _Direct = Zeze.Serialize.Vector3.ZERO;
        _PriorityData = new Zeze.Transaction.Collections.PList2<>(metagame.builtin.World.BAoiOperates.class);
        _PriorityData.variableId(5);
    }

    @SuppressWarnings("deprecation")
    public BEnterWorld(int _MapId_, long _MapInstanceId_, Zeze.Serialize.Vector3 _Position_, Zeze.Serialize.Vector3 _Direct_) {
        _MapId = _MapId_;
        _MapInstanceId = _MapInstanceId_;
        if (_Position_ == null)
            _Position_ = Zeze.Serialize.Vector3.ZERO;
        _Position = _Position_;
        if (_Direct_ == null)
            _Direct_ = Zeze.Serialize.Vector3.ZERO;
        _Direct = _Direct_;
        _PriorityData = new Zeze.Transaction.Collections.PList2<>(metagame.builtin.World.BAoiOperates.class);
        _PriorityData.variableId(5);
    }

    @Override
    public void reset() {
        setMapId(0);
        setMapInstanceId(0);
        setPosition(Zeze.Serialize.Vector3.ZERO);
        setDirect(Zeze.Serialize.Vector3.ZERO);
        _PriorityData.clear();
        _unknown_ = null;
    }

    @Override
    public metagame.builtin.World.BEnterWorld.Data toData() {
        var _d_ = new metagame.builtin.World.BEnterWorld.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((metagame.builtin.World.BEnterWorld.Data)_o_);
    }

    public void assign(BEnterWorld.Data _o_) {
        setMapId(_o_._MapId);
        setMapInstanceId(_o_._MapInstanceId);
        setPosition(_o_._Position);
        setDirect(_o_._Direct);
        _PriorityData.clear();
        for (var _e_ : _o_._PriorityData) {
            var _v_ = new metagame.builtin.World.BAoiOperates();
            _v_.assign(_e_);
            _PriorityData.add(_v_);
        }
        _unknown_ = null;
    }

    public void assign(BEnterWorld _o_) {
        setMapId(_o_.getMapId());
        setMapInstanceId(_o_.getMapInstanceId());
        setPosition(_o_.getPosition());
        setDirect(_o_.getDirect());
        _PriorityData.clear();
        for (var _e_ : _o_._PriorityData)
            _PriorityData.add(_e_.copy());
        _unknown_ = _o_._unknown_;
    }

    public BEnterWorld copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BEnterWorld copy() {
        var _c_ = new BEnterWorld();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BEnterWorld _a_, BEnterWorld _b_) {
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
        var _i2_ = Zeze.Util.Str.indent(_l_ + 8);
        _s_.append("metagame.builtin.World.BEnterWorld: {\n");
        _s_.append(_i1_).append("MapId=").append(getMapId()).append(",\n");
        _s_.append(_i1_).append("MapInstanceId=").append(getMapInstanceId()).append(",\n");
        _s_.append(_i1_).append("Position=").append(getPosition()).append(",\n");
        _s_.append(_i1_).append("Direct=").append(getDirect()).append(",\n");
        _s_.append(_i1_).append("PriorityData=[");
        if (!_PriorityData.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _PriorityData) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_PriorityData.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=");
                _v_.buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("]\n");
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
            long _x_ = getMapInstanceId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = getPosition();
            if (_x_ != null && !_x_.isZero()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.VECTOR3);
                _o_.WriteVector3(_x_);
            }
        }
        {
            var _x_ = getDirect();
            if (_x_ != null && !_x_.isZero()) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.VECTOR3);
                _o_.WriteVector3(_x_);
            }
        }
        {
            var _x_ = _PriorityData;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BEAN);
                for (var _v_ : _x_) {
                    _v_.encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
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
            setMapInstanceId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setPosition(_o_.ReadVector3(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setDirect(_o_.ReadVector3(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            var _x_ = _PriorityData;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new metagame.builtin.World.BAoiOperates(), _t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BEnterWorld))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BEnterWorld)_o_;
        if (getMapId() != _b_.getMapId())
            return false;
        if (getMapInstanceId() != _b_.getMapInstanceId())
            return false;
        if (!getPosition().equals(_b_.getPosition()))
            return false;
        if (!getDirect().equals(_b_.getDirect()))
            return false;
        if (!_PriorityData.equals(_b_._PriorityData))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _PriorityData.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _PriorityData.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getMapId() < 0)
            return true;
        if (getMapInstanceId() < 0)
            return true;
        for (var _v_ : _PriorityData) {
            if (_v_.negativeCheck())
                return true;
        }
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
                case 2: _MapInstanceId = _v_.longValue(); break;
                case 3: _Position = _v_.vector3Value(); break;
                case 4: _Direct = _v_.vector3Value(); break;
                case 5: _PriorityData.followerApply(_v_); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setMapId(_r_.getInt(_pn_ + "MapId"));
        setMapInstanceId(_r_.getLong(_pn_ + "MapInstanceId"));
        _p_.add("Position");
        setPosition(Zeze.Serialize.Helper.decodeVector3(_p_, _r_));
        _p_.remove(_p_.size() - 1);
        _p_.add("Direct");
        setDirect(Zeze.Serialize.Helper.decodeVector3(_p_, _r_));
        _p_.remove(_p_.size() - 1);
        Zeze.Serialize.Helper.decodeJsonList(_PriorityData, metagame.builtin.World.BAoiOperates.class, _r_.getString(_pn_ + "PriorityData"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendInt(_pn_ + "MapId", getMapId());
        _s_.appendLong(_pn_ + "MapInstanceId", getMapInstanceId());
        _p_.add("Position");
        Zeze.Serialize.Helper.encodeVector3(getPosition(), _p_, _s_);
        _p_.remove(_p_.size() - 1);
        _p_.add("Direct");
        Zeze.Serialize.Helper.encodeVector3(getDirect(), _p_, _s_);
        _p_.remove(_p_.size() - 1);
        _s_.appendString(_pn_ + "PriorityData", Zeze.Serialize.Helper.encodeJson(_PriorityData));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "MapId", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "MapInstanceId", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Position", "vector3", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "Direct", "vector3", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "PriorityData", "list", "", "metagame.builtin.World.BAoiOperates"));
        return _v_;
    }

/*
为了不污染根空间，改成Command了。
			<protocol name="SwitchWorld" argument="BSwitchWorld" handle="server"/> mapId==-1，进入地图由服务器控制，此时仅仅表示客户端准备好进入地图了。
			<protocol name="EnterWorld" argument="BEnterWorld" handle="client"/>
			<protocol name="EnterConfirm" argument="BEnterConfirm" handle="server"/>

			Aoi-Notify
			<protocol name="AoiEnter" argument="BAoiEnter" handle="client"/>
			<protocol name="AoiOperate" argument="BAoiOperate" handle="client"/>
			<protocol name="AoiLeave" argument="BAoiLeave" handle="client"/>
*/
@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 5049345430341027881L;

    private int _MapId;
    private long _MapInstanceId;
    private Zeze.Serialize.Vector3 _Position;
    private Zeze.Serialize.Vector3 _Direct;
    private java.util.ArrayList<metagame.builtin.World.BAoiOperates.Data> _PriorityData; // 优先数据，服务器第一次进入的时候就跟随EnterWorld发送给客户端。

    public int getMapId() {
        return _MapId;
    }

    public void setMapId(int _v_) {
        _MapId = _v_;
    }

    public long getMapInstanceId() {
        return _MapInstanceId;
    }

    public void setMapInstanceId(long _v_) {
        _MapInstanceId = _v_;
    }

    public Zeze.Serialize.Vector3 getPosition() {
        return _Position;
    }

    public void setPosition(Zeze.Serialize.Vector3 _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Position = _v_;
    }

    public Zeze.Serialize.Vector3 getDirect() {
        return _Direct;
    }

    public void setDirect(Zeze.Serialize.Vector3 _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Direct = _v_;
    }

    public java.util.ArrayList<metagame.builtin.World.BAoiOperates.Data> getPriorityData() {
        return _PriorityData;
    }

    public void setPriorityData(java.util.ArrayList<metagame.builtin.World.BAoiOperates.Data> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _PriorityData = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Position = Zeze.Serialize.Vector3.ZERO;
        _Direct = Zeze.Serialize.Vector3.ZERO;
        _PriorityData = new java.util.ArrayList<>();
    }

    @SuppressWarnings("deprecation")
    public Data(int _MapId_, long _MapInstanceId_, Zeze.Serialize.Vector3 _Position_, Zeze.Serialize.Vector3 _Direct_, java.util.ArrayList<metagame.builtin.World.BAoiOperates.Data> _PriorityData_) {
        _MapId = _MapId_;
        _MapInstanceId = _MapInstanceId_;
        if (_Position_ == null)
            _Position_ = Zeze.Serialize.Vector3.ZERO;
        _Position = _Position_;
        if (_Direct_ == null)
            _Direct_ = Zeze.Serialize.Vector3.ZERO;
        _Direct = _Direct_;
        if (_PriorityData_ == null)
            _PriorityData_ = new java.util.ArrayList<>();
        _PriorityData = _PriorityData_;
    }

    @Override
    public void reset() {
        _MapId = 0;
        _MapInstanceId = 0;
        _Position = Zeze.Serialize.Vector3.ZERO;
        _Direct = Zeze.Serialize.Vector3.ZERO;
        _PriorityData.clear();
    }

    @Override
    public metagame.builtin.World.BEnterWorld toBean() {
        var _b_ = new metagame.builtin.World.BEnterWorld();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BEnterWorld)_o_);
    }

    public void assign(BEnterWorld _o_) {
        _MapId = _o_.getMapId();
        _MapInstanceId = _o_.getMapInstanceId();
        _Position = _o_.getPosition();
        _Direct = _o_.getDirect();
        _PriorityData.clear();
        for (var _e_ : _o_._PriorityData) {
            var _v_ = new metagame.builtin.World.BAoiOperates.Data();
            _v_.assign(_e_);
            _PriorityData.add(_v_);
        }
    }

    public void assign(BEnterWorld.Data _o_) {
        _MapId = _o_._MapId;
        _MapInstanceId = _o_._MapInstanceId;
        _Position = _o_._Position;
        _Direct = _o_._Direct;
        _PriorityData.clear();
        for (var _e_ : _o_._PriorityData)
            _PriorityData.add(_e_.copy());
    }

    @Override
    public BEnterWorld.Data copy() {
        var _c_ = new BEnterWorld.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BEnterWorld.Data _a_, BEnterWorld.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BEnterWorld.Data clone() {
        return (BEnterWorld.Data)super.clone();
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
        var _i2_ = Zeze.Util.Str.indent(_l_ + 8);
        _s_.append("metagame.builtin.World.BEnterWorld: {\n");
        _s_.append(_i1_).append("MapId=").append(_MapId).append(",\n");
        _s_.append(_i1_).append("MapInstanceId=").append(_MapInstanceId).append(",\n");
        _s_.append(_i1_).append("Position=").append(_Position).append(",\n");
        _s_.append(_i1_).append("Direct=").append(_Direct).append(",\n");
        _s_.append(_i1_).append("PriorityData=[");
        if (!_PriorityData.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _PriorityData) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_PriorityData.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=");
                _v_.buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("]\n");
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
            long _x_ = _MapInstanceId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = _Position;
            if (_x_ != null && !_x_.isZero()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.VECTOR3);
                _o_.WriteVector3(_x_);
            }
        }
        {
            var _x_ = _Direct;
            if (_x_ != null && !_x_.isZero()) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.VECTOR3);
                _o_.WriteVector3(_x_);
            }
        }
        {
            var _x_ = _PriorityData;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BEAN);
                for (int _j_ = 0, _c_ = _x_.size(); _j_ < _c_; _j_++) {
                    var _v_ = _x_.get(_j_);
                    _v_.encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
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
            _MapInstanceId = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _Position = _o_.ReadVector3(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _Direct = _o_.ReadVector3(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            var _x_ = _PriorityData;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new metagame.builtin.World.BAoiOperates.Data(), _t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
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
        if (!(_o_ instanceof BEnterWorld.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BEnterWorld.Data)_o_;
        if (_MapId != _b_._MapId)
            return false;
        if (_MapInstanceId != _b_._MapInstanceId)
            return false;
        if (!_Position.equals(_b_._Position))
            return false;
        if (!_Direct.equals(_b_._Direct))
            return false;
        if (!_PriorityData.equals(_b_._PriorityData))
            return false;
        return true;
    }
}
}
