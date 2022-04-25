// auto-generated @formatter:off
package Zeze.Builtin.AutoKey;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BAutoKey extends Zeze.Transaction.Bean {
    private long _NextId;

    public long getNextId() {
        if (!isManaged())
            return _NextId;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _NextId;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__NextId)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.getValue() : _NextId;
    }

    public void setNextId(long value) {
        if (!isManaged()) {
            _NextId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__NextId(this, value));
    }

    public BAutoKey() {
         this(0);
    }

    public BAutoKey(int _varId_) {
        super(_varId_);
    }

    public void Assign(BAutoKey other) {
        setNextId(other.getNextId());
    }

    public BAutoKey CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BAutoKey Copy() {
        var copy = new BAutoKey();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BAutoKey a, BAutoKey b) {
        BAutoKey save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public static final long TYPEID = 3694349315876280858L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private static final class Log__NextId extends Zeze.Transaction.Log1<BAutoKey, Long> {
        public Log__NextId(BAutoKey self, Long value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 1; }
        @Override
        public void Commit() { this.getBeanTyped()._NextId = this.getValue(); }
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.AutoKey.BAutoKey: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("NextId").append('=').append(getNextId()).append(System.lineSeparator());
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
            long _x_ = getNextId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void Decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setNextId(_o_.ReadLong(_t_));
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
    public boolean NegativeCheck() {
        if (getNextId() < 0)
            return true;
        return false;
    }
}
