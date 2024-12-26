// auto-generated @formatter:off
package Zeze.Builtin.Zoker;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BAppendFile extends Zeze.Transaction.Bean implements BAppendFileReadOnly {
    public static final long TYPEID = -5838123267142656915L;

    private String _ServiceName;
    private String _FileName;
    private long _Offset;
    private Zeze.Net.Binary _Chunk;

    private static final java.lang.invoke.VarHandle vh_ServiceName;
    private static final java.lang.invoke.VarHandle vh_FileName;
    private static final java.lang.invoke.VarHandle vh_Offset;
    private static final java.lang.invoke.VarHandle vh_Chunk;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_ServiceName = _l_.findVarHandle(BAppendFile.class, "_ServiceName", String.class);
            vh_FileName = _l_.findVarHandle(BAppendFile.class, "_FileName", String.class);
            vh_Offset = _l_.findVarHandle(BAppendFile.class, "_Offset", long.class);
            vh_Chunk = _l_.findVarHandle(BAppendFile.class, "_Chunk", Zeze.Net.Binary.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public String getServiceName() {
        if (!isManaged())
            return _ServiceName;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ServiceName;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _ServiceName;
    }

    public void setServiceName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ServiceName = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 1, vh_ServiceName, _v_));
    }

    @Override
    public String getFileName() {
        if (!isManaged())
            return _FileName;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _FileName;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _FileName;
    }

    public void setFileName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _FileName = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 2, vh_FileName, _v_));
    }

    @Override
    public long getOffset() {
        if (!isManaged())
            return _Offset;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Offset;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _Offset;
    }

    public void setOffset(long _v_) {
        if (!isManaged()) {
            _Offset = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 3, vh_Offset, _v_));
    }

    @Override
    public Zeze.Net.Binary getChunk() {
        if (!isManaged())
            return _Chunk;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Chunk;
        var log = (Zeze.Transaction.Logs.LogBinary)_t_.getLog(objectId() + 4);
        return log != null ? log.value : _Chunk;
    }

    public void setChunk(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Chunk = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBinary(this, 4, vh_Chunk, _v_));
    }

    @SuppressWarnings("deprecation")
    public BAppendFile() {
        _ServiceName = "";
        _FileName = "";
        _Chunk = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BAppendFile(String _ServiceName_, String _FileName_, long _Offset_, Zeze.Net.Binary _Chunk_) {
        if (_ServiceName_ == null)
            _ServiceName_ = "";
        _ServiceName = _ServiceName_;
        if (_FileName_ == null)
            _FileName_ = "";
        _FileName = _FileName_;
        _Offset = _Offset_;
        if (_Chunk_ == null)
            _Chunk_ = Zeze.Net.Binary.Empty;
        _Chunk = _Chunk_;
    }

    @Override
    public void reset() {
        setServiceName("");
        setFileName("");
        setOffset(0);
        setChunk(Zeze.Net.Binary.Empty);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Zoker.BAppendFile.Data toData() {
        var _d_ = new Zeze.Builtin.Zoker.BAppendFile.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Zoker.BAppendFile.Data)_o_);
    }

    public void assign(BAppendFile.Data _o_) {
        setServiceName(_o_._ServiceName);
        setFileName(_o_._FileName);
        setOffset(_o_._Offset);
        setChunk(_o_._Chunk);
        _unknown_ = null;
    }

    public void assign(BAppendFile _o_) {
        setServiceName(_o_.getServiceName());
        setFileName(_o_.getFileName());
        setOffset(_o_.getOffset());
        setChunk(_o_.getChunk());
        _unknown_ = _o_._unknown_;
    }

    public BAppendFile copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BAppendFile copy() {
        var _c_ = new BAppendFile();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BAppendFile _a_, BAppendFile _b_) {
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
        _s_.append("Zeze.Builtin.Zoker.BAppendFile: {\n");
        _s_.append(_i1_).append("ServiceName=").append(getServiceName()).append(",\n");
        _s_.append(_i1_).append("FileName=").append(getFileName()).append(",\n");
        _s_.append(_i1_).append("Offset=").append(getOffset()).append(",\n");
        _s_.append(_i1_).append("Chunk=").append(getChunk()).append('\n');
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
            String _x_ = getServiceName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getFileName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            long _x_ = getOffset();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = getChunk();
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
            setServiceName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setFileName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setOffset(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setChunk(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BAppendFile))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BAppendFile)_o_;
        if (!getServiceName().equals(_b_.getServiceName()))
            return false;
        if (!getFileName().equals(_b_.getFileName()))
            return false;
        if (getOffset() != _b_.getOffset())
            return false;
        if (!getChunk().equals(_b_.getChunk()))
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getOffset() < 0)
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
                case 1: _ServiceName = _v_.stringValue(); break;
                case 2: _FileName = _v_.stringValue(); break;
                case 3: _Offset = _v_.longValue(); break;
                case 4: _Chunk = _v_.binaryValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setServiceName(_r_.getString(_pn_ + "ServiceName"));
        if (getServiceName() == null)
            setServiceName("");
        setFileName(_r_.getString(_pn_ + "FileName"));
        if (getFileName() == null)
            setFileName("");
        setOffset(_r_.getLong(_pn_ + "Offset"));
        setChunk(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "Chunk")));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "ServiceName", getServiceName());
        _s_.appendString(_pn_ + "FileName", getFileName());
        _s_.appendLong(_pn_ + "Offset", getOffset());
        _s_.appendBinary(_pn_ + "Chunk", getChunk());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "ServiceName", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "FileName", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Offset", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "Chunk", "binary", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -5838123267142656915L;

    private String _ServiceName;
    private String _FileName;
    private long _Offset;
    private Zeze.Net.Binary _Chunk;

    public String getServiceName() {
        return _ServiceName;
    }

    public void setServiceName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _ServiceName = _v_;
    }

    public String getFileName() {
        return _FileName;
    }

    public void setFileName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _FileName = _v_;
    }

    public long getOffset() {
        return _Offset;
    }

    public void setOffset(long _v_) {
        _Offset = _v_;
    }

    public Zeze.Net.Binary getChunk() {
        return _Chunk;
    }

    public void setChunk(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Chunk = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _ServiceName = "";
        _FileName = "";
        _Chunk = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public Data(String _ServiceName_, String _FileName_, long _Offset_, Zeze.Net.Binary _Chunk_) {
        if (_ServiceName_ == null)
            _ServiceName_ = "";
        _ServiceName = _ServiceName_;
        if (_FileName_ == null)
            _FileName_ = "";
        _FileName = _FileName_;
        _Offset = _Offset_;
        if (_Chunk_ == null)
            _Chunk_ = Zeze.Net.Binary.Empty;
        _Chunk = _Chunk_;
    }

    @Override
    public void reset() {
        _ServiceName = "";
        _FileName = "";
        _Offset = 0;
        _Chunk = Zeze.Net.Binary.Empty;
    }

    @Override
    public Zeze.Builtin.Zoker.BAppendFile toBean() {
        var _b_ = new Zeze.Builtin.Zoker.BAppendFile();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BAppendFile)_o_);
    }

    public void assign(BAppendFile _o_) {
        _ServiceName = _o_.getServiceName();
        _FileName = _o_.getFileName();
        _Offset = _o_.getOffset();
        _Chunk = _o_.getChunk();
    }

    public void assign(BAppendFile.Data _o_) {
        _ServiceName = _o_._ServiceName;
        _FileName = _o_._FileName;
        _Offset = _o_._Offset;
        _Chunk = _o_._Chunk;
    }

    @Override
    public BAppendFile.Data copy() {
        var _c_ = new BAppendFile.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BAppendFile.Data _a_, BAppendFile.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BAppendFile.Data clone() {
        return (BAppendFile.Data)super.clone();
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
        _s_.append("Zeze.Builtin.Zoker.BAppendFile: {\n");
        _s_.append(_i1_).append("ServiceName=").append(_ServiceName).append(",\n");
        _s_.append(_i1_).append("FileName=").append(_FileName).append(",\n");
        _s_.append(_i1_).append("Offset=").append(_Offset).append(",\n");
        _s_.append(_i1_).append("Chunk=").append(_Chunk).append('\n');
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
            String _x_ = _ServiceName;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = _FileName;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            long _x_ = _Offset;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = _Chunk;
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
            _ServiceName = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _FileName = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _Offset = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _Chunk = _o_.ReadBinary(_t_);
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
        if (!(_o_ instanceof BAppendFile.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BAppendFile.Data)_o_;
        if (!_ServiceName.equals(_b_._ServiceName))
            return false;
        if (!_FileName.equals(_b_._FileName))
            return false;
        if (_Offset != _b_._Offset)
            return false;
        if (!_Chunk.equals(_b_._Chunk))
            return false;
        return true;
    }
}
}
