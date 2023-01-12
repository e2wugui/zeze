// auto-generated @formatter:off
package Zeze.Builtin.Online;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BReliableNotifyConfirm extends Zeze.Transaction.Bean implements BReliableNotifyConfirmReadOnly {
    public static final long TYPEID = 7657736965823286884L;

    private String _ClientId;
    private long _ReliableNotifyConfirmIndex;
    private boolean _Sync;

    @Override
    public String getClientId() {
        if (!isManaged())
            return _ClientId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ClientId;
        var log = (Log__ClientId)txn.getLog(objectId() + 1);
        return log != null ? log.value : _ClientId;
    }

    public void setClientId(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ClientId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ClientId(this, 1, value));
    }

    @Override
    public long getReliableNotifyConfirmIndex() {
        if (!isManaged())
            return _ReliableNotifyConfirmIndex;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ReliableNotifyConfirmIndex;
        var log = (Log__ReliableNotifyConfirmIndex)txn.getLog(objectId() + 2);
        return log != null ? log.value : _ReliableNotifyConfirmIndex;
    }

    public void setReliableNotifyConfirmIndex(long value) {
        if (!isManaged()) {
            _ReliableNotifyConfirmIndex = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ReliableNotifyConfirmIndex(this, 2, value));
    }

    @Override
    public boolean isSync() {
        if (!isManaged())
            return _Sync;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Sync;
        var log = (Log__Sync)txn.getLog(objectId() + 3);
        return log != null ? log.value : _Sync;
    }

    public void setSync(boolean value) {
        if (!isManaged()) {
            _Sync = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Sync(this, 3, value));
    }

    @SuppressWarnings("deprecation")
    public BReliableNotifyConfirm() {
        _ClientId = "";
    }

    @SuppressWarnings("deprecation")
    public BReliableNotifyConfirm(String _ClientId_, long _ReliableNotifyConfirmIndex_, boolean _Sync_) {
        if (_ClientId_ == null)
            throw new IllegalArgumentException();
        _ClientId = _ClientId_;
        _ReliableNotifyConfirmIndex = _ReliableNotifyConfirmIndex_;
        _Sync = _Sync_;
    }

    public void assign(BReliableNotifyConfirm other) {
        setClientId(other.getClientId());
        setReliableNotifyConfirmIndex(other.getReliableNotifyConfirmIndex());
        setSync(other.isSync());
    }

    @Deprecated
    public void Assign(BReliableNotifyConfirm other) {
        assign(other);
    }

    public BReliableNotifyConfirm copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BReliableNotifyConfirm copy() {
        var copy = new BReliableNotifyConfirm();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BReliableNotifyConfirm Copy() {
        return copy();
    }

    public static void swap(BReliableNotifyConfirm a, BReliableNotifyConfirm b) {
        BReliableNotifyConfirm save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ClientId extends Zeze.Transaction.Logs.LogString {
        public Log__ClientId(BReliableNotifyConfirm bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BReliableNotifyConfirm)getBelong())._ClientId = value; }
    }

    private static final class Log__ReliableNotifyConfirmIndex extends Zeze.Transaction.Logs.LogLong {
        public Log__ReliableNotifyConfirmIndex(BReliableNotifyConfirm bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BReliableNotifyConfirm)getBelong())._ReliableNotifyConfirmIndex = value; }
    }

    private static final class Log__Sync extends Zeze.Transaction.Logs.LogBool {
        public Log__Sync(BReliableNotifyConfirm bean, int varId, boolean value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BReliableNotifyConfirm)getBelong())._Sync = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Online.BReliableNotifyConfirm: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ClientId=").append(getClientId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ReliableNotifyConfirmIndex=").append(getReliableNotifyConfirmIndex()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Sync=").append(isSync()).append(System.lineSeparator());
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
        {
            boolean _x_ = isSync();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
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
        if (_i_ == 3) {
            setSync(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    public boolean negativeCheck() {
        if (getReliableNotifyConfirmIndex() < 0)
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
                case 1: _ClientId = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 2: _ReliableNotifyConfirmIndex = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 3: _Sync = ((Zeze.Transaction.Logs.LogBool)vlog).value; break;
            }
        }
    }
}
