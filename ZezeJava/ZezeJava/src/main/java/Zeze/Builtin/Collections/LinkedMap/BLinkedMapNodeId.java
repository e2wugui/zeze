// auto-generated @formatter:off
package Zeze.Builtin.Collections.LinkedMap;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BLinkedMapNodeId extends Zeze.Transaction.Bean {
    private long _NodeId; // KeyValue对所属的节点ID. 每个节点有多个KeyValue对共享

    public long getNodeId() {
        if (!isManaged())
            return _NodeId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _NodeId;
        var log = (Log__NodeId)txn.getLog(objectId() + 1);
        return log != null ? log.value : _NodeId;
    }

    public void setNodeId(long value) {
        if (!isManaged()) {
            _NodeId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__NodeId(this, 1, value));
    }

    @SuppressWarnings("deprecation")
    public BLinkedMapNodeId() {
    }

    @SuppressWarnings("deprecation")
    public BLinkedMapNodeId(long _NodeId_) {
        _NodeId = _NodeId_;
    }

    public void assign(BLinkedMapNodeId other) {
        setNodeId(other.getNodeId());
    }

    @Deprecated
    public void Assign(BLinkedMapNodeId other) {
        assign(other);
    }

    public BLinkedMapNodeId copyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BLinkedMapNodeId copy() {
        var copy = new BLinkedMapNodeId();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BLinkedMapNodeId Copy() {
        return copy();
    }

    public static void swap(BLinkedMapNodeId a, BLinkedMapNodeId b) {
        BLinkedMapNodeId save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public BLinkedMapNodeId copyBean() {
        return Copy();
    }

    public static final long TYPEID = -6424218657633143196L;

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__NodeId extends Zeze.Transaction.Logs.LogLong {
        public Log__NodeId(BLinkedMapNodeId bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BLinkedMapNodeId)getBelong())._NodeId = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeId: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("NodeId").append('=').append(getNodeId()).append(System.lineSeparator());
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
            long _x_ = getNodeId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
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
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
    }

    @Override
    protected void resetChildrenRootInfo() {
    }

    @Override
    public boolean negativeCheck() {
        if (getNodeId() < 0)
            return true;
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
                case 1: _NodeId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
            }
        }
    }
}
