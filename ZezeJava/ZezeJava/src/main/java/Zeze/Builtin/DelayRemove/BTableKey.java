// auto-generated @formatter:off
package Zeze.Builtin.DelayRemove;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BTableKey extends Zeze.Transaction.Bean {
    private String _TableName;
    private Zeze.Net.Binary _EncodedKey;

    public String getTableName() {
        if (!isManaged())
            return _TableName;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _TableName;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__TableName)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.Value : _TableName;
    }

    public void setTableName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _TableName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__TableName(this, 1, value));
    }

    public Zeze.Net.Binary getEncodedKey() {
        if (!isManaged())
            return _EncodedKey;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _EncodedKey;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__EncodedKey)txn.GetLog(this.getObjectId() + 2);
        return log != null ? log.Value : _EncodedKey;
    }

    public void setEncodedKey(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _EncodedKey = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__EncodedKey(this, 2, value));
    }

    public BTableKey() {
        _TableName = "";
        _EncodedKey = Zeze.Net.Binary.Empty;
    }

    public BTableKey(String _TableName_, Zeze.Net.Binary _EncodedKey_) {
        _TableName = _TableName_;
        _EncodedKey = _EncodedKey_;
    }

    public void Assign(BTableKey other) {
        setTableName(other.getTableName());
        setEncodedKey(other.getEncodedKey());
    }

    public BTableKey CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BTableKey Copy() {
        var copy = new BTableKey();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BTableKey a, BTableKey b) {
        BTableKey save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public static final long TYPEID = 6060766480176216446L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private static final class Log__TableName extends Zeze.Transaction.Logs.LogString {
        public Log__TableName(BTableKey bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BTableKey)getBelong())._TableName = Value; }
    }

    private static final class Log__EncodedKey extends Zeze.Transaction.Logs.LogBinary {
        public Log__EncodedKey(BTableKey bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BTableKey)getBelong())._EncodedKey = Value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        BuildString(sb, 0);
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    @Override
    public void BuildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.DelayRemove.BTableKey: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("TableName").append('=').append(getTableName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("EncodedKey").append('=').append(getEncodedKey()).append(System.lineSeparator());
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append('}');
    }

    private static int _PRE_ALLOC_SIZE_ = 16;

    @Override
    public int getPreAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void setPreAllocSize(int size) {
        _PRE_ALLOC_SIZE_ = size;
    }

    @Override
    public void Encode(ByteBuffer _o_) {
        int _i_ = 0;
        {
            String _x_ = getTableName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = getEncodedKey();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void Decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setTableName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setEncodedKey(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
    }

    @Override
    protected void ResetChildrenRootInfo() {
    }

    @Override
    public boolean NegativeCheck() {
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void FollowerApply(Zeze.Transaction.Log log) {
        var vars = ((Zeze.Transaction.Collections.LogBean)log).getVariables();
        if (vars == null)
            return;
        for (var it = vars.iterator(); it.moveToNext(); ) {
            var vlog = it.value();
            switch (vlog.getVariableId()) {
                case 1: _TableName = ((Zeze.Transaction.Logs.LogString)vlog).Value; break;
                case 2: _EncodedKey = ((Zeze.Transaction.Logs.LogBinary)vlog).Value; break;
            }
        }
    }
}
