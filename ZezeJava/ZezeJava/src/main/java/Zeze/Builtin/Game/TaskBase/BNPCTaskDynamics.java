// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

import Zeze.Serialize.ByteBuffer;

// NPCTask
@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BNPCTaskDynamics extends Zeze.Transaction.Bean implements BNPCTaskDynamicsReadOnly {
    public static final long TYPEID = 1525363162945047522L;

    private long _ReceiveNpcId; // 接任务的NPC
    private long _SubmitNpcId; // 交任务的NPC

    @Override
    public long getReceiveNpcId() {
        if (!isManaged())
            return _ReceiveNpcId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ReceiveNpcId;
        var log = (Log__ReceiveNpcId)txn.getLog(objectId() + 1);
        return log != null ? log.value : _ReceiveNpcId;
    }

    public void setReceiveNpcId(long value) {
        if (!isManaged()) {
            _ReceiveNpcId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ReceiveNpcId(this, 1, value));
    }

    @Override
    public long getSubmitNpcId() {
        if (!isManaged())
            return _SubmitNpcId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _SubmitNpcId;
        var log = (Log__SubmitNpcId)txn.getLog(objectId() + 2);
        return log != null ? log.value : _SubmitNpcId;
    }

    public void setSubmitNpcId(long value) {
        if (!isManaged()) {
            _SubmitNpcId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__SubmitNpcId(this, 2, value));
    }

    @SuppressWarnings("deprecation")
    public BNPCTaskDynamics() {
    }

    @SuppressWarnings("deprecation")
    public BNPCTaskDynamics(long _ReceiveNpcId_, long _SubmitNpcId_) {
        _ReceiveNpcId = _ReceiveNpcId_;
        _SubmitNpcId = _SubmitNpcId_;
    }

    public void assign(BNPCTaskDynamics other) {
        setReceiveNpcId(other.getReceiveNpcId());
        setSubmitNpcId(other.getSubmitNpcId());
    }

    @Deprecated
    public void Assign(BNPCTaskDynamics other) {
        assign(other);
    }

    public BNPCTaskDynamics copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BNPCTaskDynamics copy() {
        var copy = new BNPCTaskDynamics();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BNPCTaskDynamics Copy() {
        return copy();
    }

    public static void swap(BNPCTaskDynamics a, BNPCTaskDynamics b) {
        BNPCTaskDynamics save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ReceiveNpcId extends Zeze.Transaction.Logs.LogLong {
        public Log__ReceiveNpcId(BNPCTaskDynamics bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BNPCTaskDynamics)getBelong())._ReceiveNpcId = value; }
    }

    private static final class Log__SubmitNpcId extends Zeze.Transaction.Logs.LogLong {
        public Log__SubmitNpcId(BNPCTaskDynamics bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BNPCTaskDynamics)getBelong())._SubmitNpcId = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.TaskBase.BNPCTaskDynamics: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ReceiveNpcId=").append(getReceiveNpcId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("SubmitNpcId=").append(getSubmitNpcId()).append(System.lineSeparator());
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
            long _x_ = getReceiveNpcId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getSubmitNpcId();
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
            setReceiveNpcId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setSubmitNpcId(_o_.ReadLong(_t_));
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
        if (getReceiveNpcId() < 0)
            return true;
        if (getSubmitNpcId() < 0)
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
                case 1: _ReceiveNpcId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 2: _SubmitNpcId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
            }
        }
    }
}
