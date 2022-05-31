// auto-generated @formatter:off
package Zeze.Builtin.GlobalCacheManagerWithRaft;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class AchillesHeelConfig extends Zeze.Transaction.Bean {
    private int _MaxNetPing;
    private int _ServerProcessTime;
    private int _ServerReleaseTimeout;

    public int getMaxNetPing() {
        if (!isManaged())
            return _MaxNetPing;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _MaxNetPing;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__MaxNetPing)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.getValue() : _MaxNetPing;
    }

    public void setMaxNetPing(int value) {
        if (!isManaged()) {
            _MaxNetPing = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__MaxNetPing(this, 1, value));
    }

    public int getServerProcessTime() {
        if (!isManaged())
            return _ServerProcessTime;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _ServerProcessTime;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ServerProcessTime)txn.GetLog(this.getObjectId() + 2);
        return log != null ? log.getValue() : _ServerProcessTime;
    }

    public void setServerProcessTime(int value) {
        if (!isManaged()) {
            _ServerProcessTime = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__ServerProcessTime(this, 2, value));
    }

    public int getServerReleaseTimeout() {
        if (!isManaged())
            return _ServerReleaseTimeout;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _ServerReleaseTimeout;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ServerReleaseTimeout)txn.GetLog(this.getObjectId() + 3);
        return log != null ? log.getValue() : _ServerReleaseTimeout;
    }

    public void setServerReleaseTimeout(int value) {
        if (!isManaged()) {
            _ServerReleaseTimeout = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__ServerReleaseTimeout(this, 3, value));
    }

    public AchillesHeelConfig() {
         this(0);
    }

    public AchillesHeelConfig(int _varId_) {
        super(_varId_);
    }

    public void Assign(AchillesHeelConfig other) {
        setMaxNetPing(other.getMaxNetPing());
        setServerProcessTime(other.getServerProcessTime());
        setServerReleaseTimeout(other.getServerReleaseTimeout());
    }

    public AchillesHeelConfig CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public AchillesHeelConfig Copy() {
        var copy = new AchillesHeelConfig();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(AchillesHeelConfig a, AchillesHeelConfig b) {
        AchillesHeelConfig save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public static final long TYPEID = -5438943650453012602L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private static final class Log__MaxNetPing extends Zeze.Transaction.Log1<AchillesHeelConfig, Integer> {
       public Log__MaxNetPing(AchillesHeelConfig bean, int varId, Integer value) { super(bean, varId, value); }
        @Override
        public void Commit() { getBeanTyped()._MaxNetPing = this.getValue(); }
    }

    private static final class Log__ServerProcessTime extends Zeze.Transaction.Log1<AchillesHeelConfig, Integer> {
       public Log__ServerProcessTime(AchillesHeelConfig bean, int varId, Integer value) { super(bean, varId, value); }
        @Override
        public void Commit() { getBeanTyped()._ServerProcessTime = this.getValue(); }
    }

    private static final class Log__ServerReleaseTimeout extends Zeze.Transaction.Log1<AchillesHeelConfig, Integer> {
       public Log__ServerReleaseTimeout(AchillesHeelConfig bean, int varId, Integer value) { super(bean, varId, value); }
        @Override
        public void Commit() { getBeanTyped()._ServerReleaseTimeout = this.getValue(); }
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.GlobalCacheManagerWithRaft.AchillesHeelConfig: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("MaxNetPing").append('=').append(getMaxNetPing()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ServerProcessTime").append('=').append(getServerProcessTime()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ServerReleaseTimeout").append('=').append(getServerReleaseTimeout()).append(System.lineSeparator());
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
    public void Decode(ByteBuffer _o_) {
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
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
    }

    @Override
    public boolean NegativeCheck() {
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
    public void FollowerApply(Zeze.Transaction.Log log) {
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
