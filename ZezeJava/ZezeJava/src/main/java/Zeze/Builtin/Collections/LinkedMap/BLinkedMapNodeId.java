// auto-generated @formatter:off
package Zeze.Builtin.Collections.LinkedMap;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BLinkedMapNodeId extends Zeze.Transaction.Bean {
    private long _NodeId; // KeyValue对所属的节点ID. 每个节点有多个KeyValue对共享

    public long getNodeId() {
        if (!isManaged())
            return _NodeId;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _NodeId;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__NodeId)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.getValue() : _NodeId;
    }

    public void setNodeId(long value) {
        if (!isManaged()) {
            _NodeId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__NodeId(this, 1, value));
    }

    public BLinkedMapNodeId() {
         this(0);
    }

    public BLinkedMapNodeId(int _varId_) {
        super(_varId_);
    }

    public void Assign(BLinkedMapNodeId other) {
        setNodeId(other.getNodeId());
    }

    public BLinkedMapNodeId CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BLinkedMapNodeId Copy() {
        var copy = new BLinkedMapNodeId();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BLinkedMapNodeId a, BLinkedMapNodeId b) {
        BLinkedMapNodeId save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public static final long TYPEID = -6424218657633143196L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private static final class Log__NodeId extends Zeze.Transaction.Log1<BLinkedMapNodeId, Long> {
       public Log__NodeId(BLinkedMapNodeId bean, int varId, Long value) { super(bean, varId, value); }
        @Override
        public void Commit() { getBeanTyped()._NodeId = this.getValue(); }
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeId: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("NodeId").append('=').append(getNodeId()).append(System.lineSeparator());
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
            long _x_ = getNodeId();
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
            setNodeId(_o_.ReadLong(_t_));
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
        if (getNodeId() < 0)
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
                case 1: _NodeId = ((Zeze.Transaction.Logs.LogLong)vlog).Value; break;
            }
        }
    }
}
