// auto-generated @formatter:off
package Zeze.Builtin.HotDistribute;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BCloseFile extends Zeze.Transaction.Bean implements BCloseFileReadOnly {
    public static final long TYPEID = -2195521187339200956L;

    private String _FileName;
    private Zeze.Net.Binary _Md5;

    @Override
    public String getFileName() {
        if (!isManaged())
            return _FileName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _FileName;
        var log = (Log__FileName)txn.getLog(objectId() + 1);
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
        txn.putLog(new Log__FileName(this, 1, value));
    }

    @Override
    public Zeze.Net.Binary getMd5() {
        if (!isManaged())
            return _Md5;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Md5;
        var log = (Log__Md5)txn.getLog(objectId() + 2);
        return log != null ? log.value : _Md5;
    }

    public void setMd5(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Md5 = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Md5(this, 2, value));
    }

    @SuppressWarnings("deprecation")
    public BCloseFile() {
        _FileName = "";
        _Md5 = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BCloseFile(String _FileName_, Zeze.Net.Binary _Md5_) {
        if (_FileName_ == null)
            _FileName_ = "";
        _FileName = _FileName_;
        if (_Md5_ == null)
            _Md5_ = Zeze.Net.Binary.Empty;
        _Md5 = _Md5_;
    }

    @Override
    public void reset() {
        setFileName("");
        setMd5(Zeze.Net.Binary.Empty);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.HotDistribute.BCloseFile.Data toData() {
        var data = new Zeze.Builtin.HotDistribute.BCloseFile.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.HotDistribute.BCloseFile.Data)other);
    }

    public void assign(BCloseFile.Data other) {
        setFileName(other._FileName);
        setMd5(other._Md5);
        _unknown_ = null;
    }

    public void assign(BCloseFile other) {
        setFileName(other.getFileName());
        setMd5(other.getMd5());
        _unknown_ = other._unknown_;
    }

    public BCloseFile copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BCloseFile copy() {
        var copy = new BCloseFile();
        copy.assign(this);
        return copy;
    }

    public static void swap(BCloseFile a, BCloseFile b) {
        BCloseFile save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__FileName extends Zeze.Transaction.Logs.LogString {
        public Log__FileName(BCloseFile bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BCloseFile)getBelong())._FileName = value; }
    }

    private static final class Log__Md5 extends Zeze.Transaction.Logs.LogBinary {
        public Log__Md5(BCloseFile bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BCloseFile)getBelong())._Md5 = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.HotDistribute.BCloseFile: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("FileName=").append(getFileName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Md5=").append(getMd5()).append(System.lineSeparator());
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
            String _x_ = getFileName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = getMd5();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
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
            setFileName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setMd5(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BCloseFile))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BCloseFile)_o_;
        if (!getFileName().equals(_b_.getFileName()))
            return false;
        if (!getMd5().equals(_b_.getMd5()))
            return false;
        return true;
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
                case 1: _FileName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 2: _Md5 = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setFileName(rs.getString(_parents_name_ + "FileName"));
        if (getFileName() == null)
            setFileName("");
        setMd5(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "Md5")));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "FileName", getFileName());
        st.appendBinary(_parents_name_ + "Md5", getMd5());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "FileName", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Md5", "binary", "", ""));
        return vars;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -2195521187339200956L;

    private String _FileName;
    private Zeze.Net.Binary _Md5;

    public String getFileName() {
        return _FileName;
    }

    public void setFileName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _FileName = value;
    }

    public Zeze.Net.Binary getMd5() {
        return _Md5;
    }

    public void setMd5(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Md5 = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _FileName = "";
        _Md5 = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public Data(String _FileName_, Zeze.Net.Binary _Md5_) {
        if (_FileName_ == null)
            _FileName_ = "";
        _FileName = _FileName_;
        if (_Md5_ == null)
            _Md5_ = Zeze.Net.Binary.Empty;
        _Md5 = _Md5_;
    }

    @Override
    public void reset() {
        _FileName = "";
        _Md5 = Zeze.Net.Binary.Empty;
    }

    @Override
    public Zeze.Builtin.HotDistribute.BCloseFile toBean() {
        var bean = new Zeze.Builtin.HotDistribute.BCloseFile();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BCloseFile)other);
    }

    public void assign(BCloseFile other) {
        _FileName = other.getFileName();
        _Md5 = other.getMd5();
    }

    public void assign(BCloseFile.Data other) {
        _FileName = other._FileName;
        _Md5 = other._Md5;
    }

    @Override
    public BCloseFile.Data copy() {
        var copy = new BCloseFile.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BCloseFile.Data a, BCloseFile.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BCloseFile.Data clone() {
        return (BCloseFile.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.HotDistribute.BCloseFile: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("FileName=").append(_FileName).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Md5=").append(_Md5).append(System.lineSeparator());
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
            String _x_ = _FileName;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = _Md5;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
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
            _FileName = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _Md5 = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
