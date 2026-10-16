// auto-generated @formatter:off
package ClientGame.Equip;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"EqualsAndHashcode", "ConstantConditions", "ConstantValue", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BEquipExtra extends Zeze.Transaction.Bean implements BEquipExtraReadOnly {
    public static final long TYPEID = 1076067654005167423L;

    private int _Attack;
    private int _Defence;
    private int _HotRemoveVar; // 这个变量在新版热更模块会被删除。

    private static final java.lang.invoke.VarHandle vh_Attack;
    private static final java.lang.invoke.VarHandle vh_Defence;
    private static final java.lang.invoke.VarHandle vh_HotRemoveVar;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_Attack = _l_.findVarHandle(BEquipExtra.class, "_Attack", int.class);
            vh_Defence = _l_.findVarHandle(BEquipExtra.class, "_Defence", int.class);
            vh_HotRemoveVar = _l_.findVarHandle(BEquipExtra.class, "_HotRemoveVar", int.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public int getAttack() {
        if (!isManaged())
            return _Attack;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Attack;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _Attack;
    }

    public void setAttack(int _v_) {
        if (!isManaged()) {
            _Attack = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 1, vh_Attack, _v_));
    }

    @Override
    public int getDefence() {
        if (!isManaged())
            return _Defence;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Defence;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _Defence;
    }

    public void setDefence(int _v_) {
        if (!isManaged()) {
            _Defence = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 2, vh_Defence, _v_));
    }

    @Override
    public int getHotRemoveVar() {
        if (!isManaged())
            return _HotRemoveVar;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _HotRemoveVar;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _HotRemoveVar;
    }

    public void setHotRemoveVar(int _v_) {
        if (!isManaged()) {
            _HotRemoveVar = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 3, vh_HotRemoveVar, _v_));
    }

    @SuppressWarnings("deprecation")
    public BEquipExtra() {
    }

    @SuppressWarnings("deprecation")
    public BEquipExtra(int _Attack_, int _Defence_, int _HotRemoveVar_) {
        _Attack = _Attack_;
        _Defence = _Defence_;
        _HotRemoveVar = _HotRemoveVar_;
    }

    @Override
    public void reset() {
        setAttack(0);
        setDefence(0);
        setHotRemoveVar(0);
        _unknown_ = null;
    }

    public void assign(BEquipExtra _o_) {
        setAttack(_o_.getAttack());
        setDefence(_o_.getDefence());
        setHotRemoveVar(_o_.getHotRemoveVar());
        _unknown_ = _o_._unknown_;
    }

    public BEquipExtra copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BEquipExtra copy() {
        var _c_ = new BEquipExtra();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BEquipExtra _a_, BEquipExtra _b_) {
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
        _s_.append("ClientGame.Equip.BEquipExtra: {\n");
        _s_.append(_i1_).append("Attack=").append(getAttack()).append(",\n");
        _s_.append(_i1_).append("Defence=").append(getDefence()).append(",\n");
        _s_.append(_i1_).append("HotRemoveVar=").append(getHotRemoveVar()).append('\n');
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
            int _x_ = getAttack();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getDefence();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getHotRemoveVar();
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
            setAttack(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setDefence(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setHotRemoveVar(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(@Nullable Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BEquipExtra))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BEquipExtra)_o_;
        if (getAttack() != _b_.getAttack())
            return false;
        if (getDefence() != _b_.getDefence())
            return false;
        if (getHotRemoveVar() != _b_.getHotRemoveVar())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getAttack() < 0)
            return true;
        if (getDefence() < 0)
            return true;
        if (getHotRemoveVar() < 0)
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
                case 1: _Attack = _v_.intValue(); break;
                case 2: _Defence = _v_.intValue(); break;
                case 3: _HotRemoveVar = _v_.intValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setAttack(_r_.getInt(_pn_ + "Attack"));
        setDefence(_r_.getInt(_pn_ + "Defence"));
        setHotRemoveVar(_r_.getInt(_pn_ + "HotRemoveVar"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendInt(_pn_ + "Attack", getAttack());
        _s_.appendInt(_pn_ + "Defence", getDefence());
        _s_.appendInt(_pn_ + "HotRemoveVar", getHotRemoveVar());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Attack", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Defence", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "HotRemoveVar", "int", "", ""));
        return _v_;
    }
}
