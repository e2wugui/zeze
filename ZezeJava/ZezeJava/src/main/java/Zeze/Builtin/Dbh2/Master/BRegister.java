// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BRegister extends Zeze.Transaction.Bean implements BRegisterReadOnly {
    public static final long TYPEID = -3200018963971290421L;

    private String _Dbh2RaftAcceptorName;

    @Override
    public String getDbh2RaftAcceptorName() {
        if (!isManaged())
            return _Dbh2RaftAcceptorName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Dbh2RaftAcceptorName;
        var log = (Log__Dbh2RaftAcceptorName)txn.getLog(objectId() + 1);
        return log != null ? log.value : _Dbh2RaftAcceptorName;
    }

    public void setDbh2RaftAcceptorName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Dbh2RaftAcceptorName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Dbh2RaftAcceptorName(this, 1, value));
    }

    @SuppressWarnings("deprecation")
    public BRegister() {
        _Dbh2RaftAcceptorName = "";
    }

    @SuppressWarnings("deprecation")
    public BRegister(String _Dbh2RaftAcceptorName_) {
        if (_Dbh2RaftAcceptorName_ == null)
            throw new IllegalArgumentException();
        _Dbh2RaftAcceptorName = _Dbh2RaftAcceptorName_;
    }

    @Override
    public Zeze.Builtin.Dbh2.Master.BRegister.Data toData() {
        var data = new Zeze.Builtin.Dbh2.Master.BRegister.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Dbh2.Master.BRegister.Data)other);
    }

    public void assign(BRegister.Data other) {
        setDbh2RaftAcceptorName(other.getDbh2RaftAcceptorName());
    }

    public void assign(BRegister other) {
        setDbh2RaftAcceptorName(other.getDbh2RaftAcceptorName());
    }

    public BRegister copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BRegister copy() {
        var copy = new BRegister();
        copy.assign(this);
        return copy;
    }

    public static void swap(BRegister a, BRegister b) {
        BRegister save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Dbh2RaftAcceptorName extends Zeze.Transaction.Logs.LogString {
        public Log__Dbh2RaftAcceptorName(BRegister bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BRegister)getBelong())._Dbh2RaftAcceptorName = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.Master.BRegister: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Dbh2RaftAcceptorName=").append(getDbh2RaftAcceptorName()).append(System.lineSeparator());
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

    @Override
    public void encode(ByteBuffer _o_) {
        int _i_ = 0;
        {
            String _x_ = getDbh2RaftAcceptorName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setDbh2RaftAcceptorName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    public boolean negativeCheck() {
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
                case 1: _Dbh2RaftAcceptorName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setDbh2RaftAcceptorName(rs.getString(_parents_name_ + "Dbh2RaftAcceptorName"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "Dbh2RaftAcceptorName", getDbh2RaftAcceptorName());
    }

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -3200018963971290421L;

    private String _Dbh2RaftAcceptorName;

    public String getDbh2RaftAcceptorName() {
        return _Dbh2RaftAcceptorName;
    }

    public void setDbh2RaftAcceptorName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Dbh2RaftAcceptorName = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Dbh2RaftAcceptorName = "";
    }

    @SuppressWarnings("deprecation")
    public Data(String _Dbh2RaftAcceptorName_) {
        if (_Dbh2RaftAcceptorName_ == null)
            throw new IllegalArgumentException();
        _Dbh2RaftAcceptorName = _Dbh2RaftAcceptorName_;
    }

    @Override
    public Zeze.Builtin.Dbh2.Master.BRegister toBean() {
        var bean = new Zeze.Builtin.Dbh2.Master.BRegister();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BRegister)other);
    }

    public void assign(BRegister other) {
        setDbh2RaftAcceptorName(other.getDbh2RaftAcceptorName());
    }

    public void assign(BRegister.Data other) {
        setDbh2RaftAcceptorName(other.getDbh2RaftAcceptorName());
    }

    @Override
    public BRegister.Data copy() {
        var copy = new BRegister.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BRegister.Data a, BRegister.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.Master.BRegister: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Dbh2RaftAcceptorName=").append(getDbh2RaftAcceptorName()).append(System.lineSeparator());
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

    @Override
    public void encode(ByteBuffer _o_) {
        int _i_ = 0;
        {
            String _x_ = getDbh2RaftAcceptorName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setDbh2RaftAcceptorName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
