// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BTaskPhaseCommitNPCTalk extends Zeze.Transaction.Bean implements BTaskPhaseCommitNPCTalkReadOnly {
    public static final long TYPEID = 4346965359965455652L;

    private long _npcId; // 交任务的NPC

    @Override
    public long getNpcId() {
        if (!isManaged())
            return _npcId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _npcId;
        var log = (Log__npcId)txn.getLog(objectId() + 1);
        return log != null ? log.value : _npcId;
    }

    public void setNpcId(long value) {
        if (!isManaged()) {
            _npcId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__npcId(this, 1, value));
    }

    @SuppressWarnings("deprecation")
    public BTaskPhaseCommitNPCTalk() {
    }

    @SuppressWarnings("deprecation")
    public BTaskPhaseCommitNPCTalk(long _npcId_) {
        _npcId = _npcId_;
    }

    public void assign(BTaskPhaseCommitNPCTalk other) {
        setNpcId(other.getNpcId());
    }

    @Deprecated
    public void Assign(BTaskPhaseCommitNPCTalk other) {
        assign(other);
    }

    public BTaskPhaseCommitNPCTalk copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTaskPhaseCommitNPCTalk copy() {
        var copy = new BTaskPhaseCommitNPCTalk();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BTaskPhaseCommitNPCTalk Copy() {
        return copy();
    }

    public static void swap(BTaskPhaseCommitNPCTalk a, BTaskPhaseCommitNPCTalk b) {
        BTaskPhaseCommitNPCTalk save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__npcId extends Zeze.Transaction.Logs.LogLong {
        public Log__npcId(BTaskPhaseCommitNPCTalk bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTaskPhaseCommitNPCTalk)getBelong())._npcId = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.TaskBase.BTaskPhaseCommitNPCTalk: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("npcId=").append(getNpcId()).append(System.lineSeparator());
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
            long _x_ = getNpcId();
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
            setNpcId(_o_.ReadLong(_t_));
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
        if (getNpcId() < 0)
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
                case 1: _npcId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
            }
        }
    }
}
