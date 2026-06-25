// auto-generated @formatter:off
package metagame.builtin.World;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BObject extends Zeze.Transaction.Bean implements BObjectReadOnly {
    public static final long TYPEID = -37733307457054924L;

    private final Zeze.Transaction.DynamicBean _Data;

    public static Zeze.Transaction.DynamicBean newDynamicBean_Data() {
        return new Zeze.Transaction.DynamicBean(1, metagame.World.World::getSpecialTypeIdFromBean, metagame.World.World::createBeanFromSpecialTypeId);
    }

    public static long getSpecialTypeIdFromBean_1(Zeze.Transaction.Bean _b_) {
        return metagame.World.World.getSpecialTypeIdFromBean(_b_);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_1(long _t_) {
        return metagame.World.World.createBeanFromSpecialTypeId(_t_);
    }

    private final Zeze.Transaction.Collections.CollOne<metagame.builtin.World.BMove> _Moving;
    private String _PlayerId;
    private String _LinkName;
    private long _LinkSid;
    private int _Type;
    private int _ConfigId;

    private static final java.lang.invoke.VarHandle vh_PlayerId;
    private static final java.lang.invoke.VarHandle vh_LinkName;
    private static final java.lang.invoke.VarHandle vh_LinkSid;
    private static final java.lang.invoke.VarHandle vh_Type;
    private static final java.lang.invoke.VarHandle vh_ConfigId;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_PlayerId = _l_.findVarHandle(BObject.class, "_PlayerId", String.class);
            vh_LinkName = _l_.findVarHandle(BObject.class, "_LinkName", String.class);
            vh_LinkSid = _l_.findVarHandle(BObject.class, "_LinkSid", long.class);
            vh_Type = _l_.findVarHandle(BObject.class, "_Type", int.class);
            vh_ConfigId = _l_.findVarHandle(BObject.class, "_ConfigId", int.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    public Zeze.Transaction.DynamicBean getData() {
        return _Data;
    }

    @Override
    public Zeze.Transaction.DynamicBeanReadOnly getDataReadOnly() {
        return _Data;
    }

    public metagame.builtin.World.BMove getMoving() {
        return _Moving.getValue();
    }

    public void setMoving(metagame.builtin.World.BMove _v_) {
        _Moving.setValue(_v_);
    }

    @Override
    public metagame.builtin.World.BMoveReadOnly getMovingReadOnly() {
        return _Moving.getValue();
    }

    @Override
    public String getPlayerId() {
        if (!isManaged())
            return _PlayerId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _PlayerId;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 3);
        return log != null ? log.stringValue() : _PlayerId;
    }

    public void setPlayerId(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _PlayerId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 3, vh_PlayerId, _v_));
    }

    @Override
    public String getLinkName() {
        if (!isManaged())
            return _LinkName;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _LinkName;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 4);
        return log != null ? log.stringValue() : _LinkName;
    }

    public void setLinkName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _LinkName = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 4, vh_LinkName, _v_));
    }

    @Override
    public long getLinkSid() {
        if (!isManaged())
            return _LinkSid;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _LinkSid;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 5);
        return log != null ? log.value : _LinkSid;
    }

    public void setLinkSid(long _v_) {
        if (!isManaged()) {
            _LinkSid = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 5, vh_LinkSid, _v_));
    }

    @Override
    public int getType() {
        if (!isManaged())
            return _Type;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Type;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 6);
        return log != null ? log.value : _Type;
    }

    public void setType(int _v_) {
        if (!isManaged()) {
            _Type = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 6, vh_Type, _v_));
    }

    @Override
    public int getConfigId() {
        if (!isManaged())
            return _ConfigId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ConfigId;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 7);
        return log != null ? log.value : _ConfigId;
    }

    public void setConfigId(int _v_) {
        if (!isManaged()) {
            _ConfigId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 7, vh_ConfigId, _v_));
    }

    @SuppressWarnings("deprecation")
    public BObject() {
        _Data = newDynamicBean_Data();
        _Moving = new Zeze.Transaction.Collections.CollOne<>(new metagame.builtin.World.BMove(), metagame.builtin.World.BMove.class);
        _Moving.variableId(2);
        _PlayerId = "";
        _LinkName = "";
    }

    @SuppressWarnings("deprecation")
    public BObject(String _PlayerId_, String _LinkName_, long _LinkSid_, int _Type_, int _ConfigId_) {
        _Data = newDynamicBean_Data();
        _Moving = new Zeze.Transaction.Collections.CollOne<>(new metagame.builtin.World.BMove(), metagame.builtin.World.BMove.class);
        _Moving.variableId(2);
        if (_PlayerId_ == null)
            _PlayerId_ = "";
        _PlayerId = _PlayerId_;
        if (_LinkName_ == null)
            _LinkName_ = "";
        _LinkName = _LinkName_;
        _LinkSid = _LinkSid_;
        _Type = _Type_;
        _ConfigId = _ConfigId_;
    }

    @Override
    public void reset() {
        _Data.reset();
        _Moving.reset();
        setPlayerId("");
        setLinkName("");
        setLinkSid(0);
        setType(0);
        setConfigId(0);
        _unknown_ = null;
    }

    @Override
    public metagame.builtin.World.BObject.Data toData() {
        var _d_ = new metagame.builtin.World.BObject.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((metagame.builtin.World.BObject.Data)_o_);
    }

    public void assign(BObject.Data _o_) {
        _Data.assign(_o_._Data);
        var _d__Moving = new metagame.builtin.World.BMove();
        _d__Moving.assign(_o_._Moving);
        _Moving.setValue(_d__Moving);
        setPlayerId(_o_._PlayerId);
        setLinkName(_o_._LinkName);
        setLinkSid(_o_._LinkSid);
        setType(_o_._Type);
        setConfigId(_o_._ConfigId);
        _unknown_ = null;
    }

    public void assign(BObject _o_) {
        _Data.assign(_o_._Data);
        _Moving.assign(_o_._Moving);
        setPlayerId(_o_.getPlayerId());
        setLinkName(_o_.getLinkName());
        setLinkSid(_o_.getLinkSid());
        setType(_o_.getType());
        setConfigId(_o_.getConfigId());
        _unknown_ = _o_._unknown_;
    }

    public BObject copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BObject copy() {
        var _c_ = new BObject();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BObject _a_, BObject _b_) {
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
        _s_.append("metagame.builtin.World.BObject: {\n");
        _s_.append(_i1_).append("Data=");
        _Data.getBean().buildString(_s_, _l_ + 8);
        _s_.append(",\n");
        _s_.append(_i1_).append("Moving=");
        _Moving.buildString(_s_, _l_ + 8);
        _s_.append(",\n");
        _s_.append(_i1_).append("PlayerId=").append(getPlayerId()).append(",\n");
        _s_.append(_i1_).append("LinkName=").append(getLinkName()).append(",\n");
        _s_.append(_i1_).append("LinkSid=").append(getLinkSid()).append(",\n");
        _s_.append(_i1_).append("Type=").append(getType()).append(",\n");
        _s_.append(_i1_).append("ConfigId=").append(getConfigId()).append('\n');
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
            var _x_ = _Data;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.DYNAMIC);
                _x_.encode(_o_);
            }
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 2, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _Moving.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        {
            String _x_ = getPlayerId();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getLinkName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            long _x_ = getLinkSid();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            int _x_ = getType();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getConfigId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 7, ByteBuffer.INTEGER);
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
            _o_.ReadDynamic(_Data, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _o_.ReadBean(_Moving, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setPlayerId(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setLinkName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setLinkSid(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            setType(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 7) {
            setConfigId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BObject))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BObject)_o_;
        if (!_Data.equals(_b_._Data))
            return false;
        if (!_Moving.equals(_b_._Moving))
            return false;
        if (!getPlayerId().equals(_b_.getPlayerId()))
            return false;
        if (!getLinkName().equals(_b_.getLinkName()))
            return false;
        if (getLinkSid() != _b_.getLinkSid())
            return false;
        if (getType() != _b_.getType())
            return false;
        if (getConfigId() != _b_.getConfigId())
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _Data.initRootInfo(_r_, this);
        _Moving.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _Data.initRootInfoWithRedo(_r_, this);
        _Moving.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        if (_Moving.negativeCheck())
            return true;
        if (getLinkSid() < 0)
            return true;
        if (getType() < 0)
            return true;
        if (getConfigId() < 0)
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
                case 1: _Data.followerApply(_v_); break;
                case 2: _Moving.followerApply(_v_); break;
                case 3: _PlayerId = _v_.stringValue(); break;
                case 4: _LinkName = _v_.stringValue(); break;
                case 5: _LinkSid = _v_.longValue(); break;
                case 6: _Type = _v_.intValue(); break;
                case 7: _ConfigId = _v_.intValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        Zeze.Serialize.Helper.decodeJsonDynamic(_Data, _r_.getString(_pn_ + "Data"));
        _p_.add("Moving");
        _Moving.decodeResultSet(_p_, _r_);
        _p_.remove(_p_.size() - 1);
        setPlayerId(_r_.getString(_pn_ + "PlayerId"));
        if (getPlayerId() == null)
            setPlayerId("");
        setLinkName(_r_.getString(_pn_ + "LinkName"));
        if (getLinkName() == null)
            setLinkName("");
        setLinkSid(_r_.getLong(_pn_ + "LinkSid"));
        setType(_r_.getInt(_pn_ + "Type"));
        setConfigId(_r_.getInt(_pn_ + "ConfigId"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "Data", Zeze.Serialize.Helper.encodeJson(_Data));
        _p_.add("Moving");
        _Moving.encodeSQLStatement(_p_, _s_);
        _p_.remove(_p_.size() - 1);
        _s_.appendString(_pn_ + "PlayerId", getPlayerId());
        _s_.appendString(_pn_ + "LinkName", getLinkName());
        _s_.appendLong(_pn_ + "LinkSid", getLinkSid());
        _s_.appendInt(_pn_ + "Type", getType());
        _s_.appendInt(_pn_ + "ConfigId", getConfigId());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Data", "dynamic", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Moving", "metagame.builtin.World.BMove", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "PlayerId", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "LinkName", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "LinkSid", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(6, "Type", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(7, "ConfigId", "int", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -37733307457054924L;

    private final DynamicData_Data _Data;

    public static final class DynamicData_Data extends Zeze.Transaction.DynamicData {
        static {
            registerJsonParser(DynamicData_Data.class);
        }

        @Override
        public long toTypeId(Zeze.Transaction.Data _d_) {
            return metagame.World.World.getSpecialTypeIdFromBean(_d_);
        }

        @Override
        public Zeze.Transaction.Data toData(long _t_) {
            return metagame.World.World.createDataFromSpecialTypeId(_t_);
        }

        @Override
        public DynamicData_Data copy() {
            return (DynamicData_Data)super.copy();
        }
    }

    private metagame.builtin.World.BMove.Data _Moving;
    private String _PlayerId;
    private String _LinkName;
    private long _LinkSid;
    private int _Type;
    private int _ConfigId;

    public DynamicData_Data getData() {
        return _Data;
    }

    public metagame.builtin.World.BMove.Data getMoving() {
        return _Moving;
    }

    public void setMoving(metagame.builtin.World.BMove.Data _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Moving = _v_;
    }

    public String getPlayerId() {
        return _PlayerId;
    }

    public void setPlayerId(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _PlayerId = _v_;
    }

    public String getLinkName() {
        return _LinkName;
    }

    public void setLinkName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _LinkName = _v_;
    }

    public long getLinkSid() {
        return _LinkSid;
    }

    public void setLinkSid(long _v_) {
        _LinkSid = _v_;
    }

    public int getType() {
        return _Type;
    }

    public void setType(int _v_) {
        _Type = _v_;
    }

    public int getConfigId() {
        return _ConfigId;
    }

    public void setConfigId(int _v_) {
        _ConfigId = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Data = new DynamicData_Data();
        _Moving = new metagame.builtin.World.BMove.Data();
        _PlayerId = "";
        _LinkName = "";
    }

    @SuppressWarnings("deprecation")
    public Data(DynamicData_Data _Data_, metagame.builtin.World.BMove.Data _Moving_, String _PlayerId_, String _LinkName_, long _LinkSid_, int _Type_, int _ConfigId_) {
        if (_Data_ == null)
            _Data_ = new DynamicData_Data();
        _Data = _Data_;
        if (_Moving_ == null)
            _Moving_ = new metagame.builtin.World.BMove.Data();
        _Moving = _Moving_;
        if (_PlayerId_ == null)
            _PlayerId_ = "";
        _PlayerId = _PlayerId_;
        if (_LinkName_ == null)
            _LinkName_ = "";
        _LinkName = _LinkName_;
        _LinkSid = _LinkSid_;
        _Type = _Type_;
        _ConfigId = _ConfigId_;
    }

    @Override
    public void reset() {
        _Data.reset();
        _Moving.reset();
        _PlayerId = "";
        _LinkName = "";
        _LinkSid = 0;
        _Type = 0;
        _ConfigId = 0;
    }

    @Override
    public metagame.builtin.World.BObject toBean() {
        var _b_ = new metagame.builtin.World.BObject();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BObject)_o_);
    }

    public void assign(BObject _o_) {
        _Data.assign(_o_._Data);
        _Moving.assign(_o_._Moving.getValue());
        _PlayerId = _o_.getPlayerId();
        _LinkName = _o_.getLinkName();
        _LinkSid = _o_.getLinkSid();
        _Type = _o_.getType();
        _ConfigId = _o_.getConfigId();
    }

    public void assign(BObject.Data _o_) {
        _Data.assign(_o_._Data);
        _Moving.assign(_o_._Moving);
        _PlayerId = _o_._PlayerId;
        _LinkName = _o_._LinkName;
        _LinkSid = _o_._LinkSid;
        _Type = _o_._Type;
        _ConfigId = _o_._ConfigId;
    }

    @Override
    public BObject.Data copy() {
        var _c_ = new BObject.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BObject.Data _a_, BObject.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BObject.Data clone() {
        return (BObject.Data)super.clone();
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
        _s_.append("metagame.builtin.World.BObject: {\n");
        _s_.append(_i1_).append("Data=");
        _Data.getData().buildString(_s_, _l_ + 8);
        _s_.append(",\n");
        _s_.append(_i1_).append("Moving=");
        _Moving.buildString(_s_, _l_ + 8);
        _s_.append(",\n");
        _s_.append(_i1_).append("PlayerId=").append(_PlayerId).append(",\n");
        _s_.append(_i1_).append("LinkName=").append(_LinkName).append(",\n");
        _s_.append(_i1_).append("LinkSid=").append(_LinkSid).append(",\n");
        _s_.append(_i1_).append("Type=").append(_Type).append(",\n");
        _s_.append(_i1_).append("ConfigId=").append(_ConfigId).append('\n');
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
            var _x_ = _Data;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.DYNAMIC);
                _x_.encode(_o_);
            }
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 2, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _Moving.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        {
            String _x_ = _PlayerId;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = _LinkName;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            long _x_ = _LinkSid;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            int _x_ = _Type;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = _ConfigId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 7, ByteBuffer.INTEGER);
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
            _o_.ReadDynamic(_Data, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _o_.ReadBean(_Moving, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _PlayerId = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _LinkName = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            _LinkSid = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            _Type = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 7) {
            _ConfigId = _o_.ReadInt(_t_);
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
        if (!(_o_ instanceof BObject.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BObject.Data)_o_;
        if (!_Data.equals(_b_._Data))
            return false;
        if (!_Moving.equals(_b_._Moving))
            return false;
        if (!_PlayerId.equals(_b_._PlayerId))
            return false;
        if (!_LinkName.equals(_b_._LinkName))
            return false;
        if (_LinkSid != _b_._LinkSid)
            return false;
        if (_Type != _b_._Type)
            return false;
        if (_ConfigId != _b_._ConfigId)
            return false;
        return true;
    }
}
}
