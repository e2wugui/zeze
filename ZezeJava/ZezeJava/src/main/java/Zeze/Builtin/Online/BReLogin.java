// auto-generated @formatter:off
package Zeze.Builtin.Online;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BReLogin extends Zeze.Transaction.Bean {
    private String _ClientId;
    private long _ReliableNotifyConfirmIndex;

    public String getClientId() {
        if (!isManaged())
            return _ClientId;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _ClientId;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ClientId)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.getValue() : _ClientId;
    }

    public void setClientId(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ClientId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__ClientId(this, 1, value));
    }

    public long getReliableNotifyConfirmIndex() {
        if (!isManaged())
            return _ReliableNotifyConfirmIndex;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _ReliableNotifyConfirmIndex;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ReliableNotifyConfirmIndex)txn.GetLog(this.getObjectId() + 2);
        return log != null ? log.getValue() : _ReliableNotifyConfirmIndex;
    }

    public void setReliableNotifyConfirmIndex(long value) {
        if (!isManaged()) {
            _ReliableNotifyConfirmIndex = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__ReliableNotifyConfirmIndex(this, 2, value));
    }

    public BReLogin() {
         this(0);
    }

    public BReLogin(int _varId_) {
        super(_varId_);
        _ClientId = "";
    }

    public void Assign(BReLogin other) {
        setClientId(other.getClientId());
        setReliableNotifyConfirmIndex(other.getReliableNotifyConfirmIndex());
    }

    public BReLogin CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BReLogin Copy() {
        var copy = new BReLogin();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BReLogin a, BReLogin b) {
        BReLogin save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public static final long TYPEID = -603574147996514517L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private static final class Log__ClientId extends Zeze.Transaction.Log1<BReLogin, String> {
       public Log__ClientId(BReLogin bean, int varId, String value) { super(bean, varId, value); }
        @Override
        public void Commit() { getBeanTyped()._ClientId = this.getValue(); }
    }

    private static final class Log__ReliableNotifyConfirmIndex extends Zeze.Transaction.Log1<BReLogin, Long> {
       public Log__ReliableNotifyConfirmIndex(BReLogin bean, int varId, Long value) { super(bean, varId, value); }
        @Override
        public void Commit() { getBeanTyped()._ReliableNotifyConfirmIndex = this.getValue(); }
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Online.BReLogin: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ClientId").append('=').append(getClientId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ReliableNotifyConfirmIndex").append('=').append(getReliableNotifyConfirmIndex()).append(System.lineSeparator());
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
            String _x_ = getClientId();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            long _x_ = getReliableNotifyConfirmIndex();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
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
            setClientId(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setReliableNotifyConfirmIndex(_o_.ReadLong(_t_));
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
                case 1: _ClientId = ((Zeze.Transaction.Logs.LogString)vlog).Value; break;
                case 2: _ReliableNotifyConfirmIndex = ((Zeze.Transaction.Logs.LogLong)vlog).Value; break;
            }
        }
    }
}
