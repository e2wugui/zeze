// auto-generated @formatter:off
package Zeze.Beans.GlobalCacheManagerWithRaft;

import Zeze.Serialize.ByteBuffer;

public final class AcquireParam extends Zeze.Transaction.Bean {
    private Zeze.Beans.GlobalCacheManagerWithRaft.GlobalTableKey _GlobalTableKey;
    private int _State;

    public Zeze.Beans.GlobalCacheManagerWithRaft.GlobalTableKey getGlobalTableKey() {
        if (!isManaged())
            return _GlobalTableKey;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _GlobalTableKey;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__GlobalTableKey)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.getValue() : _GlobalTableKey;
    }

    public void setGlobalTableKey(Zeze.Beans.GlobalCacheManagerWithRaft.GlobalTableKey value) {
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

    public AcquireParam() {
         this(0);
    }

    public AcquireParam(int _varId_) {
        super(_varId_);
        _GlobalTableKey = new Zeze.Beans.GlobalCacheManagerWithRaft.GlobalTableKey();
    }

    public void Assign(AcquireParam other) {
        setGlobalTableKey(other.getGlobalTableKey());
        setState(other.getState());
    }

    public AcquireParam CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public AcquireParam Copy() {
        var copy = new AcquireParam();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(AcquireParam a, AcquireParam b) {
        AcquireParam save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public static final long TYPEID = 3400241613738213030L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private static final class Log__GlobalTableKey extends Zeze.Transaction.Log1<AcquireParam, Zeze.Beans.GlobalCacheManagerWithRaft.GlobalTableKey> {
        public Log__GlobalTableKey(AcquireParam self, Zeze.Beans.GlobalCacheManagerWithRaft.GlobalTableKey value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 1; }
        @Override
        public void Commit() { this.getBeanTyped()._GlobalTableKey = this.getValue(); }
    }

    private static final class Log__State extends Zeze.Transaction.Log1<AcquireParam, Integer> {
        public Log__State(AcquireParam self, Integer value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 2; }
        @Override
        public void Commit() { this.getBeanTyped()._State = this.getValue(); }
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Beans.GlobalCacheManagerWithRaft.AcquireParam: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("GlobalTableKey").append('=').append(System.lineSeparator());
        getGlobalTableKey().BuildString(sb, level + 4);
        sb.append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("State").append('=').append(getState()).append(System.lineSeparator());
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
        _o_.WriteByte(0);
    }

    @SuppressWarnings("UnusedAssignment")
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
        if (getState() < 0)
            return true;
        return false;
    }
}
