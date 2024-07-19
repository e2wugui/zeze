// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BClearInUse extends Zeze.Transaction.Bean implements BClearInUseReadOnly {
    public static final long TYPEID = -5497586704851966855L;

    public static final int eSuccess = 0;
    public static final int eDefaultError = 1;
    public static final int eInstanceNotExists = 2;

    private int _LocalId; // serverId
    private String _Global; // global config, not use now, reserve for strict check.

    @Override
    public int getLocalId() {
        if (!isManaged())
            return _LocalId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _LocalId;
        var log = (Log__LocalId)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _LocalId;
    }

    public void setLocalId(int _v_) {
        if (!isManaged()) {
            _LocalId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__LocalId(this, 1, _v_));
    }

    @Override
    public String getGlobal() {
        if (!isManaged())
            return _Global;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Global;
        var log = (Log__Global)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _Global;
    }

    public void setGlobal(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Global = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__Global(this, 2, _v_));
    }

    @SuppressWarnings("deprecation")
    public BClearInUse() {
        _Global = "";
    }

    @SuppressWarnings("deprecation")
    public BClearInUse(int _LocalId_, String _Global_) {
        _LocalId = _LocalId_;
        if (_Global_ == null)
            _Global_ = "";
        _Global = _Global_;
    }

    @Override
    public void reset() {
        setLocalId(0);
        setGlobal("");
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Dbh2.Master.BClearInUse.Data toData() {
        var _d_ = new Zeze.Builtin.Dbh2.Master.BClearInUse.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Dbh2.Master.BClearInUse.Data)_o_);
    }

    public void assign(BClearInUse.Data _o_) {
        setLocalId(_o_._LocalId);
        setGlobal(_o_._Global);
        _unknown_ = null;
    }

    public void assign(BClearInUse _o_) {
        setLocalId(_o_.getLocalId());
        setGlobal(_o_.getGlobal());
        _unknown_ = _o_._unknown_;
    }

    public BClearInUse copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BClearInUse copy() {
        var _c_ = new BClearInUse();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BClearInUse _a_, BClearInUse _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__LocalId extends Zeze.Transaction.Logs.LogInt {
        public Log__LocalId(BClearInUse _b_, int _i_, int _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BClearInUse)getBelong())._LocalId = value; }
    }

    private static final class Log__Global extends Zeze.Transaction.Logs.LogString {
        public Log__Global(BClearInUse _b_, int _i_, String _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BClearInUse)getBelong())._Global = value; }
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Zeze.Builtin.Dbh2.Master.BClearInUse: {").append(System.lineSeparator());
        _l_ += 4;
        _s_.append(Zeze.Util.Str.indent(_l_)).append("LocalId=").append(getLocalId()).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Global=").append(getGlobal()).append(System.lineSeparator());
        _l_ -= 4;
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
            int _x_ = getLocalId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            String _x_ = getGlobal();
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
            setLocalId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setGlobal(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BClearInUse))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BClearInUse)_o_;
        if (getLocalId() != _b_.getLocalId())
            return false;
        if (!getGlobal().equals(_b_.getGlobal()))
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
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
                case 1: _LocalId = _v_.intValue(); break;
                case 2: _Global = _v_.stringValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setLocalId(_r_.getInt(_pn_ + "LocalId"));
        setGlobal(_r_.getString(_pn_ + "Global"));
        if (getGlobal() == null)
            setGlobal("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendInt(_pn_ + "LocalId", getLocalId());
        _s_.appendString(_pn_ + "Global", getGlobal());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "LocalId", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Global", "string", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -5497586704851966855L;

    public static final int eSuccess = 0;
    public static final int eDefaultError = 1;
    public static final int eInstanceNotExists = 2;

    private int _LocalId; // serverId
    private String _Global; // global config, not use now, reserve for strict check.

    public int getLocalId() {
        return _LocalId;
    }

    public void setLocalId(int _v_) {
        _LocalId = _v_;
    }

    public String getGlobal() {
        return _Global;
    }

    public void setGlobal(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Global = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Global = "";
    }

    @SuppressWarnings("deprecation")
    public Data(int _LocalId_, String _Global_) {
        _LocalId = _LocalId_;
        if (_Global_ == null)
            _Global_ = "";
        _Global = _Global_;
    }

    @Override
    public void reset() {
        _LocalId = 0;
        _Global = "";
    }

    @Override
    public Zeze.Builtin.Dbh2.Master.BClearInUse toBean() {
        var _b_ = new Zeze.Builtin.Dbh2.Master.BClearInUse();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BClearInUse)_o_);
    }

    public void assign(BClearInUse _o_) {
        _LocalId = _o_.getLocalId();
        _Global = _o_.getGlobal();
    }

    public void assign(BClearInUse.Data _o_) {
        _LocalId = _o_._LocalId;
        _Global = _o_._Global;
    }

    @Override
    public BClearInUse.Data copy() {
        var _c_ = new BClearInUse.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BClearInUse.Data _a_, BClearInUse.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BClearInUse.Data clone() {
        return (BClearInUse.Data)super.clone();
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Zeze.Builtin.Dbh2.Master.BClearInUse: {").append(System.lineSeparator());
        _l_ += 4;
        _s_.append(Zeze.Util.Str.indent(_l_)).append("LocalId=").append(_LocalId).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Global=").append(_Global).append(System.lineSeparator());
        _l_ -= 4;
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
            int _x_ = _LocalId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            String _x_ = _Global;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _LocalId = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _Global = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
