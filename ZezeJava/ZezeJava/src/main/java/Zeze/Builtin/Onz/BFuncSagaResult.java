// auto-generated @formatter:off
package Zeze.Builtin.Onz;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BFuncSagaResult extends Zeze.Transaction.Bean implements BFuncSagaResultReadOnly {
    public static final long TYPEID = -3292789589164263566L;

    private Zeze.Net.Binary _FuncResult;

    @Override
    public Zeze.Net.Binary getFuncResult() {
        if (!isManaged())
            return _FuncResult;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _FuncResult;
        var log = (Log__FuncResult)txn.getLog(objectId() + 1);
        return log != null ? log.value : _FuncResult;
    }

    public void setFuncResult(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _FuncResult = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__FuncResult(this, 1, value));
    }

    @SuppressWarnings("deprecation")
    public BFuncSagaResult() {
        _FuncResult = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BFuncSagaResult(Zeze.Net.Binary _FuncResult_) {
        if (_FuncResult_ == null)
            _FuncResult_ = Zeze.Net.Binary.Empty;
        _FuncResult = _FuncResult_;
    }

    @Override
    public void reset() {
        setFuncResult(Zeze.Net.Binary.Empty);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Onz.BFuncSagaResult.Data toData() {
        var data = new Zeze.Builtin.Onz.BFuncSagaResult.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Onz.BFuncSagaResult.Data)other);
    }

    public void assign(BFuncSagaResult.Data other) {
        setFuncResult(other._FuncResult);
        _unknown_ = null;
    }

    public void assign(BFuncSagaResult other) {
        setFuncResult(other.getFuncResult());
        _unknown_ = other._unknown_;
    }

    public BFuncSagaResult copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BFuncSagaResult copy() {
        var copy = new BFuncSagaResult();
        copy.assign(this);
        return copy;
    }

    public static void swap(BFuncSagaResult a, BFuncSagaResult b) {
        BFuncSagaResult save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__FuncResult extends Zeze.Transaction.Logs.LogBinary {
        public Log__FuncResult(BFuncSagaResult bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BFuncSagaResult)getBelong())._FuncResult = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Onz.BFuncSagaResult: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("FuncResult=").append(getFuncResult()).append(System.lineSeparator());
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
            var _x_ = getFuncResult();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
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
            setFuncResult(_o_.ReadBinary(_t_));
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
                case 1: _FuncResult = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setFuncResult(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "FuncResult")));
        if (getFuncResult() == null)
            setFuncResult(Zeze.Net.Binary.Empty);
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendBinary(_parents_name_ + "FuncResult", getFuncResult());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "FuncResult", "binary", "", ""));
        return vars;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -3292789589164263566L;

    private Zeze.Net.Binary _FuncResult;

    public Zeze.Net.Binary getFuncResult() {
        return _FuncResult;
    }

    public void setFuncResult(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        _FuncResult = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _FuncResult = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public Data(Zeze.Net.Binary _FuncResult_) {
        if (_FuncResult_ == null)
            _FuncResult_ = Zeze.Net.Binary.Empty;
        _FuncResult = _FuncResult_;
    }

    @Override
    public void reset() {
        _FuncResult = Zeze.Net.Binary.Empty;
    }

    @Override
    public Zeze.Builtin.Onz.BFuncSagaResult toBean() {
        var bean = new Zeze.Builtin.Onz.BFuncSagaResult();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BFuncSagaResult)other);
    }

    public void assign(BFuncSagaResult other) {
        _FuncResult = other.getFuncResult();
    }

    public void assign(BFuncSagaResult.Data other) {
        _FuncResult = other._FuncResult;
    }

    @Override
    public BFuncSagaResult.Data copy() {
        var copy = new BFuncSagaResult.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BFuncSagaResult.Data a, BFuncSagaResult.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BFuncSagaResult.Data clone() {
        return (BFuncSagaResult.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Onz.BFuncSagaResult: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("FuncResult=").append(_FuncResult).append(System.lineSeparator());
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
            var _x_ = _FuncResult;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
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
            _FuncResult = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
