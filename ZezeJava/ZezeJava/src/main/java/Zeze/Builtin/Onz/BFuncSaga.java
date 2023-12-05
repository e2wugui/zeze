// auto-generated @formatter:off
package Zeze.Builtin.Onz;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

// saga confirm
@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BFuncSaga extends Zeze.Transaction.Bean implements BFuncSagaReadOnly {
    public static final long TYPEID = -6343244793486575953L;

    private long _OnzTid;
    private String _FuncName;
    private Zeze.Net.Binary _FuncArgument;

    @Override
    public long getOnzTid() {
        if (!isManaged())
            return _OnzTid;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _OnzTid;
        var log = (Log__OnzTid)txn.getLog(objectId() + 1);
        return log != null ? log.value : _OnzTid;
    }

    public void setOnzTid(long value) {
        if (!isManaged()) {
            _OnzTid = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__OnzTid(this, 1, value));
    }

    @Override
    public String getFuncName() {
        if (!isManaged())
            return _FuncName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _FuncName;
        var log = (Log__FuncName)txn.getLog(objectId() + 2);
        return log != null ? log.value : _FuncName;
    }

    public void setFuncName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _FuncName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__FuncName(this, 2, value));
    }

    @Override
    public Zeze.Net.Binary getFuncArgument() {
        if (!isManaged())
            return _FuncArgument;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _FuncArgument;
        var log = (Log__FuncArgument)txn.getLog(objectId() + 3);
        return log != null ? log.value : _FuncArgument;
    }

    public void setFuncArgument(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _FuncArgument = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__FuncArgument(this, 3, value));
    }

    @SuppressWarnings("deprecation")
    public BFuncSaga() {
        _FuncName = "";
        _FuncArgument = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BFuncSaga(long _OnzTid_, String _FuncName_, Zeze.Net.Binary _FuncArgument_) {
        _OnzTid = _OnzTid_;
        if (_FuncName_ == null)
            _FuncName_ = "";
        _FuncName = _FuncName_;
        if (_FuncArgument_ == null)
            _FuncArgument_ = Zeze.Net.Binary.Empty;
        _FuncArgument = _FuncArgument_;
    }

    @Override
    public void reset() {
        setOnzTid(0);
        setFuncName("");
        setFuncArgument(Zeze.Net.Binary.Empty);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Onz.BFuncSaga.Data toData() {
        var data = new Zeze.Builtin.Onz.BFuncSaga.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Onz.BFuncSaga.Data)other);
    }

    public void assign(BFuncSaga.Data other) {
        setOnzTid(other._OnzTid);
        setFuncName(other._FuncName);
        setFuncArgument(other._FuncArgument);
        _unknown_ = null;
    }

    public void assign(BFuncSaga other) {
        setOnzTid(other.getOnzTid());
        setFuncName(other.getFuncName());
        setFuncArgument(other.getFuncArgument());
        _unknown_ = other._unknown_;
    }

    public BFuncSaga copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BFuncSaga copy() {
        var copy = new BFuncSaga();
        copy.assign(this);
        return copy;
    }

    public static void swap(BFuncSaga a, BFuncSaga b) {
        BFuncSaga save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__OnzTid extends Zeze.Transaction.Logs.LogLong {
        public Log__OnzTid(BFuncSaga bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BFuncSaga)getBelong())._OnzTid = value; }
    }

    private static final class Log__FuncName extends Zeze.Transaction.Logs.LogString {
        public Log__FuncName(BFuncSaga bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BFuncSaga)getBelong())._FuncName = value; }
    }

    private static final class Log__FuncArgument extends Zeze.Transaction.Logs.LogBinary {
        public Log__FuncArgument(BFuncSaga bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BFuncSaga)getBelong())._FuncArgument = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Onz.BFuncSaga: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("OnzTid=").append(getOnzTid()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("FuncName=").append(getFuncName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("FuncArgument=").append(getFuncArgument()).append(System.lineSeparator());
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
            long _x_ = getOnzTid();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            String _x_ = getFuncName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = getFuncArgument();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
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
            setOnzTid(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setFuncName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setFuncArgument(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean negativeCheck() {
        if (getOnzTid() < 0)
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
                case 1: _OnzTid = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 2: _FuncName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 3: _FuncArgument = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setOnzTid(rs.getLong(_parents_name_ + "OnzTid"));
        setFuncName(rs.getString(_parents_name_ + "FuncName"));
        if (getFuncName() == null)
            setFuncName("");
        setFuncArgument(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "FuncArgument")));
        if (getFuncArgument() == null)
            setFuncArgument(Zeze.Net.Binary.Empty);
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendLong(_parents_name_ + "OnzTid", getOnzTid());
        st.appendString(_parents_name_ + "FuncName", getFuncName());
        st.appendBinary(_parents_name_ + "FuncArgument", getFuncArgument());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "OnzTid", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "FuncName", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "FuncArgument", "binary", "", ""));
        return vars;
    }

// saga confirm
@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -6343244793486575953L;

    private long _OnzTid;
    private String _FuncName;
    private Zeze.Net.Binary _FuncArgument;

    public long getOnzTid() {
        return _OnzTid;
    }

    public void setOnzTid(long value) {
        _OnzTid = value;
    }

    public String getFuncName() {
        return _FuncName;
    }

    public void setFuncName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _FuncName = value;
    }

    public Zeze.Net.Binary getFuncArgument() {
        return _FuncArgument;
    }

    public void setFuncArgument(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        _FuncArgument = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _FuncName = "";
        _FuncArgument = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public Data(long _OnzTid_, String _FuncName_, Zeze.Net.Binary _FuncArgument_) {
        _OnzTid = _OnzTid_;
        if (_FuncName_ == null)
            _FuncName_ = "";
        _FuncName = _FuncName_;
        if (_FuncArgument_ == null)
            _FuncArgument_ = Zeze.Net.Binary.Empty;
        _FuncArgument = _FuncArgument_;
    }

    @Override
    public void reset() {
        _OnzTid = 0;
        _FuncName = "";
        _FuncArgument = Zeze.Net.Binary.Empty;
    }

    @Override
    public Zeze.Builtin.Onz.BFuncSaga toBean() {
        var bean = new Zeze.Builtin.Onz.BFuncSaga();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BFuncSaga)other);
    }

    public void assign(BFuncSaga other) {
        _OnzTid = other.getOnzTid();
        _FuncName = other.getFuncName();
        _FuncArgument = other.getFuncArgument();
    }

    public void assign(BFuncSaga.Data other) {
        _OnzTid = other._OnzTid;
        _FuncName = other._FuncName;
        _FuncArgument = other._FuncArgument;
    }

    @Override
    public BFuncSaga.Data copy() {
        var copy = new BFuncSaga.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BFuncSaga.Data a, BFuncSaga.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BFuncSaga.Data clone() {
        return (BFuncSaga.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Onz.BFuncSaga: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("OnzTid=").append(_OnzTid).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("FuncName=").append(_FuncName).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("FuncArgument=").append(_FuncArgument).append(System.lineSeparator());
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
            long _x_ = _OnzTid;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            String _x_ = _FuncName;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = _FuncArgument;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
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
            _OnzTid = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _FuncName = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _FuncArgument = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
