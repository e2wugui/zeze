// auto-generated @formatter:off
package Game.Fight;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

// 战斗相关属性，用于技能等。
@SuppressWarnings({"EqualsAndHashcode", "ConstantConditions", "ConstantValue", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BFighter extends Zeze.Transaction.Bean implements BFighterReadOnly {
    public static final long TYPEID = -527486826100084428L;

    private float _Attack;
    private float _Defence;

    private static final java.lang.invoke.VarHandle vh_Attack;
    private static final java.lang.invoke.VarHandle vh_Defence;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_Attack = _l_.findVarHandle(BFighter.class, "_Attack", float.class);
            vh_Defence = _l_.findVarHandle(BFighter.class, "_Defence", float.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public float getAttack() {
        if (!isManaged())
            return _Attack;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Attack;
        var log = (Zeze.Transaction.Logs.LogFloat)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _Attack;
    }

    public void setAttack(float _v_) {
        if (!isManaged()) {
            _Attack = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogFloat(this, 1, vh_Attack, _v_));
    }

    @Override
    public float getDefence() {
        if (!isManaged())
            return _Defence;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Defence;
        var log = (Zeze.Transaction.Logs.LogFloat)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _Defence;
    }

    public void setDefence(float _v_) {
        if (!isManaged()) {
            _Defence = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogFloat(this, 2, vh_Defence, _v_));
    }

    @SuppressWarnings("deprecation")
    public BFighter() {
    }

    @SuppressWarnings("deprecation")
    public BFighter(float _Attack_, float _Defence_) {
        _Attack = _Attack_;
        _Defence = _Defence_;
    }

    @Override
    public void reset() {
        setAttack(0);
        setDefence(0);
        _unknown_ = null;
    }

    public void assign(BFighter _o_) {
        setAttack(_o_.getAttack());
        setDefence(_o_.getDefence());
        _unknown_ = _o_._unknown_;
    }

    public BFighter copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BFighter copy() {
        var _c_ = new BFighter();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BFighter _a_, BFighter _b_) {
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
        _s_.append("Game.Fight.BFighter: {\n");
        _s_.append(_i1_).append("Attack=").append(getAttack()).append(",\n");
        _s_.append(_i1_).append("Defence=").append(getDefence()).append('\n');
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
            float _x_ = getAttack();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.FLOAT);
                _o_.WriteFloat(_x_);
            }
        }
        {
            float _x_ = getDefence();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.FLOAT);
                _o_.WriteFloat(_x_);
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
            setAttack(_o_.ReadFloat(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setDefence(_o_.ReadFloat(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(@Nullable Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BFighter))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BFighter)_o_;
        if (getAttack() != _b_.getAttack())
            return false;
        if (getDefence() != _b_.getDefence())
            return false;
        return true;
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
                case 1: _Attack = _v_.floatValue(); break;
                case 2: _Defence = _v_.floatValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setAttack(_r_.getFloat(_pn_ + "Attack"));
        setDefence(_r_.getFloat(_pn_ + "Defence"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendFloat(_pn_ + "Attack", getAttack());
        _s_.appendFloat(_pn_ + "Defence", getDefence());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Attack", "float", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Defence", "float", "", ""));
        return _v_;
    }
}
