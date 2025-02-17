// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

// Zeze.Transaction.Database.Operates 实现协议
@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BSetInUse extends Zeze.Transaction.Bean implements BSetInUseReadOnly {
    public static final long TYPEID = -6535947444840410706L;

    public static final int eSuccess = 0;
    public static final int eDefaultError = 1;
    public static final int eInstanceAlreadyExists = 2;
    public static final int eInsertInstanceError = 3;
    public static final int eGlobalNotSame = 4;
    public static final int eInsertGlobalError = 5;
    public static final int eTooManyInstanceWithoutGlobal = 6;

    private int _LocalId; // serverId
    private String _Global;

    private static final java.lang.invoke.VarHandle vh_LocalId;
    private static final java.lang.invoke.VarHandle vh_Global;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_LocalId = _l_.findVarHandle(BSetInUse.class, "_LocalId", int.class);
            vh_Global = _l_.findVarHandle(BSetInUse.class, "_Global", String.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public int getLocalId() {
        if (!isManaged())
            return _LocalId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _LocalId;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _LocalId;
    }

    public void setLocalId(int _v_) {
        if (!isManaged()) {
            _LocalId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 1, vh_LocalId, _v_));
    }

    @Override
    public String getGlobal() {
        if (!isManaged())
            return _Global;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Global;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 2);
        return log != null ? log.stringValue() : _Global;
    }

    public void setGlobal(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Global = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 2, vh_Global, _v_));
    }

    @SuppressWarnings("deprecation")
    public BSetInUse() {
        _Global = "";
    }

    @SuppressWarnings("deprecation")
    public BSetInUse(int _LocalId_, String _Global_) {
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
    public Zeze.Builtin.Dbh2.Master.BSetInUse.Data toData() {
        var _d_ = new Zeze.Builtin.Dbh2.Master.BSetInUse.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Dbh2.Master.BSetInUse.Data)_o_);
    }

    public void assign(BSetInUse.Data _o_) {
        setLocalId(_o_._LocalId);
        setGlobal(_o_._Global);
        _unknown_ = null;
    }

    public void assign(BSetInUse _o_) {
        setLocalId(_o_.getLocalId());
        setGlobal(_o_.getGlobal());
        _unknown_ = _o_._unknown_;
    }

    public BSetInUse copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BSetInUse copy() {
        var _c_ = new BSetInUse();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BSetInUse _a_, BSetInUse _b_) {
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
        _s_.append("Zeze.Builtin.Dbh2.Master.BSetInUse: {\n");
        _s_.append(_i1_).append("LocalId=").append(getLocalId()).append(",\n");
        _s_.append(_i1_).append("Global=").append(getGlobal()).append('\n');
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
        if (!(_o_ instanceof BSetInUse))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BSetInUse)_o_;
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

// Zeze.Transaction.Database.Operates 实现协议
@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -6535947444840410706L;

    public static final int eSuccess = 0;
    public static final int eDefaultError = 1;
    public static final int eInstanceAlreadyExists = 2;
    public static final int eInsertInstanceError = 3;
    public static final int eGlobalNotSame = 4;
    public static final int eInsertGlobalError = 5;
    public static final int eTooManyInstanceWithoutGlobal = 6;

    private int _LocalId; // serverId
    private String _Global;

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
    public Zeze.Builtin.Dbh2.Master.BSetInUse toBean() {
        var _b_ = new Zeze.Builtin.Dbh2.Master.BSetInUse();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BSetInUse)_o_);
    }

    public void assign(BSetInUse _o_) {
        _LocalId = _o_.getLocalId();
        _Global = _o_.getGlobal();
    }

    public void assign(BSetInUse.Data _o_) {
        _LocalId = _o_._LocalId;
        _Global = _o_._Global;
    }

    @Override
    public BSetInUse.Data copy() {
        var _c_ = new BSetInUse.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BSetInUse.Data _a_, BSetInUse.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BSetInUse.Data clone() {
        return (BSetInUse.Data)super.clone();
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
        _s_.append("Zeze.Builtin.Dbh2.Master.BSetInUse: {\n");
        _s_.append(_i1_).append("LocalId=").append(_LocalId).append(",\n");
        _s_.append(_i1_).append("Global=").append(_Global).append('\n');
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

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BSetInUse.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BSetInUse.Data)_o_;
        if (_LocalId != _b_._LocalId)
            return false;
        if (!_Global.equals(_b_._Global))
            return false;
        return true;
    }
}
}
