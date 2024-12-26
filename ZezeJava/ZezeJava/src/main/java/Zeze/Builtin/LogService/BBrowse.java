// auto-generated @formatter:off
package Zeze.Builtin.LogService;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BBrowse extends Zeze.Transaction.Bean implements BBrowseReadOnly {
    public static final long TYPEID = -5609078144289042953L;

    private long _Id;
    private int _Limit;
    private float _OffsetFactor;
    private boolean _Reset;
    private final Zeze.Transaction.Collections.CollOne<Zeze.Builtin.LogService.BCondition> _Condition;

    private static final java.lang.invoke.VarHandle vh_Id;
    private static final java.lang.invoke.VarHandle vh_Limit;
    private static final java.lang.invoke.VarHandle vh_OffsetFactor;
    private static final java.lang.invoke.VarHandle vh_Reset;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_Id = _l_.findVarHandle(BBrowse.class, "_Id", long.class);
            vh_Limit = _l_.findVarHandle(BBrowse.class, "_Limit", int.class);
            vh_OffsetFactor = _l_.findVarHandle(BBrowse.class, "_OffsetFactor", float.class);
            vh_Reset = _l_.findVarHandle(BBrowse.class, "_Reset", boolean.class);
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
    public int getLimit() {
        if (!isManaged())
            return _Limit;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Limit;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _Limit;
    }

    public void setLimit(int _v_) {
        if (!isManaged()) {
            _Limit = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 2, vh_Limit, _v_));
    }

    @Override
    public float getOffsetFactor() {
        if (!isManaged())
            return _OffsetFactor;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _OffsetFactor;
        var log = (Zeze.Transaction.Logs.LogFloat)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _OffsetFactor;
    }

    public void setOffsetFactor(float _v_) {
        if (!isManaged()) {
            _OffsetFactor = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogFloat(this, 3, vh_OffsetFactor, _v_));
    }

    @Override
    public boolean isReset() {
        if (!isManaged())
            return _Reset;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Reset;
        var log = (Zeze.Transaction.Logs.LogBool)_t_.getLog(objectId() + 4);
        return log != null ? log.value : _Reset;
    }

    public void setReset(boolean _v_) {
        if (!isManaged()) {
            _Reset = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBool(this, 4, vh_Reset, _v_));
    }

    public Zeze.Builtin.LogService.BCondition getCondition() {
        return _Condition.getValue();
    }

    public void setCondition(Zeze.Builtin.LogService.BCondition _v_) {
        _Condition.setValue(_v_);
    }

    @Override
    public Zeze.Builtin.LogService.BConditionReadOnly getConditionReadOnly() {
        return _Condition.getValue();
    }

    @SuppressWarnings("deprecation")
    public BBrowse() {
        _Condition = new Zeze.Transaction.Collections.CollOne<>(new Zeze.Builtin.LogService.BCondition(), Zeze.Builtin.LogService.BCondition.class);
        _Condition.variableId(5);
    }

    @SuppressWarnings("deprecation")
    public BBrowse(long _Id_, int _Limit_, float _OffsetFactor_, boolean _Reset_) {
        _Id = _Id_;
        _Limit = _Limit_;
        _OffsetFactor = _OffsetFactor_;
        _Reset = _Reset_;
        _Condition = new Zeze.Transaction.Collections.CollOne<>(new Zeze.Builtin.LogService.BCondition(), Zeze.Builtin.LogService.BCondition.class);
        _Condition.variableId(5);
    }

    @Override
    public void reset() {
        setId(0);
        setLimit(0);
        setOffsetFactor(0);
        setReset(false);
        _Condition.reset();
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.LogService.BBrowse.Data toData() {
        var _d_ = new Zeze.Builtin.LogService.BBrowse.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.LogService.BBrowse.Data)_o_);
    }

    public void assign(BBrowse.Data _o_) {
        setId(_o_._Id);
        setLimit(_o_._Limit);
        setOffsetFactor(_o_._OffsetFactor);
        setReset(_o_._Reset);
        var _d__Condition = new Zeze.Builtin.LogService.BCondition();
        _d__Condition.assign(_o_._Condition);
        _Condition.setValue(_d__Condition);
        _unknown_ = null;
    }

    public void assign(BBrowse _o_) {
        setId(_o_.getId());
        setLimit(_o_.getLimit());
        setOffsetFactor(_o_.getOffsetFactor());
        setReset(_o_.isReset());
        _Condition.assign(_o_._Condition);
        _unknown_ = _o_._unknown_;
    }

    public BBrowse copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BBrowse copy() {
        var _c_ = new BBrowse();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BBrowse _a_, BBrowse _b_) {
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
        _s_.append("Zeze.Builtin.LogService.BBrowse: {\n");
        _s_.append(_i1_).append("Id=").append(getId()).append(",\n");
        _s_.append(_i1_).append("Limit=").append(getLimit()).append(",\n");
        _s_.append(_i1_).append("OffsetFactor=").append(getOffsetFactor()).append(",\n");
        _s_.append(_i1_).append("Reset=").append(isReset()).append(",\n");
        _s_.append(_i1_).append("Condition=");
        _Condition.buildString(_s_, _l_ + 8);
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
            long _x_ = getId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            int _x_ = getLimit();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            float _x_ = getOffsetFactor();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.FLOAT);
                _o_.WriteFloat(_x_);
            }
        }
        {
            boolean _x_ = isReset();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 5, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _Condition.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
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
            setLimit(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setOffsetFactor(_o_.ReadFloat(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setReset(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            _o_.ReadBean(_Condition, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BBrowse))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BBrowse)_o_;
        if (getId() != _b_.getId())
            return false;
        if (getLimit() != _b_.getLimit())
            return false;
        if (getOffsetFactor() != _b_.getOffsetFactor())
            return false;
        if (isReset() != _b_.isReset())
            return false;
        if (!_Condition.equals(_b_._Condition))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _Condition.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _Condition.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getId() < 0)
            return true;
        if (getLimit() < 0)
            return true;
        if (_Condition.negativeCheck())
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
                case 2: _Limit = _v_.intValue(); break;
                case 3: _OffsetFactor = _v_.floatValue(); break;
                case 4: _Reset = _v_.booleanValue(); break;
                case 5: _Condition.followerApply(_v_); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setId(_r_.getLong(_pn_ + "Id"));
        setLimit(_r_.getInt(_pn_ + "Limit"));
        setOffsetFactor(_r_.getFloat(_pn_ + "OffsetFactor"));
        setReset(_r_.getBoolean(_pn_ + "Reset"));
        _p_.add("Condition");
        _Condition.decodeResultSet(_p_, _r_);
        _p_.remove(_p_.size() - 1);
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendLong(_pn_ + "Id", getId());
        _s_.appendInt(_pn_ + "Limit", getLimit());
        _s_.appendFloat(_pn_ + "OffsetFactor", getOffsetFactor());
        _s_.appendBoolean(_pn_ + "Reset", isReset());
        _p_.add("Condition");
        _Condition.encodeSQLStatement(_p_, _s_);
        _p_.remove(_p_.size() - 1);
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Id", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Limit", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "OffsetFactor", "float", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "Reset", "bool", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "Condition", "Zeze.Builtin.LogService.BCondition", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -5609078144289042953L;

    private long _Id;
    private int _Limit;
    private float _OffsetFactor;
    private boolean _Reset;
    private Zeze.Builtin.LogService.BCondition.Data _Condition;

    public long getId() {
        return _Id;
    }

    public void setId(long _v_) {
        _Id = _v_;
    }

    public int getLimit() {
        return _Limit;
    }

    public void setLimit(int _v_) {
        _Limit = _v_;
    }

    public float getOffsetFactor() {
        return _OffsetFactor;
    }

    public void setOffsetFactor(float _v_) {
        _OffsetFactor = _v_;
    }

    public boolean isReset() {
        return _Reset;
    }

    public void setReset(boolean _v_) {
        _Reset = _v_;
    }

    public Zeze.Builtin.LogService.BCondition.Data getCondition() {
        return _Condition;
    }

    public void setCondition(Zeze.Builtin.LogService.BCondition.Data _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Condition = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Condition = new Zeze.Builtin.LogService.BCondition.Data();
    }

    @SuppressWarnings("deprecation")
    public Data(long _Id_, int _Limit_, float _OffsetFactor_, boolean _Reset_, Zeze.Builtin.LogService.BCondition.Data _Condition_) {
        _Id = _Id_;
        _Limit = _Limit_;
        _OffsetFactor = _OffsetFactor_;
        _Reset = _Reset_;
        if (_Condition_ == null)
            _Condition_ = new Zeze.Builtin.LogService.BCondition.Data();
        _Condition = _Condition_;
    }

    @Override
    public void reset() {
        _Id = 0;
        _Limit = 0;
        _OffsetFactor = 0;
        _Reset = false;
        _Condition.reset();
    }

    @Override
    public Zeze.Builtin.LogService.BBrowse toBean() {
        var _b_ = new Zeze.Builtin.LogService.BBrowse();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BBrowse)_o_);
    }

    public void assign(BBrowse _o_) {
        _Id = _o_.getId();
        _Limit = _o_.getLimit();
        _OffsetFactor = _o_.getOffsetFactor();
        _Reset = _o_.isReset();
        _Condition.assign(_o_._Condition.getValue());
    }

    public void assign(BBrowse.Data _o_) {
        _Id = _o_._Id;
        _Limit = _o_._Limit;
        _OffsetFactor = _o_._OffsetFactor;
        _Reset = _o_._Reset;
        _Condition.assign(_o_._Condition);
    }

    @Override
    public BBrowse.Data copy() {
        var _c_ = new BBrowse.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BBrowse.Data _a_, BBrowse.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BBrowse.Data clone() {
        return (BBrowse.Data)super.clone();
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
        _s_.append("Zeze.Builtin.LogService.BBrowse: {\n");
        _s_.append(_i1_).append("Id=").append(_Id).append(",\n");
        _s_.append(_i1_).append("Limit=").append(_Limit).append(",\n");
        _s_.append(_i1_).append("OffsetFactor=").append(_OffsetFactor).append(",\n");
        _s_.append(_i1_).append("Reset=").append(_Reset).append(",\n");
        _s_.append(_i1_).append("Condition=");
        _Condition.buildString(_s_, _l_ + 8);
        _s_.append('\n');
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
            long _x_ = _Id;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            int _x_ = _Limit;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            float _x_ = _OffsetFactor;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.FLOAT);
                _o_.WriteFloat(_x_);
            }
        }
        {
            boolean _x_ = _Reset;
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 5, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _Condition.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _Id = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _Limit = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _OffsetFactor = _o_.ReadFloat(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _Reset = _o_.ReadBool(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            _o_.ReadBean(_Condition, _t_);
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
        if (!(_o_ instanceof BBrowse.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BBrowse.Data)_o_;
        if (_Id != _b_._Id)
            return false;
        if (_Limit != _b_._Limit)
            return false;
        if (_OffsetFactor != _b_._OffsetFactor)
            return false;
        if (_Reset != _b_._Reset)
            return false;
        if (!_Condition.equals(_b_._Condition))
            return false;
        return true;
    }
}
}
