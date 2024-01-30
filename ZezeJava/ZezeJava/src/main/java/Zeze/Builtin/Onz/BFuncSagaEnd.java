// auto-generated @formatter:off
package Zeze.Builtin.Onz;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BFuncSagaEnd extends Zeze.Transaction.Bean implements BFuncSagaEndReadOnly {
    public static final long TYPEID = -5966280333833227562L;

    private long _OnzTid;
    private boolean _Cancel;
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
    public boolean isCancel() {
        if (!isManaged())
            return _Cancel;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Cancel;
        var log = (Log__Cancel)txn.getLog(objectId() + 2);
        return log != null ? log.value : _Cancel;
    }

    public void setCancel(boolean value) {
        if (!isManaged()) {
            _Cancel = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Cancel(this, 2, value));
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
    public BFuncSagaEnd() {
        _FuncArgument = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BFuncSagaEnd(long _OnzTid_, boolean _Cancel_, Zeze.Net.Binary _FuncArgument_) {
        _OnzTid = _OnzTid_;
        _Cancel = _Cancel_;
        if (_FuncArgument_ == null)
            _FuncArgument_ = Zeze.Net.Binary.Empty;
        _FuncArgument = _FuncArgument_;
    }

    @Override
    public void reset() {
        setOnzTid(0);
        setCancel(false);
        setFuncArgument(Zeze.Net.Binary.Empty);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Onz.BFuncSagaEnd.Data toData() {
        var data = new Zeze.Builtin.Onz.BFuncSagaEnd.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Onz.BFuncSagaEnd.Data)other);
    }

    public void assign(BFuncSagaEnd.Data other) {
        setOnzTid(other._OnzTid);
        setCancel(other._Cancel);
        setFuncArgument(other._FuncArgument);
        _unknown_ = null;
    }

    public void assign(BFuncSagaEnd other) {
        setOnzTid(other.getOnzTid());
        setCancel(other.isCancel());
        setFuncArgument(other.getFuncArgument());
        _unknown_ = other._unknown_;
    }

    public BFuncSagaEnd copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BFuncSagaEnd copy() {
        var copy = new BFuncSagaEnd();
        copy.assign(this);
        return copy;
    }

    public static void swap(BFuncSagaEnd a, BFuncSagaEnd b) {
        BFuncSagaEnd save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__OnzTid extends Zeze.Transaction.Logs.LogLong {
        public Log__OnzTid(BFuncSagaEnd bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BFuncSagaEnd)getBelong())._OnzTid = value; }
    }

    private static final class Log__Cancel extends Zeze.Transaction.Logs.LogBool {
        public Log__Cancel(BFuncSagaEnd bean, int varId, boolean value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BFuncSagaEnd)getBelong())._Cancel = value; }
    }

    private static final class Log__FuncArgument extends Zeze.Transaction.Logs.LogBinary {
        public Log__FuncArgument(BFuncSagaEnd bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BFuncSagaEnd)getBelong())._FuncArgument = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Onz.BFuncSagaEnd: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("OnzTid=").append(getOnzTid()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Cancel=").append(isCancel()).append(',').append(System.lineSeparator());
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
            boolean _x_ = isCancel();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
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
            setCancel(_o_.ReadBool(_t_));
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
                case 2: _Cancel = ((Zeze.Transaction.Logs.LogBool)vlog).value; break;
                case 3: _FuncArgument = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setOnzTid(rs.getLong(_parents_name_ + "OnzTid"));
        setCancel(rs.getBoolean(_parents_name_ + "Cancel"));
        setFuncArgument(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "FuncArgument")));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendLong(_parents_name_ + "OnzTid", getOnzTid());
        st.appendBoolean(_parents_name_ + "Cancel", isCancel());
        st.appendBinary(_parents_name_ + "FuncArgument", getFuncArgument());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "OnzTid", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Cancel", "bool", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "FuncArgument", "binary", "", ""));
        return vars;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -5966280333833227562L;

    private long _OnzTid;
    private boolean _Cancel;
    private Zeze.Net.Binary _FuncArgument;

    public long getOnzTid() {
        return _OnzTid;
    }

    public void setOnzTid(long value) {
        _OnzTid = value;
    }

    public boolean isCancel() {
        return _Cancel;
    }

    public void setCancel(boolean value) {
        _Cancel = value;
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
        _FuncArgument = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public Data(long _OnzTid_, boolean _Cancel_, Zeze.Net.Binary _FuncArgument_) {
        _OnzTid = _OnzTid_;
        _Cancel = _Cancel_;
        if (_FuncArgument_ == null)
            _FuncArgument_ = Zeze.Net.Binary.Empty;
        _FuncArgument = _FuncArgument_;
    }

    @Override
    public void reset() {
        _OnzTid = 0;
        _Cancel = false;
        _FuncArgument = Zeze.Net.Binary.Empty;
    }

    @Override
    public Zeze.Builtin.Onz.BFuncSagaEnd toBean() {
        var bean = new Zeze.Builtin.Onz.BFuncSagaEnd();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BFuncSagaEnd)other);
    }

    public void assign(BFuncSagaEnd other) {
        _OnzTid = other.getOnzTid();
        _Cancel = other.isCancel();
        _FuncArgument = other.getFuncArgument();
    }

    public void assign(BFuncSagaEnd.Data other) {
        _OnzTid = other._OnzTid;
        _Cancel = other._Cancel;
        _FuncArgument = other._FuncArgument;
    }

    @Override
    public BFuncSagaEnd.Data copy() {
        var copy = new BFuncSagaEnd.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BFuncSagaEnd.Data a, BFuncSagaEnd.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BFuncSagaEnd.Data clone() {
        return (BFuncSagaEnd.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Onz.BFuncSagaEnd: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("OnzTid=").append(_OnzTid).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Cancel=").append(_Cancel).append(',').append(System.lineSeparator());
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
            boolean _x_ = _Cancel;
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
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
            _Cancel = _o_.ReadBool(_t_);
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
