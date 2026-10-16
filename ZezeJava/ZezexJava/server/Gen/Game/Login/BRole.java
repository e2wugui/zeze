// auto-generated @formatter:off
package Game.Login;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"EqualsAndHashcode", "ConstantConditions", "ConstantValue", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BRole extends Zeze.Transaction.Bean implements BRoleReadOnly {
    public static final long TYPEID = -8384964419164522880L;

    private long _Id;
    private String _Name;

    private static final java.lang.invoke.VarHandle vh_Id;
    private static final java.lang.invoke.VarHandle vh_Name;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_Id = _l_.findVarHandle(BRole.class, "_Id", long.class);
            vh_Name = _l_.findVarHandle(BRole.class, "_Name", String.class);
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

    @Override
    public String getName() {
        if (!isManaged())
            return _Name;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Name;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 2);
        return log != null ? log.stringValue() : _Name;
    }

    public void setName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Name = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 2, vh_Name, _v_));
    }

    @SuppressWarnings("deprecation")
    public BRole() {
        _Name = "";
    }

    @SuppressWarnings("deprecation")
    public BRole(long _Id_, @Nullable String _Name_) {
        _Id = _Id_;
        if (_Name_ == null)
            _Name_ = "";
        _Name = _Name_;
    }

    @Override
    public void reset() {
        setId(0);
        setName("");
        _unknown_ = null;
    }

    public void assign(BRole _o_) {
        setId(_o_.getId());
        setName(_o_.getName());
        _unknown_ = _o_._unknown_;
    }

    public BRole copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BRole copy() {
        var _c_ = new BRole();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BRole _a_, BRole _b_) {
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
        _s_.append("Game.Login.BRole: {\n");
        _s_.append(_i1_).append("Id=").append(getId()).append(",\n");
        _s_.append(_i1_).append("Name=").append(getName()).append('\n');
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
        {
            String _x_ = getName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
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
        if (_i_ == 2) {
            setName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(@Nullable Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BRole))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BRole)_o_;
        if (getId() != _b_.getId())
            return false;
        if (!getName().equals(_b_.getName()))
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
                case 2: _Name = _v_.stringValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setId(_r_.getLong(_pn_ + "Id"));
        setName(_r_.getString(_pn_ + "Name"));
        if (getName() == null)
            setName("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendLong(_pn_ + "Id", getId());
        _s_.appendString(_pn_ + "Name", getName());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Id", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Name", "string", "", ""));
        return _v_;
    }
}
