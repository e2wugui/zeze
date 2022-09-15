// auto-generated @formatter:off
package Zeze.Builtin.GlobalCacheManagerWithRaft;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BAchillesHeelConfig extends Zeze.Transaction.Bean {
    private int _MaxNetPing;
    private int _ServerProcessTime;
    private int _ServerReleaseTimeout;

    public int getMaxNetPing() {
        if (!isManaged())
            return _MaxNetPing;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _MaxNetPing;
        var log = (Log__MaxNetPing)txn.GetLog(objectId() + 1);
        return log != null ? log.Value : _MaxNetPing;
    }

    public void setMaxNetPing(int value) {
        if (!isManaged()) {
            _MaxNetPing = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.PutLog(new Log__MaxNetPing(this, 1, value));
    }

    public int getServerProcessTime() {
        if (!isManaged())
            return _ServerProcessTime;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ServerProcessTime;
        var log = (Log__ServerProcessTime)txn.GetLog(objectId() + 2);
        return log != null ? log.Value : _ServerProcessTime;
    }

    public void setServerProcessTime(int value) {
        if (!isManaged()) {
            _ServerProcessTime = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.PutLog(new Log__ServerProcessTime(this, 2, value));
    }

    public int getServerReleaseTimeout() {
        if (!isManaged())
            return _ServerReleaseTimeout;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ServerReleaseTimeout;
        var log = (Log__ServerReleaseTimeout)txn.GetLog(objectId() + 3);
        return log != null ? log.Value : _ServerReleaseTimeout;
    }

    public void setServerReleaseTimeout(int value) {
        if (!isManaged()) {
            _ServerReleaseTimeout = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.PutLog(new Log__ServerReleaseTimeout(this, 3, value));
    }

    @SuppressWarnings("deprecation")
    public BAchillesHeelConfig() {
    }

    @SuppressWarnings("deprecation")
    public BAchillesHeelConfig(int _MaxNetPing_, int _ServerProcessTime_, int _ServerReleaseTimeout_) {
        _MaxNetPing = _MaxNetPing_;
        _ServerProcessTime = _ServerProcessTime_;
        _ServerReleaseTimeout = _ServerReleaseTimeout_;
    }

    public void assign(BAchillesHeelConfig other) {
        setMaxNetPing(other.getMaxNetPing());
        setServerProcessTime(other.getServerProcessTime());
        setServerReleaseTimeout(other.getServerReleaseTimeout());
    }

    @Deprecated
    public void Assign(BAchillesHeelConfig other) {
        assign(other);
    }

    public BAchillesHeelConfig copyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BAchillesHeelConfig copy() {
        var copy = new BAchillesHeelConfig();
        copy.Assign(this);
        return copy;
    }

    @Deprecated
    public BAchillesHeelConfig Copy() {
        return copy();
    }

    public static void swap(BAchillesHeelConfig a, BAchillesHeelConfig b) {
        BAchillesHeelConfig save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public BAchillesHeelConfig copyBean() {
        return Copy();
    }

    public static final long TYPEID = 6351123425648255834L;

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__MaxNetPing extends Zeze.Transaction.Logs.LogInt {
        public Log__MaxNetPing(BAchillesHeelConfig bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BAchillesHeelConfig)getBelong())._MaxNetPing = Value; }
    }

    private static final class Log__ServerProcessTime extends Zeze.Transaction.Logs.LogInt {
        public Log__ServerProcessTime(BAchillesHeelConfig bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BAchillesHeelConfig)getBelong())._ServerProcessTime = Value; }
    }

    private static final class Log__ServerReleaseTimeout extends Zeze.Transaction.Logs.LogInt {
        public Log__ServerReleaseTimeout(BAchillesHeelConfig bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BAchillesHeelConfig)getBelong())._ServerReleaseTimeout = Value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.GlobalCacheManagerWithRaft.BAchillesHeelConfig: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("MaxNetPing").append('=').append(getMaxNetPing()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ServerProcessTime").append('=').append(getServerProcessTime()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ServerReleaseTimeout").append('=').append(getServerReleaseTimeout()).append(System.lineSeparator());
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
            int _x_ = getMaxNetPing();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getServerProcessTime();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getServerReleaseTimeout();
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
            setMaxNetPing(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setServerProcessTime(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setServerReleaseTimeout(_o_.ReadInt(_t_));
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
        if (getMaxNetPing() < 0)
            return true;
        if (getServerProcessTime() < 0)
            return true;
        if (getServerReleaseTimeout() < 0)
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
                case 1: _MaxNetPing = ((Zeze.Transaction.Logs.LogInt)vlog).Value; break;
                case 2: _ServerProcessTime = ((Zeze.Transaction.Logs.LogInt)vlog).Value; break;
                case 3: _ServerReleaseTimeout = ((Zeze.Transaction.Logs.LogInt)vlog).Value; break;
            }
        }
    }
}
