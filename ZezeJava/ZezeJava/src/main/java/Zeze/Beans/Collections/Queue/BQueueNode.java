// auto-generated @formatter:off
package Zeze.Beans.Collections.Queue;

import Zeze.Serialize.ByteBuffer;

public final class BQueueNode extends Zeze.Transaction.Bean {
    private long _NextNodeId; // 后一个节点ID. 0表示已到达结尾。
    private final Zeze.Transaction.Collections.PList2<Zeze.Beans.Collections.Queue.BQueueNodeValue> _Values;

    public long getNextNodeId() {
        if (!isManaged())
            return _NextNodeId;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _NextNodeId;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__NextNodeId)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.getValue() : _NextNodeId;
    }

    public void setNextNodeId(long value) {
        if (!isManaged()) {
            _NextNodeId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__NextNodeId(this, value));
    }

    public Zeze.Transaction.Collections.PList2<Zeze.Beans.Collections.Queue.BQueueNodeValue> getValues() {
        return _Values;
    }

    public BQueueNode() {
         this(0);
    }

    public BQueueNode(int _varId_) {
        super(_varId_);
        _Values = new Zeze.Transaction.Collections.PList2<>(getObjectId() + 2, (_v) -> new Log__Values(this, _v));
    }

    public void Assign(BQueueNode other) {
        setNextNodeId(other.getNextNodeId());
        getValues().clear();
        for (var e : other.getValues())
            getValues().add(e.Copy());
    }

    public BQueueNode CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BQueueNode Copy() {
        var copy = new BQueueNode();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BQueueNode a, BQueueNode b) {
        BQueueNode save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public static final long TYPEID = 1802280259208077231L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private static final class Log__NextNodeId extends Zeze.Transaction.Log1<BQueueNode, Long> {
        public Log__NextNodeId(BQueueNode self, Long value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 1; }
        @Override
        public void Commit() { this.getBeanTyped()._NextNodeId = this.getValue(); }
    }

    private static final class Log__Values extends Zeze.Transaction.Collections.PList.LogV<Zeze.Beans.Collections.Queue.BQueueNodeValue> {
        public Log__Values(BQueueNode host, org.pcollections.PVector<Zeze.Beans.Collections.Queue.BQueueNodeValue> value) { super(host, value); }
        @Override
        public long getLogKey() { return getBean().getObjectId() + 2; }
        public BQueueNode getBeanTyped() { return (BQueueNode)getBean(); }
        @Override
        public void Commit() { Commit(getBeanTyped()._Values); }
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Beans.Collections.Queue.BQueueNode: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("NextNodeId").append('=').append(getNextNodeId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Values").append("=[").append(System.lineSeparator());
        level += 4;
        for (var _item_ : getValues()) {
            sb.append(Zeze.Util.Str.indent(level)).append("Item").append('=').append(System.lineSeparator());
            _item_.BuildString(sb, level + 4);
            sb.append(',').append(System.lineSeparator());
        }
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append(']').append(System.lineSeparator());
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
            long _x_ = getNextNodeId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = getValues();
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BEAN);
                for (var _v_ : _x_)
                    _v_.Encode(_o_);
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
            setNextNodeId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = getValues();
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new Zeze.Beans.Collections.Queue.BQueueNodeValue(), _t_));
            } else
                _o_.SkipUnknownField(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Values.InitRootInfo(root, this);
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean NegativeCheck() {
        if (getNextNodeId() < 0)
            return true;
        for (var _v_ : getValues()) {
            if (_v_.NegativeCheck())
                return true;
        }
        return false;
    }
}
