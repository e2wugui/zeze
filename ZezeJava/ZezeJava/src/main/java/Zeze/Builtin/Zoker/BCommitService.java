// auto-generated @formatter:off
package Zeze.Builtin.Zoker;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BCommitService extends Zeze.Transaction.Bean implements BCommitServiceReadOnly {
    public static final long TYPEID = -31402640502625825L;

    private String _ServiceName;
    private String _VersionNo;

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
    public String getVersionNo() {
        if (!isManaged())
            return _VersionNo;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _VersionNo;
        var log = (Log__VersionNo)txn.getLog(objectId() + 2);
        return log != null ? log.value : _VersionNo;
    }

    public void setVersionNo(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _VersionNo = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__VersionNo(this, 2, value));
    }

    @SuppressWarnings("deprecation")
    public BCommitService() {
        _ServiceName = "";
        _VersionNo = "";
    }

    @SuppressWarnings("deprecation")
    public BCommitService(String _ServiceName_, String _VersionNo_) {
        if (_ServiceName_ == null)
            _ServiceName_ = "";
        _ServiceName = _ServiceName_;
        if (_VersionNo_ == null)
            _VersionNo_ = "";
        _VersionNo = _VersionNo_;
    }

    @Override
    public void reset() {
        setServiceName("");
        setVersionNo("");
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Zoker.BCommitService.Data toData() {
        var data = new Zeze.Builtin.Zoker.BCommitService.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Zoker.BCommitService.Data)other);
    }

    public void assign(BCommitService.Data other) {
        setServiceName(other._ServiceName);
        setVersionNo(other._VersionNo);
        _unknown_ = null;
    }

    public void assign(BCommitService other) {
        setServiceName(other.getServiceName());
        setVersionNo(other.getVersionNo());
        _unknown_ = other._unknown_;
    }

    public BCommitService copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BCommitService copy() {
        var copy = new BCommitService();
        copy.assign(this);
        return copy;
    }

    public static void swap(BCommitService a, BCommitService b) {
        BCommitService save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ServiceName extends Zeze.Transaction.Logs.LogString {
        public Log__ServiceName(BCommitService bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BCommitService)getBelong())._ServiceName = value; }
    }

    private static final class Log__VersionNo extends Zeze.Transaction.Logs.LogString {
        public Log__VersionNo(BCommitService bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BCommitService)getBelong())._VersionNo = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Zoker.BCommitService: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ServiceName=").append(getServiceName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("VersionNo=").append(getVersionNo()).append(System.lineSeparator());
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
            String _x_ = getVersionNo();
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
            setServiceName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setVersionNo(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BCommitService))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BCommitService)_o_;
        if (!getServiceName().equals(_b_.getServiceName()))
            return false;
        if (!getVersionNo().equals(_b_.getVersionNo()))
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
                case 1: _ServiceName = vlog.stringValue(); break;
                case 2: _VersionNo = vlog.stringValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setServiceName(rs.getString(_parents_name_ + "ServiceName"));
        if (getServiceName() == null)
            setServiceName("");
        setVersionNo(rs.getString(_parents_name_ + "VersionNo"));
        if (getVersionNo() == null)
            setVersionNo("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "ServiceName", getServiceName());
        st.appendString(_parents_name_ + "VersionNo", getVersionNo());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "ServiceName", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "VersionNo", "string", "", ""));
        return vars;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -31402640502625825L;

    private String _ServiceName;
    private String _VersionNo;

    public String getServiceName() {
        return _ServiceName;
    }

    public void setServiceName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _ServiceName = value;
    }

    public String getVersionNo() {
        return _VersionNo;
    }

    public void setVersionNo(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _VersionNo = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _ServiceName = "";
        _VersionNo = "";
    }

    @SuppressWarnings("deprecation")
    public Data(String _ServiceName_, String _VersionNo_) {
        if (_ServiceName_ == null)
            _ServiceName_ = "";
        _ServiceName = _ServiceName_;
        if (_VersionNo_ == null)
            _VersionNo_ = "";
        _VersionNo = _VersionNo_;
    }

    @Override
    public void reset() {
        _ServiceName = "";
        _VersionNo = "";
    }

    @Override
    public Zeze.Builtin.Zoker.BCommitService toBean() {
        var bean = new Zeze.Builtin.Zoker.BCommitService();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BCommitService)other);
    }

    public void assign(BCommitService other) {
        _ServiceName = other.getServiceName();
        _VersionNo = other.getVersionNo();
    }

    public void assign(BCommitService.Data other) {
        _ServiceName = other._ServiceName;
        _VersionNo = other._VersionNo;
    }

    @Override
    public BCommitService.Data copy() {
        var copy = new BCommitService.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BCommitService.Data a, BCommitService.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BCommitService.Data clone() {
        return (BCommitService.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Zoker.BCommitService: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ServiceName=").append(_ServiceName).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("VersionNo=").append(_VersionNo).append(System.lineSeparator());
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
            String _x_ = _VersionNo;
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
            _ServiceName = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _VersionNo = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
