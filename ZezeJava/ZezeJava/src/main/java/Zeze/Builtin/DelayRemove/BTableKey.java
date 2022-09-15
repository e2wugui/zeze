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
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _TableName;
        var log = (Log__TableName)txn.GetLog(objectId() + 1);
        return log != null ? log.Value : _TableName;
    }

    public void setTableName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _TableName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.PutLog(new Log__TableName(this, 1, value));
    }

    public Zeze.Net.Binary getEncodedKey() {
        if (!isManaged())
            return _EncodedKey;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _EncodedKey;
        var log = (Log__EncodedKey)txn.GetLog(objectId() + 2);
        return log != null ? log.Value : _EncodedKey;
    }

    public void setEncodedKey(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _EncodedKey = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.PutLog(new Log__EncodedKey(this, 2, value));
    }

    @SuppressWarnings("deprecation")
    public BTableKey() {
        _TableName = "";
        _EncodedKey = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BTableKey(String _TableName_, Zeze.Net.Binary _EncodedKey_) {
        if (_TableName_ == null)
            throw new IllegalArgumentException();
        _TableName = _TableName_;
        if (_EncodedKey_ == null)
            throw new IllegalArgumentException();
        _EncodedKey = _EncodedKey_;
    }

    public void assign(BTableKey other) {
        setTableName(other.getTableName());
        setEncodedKey(other.getEncodedKey());
    }

    @Deprecated
    public void Assign(BTableKey other) {
        assign(other);
    }

    public BTableKey copyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BTableKey copy() {
        var copy = new BTableKey();
        copy.Assign(this);
        return copy;
    }

    @Deprecated
    public BTableKey Copy() {
        return copy();
    }

    public static void swap(BTableKey a, BTableKey b) {
        BTableKey save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public BTableKey copyBean() {
        return Copy();
    }

    public static final long TYPEID = 6060766480176216446L;

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__TableName extends Zeze.Transaction.Logs.LogString {
        public Log__TableName(BTableKey bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTableKey)getBelong())._TableName = Value; }
    }

    private static final class Log__EncodedKey extends Zeze.Transaction.Logs.LogBinary {
        public Log__EncodedKey(BTableKey bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTableKey)getBelong())._EncodedKey = Value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.DelayRemove.BTableKey: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("TableName").append('=').append(getTableName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("EncodedKey").append('=').append(getEncodedKey()).append(System.lineSeparator());
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
    public void decode(ByteBuffer _o_) {
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
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
    }

    @Override
    protected void resetChildrenRootInfo() {
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
                case 1: _TableName = ((Zeze.Transaction.Logs.LogString)vlog).Value; break;
                case 2: _EncodedKey = ((Zeze.Transaction.Logs.LogBinary)vlog).Value; break;
            }
        }
    }
}
