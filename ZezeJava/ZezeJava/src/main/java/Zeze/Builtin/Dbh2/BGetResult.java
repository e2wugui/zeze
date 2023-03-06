// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BGetResult extends Zeze.Transaction.Bean implements BGetResultReadOnly {
    public static final long TYPEID = -3248537090181056461L;

    private boolean _Null;
    private Zeze.Net.Binary _Value;

    @Override
    public boolean isNull() {
        if (!isManaged())
            return _Null;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Null;
        var log = (Log__Null)txn.getLog(objectId() + 1);
        return log != null ? log.value : _Null;
    }

    public void setNull(boolean value) {
        if (!isManaged()) {
            _Null = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Null(this, 1, value));
    }

    @Override
    public Zeze.Net.Binary getValue() {
        if (!isManaged())
            return _Value;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Value;
        var log = (Log__Value)txn.getLog(objectId() + 2);
        return log != null ? log.value : _Value;
    }

    public void setValue(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Value = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Value(this, 2, value));
    }

    @SuppressWarnings("deprecation")
    public BGetResult() {
        _Value = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BGetResult(boolean _Null_, Zeze.Net.Binary _Value_) {
        _Null = _Null_;
        if (_Value_ == null)
            throw new IllegalArgumentException();
        _Value = _Value_;
    }

    @Override
    public Zeze.Builtin.Dbh2.BGetResultData toData() {
        var data = new Zeze.Builtin.Dbh2.BGetResultData();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Dbh2.BGetResultData)other);
    }

    public void assign(BGetResultData other) {
        setNull(other.isNull());
        setValue(other.getValue());
    }

    public void assign(BGetResult other) {
        setNull(other.isNull());
        setValue(other.getValue());
    }

    @Deprecated
    public void Assign(BGetResult other) {
        assign(other);
    }

    public BGetResult copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BGetResult copy() {
        var copy = new BGetResult();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BGetResult Copy() {
        return copy();
    }

    public static void swap(BGetResult a, BGetResult b) {
        BGetResult save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Null extends Zeze.Transaction.Logs.LogBool {
        public Log__Null(BGetResult bean, int varId, boolean value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BGetResult)getBelong())._Null = value; }
    }

    private static final class Log__Value extends Zeze.Transaction.Logs.LogBinary {
        public Log__Value(BGetResult bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BGetResult)getBelong())._Value = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.BGetResult: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Null=").append(isNull()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Value=").append(getValue()).append(System.lineSeparator());
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
            boolean _x_ = isNull();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        {
            var _x_ = getValue();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setNull(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setValue(_o_.ReadBinary(_t_));
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
                case 1: _Null = ((Zeze.Transaction.Logs.LogBool)vlog).value; break;
                case 2: _Value = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
            }
        }
    }
}
