// auto-generated @formatter:off
package Zeze.Builtin.Collections.Queue;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BQueueNode extends Zeze.Transaction.Bean {
    public static final long TYPEID = 400956918018571167L;

    private long _NextNodeId; // 后一个节点ID. 0表示已到达结尾。
    private final Zeze.Transaction.Collections.PList2<Zeze.Builtin.Collections.Queue.BQueueNodeValue> _Values;

    public long getNextNodeId() {
        if (!isManaged())
            return _NextNodeId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _NextNodeId;
        var log = (Log__NextNodeId)txn.getLog(objectId() + 1);
        return log != null ? log.value : _NextNodeId;
    }

    public void setNextNodeId(long value) {
        if (!isManaged()) {
            _NextNodeId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__NextNodeId(this, 1, value));
    }

    public Zeze.Transaction.Collections.PList2<Zeze.Builtin.Collections.Queue.BQueueNodeValue> getValues() {
        return _Values;
    }

    @SuppressWarnings("deprecation")
    public BQueueNode() {
        _Values = new Zeze.Transaction.Collections.PList2<>(Zeze.Builtin.Collections.Queue.BQueueNodeValue.class);
        _Values.variableId(2);
    }

    @SuppressWarnings("deprecation")
    public BQueueNode(long _NextNodeId_) {
        _NextNodeId = _NextNodeId_;
        _Values = new Zeze.Transaction.Collections.PList2<>(Zeze.Builtin.Collections.Queue.BQueueNodeValue.class);
        _Values.variableId(2);
    }

    public void assign(BQueueNode other) {
        setNextNodeId(other.getNextNodeId());
        getValues().clear();
        for (var e : other.getValues())
            getValues().add(e.copy());
    }

    @Deprecated
    public void Assign(BQueueNode other) {
        assign(other);
    }

    public BQueueNode copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BQueueNode copy() {
        var copy = new BQueueNode();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BQueueNode Copy() {
        return copy();
    }

    public static void swap(BQueueNode a, BQueueNode b) {
        BQueueNode save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__NextNodeId extends Zeze.Transaction.Logs.LogLong {
        public Log__NextNodeId(BQueueNode bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BQueueNode)getBelong())._NextNodeId = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Collections.Queue.BQueueNode: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("NextNodeId").append('=').append(getNextNodeId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Values").append("=[").append(System.lineSeparator());
        level += 4;
        for (var _item_ : getValues()) {
            sb.append(Zeze.Util.Str.indent(level)).append("Item").append('=').append(System.lineSeparator());
            _item_.buildString(sb, level + 4);
            sb.append(',').append(System.lineSeparator());
        }
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append(']').append(System.lineSeparator());
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
                    _v_.encode(_o_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
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
                    _x_.add(_o_.ReadBean(new Zeze.Builtin.Collections.Queue.BQueueNodeValue(), _t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Values.initRootInfo(root, this);
    }

    @Override
    protected void resetChildrenRootInfo() {
        _Values.resetRootInfo();
    }

    @Override
    public boolean negativeCheck() {
        if (getNextNodeId() < 0)
            return true;
        for (var _v_ : getValues()) {
            if (_v_.negativeCheck())
                return true;
        }
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
                case 1: _NextNodeId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 2: _Values.followerApply(vlog); break;
            }
        }
    }
}
