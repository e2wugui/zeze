// auto-generated @formatter:off
package Zeze.Builtin.DelayRemove;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BTableKey extends Zeze.Transaction.Bean implements BTableKeyReadOnly {
    public static final long TYPEID = 6060766480176216446L;

    private String _TableName;
    private Zeze.Net.Binary _EncodedKey;
    private long _EnqueueTime;

    private static final java.lang.invoke.VarHandle vh_TableName;
    private static final java.lang.invoke.VarHandle vh_EncodedKey;
    private static final java.lang.invoke.VarHandle vh_EnqueueTime;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_TableName = _l_.findVarHandle(BTableKey.class, "_TableName", String.class);
            vh_EncodedKey = _l_.findVarHandle(BTableKey.class, "_EncodedKey", Zeze.Net.Binary.class);
            vh_EnqueueTime = _l_.findVarHandle(BTableKey.class, "_EnqueueTime", long.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public String getTableName() {
        if (!isManaged())
            return _TableName;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _TableName;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 1);
        return log != null ? log.stringValue() : _TableName;
    }

    public void setTableName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _TableName = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 1, vh_TableName, _v_));
    }

    @Override
    public Zeze.Net.Binary getEncodedKey() {
        if (!isManaged())
            return _EncodedKey;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _EncodedKey;
        var log = (Zeze.Transaction.Logs.LogBinary)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _EncodedKey;
    }

    public void setEncodedKey(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _EncodedKey = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBinary(this, 2, vh_EncodedKey, _v_));
    }

    @Override
    public long getEnqueueTime() {
        if (!isManaged())
            return _EnqueueTime;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _EnqueueTime;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _EnqueueTime;
    }

    public void setEnqueueTime(long _v_) {
        if (!isManaged()) {
            _EnqueueTime = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 3, vh_EnqueueTime, _v_));
    }

    @SuppressWarnings("deprecation")
    public BTableKey() {
        _TableName = "";
        _EncodedKey = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BTableKey(String _TableName_, Zeze.Net.Binary _EncodedKey_, long _EnqueueTime_) {
        if (_TableName_ == null)
            _TableName_ = "";
        _TableName = _TableName_;
        if (_EncodedKey_ == null)
            _EncodedKey_ = Zeze.Net.Binary.Empty;
        _EncodedKey = _EncodedKey_;
        _EnqueueTime = _EnqueueTime_;
    }

    @Override
    public void reset() {
        setTableName("");
        setEncodedKey(Zeze.Net.Binary.Empty);
        setEnqueueTime(0);
        _unknown_ = null;
    }

    public void assign(BTableKey _o_) {
        setTableName(_o_.getTableName());
        setEncodedKey(_o_.getEncodedKey());
        setEnqueueTime(_o_.getEnqueueTime());
        _unknown_ = _o_._unknown_;
    }

    public BTableKey copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTableKey copy() {
        var _c_ = new BTableKey();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BTableKey _a_, BTableKey _b_) {
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
        _s_.append("Zeze.Builtin.DelayRemove.BTableKey: {\n");
        _s_.append(_i1_).append("TableName=").append(getTableName()).append(",\n");
        _s_.append(_i1_).append("EncodedKey=").append(getEncodedKey()).append(",\n");
        _s_.append(_i1_).append("EnqueueTime=").append(getEnqueueTime()).append('\n');
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
            String _x_ = getTableName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = getEncodedKey();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            long _x_ = getEnqueueTime();
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
            setTableName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setEncodedKey(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setEnqueueTime(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BTableKey))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BTableKey)_o_;
        if (!getTableName().equals(_b_.getTableName()))
            return false;
        if (!getEncodedKey().equals(_b_.getEncodedKey()))
            return false;
        if (getEnqueueTime() != _b_.getEnqueueTime())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getEnqueueTime() < 0)
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
                case 1: _TableName = _v_.stringValue(); break;
                case 2: _EncodedKey = _v_.binaryValue(); break;
                case 3: _EnqueueTime = _v_.longValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setTableName(_r_.getString(_pn_ + "TableName"));
        if (getTableName() == null)
            setTableName("");
        setEncodedKey(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "EncodedKey")));
        setEnqueueTime(_r_.getLong(_pn_ + "EnqueueTime"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "TableName", getTableName());
        _s_.appendBinary(_pn_ + "EncodedKey", getEncodedKey());
        _s_.appendLong(_pn_ + "EnqueueTime", getEnqueueTime());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "TableName", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "EncodedKey", "binary", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "EnqueueTime", "long", "", ""));
        return _v_;
    }
}
