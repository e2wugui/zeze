// auto-generated @formatter:off
package Zeze.Builtin.Provider;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BBroadcast extends Zeze.Transaction.Bean {
    private long _protocolType;
    private Zeze.Net.Binary _protocolWholeData; // 完整的协议打包，包括了 type, size
    private int _time;

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
        txn.PutLog(new Log__protocolType(this, 1, value));
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
        txn.PutLog(new Log__protocolWholeData(this, 2, value));
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
        txn.PutLog(new Log__time(this, 3, value));
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
       public Log__protocolType(BBroadcast bean, int varId, Long value) { super(bean, varId, value); }
        @Override
        public void Commit() { getBeanTyped()._protocolType = this.getValue(); }
    }

    private static final class Log__protocolWholeData extends Zeze.Transaction.Log1<BBroadcast, Zeze.Net.Binary> {
       public Log__protocolWholeData(BBroadcast bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }
        @Override
        public void Commit() { getBeanTyped()._protocolWholeData = this.getValue(); }
    }

    private static final class Log__time extends Zeze.Transaction.Log1<BBroadcast, Integer> {
       public Log__time(BBroadcast bean, int varId, Integer value) { super(bean, varId, value); }
        @Override
        public void Commit() { getBeanTyped()._time = this.getValue(); }
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
        sb.append(Zeze.Util.Str.indent(level)).append("time").append('=').append(getTime()).append(System.lineSeparator());
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
        if (getProtocolType() < 0)
            return true;
        if (getTime() < 0)
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
                case 1: _protocolType = ((Zeze.Transaction.Logs.LogLong)vlog).Value; break;
                case 2: _protocolWholeData = ((Zeze.Transaction.Logs.LogBinary)vlog).Value; break;
                case 3: _time = ((Zeze.Transaction.Logs.LogInt)vlog).Value; break;
            }
        }
    }
}
