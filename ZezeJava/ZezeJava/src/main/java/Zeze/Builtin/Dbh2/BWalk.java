// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BWalk extends Zeze.Transaction.Bean implements BWalkReadOnly {
    public static final long TYPEID = 2689376469133093665L;

    private Zeze.Net.Binary _ExclusiveStartKey;
    private int _ProposeLimit;
    private boolean _Desc;
    private Zeze.Net.Binary _Prefix;

    private static final java.lang.invoke.VarHandle vh_ExclusiveStartKey;
    private static final java.lang.invoke.VarHandle vh_ProposeLimit;
    private static final java.lang.invoke.VarHandle vh_Desc;
    private static final java.lang.invoke.VarHandle vh_Prefix;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_ExclusiveStartKey = _l_.findVarHandle(BWalk.class, "_ExclusiveStartKey", Zeze.Net.Binary.class);
            vh_ProposeLimit = _l_.findVarHandle(BWalk.class, "_ProposeLimit", int.class);
            vh_Desc = _l_.findVarHandle(BWalk.class, "_Desc", boolean.class);
            vh_Prefix = _l_.findVarHandle(BWalk.class, "_Prefix", Zeze.Net.Binary.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public Zeze.Net.Binary getExclusiveStartKey() {
        if (!isManaged())
            return _ExclusiveStartKey;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ExclusiveStartKey;
        var log = (Zeze.Transaction.Logs.LogBinary)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _ExclusiveStartKey;
    }

    public void setExclusiveStartKey(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ExclusiveStartKey = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBinary(this, 1, vh_ExclusiveStartKey, _v_));
    }

    @Override
    public int getProposeLimit() {
        if (!isManaged())
            return _ProposeLimit;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ProposeLimit;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _ProposeLimit;
    }

    public void setProposeLimit(int _v_) {
        if (!isManaged()) {
            _ProposeLimit = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 2, vh_ProposeLimit, _v_));
    }

    @Override
    public boolean isDesc() {
        if (!isManaged())
            return _Desc;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Desc;
        var log = (Zeze.Transaction.Logs.LogBool)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _Desc;
    }

    public void setDesc(boolean _v_) {
        if (!isManaged()) {
            _Desc = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBool(this, 3, vh_Desc, _v_));
    }

    @Override
    public Zeze.Net.Binary getPrefix() {
        if (!isManaged())
            return _Prefix;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Prefix;
        var log = (Zeze.Transaction.Logs.LogBinary)_t_.getLog(objectId() + 4);
        return log != null ? log.value : _Prefix;
    }

    public void setPrefix(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Prefix = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBinary(this, 4, vh_Prefix, _v_));
    }

    @SuppressWarnings("deprecation")
    public BWalk() {
        _ExclusiveStartKey = Zeze.Net.Binary.Empty;
        _Prefix = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BWalk(Zeze.Net.Binary _ExclusiveStartKey_, int _ProposeLimit_, boolean _Desc_, Zeze.Net.Binary _Prefix_) {
        if (_ExclusiveStartKey_ == null)
            _ExclusiveStartKey_ = Zeze.Net.Binary.Empty;
        _ExclusiveStartKey = _ExclusiveStartKey_;
        _ProposeLimit = _ProposeLimit_;
        _Desc = _Desc_;
        if (_Prefix_ == null)
            _Prefix_ = Zeze.Net.Binary.Empty;
        _Prefix = _Prefix_;
    }

    @Override
    public void reset() {
        setExclusiveStartKey(Zeze.Net.Binary.Empty);
        setProposeLimit(0);
        setDesc(false);
        setPrefix(Zeze.Net.Binary.Empty);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Dbh2.BWalk.Data toData() {
        var _d_ = new Zeze.Builtin.Dbh2.BWalk.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Dbh2.BWalk.Data)_o_);
    }

    public void assign(BWalk.Data _o_) {
        setExclusiveStartKey(_o_._ExclusiveStartKey);
        setProposeLimit(_o_._ProposeLimit);
        setDesc(_o_._Desc);
        setPrefix(_o_._Prefix);
        _unknown_ = null;
    }

    public void assign(BWalk _o_) {
        setExclusiveStartKey(_o_.getExclusiveStartKey());
        setProposeLimit(_o_.getProposeLimit());
        setDesc(_o_.isDesc());
        setPrefix(_o_.getPrefix());
        _unknown_ = _o_._unknown_;
    }

    public BWalk copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BWalk copy() {
        var _c_ = new BWalk();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BWalk _a_, BWalk _b_) {
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
        _s_.append("Zeze.Builtin.Dbh2.BWalk: {\n");
        _s_.append(_i1_).append("ExclusiveStartKey=").append(getExclusiveStartKey()).append(",\n");
        _s_.append(_i1_).append("ProposeLimit=").append(getProposeLimit()).append(",\n");
        _s_.append(_i1_).append("Desc=").append(isDesc()).append(",\n");
        _s_.append(_i1_).append("Prefix=").append(getPrefix()).append('\n');
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
            var _x_ = getExclusiveStartKey();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            int _x_ = getProposeLimit();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            boolean _x_ = isDesc();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        {
            var _x_ = getPrefix();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
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
            setExclusiveStartKey(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setProposeLimit(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setDesc(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setPrefix(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BWalk))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BWalk)_o_;
        if (!getExclusiveStartKey().equals(_b_.getExclusiveStartKey()))
            return false;
        if (getProposeLimit() != _b_.getProposeLimit())
            return false;
        if (isDesc() != _b_.isDesc())
            return false;
        if (!getPrefix().equals(_b_.getPrefix()))
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getProposeLimit() < 0)
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
                case 1: _ExclusiveStartKey = _v_.binaryValue(); break;
                case 2: _ProposeLimit = _v_.intValue(); break;
                case 3: _Desc = _v_.booleanValue(); break;
                case 4: _Prefix = _v_.binaryValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setExclusiveStartKey(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "ExclusiveStartKey")));
        setProposeLimit(_r_.getInt(_pn_ + "ProposeLimit"));
        setDesc(_r_.getBoolean(_pn_ + "Desc"));
        setPrefix(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "Prefix")));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendBinary(_pn_ + "ExclusiveStartKey", getExclusiveStartKey());
        _s_.appendInt(_pn_ + "ProposeLimit", getProposeLimit());
        _s_.appendBoolean(_pn_ + "Desc", isDesc());
        _s_.appendBinary(_pn_ + "Prefix", getPrefix());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "ExclusiveStartKey", "binary", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "ProposeLimit", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Desc", "bool", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "Prefix", "binary", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 2689376469133093665L;

    private Zeze.Net.Binary _ExclusiveStartKey;
    private int _ProposeLimit;
    private boolean _Desc;
    private Zeze.Net.Binary _Prefix;

    public Zeze.Net.Binary getExclusiveStartKey() {
        return _ExclusiveStartKey;
    }

    public void setExclusiveStartKey(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _ExclusiveStartKey = _v_;
    }

    public int getProposeLimit() {
        return _ProposeLimit;
    }

    public void setProposeLimit(int _v_) {
        _ProposeLimit = _v_;
    }

    public boolean isDesc() {
        return _Desc;
    }

    public void setDesc(boolean _v_) {
        _Desc = _v_;
    }

    public Zeze.Net.Binary getPrefix() {
        return _Prefix;
    }

    public void setPrefix(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Prefix = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _ExclusiveStartKey = Zeze.Net.Binary.Empty;
        _Prefix = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public Data(Zeze.Net.Binary _ExclusiveStartKey_, int _ProposeLimit_, boolean _Desc_, Zeze.Net.Binary _Prefix_) {
        if (_ExclusiveStartKey_ == null)
            _ExclusiveStartKey_ = Zeze.Net.Binary.Empty;
        _ExclusiveStartKey = _ExclusiveStartKey_;
        _ProposeLimit = _ProposeLimit_;
        _Desc = _Desc_;
        if (_Prefix_ == null)
            _Prefix_ = Zeze.Net.Binary.Empty;
        _Prefix = _Prefix_;
    }

    @Override
    public void reset() {
        _ExclusiveStartKey = Zeze.Net.Binary.Empty;
        _ProposeLimit = 0;
        _Desc = false;
        _Prefix = Zeze.Net.Binary.Empty;
    }

    @Override
    public Zeze.Builtin.Dbh2.BWalk toBean() {
        var _b_ = new Zeze.Builtin.Dbh2.BWalk();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BWalk)_o_);
    }

    public void assign(BWalk _o_) {
        _ExclusiveStartKey = _o_.getExclusiveStartKey();
        _ProposeLimit = _o_.getProposeLimit();
        _Desc = _o_.isDesc();
        _Prefix = _o_.getPrefix();
    }

    public void assign(BWalk.Data _o_) {
        _ExclusiveStartKey = _o_._ExclusiveStartKey;
        _ProposeLimit = _o_._ProposeLimit;
        _Desc = _o_._Desc;
        _Prefix = _o_._Prefix;
    }

    @Override
    public BWalk.Data copy() {
        var _c_ = new BWalk.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BWalk.Data _a_, BWalk.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BWalk.Data clone() {
        return (BWalk.Data)super.clone();
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
        _s_.append("Zeze.Builtin.Dbh2.BWalk: {\n");
        _s_.append(_i1_).append("ExclusiveStartKey=").append(_ExclusiveStartKey).append(",\n");
        _s_.append(_i1_).append("ProposeLimit=").append(_ProposeLimit).append(",\n");
        _s_.append(_i1_).append("Desc=").append(_Desc).append(",\n");
        _s_.append(_i1_).append("Prefix=").append(_Prefix).append('\n');
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
            var _x_ = _ExclusiveStartKey;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            int _x_ = _ProposeLimit;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            boolean _x_ = _Desc;
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        {
            var _x_ = _Prefix;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _ExclusiveStartKey = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _ProposeLimit = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _Desc = _o_.ReadBool(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _Prefix = _o_.ReadBinary(_t_);
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
        if (!(_o_ instanceof BWalk.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BWalk.Data)_o_;
        if (!_ExclusiveStartKey.equals(_b_._ExclusiveStartKey))
            return false;
        if (_ProposeLimit != _b_._ProposeLimit)
            return false;
        if (_Desc != _b_._Desc)
            return false;
        if (!_Prefix.equals(_b_._Prefix))
            return false;
        return true;
    }
}
}
