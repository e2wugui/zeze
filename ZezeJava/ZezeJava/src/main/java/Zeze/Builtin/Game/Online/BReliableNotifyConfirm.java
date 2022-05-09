// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BReliableNotifyConfirm extends Zeze.Transaction.Bean {
    private long _ReliableNotifyConfirmCount;

    public long getReliableNotifyConfirmCount() {
        if (!isManaged())
            return _ReliableNotifyConfirmCount;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _ReliableNotifyConfirmCount;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ReliableNotifyConfirmCount)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.getValue() : _ReliableNotifyConfirmCount;
    }

    public void setReliableNotifyConfirmCount(long value) {
        if (!isManaged()) {
            _ReliableNotifyConfirmCount = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__ReliableNotifyConfirmCount(this, 1, value));
    }

    public BReliableNotifyConfirm() {
         this(0);
    }

    public BReliableNotifyConfirm(int _varId_) {
        super(_varId_);
    }

    public void Assign(BReliableNotifyConfirm other) {
        setReliableNotifyConfirmCount(other.getReliableNotifyConfirmCount());
    }

    public BReliableNotifyConfirm CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BReliableNotifyConfirm Copy() {
        var copy = new BReliableNotifyConfirm();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BReliableNotifyConfirm a, BReliableNotifyConfirm b) {
        BReliableNotifyConfirm save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public static final long TYPEID = -6588057877320371892L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private static final class Log__ReliableNotifyConfirmCount extends Zeze.Transaction.Log1<BReliableNotifyConfirm, Long> {
       public Log__ReliableNotifyConfirmCount(BReliableNotifyConfirm bean, int varId, Long value) { super(bean, varId, value); }
        @Override
        public void Commit() { getBeanTyped()._ReliableNotifyConfirmCount = this.getValue(); }
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.Online.BReliableNotifyConfirm: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ReliableNotifyConfirmCount").append('=').append(getReliableNotifyConfirmCount()).append(System.lineSeparator());
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
            long _x_ = getReliableNotifyConfirmCount();
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
            setReliableNotifyConfirmCount(_o_.ReadLong(_t_));
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
        if (getReliableNotifyConfirmCount() < 0)
            return true;
        return false;
    }

    @Override
    public void FollowerApply(Zeze.Transaction.Log log) {
        var vars = ((Zeze.Transaction.Collections.LogBean)log).getVariables();
        if (vars == null)
            return;
        for (var it = vars.iterator(); it.moveToNext(); ) {
            var vlog = it.value();
            switch (vlog.getVariableId()) {
                case 1: _ReliableNotifyConfirmCount = ((Zeze.Transaction.Logs.LogLong)vlog).Value; break;
            }
        }
    }
}
