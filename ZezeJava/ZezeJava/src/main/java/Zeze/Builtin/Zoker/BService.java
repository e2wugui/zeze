// auto-generated @formatter:off
package Zeze.Builtin.Zoker;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

// 服务：查询，启动，关闭
@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BService extends Zeze.Transaction.Bean implements BServiceReadOnly {
    public static final long TYPEID = 8648379280162192984L;

    private String _ServiceName;
    private String _State; // Running,Stopped
    private String _Ps; // some ps result ...

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
    public String getState() {
        if (!isManaged())
            return _State;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _State;
        var log = (Log__State)txn.getLog(objectId() + 2);
        return log != null ? log.value : _State;
    }

    public void setState(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _State = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__State(this, 2, value));
    }

    @Override
    public String getPs() {
        if (!isManaged())
            return _Ps;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Ps;
        var log = (Log__Ps)txn.getLog(objectId() + 3);
        return log != null ? log.value : _Ps;
    }

    public void setPs(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Ps = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Ps(this, 3, value));
    }

    @SuppressWarnings("deprecation")
    public BService() {
        _ServiceName = "";
        _State = "";
        _Ps = "";
    }

    @SuppressWarnings("deprecation")
    public BService(String _ServiceName_, String _State_, String _Ps_) {
        if (_ServiceName_ == null)
            _ServiceName_ = "";
        _ServiceName = _ServiceName_;
        if (_State_ == null)
            _State_ = "";
        _State = _State_;
        if (_Ps_ == null)
            _Ps_ = "";
        _Ps = _Ps_;
    }

    @Override
    public void reset() {
        setServiceName("");
        setState("");
        setPs("");
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Zoker.BService.Data toData() {
        var data = new Zeze.Builtin.Zoker.BService.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Zoker.BService.Data)other);
    }

    public void assign(BService.Data other) {
        setServiceName(other._ServiceName);
        setState(other._State);
        setPs(other._Ps);
        _unknown_ = null;
    }

    public void assign(BService other) {
        setServiceName(other.getServiceName());
        setState(other.getState());
        setPs(other.getPs());
        _unknown_ = other._unknown_;
    }

    public BService copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BService copy() {
        var copy = new BService();
        copy.assign(this);
        return copy;
    }

    public static void swap(BService a, BService b) {
        BService save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ServiceName extends Zeze.Transaction.Logs.LogString {
        public Log__ServiceName(BService bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BService)getBelong())._ServiceName = value; }
    }

    private static final class Log__State extends Zeze.Transaction.Logs.LogString {
        public Log__State(BService bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BService)getBelong())._State = value; }
    }

    private static final class Log__Ps extends Zeze.Transaction.Logs.LogString {
        public Log__Ps(BService bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BService)getBelong())._Ps = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Zoker.BService: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ServiceName=").append(getServiceName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("State=").append(getState()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Ps=").append(getPs()).append(System.lineSeparator());
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
            String _x_ = getState();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getPs();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
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
            setState(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setPs(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BService))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BService)_o_;
        if (!getServiceName().equals(_b_.getServiceName()))
            return false;
        if (!getState().equals(_b_.getState()))
            return false;
        if (!getPs().equals(_b_.getPs()))
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
                case 2: _State = vlog.stringValue(); break;
                case 3: _Ps = vlog.stringValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setServiceName(rs.getString(_parents_name_ + "ServiceName"));
        if (getServiceName() == null)
            setServiceName("");
        setState(rs.getString(_parents_name_ + "State"));
        if (getState() == null)
            setState("");
        setPs(rs.getString(_parents_name_ + "Ps"));
        if (getPs() == null)
            setPs("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "ServiceName", getServiceName());
        st.appendString(_parents_name_ + "State", getState());
        st.appendString(_parents_name_ + "Ps", getPs());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "ServiceName", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "State", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Ps", "string", "", ""));
        return vars;
    }

// 服务：查询，启动，关闭
@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 8648379280162192984L;

    private String _ServiceName;
    private String _State; // Running,Stopped
    private String _Ps; // some ps result ...

    public String getServiceName() {
        return _ServiceName;
    }

    public void setServiceName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _ServiceName = value;
    }

    public String getState() {
        return _State;
    }

    public void setState(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _State = value;
    }

    public String getPs() {
        return _Ps;
    }

    public void setPs(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Ps = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _ServiceName = "";
        _State = "";
        _Ps = "";
    }

    @SuppressWarnings("deprecation")
    public Data(String _ServiceName_, String _State_, String _Ps_) {
        if (_ServiceName_ == null)
            _ServiceName_ = "";
        _ServiceName = _ServiceName_;
        if (_State_ == null)
            _State_ = "";
        _State = _State_;
        if (_Ps_ == null)
            _Ps_ = "";
        _Ps = _Ps_;
    }

    @Override
    public void reset() {
        _ServiceName = "";
        _State = "";
        _Ps = "";
    }

    @Override
    public Zeze.Builtin.Zoker.BService toBean() {
        var bean = new Zeze.Builtin.Zoker.BService();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BService)other);
    }

    public void assign(BService other) {
        _ServiceName = other.getServiceName();
        _State = other.getState();
        _Ps = other.getPs();
    }

    public void assign(BService.Data other) {
        _ServiceName = other._ServiceName;
        _State = other._State;
        _Ps = other._Ps;
    }

    @Override
    public BService.Data copy() {
        var copy = new BService.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BService.Data a, BService.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BService.Data clone() {
        return (BService.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Zoker.BService: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ServiceName=").append(_ServiceName).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("State=").append(_State).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Ps=").append(_Ps).append(System.lineSeparator());
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
            String _x_ = _State;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = _Ps;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
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
            _State = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _Ps = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
