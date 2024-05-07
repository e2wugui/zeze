// auto-generated @formatter:off
package Zeze.Builtin.LogService;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BNewSession extends Zeze.Transaction.Bean implements BNewSessionReadOnly {
    public static final long TYPEID = 4447234831022031083L;

    private String _LogName;

    @Override
    public String getLogName() {
        if (!isManaged())
            return _LogName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _LogName;
        var log = (Log__LogName)txn.getLog(objectId() + 1);
        return log != null ? log.value : _LogName;
    }

    public void setLogName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _LogName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__LogName(this, 1, value));
    }

    @SuppressWarnings("deprecation")
    public BNewSession() {
        _LogName = "";
    }

    @SuppressWarnings("deprecation")
    public BNewSession(String _LogName_) {
        if (_LogName_ == null)
            _LogName_ = "";
        _LogName = _LogName_;
    }

    @Override
    public void reset() {
        setLogName("");
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.LogService.BNewSession.Data toData() {
        var data = new Zeze.Builtin.LogService.BNewSession.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.LogService.BNewSession.Data)other);
    }

    public void assign(BNewSession.Data other) {
        setLogName(other._LogName);
        _unknown_ = null;
    }

    public void assign(BNewSession other) {
        setLogName(other.getLogName());
        _unknown_ = other._unknown_;
    }

    public BNewSession copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BNewSession copy() {
        var copy = new BNewSession();
        copy.assign(this);
        return copy;
    }

    public static void swap(BNewSession a, BNewSession b) {
        BNewSession save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__LogName extends Zeze.Transaction.Logs.LogString {
        public Log__LogName(BNewSession bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BNewSession)getBelong())._LogName = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.LogService.BNewSession: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("LogName=").append(getLogName()).append(System.lineSeparator());
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
            String _x_ = getLogName();
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
            setLogName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BNewSession))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BNewSession)_o_;
        if (!getLogName().equals(_b_.getLogName()))
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
                case 1: _LogName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setLogName(rs.getString(_parents_name_ + "LogName"));
        if (getLogName() == null)
            setLogName("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "LogName", getLogName());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "LogName", "string", "", ""));
        return vars;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 4447234831022031083L;

    private String _LogName;

    public String getLogName() {
        return _LogName;
    }

    public void setLogName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _LogName = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _LogName = "";
    }

    @SuppressWarnings("deprecation")
    public Data(String _LogName_) {
        if (_LogName_ == null)
            _LogName_ = "";
        _LogName = _LogName_;
    }

    @Override
    public void reset() {
        _LogName = "";
    }

    @Override
    public Zeze.Builtin.LogService.BNewSession toBean() {
        var bean = new Zeze.Builtin.LogService.BNewSession();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BNewSession)other);
    }

    public void assign(BNewSession other) {
        _LogName = other.getLogName();
    }

    public void assign(BNewSession.Data other) {
        _LogName = other._LogName;
    }

    @Override
    public BNewSession.Data copy() {
        var copy = new BNewSession.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BNewSession.Data a, BNewSession.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BNewSession.Data clone() {
        return (BNewSession.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.LogService.BNewSession: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("LogName=").append(_LogName).append(System.lineSeparator());
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
            String _x_ = _LogName;
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
            _LogName = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
