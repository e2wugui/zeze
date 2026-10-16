// auto-generated @formatter:off
package Zeze.Builtin.Game.Rank;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"EqualsAndHashcode", "ConstantConditions", "ConstantValue", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BRankValue extends Zeze.Transaction.Bean implements BRankValueReadOnly {
    public static final long TYPEID = 2276228832088785165L;

    private long _RoleId;
    private final Zeze.Transaction.DynamicBean _Dynamic;

    public static Zeze.Transaction.DynamicBean newDynamicBean_Dynamic() {
        return new Zeze.Transaction.DynamicBean(2, Zeze.Game.Rank::getSpecialTypeIdFromBean, Zeze.Game.Rank::createBeanFromSpecialTypeId);
    }

    public static long getSpecialTypeIdFromBean_2(Zeze.Transaction.Bean _b_) {
        return Zeze.Game.Rank.getSpecialTypeIdFromBean(_b_);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_2(long _t_) {
        return Zeze.Game.Rank.createBeanFromSpecialTypeId(_t_);
    }

    private static final java.lang.invoke.VarHandle vh_RoleId;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_RoleId = _l_.findVarHandle(BRankValue.class, "_RoleId", long.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public long getRoleId() {
        if (!isManaged())
            return _RoleId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _RoleId;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _RoleId;
    }

    public void setRoleId(long _v_) {
        if (!isManaged()) {
            _RoleId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 1, vh_RoleId, _v_));
    }

    public Zeze.Transaction.DynamicBean getDynamic() {
        return _Dynamic;
    }

    @Override
    public Zeze.Transaction.DynamicBeanReadOnly getDynamicReadOnly() {
        return _Dynamic;
    }

    @SuppressWarnings("deprecation")
    public BRankValue() {
        _Dynamic = newDynamicBean_Dynamic();
    }

    @SuppressWarnings("deprecation")
    public BRankValue(long _RoleId_) {
        _RoleId = _RoleId_;
        _Dynamic = newDynamicBean_Dynamic();
    }

    @Override
    public void reset() {
        setRoleId(0);
        _Dynamic.reset();
        _unknown_ = null;
    }

    public void assign(BRankValue _o_) {
        setRoleId(_o_.getRoleId());
        _Dynamic.assign(_o_._Dynamic);
        _unknown_ = _o_._unknown_;
    }

    public BRankValue copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BRankValue copy() {
        var _c_ = new BRankValue();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BRankValue _a_, BRankValue _b_) {
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
        _s_.append("Zeze.Builtin.Game.Rank.BRankValue: {\n");
        _s_.append(_i1_).append("RoleId=").append(getRoleId()).append(",\n");
        _s_.append(_i1_).append("Dynamic=");
        _Dynamic.getBean().buildString(_s_, _l_ + 8);
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
            long _x_ = getRoleId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = _Dynamic;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.DYNAMIC);
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
            setRoleId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _o_.ReadDynamic(_Dynamic, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(@Nullable Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BRankValue))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BRankValue)_o_;
        if (getRoleId() != _b_.getRoleId())
            return false;
        if (!_Dynamic.equals(_b_._Dynamic))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _Dynamic.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _Dynamic.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getRoleId() < 0)
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
                case 1: _RoleId = _v_.longValue(); break;
                case 2: _Dynamic.followerApply(_v_); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setRoleId(_r_.getLong(_pn_ + "RoleId"));
        Zeze.Serialize.Helper.decodeJsonDynamic(_Dynamic, _r_.getString(_pn_ + "Dynamic"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendLong(_pn_ + "RoleId", getRoleId());
        _s_.appendString(_pn_ + "Dynamic", Zeze.Serialize.Helper.encodeJson(_Dynamic));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "RoleId", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Dynamic", "dynamic", "", ""));
        return _v_;
    }
}
