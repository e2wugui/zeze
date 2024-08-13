// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BSaveDataWithSameVersion extends Zeze.Transaction.Bean implements BSaveDataWithSameVersionReadOnly {
    public static final long TYPEID = -2351623358007351459L;

    public static final int eSuccess = 0;
    public static final int eDefaultError = 1;
    public static final int eVersionMismatch = 2;
    public static final int eUpdateError = 3;

    private Zeze.Net.Binary _Key;
    private Zeze.Net.Binary _Data;
    private long _Version;

    private static final java.lang.invoke.VarHandle vh_Key;
    private static final java.lang.invoke.VarHandle vh_Data;
    private static final java.lang.invoke.VarHandle vh_Version;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_Key = _l_.findVarHandle(BSaveDataWithSameVersion.class, "_Key", Zeze.Net.Binary.class);
            vh_Data = _l_.findVarHandle(BSaveDataWithSameVersion.class, "_Data", Zeze.Net.Binary.class);
            vh_Version = _l_.findVarHandle(BSaveDataWithSameVersion.class, "_Version", long.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public Zeze.Net.Binary getKey() {
        if (!isManaged())
            return _Key;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Key;
        var log = (Zeze.Transaction.Logs.LogBinary)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _Key;
    }

    public void setKey(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Key = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBinary(this, 1, vh_Key, _v_));
    }

    @Override
    public Zeze.Net.Binary getData() {
        if (!isManaged())
            return _Data;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Data;
        var log = (Zeze.Transaction.Logs.LogBinary)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _Data;
    }

    public void setData(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Data = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBinary(this, 2, vh_Data, _v_));
    }

    @Override
    public long getVersion() {
        if (!isManaged())
            return _Version;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Version;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _Version;
    }

    public void setVersion(long _v_) {
        if (!isManaged()) {
            _Version = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 3, vh_Version, _v_));
    }

    @SuppressWarnings("deprecation")
    public BSaveDataWithSameVersion() {
        _Key = Zeze.Net.Binary.Empty;
        _Data = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BSaveDataWithSameVersion(Zeze.Net.Binary _Key_, Zeze.Net.Binary _Data_, long _Version_) {
        if (_Key_ == null)
            _Key_ = Zeze.Net.Binary.Empty;
        _Key = _Key_;
        if (_Data_ == null)
            _Data_ = Zeze.Net.Binary.Empty;
        _Data = _Data_;
        _Version = _Version_;
    }

    @Override
    public void reset() {
        setKey(Zeze.Net.Binary.Empty);
        setData(Zeze.Net.Binary.Empty);
        setVersion(0);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Dbh2.Master.BSaveDataWithSameVersion.Data toData() {
        var _d_ = new Zeze.Builtin.Dbh2.Master.BSaveDataWithSameVersion.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Dbh2.Master.BSaveDataWithSameVersion.Data)_o_);
    }

    public void assign(BSaveDataWithSameVersion.Data _o_) {
        setKey(_o_._Key);
        setData(_o_._Data);
        setVersion(_o_._Version);
        _unknown_ = null;
    }

    public void assign(BSaveDataWithSameVersion _o_) {
        setKey(_o_.getKey());
        setData(_o_.getData());
        setVersion(_o_.getVersion());
        _unknown_ = _o_._unknown_;
    }

    public BSaveDataWithSameVersion copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BSaveDataWithSameVersion copy() {
        var _c_ = new BSaveDataWithSameVersion();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BSaveDataWithSameVersion _a_, BSaveDataWithSameVersion _b_) {
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
        _s_.append("Zeze.Builtin.Dbh2.Master.BSaveDataWithSameVersion: {\n");
        _s_.append(_i1_).append("Key=").append(getKey()).append(",\n");
        _s_.append(_i1_).append("Data=").append(getData()).append(",\n");
        _s_.append(_i1_).append("Version=").append(getVersion()).append('\n');
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
            var _x_ = getKey();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            var _x_ = getData();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            long _x_ = getVersion();
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
            setKey(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setData(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setVersion(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BSaveDataWithSameVersion))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BSaveDataWithSameVersion)_o_;
        if (!getKey().equals(_b_.getKey()))
            return false;
        if (!getData().equals(_b_.getData()))
            return false;
        if (getVersion() != _b_.getVersion())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getVersion() < 0)
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
                case 1: _Key = _v_.binaryValue(); break;
                case 2: _Data = _v_.binaryValue(); break;
                case 3: _Version = _v_.longValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setKey(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "Key")));
        setData(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "Data")));
        setVersion(_r_.getLong(_pn_ + "Version"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendBinary(_pn_ + "Key", getKey());
        _s_.appendBinary(_pn_ + "Data", getData());
        _s_.appendLong(_pn_ + "Version", getVersion());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Key", "binary", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Data", "binary", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Version", "long", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -2351623358007351459L;

    public static final int eSuccess = 0;
    public static final int eDefaultError = 1;
    public static final int eVersionMismatch = 2;
    public static final int eUpdateError = 3;

    private Zeze.Net.Binary _Key;
    private Zeze.Net.Binary _Data;
    private long _Version;

    public Zeze.Net.Binary getKey() {
        return _Key;
    }

    public void setKey(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Key = _v_;
    }

    public Zeze.Net.Binary getData() {
        return _Data;
    }

    public void setData(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Data = _v_;
    }

    public long getVersion() {
        return _Version;
    }

    public void setVersion(long _v_) {
        _Version = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Key = Zeze.Net.Binary.Empty;
        _Data = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public Data(Zeze.Net.Binary _Key_, Zeze.Net.Binary _Data_, long _Version_) {
        if (_Key_ == null)
            _Key_ = Zeze.Net.Binary.Empty;
        _Key = _Key_;
        if (_Data_ == null)
            _Data_ = Zeze.Net.Binary.Empty;
        _Data = _Data_;
        _Version = _Version_;
    }

    @Override
    public void reset() {
        _Key = Zeze.Net.Binary.Empty;
        _Data = Zeze.Net.Binary.Empty;
        _Version = 0;
    }

    @Override
    public Zeze.Builtin.Dbh2.Master.BSaveDataWithSameVersion toBean() {
        var _b_ = new Zeze.Builtin.Dbh2.Master.BSaveDataWithSameVersion();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BSaveDataWithSameVersion)_o_);
    }

    public void assign(BSaveDataWithSameVersion _o_) {
        _Key = _o_.getKey();
        _Data = _o_.getData();
        _Version = _o_.getVersion();
    }

    public void assign(BSaveDataWithSameVersion.Data _o_) {
        _Key = _o_._Key;
        _Data = _o_._Data;
        _Version = _o_._Version;
    }

    @Override
    public BSaveDataWithSameVersion.Data copy() {
        var _c_ = new BSaveDataWithSameVersion.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BSaveDataWithSameVersion.Data _a_, BSaveDataWithSameVersion.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BSaveDataWithSameVersion.Data clone() {
        return (BSaveDataWithSameVersion.Data)super.clone();
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
        _s_.append("Zeze.Builtin.Dbh2.Master.BSaveDataWithSameVersion: {\n");
        _s_.append(_i1_).append("Key=").append(_Key).append(",\n");
        _s_.append(_i1_).append("Data=").append(_Data).append(",\n");
        _s_.append(_i1_).append("Version=").append(_Version).append('\n');
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
            var _x_ = _Key;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            var _x_ = _Data;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            long _x_ = _Version;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _Key = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _Data = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _Version = _o_.ReadLong(_t_);
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
        if (!(_o_ instanceof BSaveDataWithSameVersion.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BSaveDataWithSameVersion.Data)_o_;
        if (!_Key.equals(_b_._Key))
            return false;
        if (!_Data.equals(_b_._Data))
            return false;
        if (_Version != _b_._Version)
            return false;
        return true;
    }
}
}
