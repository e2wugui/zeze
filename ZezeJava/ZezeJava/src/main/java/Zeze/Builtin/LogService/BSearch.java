// auto-generated @formatter:off
package Zeze.Builtin.LogService;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BSearch extends Zeze.Transaction.Bean implements BSearchReadOnly {
    public static final long TYPEID = 7436194280707275049L;

    private long _Id;
    private int _Limit;
    private boolean _Reset;
    private final Zeze.Transaction.Collections.CollOne<Zeze.Builtin.LogService.BCondition> _Condition;

    @Override
    public long getId() {
        if (!isManaged())
            return _Id;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Id;
        var log = (Log__Id)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _Id;
    }

    public void setId(long _v_) {
        if (!isManaged()) {
            _Id = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__Id(this, 1, _v_));
    }

    @Override
    public int getLimit() {
        if (!isManaged())
            return _Limit;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Limit;
        var log = (Log__Limit)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _Limit;
    }

    public void setLimit(int _v_) {
        if (!isManaged()) {
            _Limit = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__Limit(this, 2, _v_));
    }

    @Override
    public boolean isReset() {
        if (!isManaged())
            return _Reset;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Reset;
        var log = (Log__Reset)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _Reset;
    }

    public void setReset(boolean _v_) {
        if (!isManaged()) {
            _Reset = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__Reset(this, 3, _v_));
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
    public BSearch() {
        _Condition = new Zeze.Transaction.Collections.CollOne<>(new Zeze.Builtin.LogService.BCondition(), Zeze.Builtin.LogService.BCondition.class);
        _Condition.variableId(4);
    }

    @SuppressWarnings("deprecation")
    public BSearch(long _Id_, int _Limit_, boolean _Reset_) {
        _Id = _Id_;
        _Limit = _Limit_;
        _Reset = _Reset_;
        _Condition = new Zeze.Transaction.Collections.CollOne<>(new Zeze.Builtin.LogService.BCondition(), Zeze.Builtin.LogService.BCondition.class);
        _Condition.variableId(4);
    }

    @Override
    public void reset() {
        setId(0);
        setLimit(0);
        setReset(false);
        _Condition.reset();
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.LogService.BSearch.Data toData() {
        var _d_ = new Zeze.Builtin.LogService.BSearch.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.LogService.BSearch.Data)_o_);
    }

    public void assign(BSearch.Data _o_) {
        setId(_o_._Id);
        setLimit(_o_._Limit);
        setReset(_o_._Reset);
        var _d__Condition = new Zeze.Builtin.LogService.BCondition();
        _d__Condition.assign(_o_._Condition);
        _Condition.setValue(_d__Condition);
        _unknown_ = null;
    }

    public void assign(BSearch _o_) {
        setId(_o_.getId());
        setLimit(_o_.getLimit());
        setReset(_o_.isReset());
        _Condition.assign(_o_._Condition);
        _unknown_ = _o_._unknown_;
    }

    public BSearch copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BSearch copy() {
        var _c_ = new BSearch();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BSearch _a_, BSearch _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Id extends Zeze.Transaction.Logs.LogLong {
        public Log__Id(BSearch _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BSearch)getBelong())._Id = value; }
    }

    private static final class Log__Limit extends Zeze.Transaction.Logs.LogInt {
        public Log__Limit(BSearch _b_, int _i_, int _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BSearch)getBelong())._Limit = value; }
    }

    private static final class Log__Reset extends Zeze.Transaction.Logs.LogBool {
        public Log__Reset(BSearch _b_, int _i_, boolean _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BSearch)getBelong())._Reset = value; }
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
        _s_.append("Zeze.Builtin.LogService.BSearch: {\n");
        _s_.append(_i1_).append("Id=").append(getId()).append(",\n");
        _s_.append(_i1_).append("Limit=").append(getLimit()).append(",\n");
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
            boolean _x_ = isReset();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 4, ByteBuffer.BEAN);
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
            setReset(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
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
        if (!(_o_ instanceof BSearch))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BSearch)_o_;
        if (getId() != _b_.getId())
            return false;
        if (getLimit() != _b_.getLimit())
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
                case 3: _Reset = _v_.booleanValue(); break;
                case 4: _Condition.followerApply(_v_); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setId(_r_.getLong(_pn_ + "Id"));
        setLimit(_r_.getInt(_pn_ + "Limit"));
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
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Reset", "bool", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "Condition", "Zeze.Builtin.LogService.BCondition", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 7436194280707275049L;

    private long _Id;
    private int _Limit;
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
    public Data(long _Id_, int _Limit_, boolean _Reset_, Zeze.Builtin.LogService.BCondition.Data _Condition_) {
        _Id = _Id_;
        _Limit = _Limit_;
        _Reset = _Reset_;
        if (_Condition_ == null)
            _Condition_ = new Zeze.Builtin.LogService.BCondition.Data();
        _Condition = _Condition_;
    }

    @Override
    public void reset() {
        _Id = 0;
        _Limit = 0;
        _Reset = false;
        _Condition.reset();
    }

    @Override
    public Zeze.Builtin.LogService.BSearch toBean() {
        var _b_ = new Zeze.Builtin.LogService.BSearch();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BSearch)_o_);
    }

    public void assign(BSearch _o_) {
        _Id = _o_.getId();
        _Limit = _o_.getLimit();
        _Reset = _o_.isReset();
        _Condition.assign(_o_._Condition.getValue());
    }

    public void assign(BSearch.Data _o_) {
        _Id = _o_._Id;
        _Limit = _o_._Limit;
        _Reset = _o_._Reset;
        _Condition.assign(_o_._Condition);
    }

    @Override
    public BSearch.Data copy() {
        var _c_ = new BSearch.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BSearch.Data _a_, BSearch.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BSearch.Data clone() {
        return (BSearch.Data)super.clone();
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
        _s_.append("Zeze.Builtin.LogService.BSearch: {\n");
        _s_.append(_i1_).append("Id=").append(_Id).append(",\n");
        _s_.append(_i1_).append("Limit=").append(_Limit).append(",\n");
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
            boolean _x_ = _Reset;
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 4, ByteBuffer.BEAN);
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
            _Reset = _o_.ReadBool(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _o_.ReadBean(_Condition, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
