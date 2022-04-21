// auto-generated @formatter:off
package Zeze.Builtin.DelayRemove;

import Zeze.Serialize.ByteBuffer;

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
        return log != null ? log.getValue() : _TableName;
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
        txn.PutLog(new Log__TableName(this, value));
    }

    public Zeze.Net.Binary getEncodedKey() {
        if (!isManaged())
            return _EncodedKey;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _EncodedKey;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__EncodedKey)txn.GetLog(this.getObjectId() + 2);
        return log != null ? log.getValue() : _EncodedKey;
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
        txn.PutLog(new Log__EncodedKey(this, value));
    }

    public BTableKey() {
         this(0);
    }

    public BTableKey(int _varId_) {
        super(_varId_);
        _TableName = "";
        _EncodedKey = Zeze.Net.Binary.Empty;
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

    private static final class Log__TableName extends Zeze.Transaction.Log1<BTableKey, String> {
        public Log__TableName(BTableKey self, String value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 1; }
        @Override
        public void Commit() { this.getBeanTyped()._TableName = this.getValue(); }
    }

    private static final class Log__EncodedKey extends Zeze.Transaction.Log1<BTableKey, Zeze.Net.Binary> {
        public Log__EncodedKey(BTableKey self, Zeze.Net.Binary value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 2; }
        @Override
        public void Commit() { this.getBeanTyped()._EncodedKey = this.getValue(); }
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

    @SuppressWarnings("UnusedAssignment")
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

    @SuppressWarnings("UnusedAssignment")
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

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean NegativeCheck() {
        return false;
    }
}
