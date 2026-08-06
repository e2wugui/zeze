// auto-generated @formatter:off
package Zeze.Builtin.SafeBatch;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BBatch extends Zeze.Transaction.Bean implements BBatchReadOnly {
    public static final long TYPEID = -7789763943994450899L;

    private String _AppInstanceId;
    private String _TableName;
    private Zeze.Net.Binary _RecordKey;
    private Zeze.Net.Binary _LastKey;
    private int _ProposeLimit;
    private String _JobClass;
    private int _Worker;

    private static final java.lang.invoke.VarHandle vh_AppInstanceId;
    private static final java.lang.invoke.VarHandle vh_TableName;
    private static final java.lang.invoke.VarHandle vh_RecordKey;
    private static final java.lang.invoke.VarHandle vh_LastKey;
    private static final java.lang.invoke.VarHandle vh_ProposeLimit;
    private static final java.lang.invoke.VarHandle vh_JobClass;
    private static final java.lang.invoke.VarHandle vh_Worker;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_AppInstanceId = _l_.findVarHandle(BBatch.class, "_AppInstanceId", String.class);
            vh_TableName = _l_.findVarHandle(BBatch.class, "_TableName", String.class);
            vh_RecordKey = _l_.findVarHandle(BBatch.class, "_RecordKey", Zeze.Net.Binary.class);
            vh_LastKey = _l_.findVarHandle(BBatch.class, "_LastKey", Zeze.Net.Binary.class);
            vh_ProposeLimit = _l_.findVarHandle(BBatch.class, "_ProposeLimit", int.class);
            vh_JobClass = _l_.findVarHandle(BBatch.class, "_JobClass", String.class);
            vh_Worker = _l_.findVarHandle(BBatch.class, "_Worker", int.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public String getAppInstanceId() {
        if (!isManaged())
            return _AppInstanceId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _AppInstanceId;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 1);
        return log != null ? log.stringValue() : _AppInstanceId;
    }

    public void setAppInstanceId(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _AppInstanceId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 1, vh_AppInstanceId, _v_));
    }

    @Override
    public String getTableName() {
        if (!isManaged())
            return _TableName;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _TableName;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 2);
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
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 2, vh_TableName, _v_));
    }

    @Override
    public Zeze.Net.Binary getRecordKey() {
        if (!isManaged())
            return _RecordKey;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _RecordKey;
        var log = (Zeze.Transaction.Logs.LogBinary)_t_.getLog(objectId() + 3);
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
        _t_.putLog(new Zeze.Transaction.Logs.LogBinary(this, 3, vh_RecordKey, _v_));
    }

    @Override
    public Zeze.Net.Binary getLastKey() {
        if (!isManaged())
            return _LastKey;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _LastKey;
        var log = (Zeze.Transaction.Logs.LogBinary)_t_.getLog(objectId() + 4);
        return log != null ? log.value : _LastKey;
    }

    public void setLastKey(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _LastKey = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBinary(this, 4, vh_LastKey, _v_));
    }

    @Override
    public int getProposeLimit() {
        if (!isManaged())
            return _ProposeLimit;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ProposeLimit;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 5);
        return log != null ? log.value : _ProposeLimit;
    }

    public void setProposeLimit(int _v_) {
        if (!isManaged()) {
            _ProposeLimit = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 5, vh_ProposeLimit, _v_));
    }

    @Override
    public String getJobClass() {
        if (!isManaged())
            return _JobClass;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _JobClass;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 6);
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
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 6, vh_JobClass, _v_));
    }

    @Override
    public int getWorker() {
        if (!isManaged())
            return _Worker;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Worker;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 7);
        return log != null ? log.value : _Worker;
    }

    public void setWorker(int _v_) {
        if (!isManaged()) {
            _Worker = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 7, vh_Worker, _v_));
    }

    @SuppressWarnings("deprecation")
    public BBatch() {
        _AppInstanceId = "";
        _TableName = "";
        _RecordKey = Zeze.Net.Binary.Empty;
        _LastKey = Zeze.Net.Binary.Empty;
        _JobClass = "";
    }

    @SuppressWarnings("deprecation")
    public BBatch(String _AppInstanceId_, String _TableName_, Zeze.Net.Binary _RecordKey_, Zeze.Net.Binary _LastKey_, int _ProposeLimit_, String _JobClass_, int _Worker_) {
        if (_AppInstanceId_ == null)
            _AppInstanceId_ = "";
        _AppInstanceId = _AppInstanceId_;
        if (_TableName_ == null)
            _TableName_ = "";
        _TableName = _TableName_;
        if (_RecordKey_ == null)
            _RecordKey_ = Zeze.Net.Binary.Empty;
        _RecordKey = _RecordKey_;
        if (_LastKey_ == null)
            _LastKey_ = Zeze.Net.Binary.Empty;
        _LastKey = _LastKey_;
        _ProposeLimit = _ProposeLimit_;
        if (_JobClass_ == null)
            _JobClass_ = "";
        _JobClass = _JobClass_;
        _Worker = _Worker_;
    }

    @Override
    public void reset() {
        setAppInstanceId("");
        setTableName("");
        setRecordKey(Zeze.Net.Binary.Empty);
        setLastKey(Zeze.Net.Binary.Empty);
        setProposeLimit(0);
        setJobClass("");
        setWorker(0);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.SafeBatch.BBatch.Data toData() {
        var _d_ = new Zeze.Builtin.SafeBatch.BBatch.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.SafeBatch.BBatch.Data)_o_);
    }

    public void assign(BBatch.Data _o_) {
        setAppInstanceId(_o_._AppInstanceId);
        setTableName(_o_._TableName);
        setRecordKey(_o_._RecordKey);
        setLastKey(_o_._LastKey);
        setProposeLimit(_o_._ProposeLimit);
        setJobClass(_o_._JobClass);
        setWorker(_o_._Worker);
        _unknown_ = null;
    }

    public void assign(BBatch _o_) {
        setAppInstanceId(_o_.getAppInstanceId());
        setTableName(_o_.getTableName());
        setRecordKey(_o_.getRecordKey());
        setLastKey(_o_.getLastKey());
        setProposeLimit(_o_.getProposeLimit());
        setJobClass(_o_.getJobClass());
        setWorker(_o_.getWorker());
        _unknown_ = _o_._unknown_;
    }

    public BBatch copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BBatch copy() {
        var _c_ = new BBatch();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BBatch _a_, BBatch _b_) {
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
        _s_.append("Zeze.Builtin.SafeBatch.BBatch: {\n");
        _s_.append(_i1_).append("AppInstanceId=").append(getAppInstanceId()).append(",\n");
        _s_.append(_i1_).append("TableName=").append(getTableName()).append(",\n");
        _s_.append(_i1_).append("RecordKey=").append(getRecordKey()).append(",\n");
        _s_.append(_i1_).append("LastKey=").append(getLastKey()).append(",\n");
        _s_.append(_i1_).append("ProposeLimit=").append(getProposeLimit()).append(",\n");
        _s_.append(_i1_).append("JobClass=").append(getJobClass()).append(",\n");
        _s_.append(_i1_).append("Worker=").append(getWorker()).append('\n');
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
            String _x_ = getAppInstanceId();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getTableName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = getRecordKey();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            var _x_ = getLastKey();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            int _x_ = getProposeLimit();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            String _x_ = getJobClass();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            int _x_ = getWorker();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 7, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
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
            setAppInstanceId(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setTableName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setRecordKey(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setLastKey(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setProposeLimit(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            setJobClass(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 7) {
            setWorker(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BBatch))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BBatch)_o_;
        if (!getAppInstanceId().equals(_b_.getAppInstanceId()))
            return false;
        if (!getTableName().equals(_b_.getTableName()))
            return false;
        if (!getRecordKey().equals(_b_.getRecordKey()))
            return false;
        if (!getLastKey().equals(_b_.getLastKey()))
            return false;
        if (getProposeLimit() != _b_.getProposeLimit())
            return false;
        if (!getJobClass().equals(_b_.getJobClass()))
            return false;
        if (getWorker() != _b_.getWorker())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getProposeLimit() < 0)
            return true;
        if (getWorker() < 0)
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
                case 1: _AppInstanceId = _v_.stringValue(); break;
                case 2: _TableName = _v_.stringValue(); break;
                case 3: _RecordKey = _v_.binaryValue(); break;
                case 4: _LastKey = _v_.binaryValue(); break;
                case 5: _ProposeLimit = _v_.intValue(); break;
                case 6: _JobClass = _v_.stringValue(); break;
                case 7: _Worker = _v_.intValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setAppInstanceId(_r_.getString(_pn_ + "AppInstanceId"));
        if (getAppInstanceId() == null)
            setAppInstanceId("");
        setTableName(_r_.getString(_pn_ + "TableName"));
        if (getTableName() == null)
            setTableName("");
        setRecordKey(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "RecordKey")));
        setLastKey(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "LastKey")));
        setProposeLimit(_r_.getInt(_pn_ + "ProposeLimit"));
        setJobClass(_r_.getString(_pn_ + "JobClass"));
        if (getJobClass() == null)
            setJobClass("");
        setWorker(_r_.getInt(_pn_ + "Worker"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "AppInstanceId", getAppInstanceId());
        _s_.appendString(_pn_ + "TableName", getTableName());
        _s_.appendBinary(_pn_ + "RecordKey", getRecordKey());
        _s_.appendBinary(_pn_ + "LastKey", getLastKey());
        _s_.appendInt(_pn_ + "ProposeLimit", getProposeLimit());
        _s_.appendString(_pn_ + "JobClass", getJobClass());
        _s_.appendInt(_pn_ + "Worker", getWorker());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "AppInstanceId", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "TableName", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "RecordKey", "binary", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "LastKey", "binary", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "ProposeLimit", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(6, "JobClass", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(7, "Worker", "int", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -7789763943994450899L;

    private String _AppInstanceId;
    private String _TableName;
    private Zeze.Net.Binary _RecordKey;
    private Zeze.Net.Binary _LastKey;
    private int _ProposeLimit;
    private String _JobClass;
    private int _Worker;

    public String getAppInstanceId() {
        return _AppInstanceId;
    }

    public void setAppInstanceId(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _AppInstanceId = _v_;
    }

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

    public Zeze.Net.Binary getLastKey() {
        return _LastKey;
    }

    public void setLastKey(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _LastKey = _v_;
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

    public int getWorker() {
        return _Worker;
    }

    public void setWorker(int _v_) {
        _Worker = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _AppInstanceId = "";
        _TableName = "";
        _RecordKey = Zeze.Net.Binary.Empty;
        _LastKey = Zeze.Net.Binary.Empty;
        _JobClass = "";
    }

    @SuppressWarnings("deprecation")
    public Data(String _AppInstanceId_, String _TableName_, Zeze.Net.Binary _RecordKey_, Zeze.Net.Binary _LastKey_, int _ProposeLimit_, String _JobClass_, int _Worker_) {
        if (_AppInstanceId_ == null)
            _AppInstanceId_ = "";
        _AppInstanceId = _AppInstanceId_;
        if (_TableName_ == null)
            _TableName_ = "";
        _TableName = _TableName_;
        if (_RecordKey_ == null)
            _RecordKey_ = Zeze.Net.Binary.Empty;
        _RecordKey = _RecordKey_;
        if (_LastKey_ == null)
            _LastKey_ = Zeze.Net.Binary.Empty;
        _LastKey = _LastKey_;
        _ProposeLimit = _ProposeLimit_;
        if (_JobClass_ == null)
            _JobClass_ = "";
        _JobClass = _JobClass_;
        _Worker = _Worker_;
    }

    @Override
    public void reset() {
        _AppInstanceId = "";
        _TableName = "";
        _RecordKey = Zeze.Net.Binary.Empty;
        _LastKey = Zeze.Net.Binary.Empty;
        _ProposeLimit = 0;
        _JobClass = "";
        _Worker = 0;
    }

    @Override
    public Zeze.Builtin.SafeBatch.BBatch toBean() {
        var _b_ = new Zeze.Builtin.SafeBatch.BBatch();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BBatch)_o_);
    }

    public void assign(BBatch _o_) {
        _AppInstanceId = _o_.getAppInstanceId();
        _TableName = _o_.getTableName();
        _RecordKey = _o_.getRecordKey();
        _LastKey = _o_.getLastKey();
        _ProposeLimit = _o_.getProposeLimit();
        _JobClass = _o_.getJobClass();
        _Worker = _o_.getWorker();
    }

    public void assign(BBatch.Data _o_) {
        _AppInstanceId = _o_._AppInstanceId;
        _TableName = _o_._TableName;
        _RecordKey = _o_._RecordKey;
        _LastKey = _o_._LastKey;
        _ProposeLimit = _o_._ProposeLimit;
        _JobClass = _o_._JobClass;
        _Worker = _o_._Worker;
    }

    @Override
    public BBatch.Data copy() {
        var _c_ = new BBatch.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BBatch.Data _a_, BBatch.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BBatch.Data clone() {
        return (BBatch.Data)super.clone();
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
        _s_.append("Zeze.Builtin.SafeBatch.BBatch: {\n");
        _s_.append(_i1_).append("AppInstanceId=").append(_AppInstanceId).append(",\n");
        _s_.append(_i1_).append("TableName=").append(_TableName).append(",\n");
        _s_.append(_i1_).append("RecordKey=").append(_RecordKey).append(",\n");
        _s_.append(_i1_).append("LastKey=").append(_LastKey).append(",\n");
        _s_.append(_i1_).append("ProposeLimit=").append(_ProposeLimit).append(",\n");
        _s_.append(_i1_).append("JobClass=").append(_JobClass).append(",\n");
        _s_.append(_i1_).append("Worker=").append(_Worker).append('\n');
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
            String _x_ = _AppInstanceId;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = _TableName;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = _RecordKey;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            var _x_ = _LastKey;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            int _x_ = _ProposeLimit;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            String _x_ = _JobClass;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            int _x_ = _Worker;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 7, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _AppInstanceId = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _TableName = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _RecordKey = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _LastKey = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            _ProposeLimit = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            _JobClass = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 7) {
            _Worker = _o_.ReadInt(_t_);
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
        if (!(_o_ instanceof BBatch.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BBatch.Data)_o_;
        if (!_AppInstanceId.equals(_b_._AppInstanceId))
            return false;
        if (!_TableName.equals(_b_._TableName))
            return false;
        if (!_RecordKey.equals(_b_._RecordKey))
            return false;
        if (!_LastKey.equals(_b_._LastKey))
            return false;
        if (_ProposeLimit != _b_._ProposeLimit)
            return false;
        if (!_JobClass.equals(_b_._JobClass))
            return false;
        if (_Worker != _b_._Worker)
            return false;
        return true;
    }
}
}
