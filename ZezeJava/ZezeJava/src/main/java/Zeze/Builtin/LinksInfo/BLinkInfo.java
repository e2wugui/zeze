// auto-generated @formatter:off
package Zeze.Builtin.LinksInfo;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BLinkInfo extends Zeze.Transaction.Bean implements BLinkInfoReadOnly {
    public static final long TYPEID = -4351959562089154457L;

    private String _Ip;
    private int _Port;

    @Override
    public String getIp() {
        if (!isManaged())
            return _Ip;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Ip;
        var log = (Log__Ip)txn.getLog(objectId() + 1);
        return log != null ? log.value : _Ip;
    }

    public void setIp(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Ip = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Ip(this, 1, value));
    }

    @Override
    public int getPort() {
        if (!isManaged())
            return _Port;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Port;
        var log = (Log__Port)txn.getLog(objectId() + 2);
        return log != null ? log.value : _Port;
    }

    public void setPort(int value) {
        if (!isManaged()) {
            _Port = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Port(this, 2, value));
    }

    @SuppressWarnings("deprecation")
    public BLinkInfo() {
        _Ip = "";
    }

    @SuppressWarnings("deprecation")
    public BLinkInfo(String _Ip_, int _Port_) {
        if (_Ip_ == null)
            _Ip_ = "";
        _Ip = _Ip_;
        _Port = _Port_;
    }

    @Override
    public void reset() {
        setIp("");
        setPort(0);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.LinksInfo.BLinkInfo.Data toData() {
        var data = new Zeze.Builtin.LinksInfo.BLinkInfo.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.LinksInfo.BLinkInfo.Data)other);
    }

    public void assign(BLinkInfo.Data other) {
        setIp(other._Ip);
        setPort(other._Port);
        _unknown_ = null;
    }

    public void assign(BLinkInfo other) {
        setIp(other.getIp());
        setPort(other.getPort());
        _unknown_ = other._unknown_;
    }

    public BLinkInfo copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BLinkInfo copy() {
        var copy = new BLinkInfo();
        copy.assign(this);
        return copy;
    }

    public static void swap(BLinkInfo a, BLinkInfo b) {
        BLinkInfo save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Ip extends Zeze.Transaction.Logs.LogString {
        public Log__Ip(BLinkInfo bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BLinkInfo)getBelong())._Ip = value; }
    }

    private static final class Log__Port extends Zeze.Transaction.Logs.LogInt {
        public Log__Port(BLinkInfo bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BLinkInfo)getBelong())._Port = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.LinksInfo.BLinkInfo: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Ip=").append(getIp()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Port=").append(getPort()).append(System.lineSeparator());
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
            String _x_ = getIp();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            int _x_ = getPort();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
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
            setIp(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setPort(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BLinkInfo))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BLinkInfo)_o_;
        if (!getIp().equals(_b_.getIp()))
            return false;
        if (getPort() != _b_.getPort())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getPort() < 0)
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
                case 1: _Ip = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 2: _Port = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setIp(rs.getString(_parents_name_ + "Ip"));
        if (getIp() == null)
            setIp("");
        setPort(rs.getInt(_parents_name_ + "Port"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "Ip", getIp());
        st.appendInt(_parents_name_ + "Port", getPort());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Ip", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Port", "int", "", ""));
        return vars;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -4351959562089154457L;

    private String _Ip;
    private int _Port;

    public String getIp() {
        return _Ip;
    }

    public void setIp(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Ip = value;
    }

    public int getPort() {
        return _Port;
    }

    public void setPort(int value) {
        _Port = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Ip = "";
    }

    @SuppressWarnings("deprecation")
    public Data(String _Ip_, int _Port_) {
        if (_Ip_ == null)
            _Ip_ = "";
        _Ip = _Ip_;
        _Port = _Port_;
    }

    @Override
    public void reset() {
        _Ip = "";
        _Port = 0;
    }

    @Override
    public Zeze.Builtin.LinksInfo.BLinkInfo toBean() {
        var bean = new Zeze.Builtin.LinksInfo.BLinkInfo();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BLinkInfo)other);
    }

    public void assign(BLinkInfo other) {
        _Ip = other.getIp();
        _Port = other.getPort();
    }

    public void assign(BLinkInfo.Data other) {
        _Ip = other._Ip;
        _Port = other._Port;
    }

    @Override
    public BLinkInfo.Data copy() {
        var copy = new BLinkInfo.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BLinkInfo.Data a, BLinkInfo.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BLinkInfo.Data clone() {
        return (BLinkInfo.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.LinksInfo.BLinkInfo: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Ip=").append(_Ip).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Port=").append(_Port).append(System.lineSeparator());
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
            String _x_ = _Ip;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            int _x_ = _Port;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
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
            _Ip = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _Port = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
