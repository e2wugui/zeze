// auto-generated @formatter:off
package Zeze.Builtin.ServiceManagerWithRaft;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BOfflineNotify extends Zeze.Transaction.Bean implements BOfflineNotifyReadOnly {
    public static final long TYPEID = -4045243476564673651L;

    private int _ServerId;
    private String _NotifyId;
    private long _NotifySerialId;
    private Zeze.Net.Binary _NotifyContext;

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
    public String getNotifyId() {
        if (!isManaged())
            return _NotifyId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _NotifyId;
        var log = (Log__NotifyId)txn.getLog(objectId() + 2);
        return log != null ? log.value : _NotifyId;
    }

    public void setNotifyId(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _NotifyId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__NotifyId(this, 2, value));
    }

    @Override
    public long getNotifySerialId() {
        if (!isManaged())
            return _NotifySerialId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _NotifySerialId;
        var log = (Log__NotifySerialId)txn.getLog(objectId() + 3);
        return log != null ? log.value : _NotifySerialId;
    }

    public void setNotifySerialId(long value) {
        if (!isManaged()) {
            _NotifySerialId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__NotifySerialId(this, 3, value));
    }

    @Override
    public Zeze.Net.Binary getNotifyContext() {
        if (!isManaged())
            return _NotifyContext;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _NotifyContext;
        var log = (Log__NotifyContext)txn.getLog(objectId() + 4);
        return log != null ? log.value : _NotifyContext;
    }

    public void setNotifyContext(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _NotifyContext = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__NotifyContext(this, 4, value));
    }

    @SuppressWarnings("deprecation")
    public BOfflineNotify() {
        _NotifyId = "";
        _NotifyContext = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BOfflineNotify(int _ServerId_, String _NotifyId_, long _NotifySerialId_, Zeze.Net.Binary _NotifyContext_) {
        _ServerId = _ServerId_;
        if (_NotifyId_ == null)
            throw new IllegalArgumentException();
        _NotifyId = _NotifyId_;
        _NotifySerialId = _NotifySerialId_;
        if (_NotifyContext_ == null)
            throw new IllegalArgumentException();
        _NotifyContext = _NotifyContext_;
    }

    public void assign(BOfflineNotify other) {
        setServerId(other.getServerId());
        setNotifyId(other.getNotifyId());
        setNotifySerialId(other.getNotifySerialId());
        setNotifyContext(other.getNotifyContext());
    }

    @Deprecated
    public void Assign(BOfflineNotify other) {
        assign(other);
    }

    public BOfflineNotify copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BOfflineNotify copy() {
        var copy = new BOfflineNotify();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BOfflineNotify Copy() {
        return copy();
    }

    public static void swap(BOfflineNotify a, BOfflineNotify b) {
        BOfflineNotify save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ServerId extends Zeze.Transaction.Logs.LogInt {
        public Log__ServerId(BOfflineNotify bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BOfflineNotify)getBelong())._ServerId = value; }
    }

    private static final class Log__NotifyId extends Zeze.Transaction.Logs.LogString {
        public Log__NotifyId(BOfflineNotify bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BOfflineNotify)getBelong())._NotifyId = value; }
    }

    private static final class Log__NotifySerialId extends Zeze.Transaction.Logs.LogLong {
        public Log__NotifySerialId(BOfflineNotify bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BOfflineNotify)getBelong())._NotifySerialId = value; }
    }

    private static final class Log__NotifyContext extends Zeze.Transaction.Logs.LogBinary {
        public Log__NotifyContext(BOfflineNotify bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BOfflineNotify)getBelong())._NotifyContext = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.ServiceManagerWithRaft.BOfflineNotify: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ServerId=").append(getServerId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("NotifyId=").append(getNotifyId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("NotifySerialId=").append(getNotifySerialId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("NotifyContext=").append(getNotifyContext()).append(System.lineSeparator());
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
            String _x_ = getNotifyId();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            long _x_ = getNotifySerialId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = getNotifyContext();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
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
            setNotifyId(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setNotifySerialId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setNotifyContext(_o_.ReadBinary(_t_));
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
        if (getNotifySerialId() < 0)
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
                case 2: _NotifyId = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 3: _NotifySerialId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 4: _NotifyContext = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
            }
        }
    }
}
