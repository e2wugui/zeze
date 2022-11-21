// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BIndex extends Zeze.Transaction.Bean implements BIndexReadOnly {
    public static final long TYPEID = 8921847554177605341L;

    private int _ServerId;
    private long _NodeId;

    @Override
    public int getServerId() {
        if (!isManaged())
            return _ServerId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ServerId;
        var log = (Log__ServerId)txn.getLog(objectId() + 1);
        return log != null ? log.value : _ServerId;
    }

    public void setServerId(int value) {
        if (!isManaged()) {
            _ServerId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ServerId(this, 1, value));
    }

    @Override
    public long getNodeId() {
        if (!isManaged())
            return _NodeId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _NodeId;
        var log = (Log__NodeId)txn.getLog(objectId() + 2);
        return log != null ? log.value : _NodeId;
    }

    public void setNodeId(long value) {
        if (!isManaged()) {
            _NodeId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__NodeId(this, 2, value));
    }

    @SuppressWarnings("deprecation")
    public BIndex() {
    }

    @SuppressWarnings("deprecation")
    public BIndex(int _ServerId_, long _NodeId_) {
        _ServerId = _ServerId_;
        _NodeId = _NodeId_;
    }

    public void assign(BIndex other) {
        setServerId(other.getServerId());
        setNodeId(other.getNodeId());
    }

    @Deprecated
    public void Assign(BIndex other) {
        assign(other);
    }

    public BIndex copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BIndex copy() {
        var copy = new BIndex();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BIndex Copy() {
        return copy();
    }

    public static void swap(BIndex a, BIndex b) {
        BIndex save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ServerId extends Zeze.Transaction.Logs.LogInt {
        public Log__ServerId(BIndex bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BIndex)getBelong())._ServerId = value; }
    }

    private static final class Log__NodeId extends Zeze.Transaction.Logs.LogLong {
        public Log__NodeId(BIndex bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BIndex)getBelong())._NodeId = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Timer.BIndex: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ServerId").append('=').append(getServerId()).append(',').append(System.lineSeparator());
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
            int _x_ = getServerId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            long _x_ = getNodeId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
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
            setServerId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
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
        if (getServerId() < 0)
            return true;
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
                case 1: _ServerId = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 2: _NodeId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
            }
        }
    }
}
