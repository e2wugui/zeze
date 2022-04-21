// auto-generated @formatter:off
package Zeze.Builtin.Provider;

import Zeze.Serialize.ByteBuffer;

public final class BBroadcast extends Zeze.Transaction.Bean {
    private long _protocolType;
    private Zeze.Net.Binary _protocolWholeData; // 完整的协议打包，包括了 type, size
    private int _time;
    private long _ConfirmSerialId; // 不为0的时候，linkd发送SendConfirm回逻辑服务器

    public long getProtocolType() {
        if (!isManaged())
            return _protocolType;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _protocolType;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__protocolType)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.getValue() : _protocolType;
    }

    public void setProtocolType(long value) {
        if (!isManaged()) {
            _protocolType = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__protocolType(this, value));
    }

    public Zeze.Net.Binary getProtocolWholeData() {
        if (!isManaged())
            return _protocolWholeData;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _protocolWholeData;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__protocolWholeData)txn.GetLog(this.getObjectId() + 2);
        return log != null ? log.getValue() : _protocolWholeData;
    }

    public void setProtocolWholeData(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _protocolWholeData = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__protocolWholeData(this, value));
    }

    public int getTime() {
        if (!isManaged())
            return _time;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _time;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__time)txn.GetLog(this.getObjectId() + 3);
        return log != null ? log.getValue() : _time;
    }

    public void setTime(int value) {
        if (!isManaged()) {
            _time = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__time(this, value));
    }

    public long getConfirmSerialId() {
        if (!isManaged())
            return _ConfirmSerialId;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _ConfirmSerialId;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ConfirmSerialId)txn.GetLog(this.getObjectId() + 4);
        return log != null ? log.getValue() : _ConfirmSerialId;
    }

    public void setConfirmSerialId(long value) {
        if (!isManaged()) {
            _ConfirmSerialId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__ConfirmSerialId(this, value));
    }

    public BBroadcast() {
         this(0);
    }

    public BBroadcast(int _varId_) {
        super(_varId_);
        _protocolWholeData = Zeze.Net.Binary.Empty;
    }

    public void Assign(BBroadcast other) {
        setProtocolType(other.getProtocolType());
        setProtocolWholeData(other.getProtocolWholeData());
        setTime(other.getTime());
        setConfirmSerialId(other.getConfirmSerialId());
    }

    public BBroadcast CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BBroadcast Copy() {
        var copy = new BBroadcast();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BBroadcast a, BBroadcast b) {
        BBroadcast save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public static final long TYPEID = -6926497733546172658L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private static final class Log__protocolType extends Zeze.Transaction.Log1<BBroadcast, Long> {
        public Log__protocolType(BBroadcast self, Long value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 1; }
        @Override
        public void Commit() { this.getBeanTyped()._protocolType = this.getValue(); }
    }

    private static final class Log__protocolWholeData extends Zeze.Transaction.Log1<BBroadcast, Zeze.Net.Binary> {
        public Log__protocolWholeData(BBroadcast self, Zeze.Net.Binary value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 2; }
        @Override
        public void Commit() { this.getBeanTyped()._protocolWholeData = this.getValue(); }
    }

    private static final class Log__time extends Zeze.Transaction.Log1<BBroadcast, Integer> {
        public Log__time(BBroadcast self, Integer value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 3; }
        @Override
        public void Commit() { this.getBeanTyped()._time = this.getValue(); }
    }

    private static final class Log__ConfirmSerialId extends Zeze.Transaction.Log1<BBroadcast, Long> {
        public Log__ConfirmSerialId(BBroadcast self, Long value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 4; }
        @Override
        public void Commit() { this.getBeanTyped()._ConfirmSerialId = this.getValue(); }
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Provider.BBroadcast: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("protocolType").append('=').append(getProtocolType()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("protocolWholeData").append('=').append(getProtocolWholeData()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("time").append('=').append(getTime()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ConfirmSerialId").append('=').append(getConfirmSerialId()).append(System.lineSeparator());
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

    @SuppressWarnings("UnusedAssignment")
    @Override
    public void Encode(ByteBuffer _o_) {
        int _i_ = 0;
        {
            long _x_ = getProtocolType();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = getProtocolWholeData();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            int _x_ = getTime();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            long _x_ = getConfirmSerialId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @SuppressWarnings("UnusedAssignment")
    @Override
    public void Decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setProtocolType(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setProtocolWholeData(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setTime(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setConfirmSerialId(_o_.ReadLong(_t_));
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

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean NegativeCheck() {
        if (getProtocolType() < 0)
            return true;
        if (getTime() < 0)
            return true;
        if (getConfirmSerialId() < 0)
            return true;
        return false;
    }
}
