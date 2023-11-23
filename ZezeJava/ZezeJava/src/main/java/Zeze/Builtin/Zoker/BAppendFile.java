// auto-generated @formatter:off
package Zeze.Builtin.Zoker;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BAppendFile extends Zeze.Transaction.Bean implements BAppendFileReadOnly {
    public static final long TYPEID = -5838123267142656915L;

    private String _ServiceName;
    private String _FileName;
    private long _Offset;
    private Zeze.Net.Binary _Chunk;

    @Override
    public String getServiceName() {
        if (!isManaged())
            return _ServiceName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ServiceName;
        var log = (Log__ServiceName)txn.getLog(objectId() + 1);
        return log != null ? log.value : _ServiceName;
    }

    public void setServiceName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ServiceName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ServiceName(this, 1, value));
    }

    @Override
    public String getFileName() {
        if (!isManaged())
            return _FileName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _FileName;
        var log = (Log__FileName)txn.getLog(objectId() + 2);
        return log != null ? log.value : _FileName;
    }

    public void setFileName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _FileName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__FileName(this, 2, value));
    }

    @Override
    public long getOffset() {
        if (!isManaged())
            return _Offset;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Offset;
        var log = (Log__Offset)txn.getLog(objectId() + 3);
        return log != null ? log.value : _Offset;
    }

    public void setOffset(long value) {
        if (!isManaged()) {
            _Offset = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Offset(this, 3, value));
    }

    @Override
    public Zeze.Net.Binary getChunk() {
        if (!isManaged())
            return _Chunk;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Chunk;
        var log = (Log__Chunk)txn.getLog(objectId() + 4);
        return log != null ? log.value : _Chunk;
    }

    public void setChunk(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Chunk = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Chunk(this, 4, value));
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
        var data = new Zeze.Builtin.Zoker.BAppendFile.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Zoker.BAppendFile.Data)other);
    }

    public void assign(BAppendFile.Data other) {
        setServiceName(other._ServiceName);
        setFileName(other._FileName);
        setOffset(other._Offset);
        setChunk(other._Chunk);
        _unknown_ = null;
    }

    public void assign(BAppendFile other) {
        setServiceName(other.getServiceName());
        setFileName(other.getFileName());
        setOffset(other.getOffset());
        setChunk(other.getChunk());
        _unknown_ = other._unknown_;
    }

    public BAppendFile copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BAppendFile copy() {
        var copy = new BAppendFile();
        copy.assign(this);
        return copy;
    }

    public static void swap(BAppendFile a, BAppendFile b) {
        BAppendFile save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ServiceName extends Zeze.Transaction.Logs.LogString {
        public Log__ServiceName(BAppendFile bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BAppendFile)getBelong())._ServiceName = value; }
    }

    private static final class Log__FileName extends Zeze.Transaction.Logs.LogString {
        public Log__FileName(BAppendFile bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BAppendFile)getBelong())._FileName = value; }
    }

    private static final class Log__Offset extends Zeze.Transaction.Logs.LogLong {
        public Log__Offset(BAppendFile bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BAppendFile)getBelong())._Offset = value; }
    }

    private static final class Log__Chunk extends Zeze.Transaction.Logs.LogBinary {
        public Log__Chunk(BAppendFile bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BAppendFile)getBelong())._Chunk = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Zoker.BAppendFile: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ServiceName=").append(getServiceName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("FileName=").append(getFileName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Offset=").append(getOffset()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Chunk=").append(getChunk()).append(System.lineSeparator());
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append('}');
    }

    private static int _PRE_ALLOC_SIZE_ = 16;

    @Override
    public int preAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void preAllocSize(int size) {
        _PRE_ALLOC_SIZE_ = size;
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
    public boolean negativeCheck() {
        if (getOffset() < 0)
            return true;
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void followerApply(Zeze.Transaction.Log log) {
        var vars = ((Zeze.Transaction.Collections.LogBean)log).getVariables();
        if (vars == null)
            return;
        for (var it = vars.iterator(); it.moveToNext(); ) {
            var vlog = it.value();
            switch (vlog.getVariableId()) {
                case 1: _ServiceName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 2: _FileName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 3: _Offset = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 4: _Chunk = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setServiceName(rs.getString(_parents_name_ + "ServiceName"));
        if (getServiceName() == null)
            setServiceName("");
        setFileName(rs.getString(_parents_name_ + "FileName"));
        if (getFileName() == null)
            setFileName("");
        setOffset(rs.getLong(_parents_name_ + "Offset"));
        setChunk(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "Chunk")));
        if (getChunk() == null)
            setChunk(Zeze.Net.Binary.Empty);
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "ServiceName", getServiceName());
        st.appendString(_parents_name_ + "FileName", getFileName());
        st.appendLong(_parents_name_ + "Offset", getOffset());
        st.appendBinary(_parents_name_ + "Chunk", getChunk());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "ServiceName", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "FileName", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Offset", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "Chunk", "binary", "", ""));
        return vars;
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

    public void setServiceName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _ServiceName = value;
    }

    public String getFileName() {
        return _FileName;
    }

    public void setFileName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _FileName = value;
    }

    public long getOffset() {
        return _Offset;
    }

    public void setOffset(long value) {
        _Offset = value;
    }

    public Zeze.Net.Binary getChunk() {
        return _Chunk;
    }

    public void setChunk(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Chunk = value;
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
        var bean = new Zeze.Builtin.Zoker.BAppendFile();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BAppendFile)other);
    }

    public void assign(BAppendFile other) {
        _ServiceName = other.getServiceName();
        _FileName = other.getFileName();
        _Offset = other.getOffset();
        _Chunk = other.getChunk();
    }

    public void assign(BAppendFile.Data other) {
        _ServiceName = other._ServiceName;
        _FileName = other._FileName;
        _Offset = other._Offset;
        _Chunk = other._Chunk;
    }

    @Override
    public BAppendFile.Data copy() {
        var copy = new BAppendFile.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BAppendFile.Data a, BAppendFile.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
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
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Zoker.BAppendFile: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ServiceName=").append(_ServiceName).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("FileName=").append(_FileName).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Offset=").append(_Offset).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Chunk=").append(_Chunk).append(System.lineSeparator());
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append('}');
    }

    @Override
    public int preAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void preAllocSize(int size) {
        _PRE_ALLOC_SIZE_ = size;
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
}
}
