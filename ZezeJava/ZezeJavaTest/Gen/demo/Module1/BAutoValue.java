// auto-generated @formatter:off
package demo.Module1;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

// bean的注释
@SuppressWarnings({"EqualsAndHashcode", "ConstantConditions", "ConstantValue", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BAutoValue extends Zeze.Transaction.Bean implements BAutoValueReadOnly {
    public static final long TYPEID = 6242142801771358229L;

    private long _Current;
    private String _Name;
    private long _LocalId;

    private static final java.lang.invoke.VarHandle vh_Current;
    private static final java.lang.invoke.VarHandle vh_Name;
    private static final java.lang.invoke.VarHandle vh_LocalId;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_Current = _l_.findVarHandle(BAutoValue.class, "_Current", long.class);
            vh_Name = _l_.findVarHandle(BAutoValue.class, "_Name", String.class);
            vh_LocalId = _l_.findVarHandle(BAutoValue.class, "_LocalId", long.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public long getCurrent() {
        if (!isManaged())
            return _Current;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Current;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _Current;
    }

    public void setCurrent(long _v_) {
        if (!isManaged()) {
            _Current = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 1, vh_Current, _v_));
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

    @Override
    public long getLocalId() {
        if (!isManaged())
            return _LocalId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _LocalId;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _LocalId;
    }

    public void setLocalId(long _v_) {
        if (!isManaged()) {
            _LocalId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 3, vh_LocalId, _v_));
    }

    @SuppressWarnings("deprecation")
    public BAutoValue() {
        _Name = "";
    }

    @SuppressWarnings("deprecation")
    public BAutoValue(long _Current_, @Nullable String _Name_, long _LocalId_) {
        _Current = _Current_;
        if (_Name_ == null)
            _Name_ = "";
        _Name = _Name_;
        _LocalId = _LocalId_;
    }

    @Override
    public void reset() {
        setCurrent(0);
        setName("");
        setLocalId(0);
        _unknown_ = null;
    }

    public void assign(BAutoValue _o_) {
        setCurrent(_o_.getCurrent());
        setName(_o_.getName());
        setLocalId(_o_.getLocalId());
        _unknown_ = _o_._unknown_;
    }

    public BAutoValue copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BAutoValue copy() {
        var _c_ = new BAutoValue();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BAutoValue _a_, BAutoValue _b_) {
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
        _s_.append("demo.Module1.BAutoValue: {\n");
        _s_.append(_i1_).append("Current=").append(getCurrent()).append(",\n");
        _s_.append(_i1_).append("Name=").append(getName()).append(",\n");
        _s_.append(_i1_).append("LocalId=").append(getLocalId()).append('\n');
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
            long _x_ = getCurrent();
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
        {
            long _x_ = getLocalId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
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
            setCurrent(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setLocalId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(@Nullable Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BAutoValue))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BAutoValue)_o_;
        if (getCurrent() != _b_.getCurrent())
            return false;
        if (!getName().equals(_b_.getName()))
            return false;
        if (getLocalId() != _b_.getLocalId())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getCurrent() < 0)
            return true;
        if (getLocalId() < 0)
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
                case 1: _Current = _v_.longValue(); break;
                case 2: _Name = _v_.stringValue(); break;
                case 3: _LocalId = _v_.longValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setCurrent(_r_.getLong(_pn_ + "Current"));
        setName(_r_.getString(_pn_ + "Name"));
        if (getName() == null)
            setName("");
        setLocalId(_r_.getLong(_pn_ + "LocalId"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendLong(_pn_ + "Current", getCurrent());
        _s_.appendString(_pn_ + "Name", getName());
        _s_.appendLong(_pn_ + "LocalId", getLocalId());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Current", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Name", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "LocalId", "long", "", ""));
        return _v_;
    }
}
