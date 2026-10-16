// auto-generated @formatter:off
package Game.Equip;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"EqualsAndHashcode", "ConstantConditions", "ConstantValue", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BItem extends Zeze.Transaction.Bean implements BItemReadOnly {
    public static final long TYPEID = -8453741022330215908L;

    private int _Id;
    private int _Number;
    private final Zeze.Transaction.DynamicBean _Extra;

    public static final long DynamicTypeId_Extra_Game_Item_BHorseExtra = -6414823809446200925L;

    public static final long DynamicTypeId_Extra_Game_Item_BFoodExtra = -5635260117858385112L;

    public static final long DynamicTypeId_Extra_Game_Equip_BEquipExtra = 1076067654005167423L;

    public static Zeze.Transaction.DynamicBean newDynamicBean_Extra() {
        return new Zeze.Transaction.DynamicBean(3, BItem::getSpecialTypeIdFromBean_3, BItem::createBeanFromSpecialTypeId_3);
    }

    public static long getSpecialTypeIdFromBean_3(Zeze.Transaction.Bean _b_) {
        var _t_ = _b_.typeId();
        if (_t_ == Zeze.Transaction.EmptyBean.TYPEID)
            return Zeze.Transaction.EmptyBean.TYPEID;
        if (_t_ == -6414823809446200925L)
            return -6414823809446200925L; // Game.Item.BHorseExtra
        if (_t_ == -5635260117858385112L)
            return -5635260117858385112L; // Game.Item.BFoodExtra
        if (_t_ == 1076067654005167423L)
            return 1076067654005167423L; // Game.Equip.BEquipExtra
        throw new UnsupportedOperationException("Unknown Bean! dynamic@Game.Equip.BItem:Extra:" + _t_);
    }

    public static Zeze.Transaction.@Nullable Bean createBeanFromSpecialTypeId_3(long _t_) {
        if (_t_ == -6414823809446200925L)
            return new Game.Item.BHorseExtra();
        if (_t_ == -5635260117858385112L)
            return new Game.Item.BFoodExtra();
        if (_t_ == 1076067654005167423L)
            return new Game.Equip.BEquipExtra();
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
    private static final java.lang.invoke.VarHandle vh_Number;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_Id = _l_.findVarHandle(BItem.class, "_Id", int.class);
            vh_Number = _l_.findVarHandle(BItem.class, "_Number", int.class);
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
    public int getNumber() {
        if (!isManaged())
            return _Number;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Number;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _Number;
    }

    public void setNumber(int _v_) {
        if (!isManaged()) {
            _Number = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 2, vh_Number, _v_));
    }

    public Zeze.Transaction.DynamicBean getExtra() {
        return _Extra;
    }

    public Game.Item.BHorseExtra getExtra_Game_Item_BHorseExtra() {
        return (Game.Item.BHorseExtra)_Extra.getBean();
    }

    public void setExtra(Game.Item.BHorseExtra _v_) {
        _Extra.setBean(_v_);
    }

    public Game.Item.BFoodExtra getExtra_Game_Item_BFoodExtra() {
        return (Game.Item.BFoodExtra)_Extra.getBean();
    }

    public void setExtra(Game.Item.BFoodExtra _v_) {
        _Extra.setBean(_v_);
    }

    public Game.Equip.BEquipExtra getExtra_Game_Equip_BEquipExtra() {
        return (Game.Equip.BEquipExtra)_Extra.getBean();
    }

    public void setExtra(Game.Equip.BEquipExtra _v_) {
        _Extra.setBean(_v_);
    }

    @Override
    public Zeze.Transaction.DynamicBeanReadOnly getExtraReadOnly() {
        return _Extra;
    }

    @Override
    public Game.Item.BHorseExtraReadOnly getExtra_Game_Item_BHorseExtraReadOnly() {
        return (Game.Item.BHorseExtra)_Extra.getBean();
    }

    @Override
    public Game.Item.BFoodExtraReadOnly getExtra_Game_Item_BFoodExtraReadOnly() {
        return (Game.Item.BFoodExtra)_Extra.getBean();
    }

    @Override
    public Game.Equip.BEquipExtraReadOnly getExtra_Game_Equip_BEquipExtraReadOnly() {
        return (Game.Equip.BEquipExtra)_Extra.getBean();
    }

    @SuppressWarnings("deprecation")
    public BItem() {
        _Extra = newDynamicBean_Extra();
    }

    @SuppressWarnings("deprecation")
    public BItem(int _Id_, int _Number_) {
        _Id = _Id_;
        _Number = _Number_;
        _Extra = newDynamicBean_Extra();
    }

    @Override
    public void reset() {
        setId(0);
        setNumber(0);
        _Extra.reset();
        _unknown_ = null;
    }

    public void assign(BItem _o_) {
        setId(_o_.getId());
        setNumber(_o_.getNumber());
        _Extra.assign(_o_._Extra);
        _unknown_ = _o_._unknown_;
    }

    public BItem copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BItem copy() {
        var _c_ = new BItem();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BItem _a_, BItem _b_) {
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
        _s_.append("Game.Equip.BItem: {\n");
        _s_.append(_i1_).append("Id=").append(getId()).append(",\n");
        _s_.append(_i1_).append("Number=").append(getNumber()).append(",\n");
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
            int _x_ = getNumber();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = _Extra;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.DYNAMIC);
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
            setNumber(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
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
        if (!(_o_ instanceof BItem))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BItem)_o_;
        if (getId() != _b_.getId())
            return false;
        if (getNumber() != _b_.getNumber())
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
        if (getNumber() < 0)
            return true;
        if (_Extra.negativeCheck())
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
                case 2: _Number = _v_.intValue(); break;
                case 3: _Extra.followerApply(_v_); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setId(_r_.getInt(_pn_ + "Id"));
        setNumber(_r_.getInt(_pn_ + "Number"));
        Zeze.Serialize.Helper.decodeJsonDynamic(_Extra, _r_.getString(_pn_ + "Extra"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendInt(_pn_ + "Id", getId());
        _s_.appendInt(_pn_ + "Number", getNumber());
        _s_.appendString(_pn_ + "Extra", Zeze.Serialize.Helper.encodeJson(_Extra));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Id", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Number", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Extra", "dynamic", "", "Game.Item.BHorseExtra,Game.Item.BFoodExtra,Game.Equip.BEquipExtra"));
        return _v_;
    }
}
