// auto-generated @formatter:off
package Zeze.Builtin.Collections.LinkedMap;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BLinkedMap extends Zeze.Transaction.Bean {
    private long _HeadNodeId;
    private long _TailNodeId;
    private long _Count;
    private long _LastNodeId; // 最近分配过的NodeId, 用于下次分配

    public long getHeadNodeId() {
        if (!isManaged())
            return _HeadNodeId;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _HeadNodeId;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__HeadNodeId)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.Value : _HeadNodeId;
    }

    public void setHeadNodeId(long value) {
        if (!isManaged()) {
            _HeadNodeId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__HeadNodeId(this, 1, value));
    }

    public long getTailNodeId() {
        if (!isManaged())
            return _TailNodeId;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _TailNodeId;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__TailNodeId)txn.GetLog(this.getObjectId() + 2);
        return log != null ? log.Value : _TailNodeId;
    }

    public void setTailNodeId(long value) {
        if (!isManaged()) {
            _TailNodeId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__TailNodeId(this, 2, value));
    }

    public long getCount() {
        if (!isManaged())
            return _Count;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _Count;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__Count)txn.GetLog(this.getObjectId() + 3);
        return log != null ? log.Value : _Count;
    }

    public void setCount(long value) {
        if (!isManaged()) {
            _Count = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__Count(this, 3, value));
    }

    public long getLastNodeId() {
        if (!isManaged())
            return _LastNodeId;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _LastNodeId;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__LastNodeId)txn.GetLog(this.getObjectId() + 4);
        return log != null ? log.Value : _LastNodeId;
    }

    public void setLastNodeId(long value) {
        if (!isManaged()) {
            _LastNodeId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__LastNodeId(this, 4, value));
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
        setCount(other.getCount());
        setLastNodeId(other.getLastNodeId());
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

    public static final long TYPEID = -8443895985300072767L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private static final class Log__HeadNodeId extends Zeze.Transaction.Logs.LogLong {
        public Log__HeadNodeId(BLinkedMap bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BLinkedMap)getBelong())._HeadNodeId = Value; }
    }

    private static final class Log__TailNodeId extends Zeze.Transaction.Logs.LogLong {
        public Log__TailNodeId(BLinkedMap bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BLinkedMap)getBelong())._TailNodeId = Value; }
    }

    private static final class Log__Count extends Zeze.Transaction.Logs.LogLong {
        public Log__Count(BLinkedMap bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BLinkedMap)getBelong())._Count = Value; }
    }

    private static final class Log__LastNodeId extends Zeze.Transaction.Logs.LogLong {
        public Log__LastNodeId(BLinkedMap bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BLinkedMap)getBelong())._LastNodeId = Value; }
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Collections.LinkedMap.BLinkedMap: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("HeadNodeId").append('=').append(getHeadNodeId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("TailNodeId").append('=').append(getTailNodeId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Count").append('=').append(getCount()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("LastNodeId").append('=').append(getLastNodeId()).append(System.lineSeparator());
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
            long _x_ = getCount();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getLastNodeId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
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
            setHeadNodeId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setTailNodeId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setCount(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setLastNodeId(_o_.ReadLong(_t_));
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
        if (getHeadNodeId() < 0)
            return true;
        if (getTailNodeId() < 0)
            return true;
        if (getCount() < 0)
            return true;
        if (getLastNodeId() < 0)
            return true;
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
                case 1: _HeadNodeId = ((Zeze.Transaction.Logs.LogLong)vlog).Value; break;
                case 2: _TailNodeId = ((Zeze.Transaction.Logs.LogLong)vlog).Value; break;
                case 3: _Count = ((Zeze.Transaction.Logs.LogLong)vlog).Value; break;
                case 4: _LastNodeId = ((Zeze.Transaction.Logs.LogLong)vlog).Value; break;
            }
        }
    }
}
