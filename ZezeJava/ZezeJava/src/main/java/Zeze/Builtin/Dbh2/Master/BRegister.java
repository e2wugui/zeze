// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BRegister extends Zeze.Transaction.Bean implements BRegisterReadOnly {
    public static final long TYPEID = -3200018963971290421L;

    private String _Dbh2RaftAcceptorName;
    private int _Port;
    private int _BucketCount;

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

    @Override
    public int getBucketCount() {
        if (!isManaged())
            return _BucketCount;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _BucketCount;
        var log = (Log__BucketCount)txn.getLog(objectId() + 3);
        return log != null ? log.value : _BucketCount;
    }

    public void setBucketCount(int value) {
        if (!isManaged()) {
            _BucketCount = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__BucketCount(this, 3, value));
    }

    @SuppressWarnings("deprecation")
    public BRegister() {
        _Dbh2RaftAcceptorName = "";
    }

    @SuppressWarnings("deprecation")
    public BRegister(String _Dbh2RaftAcceptorName_, int _Port_, int _BucketCount_) {
        if (_Dbh2RaftAcceptorName_ == null)
            _Dbh2RaftAcceptorName_ = "";
        _Dbh2RaftAcceptorName = _Dbh2RaftAcceptorName_;
        _Port = _Port_;
        _BucketCount = _BucketCount_;
    }

    @Override
    public void reset() {
        setDbh2RaftAcceptorName("");
        setPort(0);
        setBucketCount(0);
        _unknown_ = null;
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
        setDbh2RaftAcceptorName(other._Dbh2RaftAcceptorName);
        setPort(other._Port);
        setBucketCount(other._BucketCount);
        _unknown_ = null;
    }

    public void assign(BRegister other) {
        setDbh2RaftAcceptorName(other.getDbh2RaftAcceptorName());
        setPort(other.getPort());
        setBucketCount(other.getBucketCount());
        _unknown_ = other._unknown_;
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

    private static final class Log__Port extends Zeze.Transaction.Logs.LogInt {
        public Log__Port(BRegister bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BRegister)getBelong())._Port = value; }
    }

    private static final class Log__BucketCount extends Zeze.Transaction.Logs.LogInt {
        public Log__BucketCount(BRegister bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BRegister)getBelong())._BucketCount = value; }
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
        sb.append(Zeze.Util.Str.indent(level)).append("Dbh2RaftAcceptorName=").append(getDbh2RaftAcceptorName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Port=").append(getPort()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("BucketCount=").append(getBucketCount()).append(System.lineSeparator());
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
            String _x_ = getDbh2RaftAcceptorName();
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
        {
            int _x_ = getBucketCount();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
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
            setDbh2RaftAcceptorName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setPort(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setBucketCount(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean negativeCheck() {
        if (getPort() < 0)
            return true;
        if (getBucketCount() < 0)
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
                case 1: _Dbh2RaftAcceptorName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 2: _Port = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 3: _BucketCount = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setDbh2RaftAcceptorName(rs.getString(_parents_name_ + "Dbh2RaftAcceptorName"));
        if (getDbh2RaftAcceptorName() == null)
            setDbh2RaftAcceptorName("");
        setPort(rs.getInt(_parents_name_ + "Port"));
        setBucketCount(rs.getInt(_parents_name_ + "BucketCount"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "Dbh2RaftAcceptorName", getDbh2RaftAcceptorName());
        st.appendInt(_parents_name_ + "Port", getPort());
        st.appendInt(_parents_name_ + "BucketCount", getBucketCount());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Dbh2RaftAcceptorName", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Port", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "BucketCount", "int", "", ""));
        return vars;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -3200018963971290421L;

    private String _Dbh2RaftAcceptorName;
    private int _Port;
    private int _BucketCount;

    public String getDbh2RaftAcceptorName() {
        return _Dbh2RaftAcceptorName;
    }

    public void setDbh2RaftAcceptorName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Dbh2RaftAcceptorName = value;
    }

    public int getPort() {
        return _Port;
    }

    public void setPort(int value) {
        _Port = value;
    }

    public int getBucketCount() {
        return _BucketCount;
    }

    public void setBucketCount(int value) {
        _BucketCount = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Dbh2RaftAcceptorName = "";
    }

    @SuppressWarnings("deprecation")
    public Data(String _Dbh2RaftAcceptorName_, int _Port_, int _BucketCount_) {
        if (_Dbh2RaftAcceptorName_ == null)
            _Dbh2RaftAcceptorName_ = "";
        _Dbh2RaftAcceptorName = _Dbh2RaftAcceptorName_;
        _Port = _Port_;
        _BucketCount = _BucketCount_;
    }

    @Override
    public void reset() {
        _Dbh2RaftAcceptorName = "";
        _Port = 0;
        _BucketCount = 0;
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
        _Dbh2RaftAcceptorName = other.getDbh2RaftAcceptorName();
        _Port = other.getPort();
        _BucketCount = other.getBucketCount();
    }

    public void assign(BRegister.Data other) {
        _Dbh2RaftAcceptorName = other._Dbh2RaftAcceptorName;
        _Port = other._Port;
        _BucketCount = other._BucketCount;
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
    public BRegister.Data clone() {
        return (BRegister.Data)super.clone();
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
        sb.append(Zeze.Util.Str.indent(level)).append("Dbh2RaftAcceptorName=").append(_Dbh2RaftAcceptorName).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Port=").append(_Port).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("BucketCount=").append(_BucketCount).append(System.lineSeparator());
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
            String _x_ = _Dbh2RaftAcceptorName;
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
        {
            int _x_ = _BucketCount;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _Dbh2RaftAcceptorName = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _Port = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _BucketCount = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
