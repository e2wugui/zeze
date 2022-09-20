// auto-generated @formatter:off
package Zeze.Builtin.Provider;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BBroadcast extends Zeze.Transaction.Bean {
    public static final long TYPEID = -6926497733546172658L;

    private long _protocolType;
    private Zeze.Net.Binary _protocolWholeData; // 完整的协议打包，包括了 type, size
    private int _time;

    public long getProtocolType() {
        if (!isManaged())
            return _protocolType;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _protocolType;
        var log = (Log__protocolType)txn.getLog(objectId() + 1);
        return log != null ? log.value : _protocolType;
    }

    public void setProtocolType(long value) {
        if (!isManaged()) {
            _protocolType = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__protocolType(this, 1, value));
    }

    public Zeze.Net.Binary getProtocolWholeData() {
        if (!isManaged())
            return _protocolWholeData;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _protocolWholeData;
        var log = (Log__protocolWholeData)txn.getLog(objectId() + 2);
        return log != null ? log.value : _protocolWholeData;
    }

    public void setProtocolWholeData(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _protocolWholeData = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__protocolWholeData(this, 2, value));
    }

    public int getTime() {
        if (!isManaged())
            return _time;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _time;
        var log = (Log__time)txn.getLog(objectId() + 3);
        return log != null ? log.value : _time;
    }

    public void setTime(int value) {
        if (!isManaged()) {
            _time = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__time(this, 3, value));
    }

    @SuppressWarnings("deprecation")
    public BBroadcast() {
        _protocolWholeData = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BBroadcast(long _protocolType_, Zeze.Net.Binary _protocolWholeData_, int _time_) {
        _protocolType = _protocolType_;
        if (_protocolWholeData_ == null)
            throw new IllegalArgumentException();
        _protocolWholeData = _protocolWholeData_;
        _time = _time_;
    }

    public void assign(BBroadcast other) {
        setProtocolType(other.getProtocolType());
        setProtocolWholeData(other.getProtocolWholeData());
        setTime(other.getTime());
    }

    @Deprecated
    public void Assign(BBroadcast other) {
        assign(other);
    }

    public BBroadcast copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BBroadcast copy() {
        var copy = new BBroadcast();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BBroadcast Copy() {
        return copy();
    }

    public static void swap(BBroadcast a, BBroadcast b) {
        BBroadcast save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__protocolType extends Zeze.Transaction.Logs.LogLong {
        public Log__protocolType(BBroadcast bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BBroadcast)getBelong())._protocolType = value; }
    }

    private static final class Log__protocolWholeData extends Zeze.Transaction.Logs.LogBinary {
        public Log__protocolWholeData(BBroadcast bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BBroadcast)getBelong())._protocolWholeData = value; }
    }

    private static final class Log__time extends Zeze.Transaction.Logs.LogInt {
        public Log__time(BBroadcast bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BBroadcast)getBelong())._time = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Provider.BBroadcast: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("protocolType").append('=').append(getProtocolType()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("protocolWholeData").append('=').append(getProtocolWholeData()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("time").append('=').append(getTime()).append(System.lineSeparator());
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
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
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
        if (getProtocolType() < 0)
            return true;
        if (getTime() < 0)
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
                case 1: _protocolType = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 2: _protocolWholeData = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
                case 3: _time = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
            }
        }
    }
}
