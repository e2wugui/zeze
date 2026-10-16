// auto-generated @formatter:off
package demo.Game;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"EqualsAndHashcode", "ConstantConditions", "ConstantValue", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BItem extends Zeze.Transaction.Bean implements BItemReadOnly {
    public static final long TYPEID = -5832249605075278139L;

    private int _ItemId;
    private int _Count;
    private int _Atk;
    private int _Def;
    private int _AddHp;
    private int _Price;

    private transient @Nullable Object __zeze_map_key__;

    @Override
    public @Nullable Object mapKey() {
        return __zeze_map_key__;
    }

    @Override
    public void mapKey(Object _v_) {
        __zeze_map_key__ = _v_;
    }

    private static final java.lang.invoke.VarHandle vh_ItemId;
    private static final java.lang.invoke.VarHandle vh_Count;
    private static final java.lang.invoke.VarHandle vh_Atk;
    private static final java.lang.invoke.VarHandle vh_Def;
    private static final java.lang.invoke.VarHandle vh_AddHp;
    private static final java.lang.invoke.VarHandle vh_Price;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_ItemId = _l_.findVarHandle(BItem.class, "_ItemId", int.class);
            vh_Count = _l_.findVarHandle(BItem.class, "_Count", int.class);
            vh_Atk = _l_.findVarHandle(BItem.class, "_Atk", int.class);
            vh_Def = _l_.findVarHandle(BItem.class, "_Def", int.class);
            vh_AddHp = _l_.findVarHandle(BItem.class, "_AddHp", int.class);
            vh_Price = _l_.findVarHandle(BItem.class, "_Price", int.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public int getItemId() {
        if (!isManaged())
            return _ItemId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ItemId;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _ItemId;
    }

    public void setItemId(int _v_) {
        if (!isManaged()) {
            _ItemId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 1, vh_ItemId, _v_));
    }

    @Override
    public int getCount() {
        if (!isManaged())
            return _Count;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Count;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _Count;
    }

    public void setCount(int _v_) {
        if (!isManaged()) {
            _Count = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 2, vh_Count, _v_));
    }

    @Override
    public int getAtk() {
        if (!isManaged())
            return _Atk;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Atk;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _Atk;
    }

    public void setAtk(int _v_) {
        if (!isManaged()) {
            _Atk = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 3, vh_Atk, _v_));
    }

    @Override
    public int getDef() {
        if (!isManaged())
            return _Def;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Def;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 4);
        return log != null ? log.value : _Def;
    }

    public void setDef(int _v_) {
        if (!isManaged()) {
            _Def = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 4, vh_Def, _v_));
    }

    @Override
    public int getAddHp() {
        if (!isManaged())
            return _AddHp;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _AddHp;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 5);
        return log != null ? log.value : _AddHp;
    }

    public void setAddHp(int _v_) {
        if (!isManaged()) {
            _AddHp = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 5, vh_AddHp, _v_));
    }

    @Override
    public int getPrice() {
        if (!isManaged())
            return _Price;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Price;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 6);
        return log != null ? log.value : _Price;
    }

    public void setPrice(int _v_) {
        if (!isManaged()) {
            _Price = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 6, vh_Price, _v_));
    }

    @SuppressWarnings("deprecation")
    public BItem() {
    }

    @SuppressWarnings("deprecation")
    public BItem(int _ItemId_, int _Count_, int _Atk_, int _Def_, int _AddHp_, int _Price_) {
        _ItemId = _ItemId_;
        _Count = _Count_;
        _Atk = _Atk_;
        _Def = _Def_;
        _AddHp = _AddHp_;
        _Price = _Price_;
    }

    @Override
    public void reset() {
        setItemId(0);
        setCount(0);
        setAtk(0);
        setDef(0);
        setAddHp(0);
        setPrice(0);
        _unknown_ = null;
    }

    public void assign(BItem _o_) {
        setItemId(_o_.getItemId());
        setCount(_o_.getCount());
        setAtk(_o_.getAtk());
        setDef(_o_.getDef());
        setAddHp(_o_.getAddHp());
        setPrice(_o_.getPrice());
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
        _s_.append("demo.Game.BItem: {\n");
        _s_.append(_i1_).append("ItemId=").append(getItemId()).append(",\n");
        _s_.append(_i1_).append("Count=").append(getCount()).append(",\n");
        _s_.append(_i1_).append("Atk=").append(getAtk()).append(",\n");
        _s_.append(_i1_).append("Def=").append(getDef()).append(",\n");
        _s_.append(_i1_).append("AddHp=").append(getAddHp()).append(",\n");
        _s_.append(_i1_).append("Price=").append(getPrice()).append('\n');
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
            int _x_ = getItemId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getCount();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getAtk();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getDef();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getAddHp();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getPrice();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.INTEGER);
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
            setItemId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setCount(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setAtk(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setDef(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setAddHp(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            setPrice(_o_.ReadInt(_t_));
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
        if (getItemId() != _b_.getItemId())
            return false;
        if (getCount() != _b_.getCount())
            return false;
        if (getAtk() != _b_.getAtk())
            return false;
        if (getDef() != _b_.getDef())
            return false;
        if (getAddHp() != _b_.getAddHp())
            return false;
        if (getPrice() != _b_.getPrice())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getItemId() < 0)
            return true;
        if (getCount() < 0)
            return true;
        if (getAtk() < 0)
            return true;
        if (getDef() < 0)
            return true;
        if (getAddHp() < 0)
            return true;
        if (getPrice() < 0)
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
                case 1: _ItemId = _v_.intValue(); break;
                case 2: _Count = _v_.intValue(); break;
                case 3: _Atk = _v_.intValue(); break;
                case 4: _Def = _v_.intValue(); break;
                case 5: _AddHp = _v_.intValue(); break;
                case 6: _Price = _v_.intValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setItemId(_r_.getInt(_pn_ + "ItemId"));
        setCount(_r_.getInt(_pn_ + "Count"));
        setAtk(_r_.getInt(_pn_ + "Atk"));
        setDef(_r_.getInt(_pn_ + "Def"));
        setAddHp(_r_.getInt(_pn_ + "AddHp"));
        setPrice(_r_.getInt(_pn_ + "Price"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendInt(_pn_ + "ItemId", getItemId());
        _s_.appendInt(_pn_ + "Count", getCount());
        _s_.appendInt(_pn_ + "Atk", getAtk());
        _s_.appendInt(_pn_ + "Def", getDef());
        _s_.appendInt(_pn_ + "AddHp", getAddHp());
        _s_.appendInt(_pn_ + "Price", getPrice());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "ItemId", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Count", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Atk", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "Def", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "AddHp", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(6, "Price", "int", "", ""));
        return _v_;
    }
}
