// auto-generated @formatter:off
package Zeze.Builtin.Zoker;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BStopService extends Zeze.Transaction.Bean implements BStopServiceReadOnly {
    public static final long TYPEID = 9090863522815062458L;

    private String _ServiceName;
    private boolean _Force; // like kill -9

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
    public boolean isForce() {
        if (!isManaged())
            return _Force;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Force;
        var log = (Log__Force)txn.getLog(objectId() + 2);
        return log != null ? log.value : _Force;
    }

    public void setForce(boolean value) {
        if (!isManaged()) {
            _Force = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Force(this, 2, value));
    }

    @SuppressWarnings("deprecation")
    public BStopService() {
        _ServiceName = "";
    }

    @SuppressWarnings("deprecation")
    public BStopService(String _ServiceName_, boolean _Force_) {
        if (_ServiceName_ == null)
            _ServiceName_ = "";
        _ServiceName = _ServiceName_;
        _Force = _Force_;
    }

    @Override
    public void reset() {
        setServiceName("");
        setForce(false);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Zoker.BStopService.Data toData() {
        var data = new Zeze.Builtin.Zoker.BStopService.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Zoker.BStopService.Data)other);
    }

    public void assign(BStopService.Data other) {
        setServiceName(other._ServiceName);
        setForce(other._Force);
        _unknown_ = null;
    }

    public void assign(BStopService other) {
        setServiceName(other.getServiceName());
        setForce(other.isForce());
        _unknown_ = other._unknown_;
    }

    public BStopService copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BStopService copy() {
        var copy = new BStopService();
        copy.assign(this);
        return copy;
    }

    public static void swap(BStopService a, BStopService b) {
        BStopService save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ServiceName extends Zeze.Transaction.Logs.LogString {
        public Log__ServiceName(BStopService bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BStopService)getBelong())._ServiceName = value; }
    }

    private static final class Log__Force extends Zeze.Transaction.Logs.LogBool {
        public Log__Force(BStopService bean, int varId, boolean value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BStopService)getBelong())._Force = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Zoker.BStopService: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ServiceName=").append(getServiceName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Force=").append(isForce()).append(System.lineSeparator());
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
            boolean _x_ = isForce();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        _o_.writeAllUnknownFields(_i_, _ui_, _u_);
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        ByteBuffer _u_ = null;
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setServiceName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setForce(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
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
                case 2: _Force = ((Zeze.Transaction.Logs.LogBool)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setServiceName(rs.getString(_parents_name_ + "ServiceName"));
        if (getServiceName() == null)
            setServiceName("");
        setForce(rs.getBoolean(_parents_name_ + "Force"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "ServiceName", getServiceName());
        st.appendBoolean(_parents_name_ + "Force", isForce());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "ServiceName", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Force", "bool", "", ""));
        return vars;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 9090863522815062458L;

    private String _ServiceName;
    private boolean _Force; // like kill -9

    public String getServiceName() {
        return _ServiceName;
    }

    public void setServiceName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _ServiceName = value;
    }

    public boolean isForce() {
        return _Force;
    }

    public void setForce(boolean value) {
        _Force = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _ServiceName = "";
    }

    @SuppressWarnings("deprecation")
    public Data(String _ServiceName_, boolean _Force_) {
        if (_ServiceName_ == null)
            _ServiceName_ = "";
        _ServiceName = _ServiceName_;
        _Force = _Force_;
    }

    @Override
    public void reset() {
        _ServiceName = "";
        _Force = false;
    }

    @Override
    public Zeze.Builtin.Zoker.BStopService toBean() {
        var bean = new Zeze.Builtin.Zoker.BStopService();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BStopService)other);
    }

    public void assign(BStopService other) {
        _ServiceName = other.getServiceName();
        _Force = other.isForce();
    }

    public void assign(BStopService.Data other) {
        _ServiceName = other._ServiceName;
        _Force = other._Force;
    }

    @Override
    public BStopService.Data copy() {
        var copy = new BStopService.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BStopService.Data a, BStopService.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BStopService.Data clone() {
        return (BStopService.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Zoker.BStopService: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ServiceName=").append(_ServiceName).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Force=").append(_Force).append(System.lineSeparator());
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
            boolean _x_ = _Force;
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _ServiceName = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _Force = _o_.ReadBool(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
