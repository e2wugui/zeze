// auto-generated @formatter:off
package Zeze.Builtin.GlobalCacheManagerWithRaft;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class ReduceParam extends Zeze.Transaction.Bean {
    private Zeze.Builtin.GlobalCacheManagerWithRaft.GlobalTableKey _GlobalTableKey;
    private int _State;
    private long _GlobalSerialId;

    public Zeze.Builtin.GlobalCacheManagerWithRaft.GlobalTableKey getGlobalTableKey() {
        if (!isManaged())
            return _GlobalTableKey;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _GlobalTableKey;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__GlobalTableKey)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.getValue() : _GlobalTableKey;
    }

    public void setGlobalTableKey(Zeze.Builtin.GlobalCacheManagerWithRaft.GlobalTableKey value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _GlobalTableKey = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__GlobalTableKey(this, value));
    }

    public int getState() {
        if (!isManaged())
            return _State;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _State;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__State)txn.GetLog(this.getObjectId() + 2);
        return log != null ? log.getValue() : _State;
    }

    public void setState(int value) {
        if (!isManaged()) {
            _State = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__State(this, value));
    }

    public long getGlobalSerialId() {
        if (!isManaged())
            return _GlobalSerialId;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _GlobalSerialId;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__GlobalSerialId)txn.GetLog(this.getObjectId() + 3);
        return log != null ? log.getValue() : _GlobalSerialId;
    }

    public void setGlobalSerialId(long value) {
        if (!isManaged()) {
            _GlobalSerialId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__GlobalSerialId(this, value));
    }

    public ReduceParam() {
         this(0);
    }

    public ReduceParam(int _varId_) {
        super(_varId_);
        _GlobalTableKey = new Zeze.Builtin.GlobalCacheManagerWithRaft.GlobalTableKey();
    }

    public void Assign(ReduceParam other) {
        setGlobalTableKey(other.getGlobalTableKey());
        setState(other.getState());
        setGlobalSerialId(other.getGlobalSerialId());
    }

    public ReduceParam CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public ReduceParam Copy() {
        var copy = new ReduceParam();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(ReduceParam a, ReduceParam b) {
        ReduceParam save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public static final long TYPEID = -4489915946741208436L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private static final class Log__GlobalTableKey extends Zeze.Transaction.Log1<ReduceParam, Zeze.Builtin.GlobalCacheManagerWithRaft.GlobalTableKey> {
        public Log__GlobalTableKey(ReduceParam self, Zeze.Builtin.GlobalCacheManagerWithRaft.GlobalTableKey value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 1; }
        @Override
        public void Commit() { this.getBeanTyped()._GlobalTableKey = this.getValue(); }
    }

    private static final class Log__State extends Zeze.Transaction.Log1<ReduceParam, Integer> {
        public Log__State(ReduceParam self, Integer value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 2; }
        @Override
        public void Commit() { this.getBeanTyped()._State = this.getValue(); }
    }

    private static final class Log__GlobalSerialId extends Zeze.Transaction.Log1<ReduceParam, Long> {
        public Log__GlobalSerialId(ReduceParam self, Long value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 3; }
        @Override
        public void Commit() { this.getBeanTyped()._GlobalSerialId = this.getValue(); }
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.GlobalCacheManagerWithRaft.ReduceParam: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("GlobalTableKey").append('=').append(System.lineSeparator());
        getGlobalTableKey().BuildString(sb, level + 4);
        sb.append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("State").append('=').append(getState()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("GlobalSerialId").append('=').append(getGlobalSerialId()).append(System.lineSeparator());
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
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 1, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            getGlobalTableKey().Encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        {
            int _x_ = getState();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            long _x_ = getGlobalSerialId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
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
            _o_.ReadBean(getGlobalTableKey(), _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setState(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setGlobalSerialId(_o_.ReadLong(_t_));
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
        if (getState() < 0)
            return true;
        if (getGlobalSerialId() < 0)
            return true;
        return false;
    }
}
