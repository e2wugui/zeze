// auto-generated @formatter:off
package Game.Equip;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"EqualsAndHashcode", "ConstantConditions", "ConstantValue", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BRelational extends Zeze.Transaction.Bean implements BRelationalReadOnly {
    public static final long TYPEID = -797240973525338424L;

    private long _Id;

    private static final java.lang.invoke.VarHandle vh_Id;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_Id = _l_.findVarHandle(BRelational.class, "_Id", long.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public long getId() {
        if (!isManaged())
            return _Id;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Id;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _Id;
    }

    public void setId(long _v_) {
        if (!isManaged()) {
            _Id = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 1, vh_Id, _v_));
    }

    @SuppressWarnings("deprecation")
    public BRelational() {
    }

    @SuppressWarnings("deprecation")
    public BRelational(long _Id_) {
        _Id = _Id_;
    }

    @Override
    public void reset() {
        setId(0);
        _unknown_ = null;
    }

    public void assign(BRelational _o_) {
        setId(_o_.getId());
        _unknown_ = _o_._unknown_;
    }

    public BRelational copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BRelational copy() {
        var _c_ = new BRelational();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BRelational _a_, BRelational _b_) {
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
        _s_.append("Game.Equip.BRelational: {\n");
        _s_.append(_i1_).append("Id=").append(getId()).append('\n');
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
            long _x_ = getId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
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
            setId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(@Nullable Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BRelational))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BRelational)_o_;
        if (getId() != _b_.getId())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getId() < 0)
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
                case 1: _Id = _v_.longValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setId(_r_.getLong(_pn_ + "Id"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendLong(_pn_ + "Id", getId());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Id", "long", "", ""));
        return _v_;
    }
}
