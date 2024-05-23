// auto-generated @formatter:off
package Zeze.Builtin.HotDistribute;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BOpenFile extends Zeze.Transaction.Bean implements BOpenFileReadOnly {
    public static final long TYPEID = 6565966142026805362L;

    private String _FileName;

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

    @SuppressWarnings("deprecation")
    public BOpenFile() {
        _FileName = "";
    }

    @SuppressWarnings("deprecation")
    public BOpenFile(String _FileName_) {
        if (_FileName_ == null)
            _FileName_ = "";
        _FileName = _FileName_;
    }

    @Override
    public void reset() {
        setFileName("");
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.HotDistribute.BOpenFile.Data toData() {
        var data = new Zeze.Builtin.HotDistribute.BOpenFile.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.HotDistribute.BOpenFile.Data)other);
    }

    public void assign(BOpenFile.Data other) {
        setFileName(other._FileName);
        _unknown_ = null;
    }

    public void assign(BOpenFile other) {
        setFileName(other.getFileName());
        _unknown_ = other._unknown_;
    }

    public BOpenFile copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BOpenFile copy() {
        var copy = new BOpenFile();
        copy.assign(this);
        return copy;
    }

    public static void swap(BOpenFile a, BOpenFile b) {
        BOpenFile save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__FileName extends Zeze.Transaction.Logs.LogString {
        public Log__FileName(BOpenFile bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BOpenFile)getBelong())._FileName = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.HotDistribute.BOpenFile: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("FileName=").append(getFileName()).append(System.lineSeparator());
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
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BOpenFile))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BOpenFile)_o_;
        if (!getFileName().equals(_b_.getFileName()))
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
                case 1: _FileName = vlog.stringValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setFileName(rs.getString(_parents_name_ + "FileName"));
        if (getFileName() == null)
            setFileName("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "FileName", getFileName());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "FileName", "string", "", ""));
        return vars;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 6565966142026805362L;

    private String _FileName;

    public String getFileName() {
        return _FileName;
    }

    public void setFileName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _FileName = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _FileName = "";
    }

    @SuppressWarnings("deprecation")
    public Data(String _FileName_) {
        if (_FileName_ == null)
            _FileName_ = "";
        _FileName = _FileName_;
    }

    @Override
    public void reset() {
        _FileName = "";
    }

    @Override
    public Zeze.Builtin.HotDistribute.BOpenFile toBean() {
        var bean = new Zeze.Builtin.HotDistribute.BOpenFile();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BOpenFile)other);
    }

    public void assign(BOpenFile other) {
        _FileName = other.getFileName();
    }

    public void assign(BOpenFile.Data other) {
        _FileName = other._FileName;
    }

    @Override
    public BOpenFile.Data copy() {
        var copy = new BOpenFile.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BOpenFile.Data a, BOpenFile.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BOpenFile.Data clone() {
        return (BOpenFile.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.HotDistribute.BOpenFile: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("FileName=").append(_FileName).append(System.lineSeparator());
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
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
