// auto-generated @formatter:off
package Zeze.Beans.Collections.LinkedMap;

import Zeze.Serialize.ByteBuffer;

public final class BLinkedMap extends Zeze.Transaction.Bean {
    private long _HeadNodeId;
    private long _TailNodeId;
    private long _FirstNotTopNodeId;
    private long _Count;

    public long getHeadNodeId() {
        if (!isManaged())
            return _HeadNodeId;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _HeadNodeId;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__HeadNodeId)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.getValue() : _HeadNodeId;
    }

    public void setHeadNodeId(long value) {
        if (!isManaged()) {
            _HeadNodeId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__HeadNodeId(this, value));
    }

    public long getTailNodeId() {
        if (!isManaged())
            return _TailNodeId;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _TailNodeId;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__TailNodeId)txn.GetLog(this.getObjectId() + 2);
        return log != null ? log.getValue() : _TailNodeId;
    }

    public void setTailNodeId(long value) {
        if (!isManaged()) {
            _TailNodeId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__TailNodeId(this, value));
    }

    public long getFirstNotTopNodeId() {
        if (!isManaged())
            return _FirstNotTopNodeId;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _FirstNotTopNodeId;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__FirstNotTopNodeId)txn.GetLog(this.getObjectId() + 3);
        return log != null ? log.getValue() : _FirstNotTopNodeId;
    }

    public void setFirstNotTopNodeId(long value) {
        if (!isManaged()) {
            _FirstNotTopNodeId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__FirstNotTopNodeId(this, value));
    }

    public long getCount() {
        if (!isManaged())
            return _Count;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _Count;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__Count)txn.GetLog(this.getObjectId() + 4);
        return log != null ? log.getValue() : _Count;
    }

    public void setCount(long value) {
        if (!isManaged()) {
            _Count = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__Count(this, value));
    }

    public BLinkedMap() {
         this(0);
    }

    public BLinkedMap(int _varId_) {
        super(_varId_);
    }

    public void Assign(BLinkedMap other) {
        setHeadNodeId(other.getHeadNodeId());
        setTailNodeId(other.getTailNodeId());
        setFirstNotTopNodeId(other.getFirstNotTopNodeId());
        setCount(other.getCount());
    }

    public BLinkedMap CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BLinkedMap Copy() {
        var copy = new BLinkedMap();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BLinkedMap a, BLinkedMap b) {
        BLinkedMap save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public static final long TYPEID = -3967858046935728431L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private static final class Log__HeadNodeId extends Zeze.Transaction.Log1<BLinkedMap, Long> {
        public Log__HeadNodeId(BLinkedMap self, Long value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 1; }
        @Override
        public void Commit() { this.getBeanTyped()._HeadNodeId = this.getValue(); }
    }

    private static final class Log__TailNodeId extends Zeze.Transaction.Log1<BLinkedMap, Long> {
        public Log__TailNodeId(BLinkedMap self, Long value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 2; }
        @Override
        public void Commit() { this.getBeanTyped()._TailNodeId = this.getValue(); }
    }

    private static final class Log__FirstNotTopNodeId extends Zeze.Transaction.Log1<BLinkedMap, Long> {
        public Log__FirstNotTopNodeId(BLinkedMap self, Long value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 3; }
        @Override
        public void Commit() { this.getBeanTyped()._FirstNotTopNodeId = this.getValue(); }
    }

    private static final class Log__Count extends Zeze.Transaction.Log1<BLinkedMap, Long> {
        public Log__Count(BLinkedMap self, Long value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 4; }
        @Override
        public void Commit() { this.getBeanTyped()._Count = this.getValue(); }
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Beans.Collections.LinkedMap.BLinkedMap: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("HeadNodeId").append('=').append(getHeadNodeId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("TailNodeId").append('=').append(getTailNodeId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("FirstNotTopNodeId").append('=').append(getFirstNotTopNodeId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Count").append('=').append(getCount()).append(System.lineSeparator());
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
            long _x_ = getHeadNodeId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getTailNodeId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getFirstNotTopNodeId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getCount();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
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
            setHeadNodeId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setTailNodeId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setFirstNotTopNodeId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setCount(_o_.ReadLong(_t_));
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
        if (getHeadNodeId() < 0)
            return true;
        if (getTailNodeId() < 0)
            return true;
        if (getFirstNotTopNodeId() < 0)
            return true;
        if (getCount() < 0)
            return true;
        return false;
    }
}
