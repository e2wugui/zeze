// auto-generated @formatter:off
package Game.Buf;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

// buf 应该尽量通过配置控制：冲突，覆盖，增加属性，生命期等
@SuppressWarnings({"EqualsAndHashcode", "ConstantConditions", "ConstantValue", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BBuf extends Zeze.Transaction.Bean implements BBufReadOnly {
    public static final long TYPEID = -4634900835369009583L;

    private int _Id;
    private long _AttachTime; // 加入时间
    private long _ContinueTime; // 持续时间
    private final Zeze.Transaction.DynamicBean _Extra;

    public static final long DynamicTypeId_Extra_Game_Buf_BBufExtra = 7506982410669108623L;

    public static Zeze.Transaction.DynamicBean newDynamicBean_Extra() {
        return new Zeze.Transaction.DynamicBean(4, BBuf::getSpecialTypeIdFromBean_4, BBuf::createBeanFromSpecialTypeId_4);
    }

    public static long getSpecialTypeIdFromBean_4(Zeze.Transaction.Bean _b_) {
        var _t_ = _b_.typeId();
        if (_t_ == Zeze.Transaction.EmptyBean.TYPEID)
            return Zeze.Transaction.EmptyBean.TYPEID;
        if (_t_ == 7506982410669108623L)
            return 7506982410669108623L; // Game.Buf.BBufExtra
        throw new UnsupportedOperationException("Unknown Bean! dynamic@Game.Buf.BBuf:Extra:" + _t_);
    }

    public static Zeze.Transaction.@Nullable Bean createBeanFromSpecialTypeId_4(long _t_) {
        if (_t_ == 7506982410669108623L)
            return new Game.Buf.BBufExtra();
        if (_t_ == Zeze.Transaction.EmptyBean.TYPEID)
            return new Zeze.Transaction.EmptyBean();
        return null;
    }

    private transient @Nullable Object __zeze_map_key__;

    @Override
    public @Nullable Object mapKey() {
        return __zeze_map_key__;
    }

    @Override
    public void mapKey(Object _v_) {
        __zeze_map_key__ = _v_;
    }

    private static final java.lang.invoke.VarHandle vh_Id;
    private static final java.lang.invoke.VarHandle vh_AttachTime;
    private static final java.lang.invoke.VarHandle vh_ContinueTime;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_Id = _l_.findVarHandle(BBuf.class, "_Id", int.class);
            vh_AttachTime = _l_.findVarHandle(BBuf.class, "_AttachTime", long.class);
            vh_ContinueTime = _l_.findVarHandle(BBuf.class, "_ContinueTime", long.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public int getId() {
        if (!isManaged())
            return _Id;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Id;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _Id;
    }

    public void setId(int _v_) {
        if (!isManaged()) {
            _Id = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 1, vh_Id, _v_));
    }

    @Override
    public long getAttachTime() {
        if (!isManaged())
            return _AttachTime;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _AttachTime;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _AttachTime;
    }

    public void setAttachTime(long _v_) {
        if (!isManaged()) {
            _AttachTime = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 2, vh_AttachTime, _v_));
    }

    @Override
    public long getContinueTime() {
        if (!isManaged())
            return _ContinueTime;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ContinueTime;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _ContinueTime;
    }

    public void setContinueTime(long _v_) {
        if (!isManaged()) {
            _ContinueTime = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 3, vh_ContinueTime, _v_));
    }

    public Zeze.Transaction.DynamicBean getExtra() {
        return _Extra;
    }

    public Game.Buf.BBufExtra getExtra_Game_Buf_BBufExtra() {
        return (Game.Buf.BBufExtra)_Extra.getBean();
    }

    public void setExtra(Game.Buf.BBufExtra _v_) {
        _Extra.setBean(_v_);
    }

    @Override
    public Zeze.Transaction.DynamicBeanReadOnly getExtraReadOnly() {
        return _Extra;
    }

    @Override
    public Game.Buf.BBufExtraReadOnly getExtra_Game_Buf_BBufExtraReadOnly() {
        return (Game.Buf.BBufExtra)_Extra.getBean();
    }

    @SuppressWarnings("deprecation")
    public BBuf() {
        _Extra = newDynamicBean_Extra();
    }

    @SuppressWarnings("deprecation")
    public BBuf(int _Id_, long _AttachTime_, long _ContinueTime_) {
        _Id = _Id_;
        _AttachTime = _AttachTime_;
        _ContinueTime = _ContinueTime_;
        _Extra = newDynamicBean_Extra();
    }

    @Override
    public void reset() {
        setId(0);
        setAttachTime(0);
        setContinueTime(0);
        _Extra.reset();
        _unknown_ = null;
    }

    public void assign(BBuf _o_) {
        setId(_o_.getId());
        setAttachTime(_o_.getAttachTime());
        setContinueTime(_o_.getContinueTime());
        _Extra.assign(_o_._Extra);
        _unknown_ = _o_._unknown_;
    }

    public BBuf copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BBuf copy() {
        var _c_ = new BBuf();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BBuf _a_, BBuf _b_) {
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
        _s_.append("Game.Buf.BBuf: {\n");
        _s_.append(_i1_).append("Id=").append(getId()).append(",\n");
        _s_.append(_i1_).append("AttachTime=").append(getAttachTime()).append(",\n");
        _s_.append(_i1_).append("ContinueTime=").append(getContinueTime()).append(",\n");
        _s_.append(_i1_).append("Extra=");
        _Extra.getBean().buildString(_s_, _l_ + 8);
        _s_.append('\n');
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

    private byte @Nullable [] _unknown_;

    public byte @Nullable [] unknown() {
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
            int _x_ = getId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            long _x_ = getAttachTime();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getContinueTime();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = _Extra;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.DYNAMIC);
                _x_.encode(_o_);
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
            setId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setAttachTime(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setContinueTime(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _o_.ReadDynamic(_Extra, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(@Nullable Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BBuf))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BBuf)_o_;
        if (getId() != _b_.getId())
            return false;
        if (getAttachTime() != _b_.getAttachTime())
            return false;
        if (getContinueTime() != _b_.getContinueTime())
            return false;
        if (!_Extra.equals(_b_._Extra))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _Extra.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _Extra.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getId() < 0)
            return true;
        if (getAttachTime() < 0)
            return true;
        if (getContinueTime() < 0)
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
                case 1: _Id = _v_.intValue(); break;
                case 2: _AttachTime = _v_.longValue(); break;
                case 3: _ContinueTime = _v_.longValue(); break;
                case 4: _Extra.followerApply(_v_); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setId(_r_.getInt(_pn_ + "Id"));
        setAttachTime(_r_.getLong(_pn_ + "AttachTime"));
        setContinueTime(_r_.getLong(_pn_ + "ContinueTime"));
        Zeze.Serialize.Helper.decodeJsonDynamic(_Extra, _r_.getString(_pn_ + "Extra"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendInt(_pn_ + "Id", getId());
        _s_.appendLong(_pn_ + "AttachTime", getAttachTime());
        _s_.appendLong(_pn_ + "ContinueTime", getContinueTime());
        _s_.appendString(_pn_ + "Extra", Zeze.Serialize.Helper.encodeJson(_Extra));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Id", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "AttachTime", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "ContinueTime", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "Extra", "dynamic", "", "Game.Buf.BBufExtra"));
        return _v_;
    }
}
