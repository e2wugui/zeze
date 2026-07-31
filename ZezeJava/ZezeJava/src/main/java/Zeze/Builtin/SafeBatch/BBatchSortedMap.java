// auto-generated @formatter:off
package Zeze.Builtin.SafeBatch;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BBatchSortedMap extends Zeze.Transaction.Bean implements BBatchSortedMapReadOnly {
    public static final long TYPEID = -5556181914916037420L;

    private String _TableName;
    private Zeze.Net.Binary _RecordKey;
    private Zeze.Net.Binary _LastMapKey;
    private int _ProposeLimit;
    private String _JobClass;
    private Zeze.Net.Binary _OneByOneKey;

    private static final java.lang.invoke.VarHandle vh_TableName;
    private static final java.lang.invoke.VarHandle vh_RecordKey;
    private static final java.lang.invoke.VarHandle vh_LastMapKey;
    private static final java.lang.invoke.VarHandle vh_ProposeLimit;
    private static final java.lang.invoke.VarHandle vh_JobClass;
    private static final java.lang.invoke.VarHandle vh_OneByOneKey;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_TableName = _l_.findVarHandle(BBatchSortedMap.class, "_TableName", String.class);
            vh_RecordKey = _l_.findVarHandle(BBatchSortedMap.class, "_RecordKey", Zeze.Net.Binary.class);
            vh_LastMapKey = _l_.findVarHandle(BBatchSortedMap.class, "_LastMapKey", Zeze.Net.Binary.class);
            vh_ProposeLimit = _l_.findVarHandle(BBatchSortedMap.class, "_ProposeLimit", int.class);
            vh_JobClass = _l_.findVarHandle(BBatchSortedMap.class, "_JobClass", String.class);
            vh_OneByOneKey = _l_.findVarHandle(BBatchSortedMap.class, "_OneByOneKey", Zeze.Net.Binary.class);
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
    public Zeze.Net.Binary getRecordKey() {
        if (!isManaged())
            return _RecordKey;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _RecordKey;
        var log = (Zeze.Transaction.Logs.LogBinary)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _RecordKey;
    }

    public void setRecordKey(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _RecordKey = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBinary(this, 2, vh_RecordKey, _v_));
    }

    @Override
    public Zeze.Net.Binary getLastMapKey() {
        if (!isManaged())
            return _LastMapKey;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _LastMapKey;
        var log = (Zeze.Transaction.Logs.LogBinary)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _LastMapKey;
    }

    public void setLastMapKey(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _LastMapKey = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBinary(this, 3, vh_LastMapKey, _v_));
    }

    @Override
    public int getProposeLimit() {
        if (!isManaged())
            return _ProposeLimit;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ProposeLimit;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 4);
        return log != null ? log.value : _ProposeLimit;
    }

    public void setProposeLimit(int _v_) {
        if (!isManaged()) {
            _ProposeLimit = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 4, vh_ProposeLimit, _v_));
    }

    @Override
    public String getJobClass() {
        if (!isManaged())
            return _JobClass;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _JobClass;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 5);
        return log != null ? log.stringValue() : _JobClass;
    }

    public void setJobClass(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _JobClass = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 5, vh_JobClass, _v_));
    }

    @Override
    public Zeze.Net.Binary getOneByOneKey() {
        if (!isManaged())
            return _OneByOneKey;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _OneByOneKey;
        var log = (Zeze.Transaction.Logs.LogBinary)_t_.getLog(objectId() + 6);
        return log != null ? log.value : _OneByOneKey;
    }

    public void setOneByOneKey(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _OneByOneKey = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBinary(this, 6, vh_OneByOneKey, _v_));
    }

    @SuppressWarnings("deprecation")
    public BBatchSortedMap() {
        _TableName = "";
        _RecordKey = Zeze.Net.Binary.Empty;
        _LastMapKey = Zeze.Net.Binary.Empty;
        _JobClass = "";
        _OneByOneKey = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BBatchSortedMap(String _TableName_, Zeze.Net.Binary _RecordKey_, Zeze.Net.Binary _LastMapKey_, int _ProposeLimit_, String _JobClass_, Zeze.Net.Binary _OneByOneKey_) {
        if (_TableName_ == null)
            _TableName_ = "";
        _TableName = _TableName_;
        if (_RecordKey_ == null)
            _RecordKey_ = Zeze.Net.Binary.Empty;
        _RecordKey = _RecordKey_;
        if (_LastMapKey_ == null)
            _LastMapKey_ = Zeze.Net.Binary.Empty;
        _LastMapKey = _LastMapKey_;
        _ProposeLimit = _ProposeLimit_;
        if (_JobClass_ == null)
            _JobClass_ = "";
        _JobClass = _JobClass_;
        if (_OneByOneKey_ == null)
            _OneByOneKey_ = Zeze.Net.Binary.Empty;
        _OneByOneKey = _OneByOneKey_;
    }

    @Override
    public void reset() {
        setTableName("");
        setRecordKey(Zeze.Net.Binary.Empty);
        setLastMapKey(Zeze.Net.Binary.Empty);
        setProposeLimit(0);
        setJobClass("");
        setOneByOneKey(Zeze.Net.Binary.Empty);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.SafeBatch.BBatchSortedMap.Data toData() {
        var _d_ = new Zeze.Builtin.SafeBatch.BBatchSortedMap.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.SafeBatch.BBatchSortedMap.Data)_o_);
    }

    public void assign(BBatchSortedMap.Data _o_) {
        setTableName(_o_._TableName);
        setRecordKey(_o_._RecordKey);
        setLastMapKey(_o_._LastMapKey);
        setProposeLimit(_o_._ProposeLimit);
        setJobClass(_o_._JobClass);
        setOneByOneKey(_o_._OneByOneKey);
        _unknown_ = null;
    }

    public void assign(BBatchSortedMap _o_) {
        setTableName(_o_.getTableName());
        setRecordKey(_o_.getRecordKey());
        setLastMapKey(_o_.getLastMapKey());
        setProposeLimit(_o_.getProposeLimit());
        setJobClass(_o_.getJobClass());
        setOneByOneKey(_o_.getOneByOneKey());
        _unknown_ = _o_._unknown_;
    }

    public BBatchSortedMap copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BBatchSortedMap copy() {
        var _c_ = new BBatchSortedMap();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BBatchSortedMap _a_, BBatchSortedMap _b_) {
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
        _s_.append("Zeze.Builtin.SafeBatch.BBatchSortedMap: {\n");
        _s_.append(_i1_).append("TableName=").append(getTableName()).append(",\n");
        _s_.append(_i1_).append("RecordKey=").append(getRecordKey()).append(",\n");
        _s_.append(_i1_).append("LastMapKey=").append(getLastMapKey()).append(",\n");
        _s_.append(_i1_).append("ProposeLimit=").append(getProposeLimit()).append(",\n");
        _s_.append(_i1_).append("JobClass=").append(getJobClass()).append(",\n");
        _s_.append(_i1_).append("OneByOneKey=").append(getOneByOneKey()).append('\n');
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
            var _x_ = getRecordKey();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            var _x_ = getLastMapKey();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            int _x_ = getProposeLimit();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            String _x_ = getJobClass();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = getOneByOneKey();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.BYTES);
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
            setTableName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setRecordKey(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setLastMapKey(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setProposeLimit(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setJobClass(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            setOneByOneKey(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BBatchSortedMap))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BBatchSortedMap)_o_;
        if (!getTableName().equals(_b_.getTableName()))
            return false;
        if (!getRecordKey().equals(_b_.getRecordKey()))
            return false;
        if (!getLastMapKey().equals(_b_.getLastMapKey()))
            return false;
        if (getProposeLimit() != _b_.getProposeLimit())
            return false;
        if (!getJobClass().equals(_b_.getJobClass()))
            return false;
        if (!getOneByOneKey().equals(_b_.getOneByOneKey()))
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
                case 1: _TableName = _v_.stringValue(); break;
                case 2: _RecordKey = _v_.binaryValue(); break;
                case 3: _LastMapKey = _v_.binaryValue(); break;
                case 4: _ProposeLimit = _v_.intValue(); break;
                case 5: _JobClass = _v_.stringValue(); break;
                case 6: _OneByOneKey = _v_.binaryValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setTableName(_r_.getString(_pn_ + "TableName"));
        if (getTableName() == null)
            setTableName("");
        setRecordKey(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "RecordKey")));
        setLastMapKey(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "LastMapKey")));
        setProposeLimit(_r_.getInt(_pn_ + "ProposeLimit"));
        setJobClass(_r_.getString(_pn_ + "JobClass"));
        if (getJobClass() == null)
            setJobClass("");
        setOneByOneKey(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "OneByOneKey")));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "TableName", getTableName());
        _s_.appendBinary(_pn_ + "RecordKey", getRecordKey());
        _s_.appendBinary(_pn_ + "LastMapKey", getLastMapKey());
        _s_.appendInt(_pn_ + "ProposeLimit", getProposeLimit());
        _s_.appendString(_pn_ + "JobClass", getJobClass());
        _s_.appendBinary(_pn_ + "OneByOneKey", getOneByOneKey());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "TableName", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "RecordKey", "binary", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "LastMapKey", "binary", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "ProposeLimit", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "JobClass", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(6, "OneByOneKey", "binary", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -5556181914916037420L;

    private String _TableName;
    private Zeze.Net.Binary _RecordKey;
    private Zeze.Net.Binary _LastMapKey;
    private int _ProposeLimit;
    private String _JobClass;
    private Zeze.Net.Binary _OneByOneKey;

    public String getTableName() {
        return _TableName;
    }

    public void setTableName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _TableName = _v_;
    }

    public Zeze.Net.Binary getRecordKey() {
        return _RecordKey;
    }

    public void setRecordKey(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _RecordKey = _v_;
    }

    public Zeze.Net.Binary getLastMapKey() {
        return _LastMapKey;
    }

    public void setLastMapKey(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _LastMapKey = _v_;
    }

    public int getProposeLimit() {
        return _ProposeLimit;
    }

    public void setProposeLimit(int _v_) {
        _ProposeLimit = _v_;
    }

    public String getJobClass() {
        return _JobClass;
    }

    public void setJobClass(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _JobClass = _v_;
    }

    public Zeze.Net.Binary getOneByOneKey() {
        return _OneByOneKey;
    }

    public void setOneByOneKey(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _OneByOneKey = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _TableName = "";
        _RecordKey = Zeze.Net.Binary.Empty;
        _LastMapKey = Zeze.Net.Binary.Empty;
        _JobClass = "";
        _OneByOneKey = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public Data(String _TableName_, Zeze.Net.Binary _RecordKey_, Zeze.Net.Binary _LastMapKey_, int _ProposeLimit_, String _JobClass_, Zeze.Net.Binary _OneByOneKey_) {
        if (_TableName_ == null)
            _TableName_ = "";
        _TableName = _TableName_;
        if (_RecordKey_ == null)
            _RecordKey_ = Zeze.Net.Binary.Empty;
        _RecordKey = _RecordKey_;
        if (_LastMapKey_ == null)
            _LastMapKey_ = Zeze.Net.Binary.Empty;
        _LastMapKey = _LastMapKey_;
        _ProposeLimit = _ProposeLimit_;
        if (_JobClass_ == null)
            _JobClass_ = "";
        _JobClass = _JobClass_;
        if (_OneByOneKey_ == null)
            _OneByOneKey_ = Zeze.Net.Binary.Empty;
        _OneByOneKey = _OneByOneKey_;
    }

    @Override
    public void reset() {
        _TableName = "";
        _RecordKey = Zeze.Net.Binary.Empty;
        _LastMapKey = Zeze.Net.Binary.Empty;
        _ProposeLimit = 0;
        _JobClass = "";
        _OneByOneKey = Zeze.Net.Binary.Empty;
    }

    @Override
    public Zeze.Builtin.SafeBatch.BBatchSortedMap toBean() {
        var _b_ = new Zeze.Builtin.SafeBatch.BBatchSortedMap();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BBatchSortedMap)_o_);
    }

    public void assign(BBatchSortedMap _o_) {
        _TableName = _o_.getTableName();
        _RecordKey = _o_.getRecordKey();
        _LastMapKey = _o_.getLastMapKey();
        _ProposeLimit = _o_.getProposeLimit();
        _JobClass = _o_.getJobClass();
        _OneByOneKey = _o_.getOneByOneKey();
    }

    public void assign(BBatchSortedMap.Data _o_) {
        _TableName = _o_._TableName;
        _RecordKey = _o_._RecordKey;
        _LastMapKey = _o_._LastMapKey;
        _ProposeLimit = _o_._ProposeLimit;
        _JobClass = _o_._JobClass;
        _OneByOneKey = _o_._OneByOneKey;
    }

    @Override
    public BBatchSortedMap.Data copy() {
        var _c_ = new BBatchSortedMap.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BBatchSortedMap.Data _a_, BBatchSortedMap.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BBatchSortedMap.Data clone() {
        return (BBatchSortedMap.Data)super.clone();
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
        _s_.append("Zeze.Builtin.SafeBatch.BBatchSortedMap: {\n");
        _s_.append(_i1_).append("TableName=").append(_TableName).append(",\n");
        _s_.append(_i1_).append("RecordKey=").append(_RecordKey).append(",\n");
        _s_.append(_i1_).append("LastMapKey=").append(_LastMapKey).append(",\n");
        _s_.append(_i1_).append("ProposeLimit=").append(_ProposeLimit).append(",\n");
        _s_.append(_i1_).append("JobClass=").append(_JobClass).append(",\n");
        _s_.append(_i1_).append("OneByOneKey=").append(_OneByOneKey).append('\n');
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
            String _x_ = _TableName;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = _RecordKey;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            var _x_ = _LastMapKey;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            int _x_ = _ProposeLimit;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            String _x_ = _JobClass;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = _OneByOneKey;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.BYTES);
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
            _TableName = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _RecordKey = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _LastMapKey = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _ProposeLimit = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            _JobClass = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            _OneByOneKey = _o_.ReadBinary(_t_);
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
        if (!(_o_ instanceof BBatchSortedMap.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BBatchSortedMap.Data)_o_;
        if (!_TableName.equals(_b_._TableName))
            return false;
        if (!_RecordKey.equals(_b_._RecordKey))
            return false;
        if (!_LastMapKey.equals(_b_._LastMapKey))
            return false;
        if (_ProposeLimit != _b_._ProposeLimit)
            return false;
        if (!_JobClass.equals(_b_._JobClass))
            return false;
        if (!_OneByOneKey.equals(_b_._OneByOneKey))
            return false;
        return true;
    }
}
}
