// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BReliableNotifyConfirm extends Zeze.Transaction.Bean {
    private long _ReliableNotifyConfirmIndex;
    private boolean _Sync;

    public long getReliableNotifyConfirmIndex() {
        if (!isManaged())
            return _ReliableNotifyConfirmIndex;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _ReliableNotifyConfirmIndex;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ReliableNotifyConfirmIndex)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.Value : _ReliableNotifyConfirmIndex;
    }

    public void setReliableNotifyConfirmIndex(long value) {
        if (!isManaged()) {
            _ReliableNotifyConfirmIndex = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__ReliableNotifyConfirmIndex(this, 1, value));
    }

    public boolean isSync() {
        if (!isManaged())
            return _Sync;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _Sync;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__Sync)txn.GetLog(this.getObjectId() + 2);
        return log != null ? log.Value : _Sync;
    }

    public void setSync(boolean value) {
        if (!isManaged()) {
            _Sync = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__Sync(this, 2, value));
    }

    public BReliableNotifyConfirm() {
         this(0);
    }

    public BReliableNotifyConfirm(int _varId_) {
        super(_varId_);
    }

    public void Assign(BReliableNotifyConfirm other) {
        setReliableNotifyConfirmIndex(other.getReliableNotifyConfirmIndex());
        setSync(other.isSync());
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

    private static final class Log__ReliableNotifyConfirmIndex extends Zeze.Transaction.Logs.LogLong {
        public Log__ReliableNotifyConfirmIndex(BReliableNotifyConfirm bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BReliableNotifyConfirm)getBelong())._ReliableNotifyConfirmIndex = Value; }
    }

    private static final class Log__Sync extends Zeze.Transaction.Logs.LogBool {
        public Log__Sync(BReliableNotifyConfirm bean, int varId, boolean value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BReliableNotifyConfirm)getBelong())._Sync = Value; }
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
        sb.append(Zeze.Util.Str.indent(level)).append("ReliableNotifyConfirmIndex").append('=').append(getReliableNotifyConfirmIndex()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Sync").append('=').append(isSync()).append(System.lineSeparator());
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
            long _x_ = getReliableNotifyConfirmIndex();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            boolean _x_ = isSync();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void Decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setReliableNotifyConfirmIndex(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setSync(_o_.ReadBool(_t_));
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
        if (getReliableNotifyConfirmIndex() < 0)
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
                case 1: _ReliableNotifyConfirmIndex = ((Zeze.Transaction.Logs.LogLong)vlog).Value; break;
                case 2: _Sync = ((Zeze.Transaction.Logs.LogBool)vlog).Value; break;
            }
        }
    }
}
