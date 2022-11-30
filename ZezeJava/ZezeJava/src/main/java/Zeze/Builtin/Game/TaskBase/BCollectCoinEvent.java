// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BCollectCoinEvent extends Zeze.Transaction.Bean implements BCollectCoinEventReadOnly {
    public static final long TYPEID = -6706619269913025707L;

    private String _name;
    private long _coinCount;

    @Override
    public String getName() {
        if (!isManaged())
            return _name;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _name;
        var log = (Log__name)txn.getLog(objectId() + 1);
        return log != null ? log.value : _name;
    }

    public void setName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _name = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__name(this, 1, value));
    }

    @Override
    public long getCoinCount() {
        if (!isManaged())
            return _coinCount;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _coinCount;
        var log = (Log__coinCount)txn.getLog(objectId() + 2);
        return log != null ? log.value : _coinCount;
    }

    public void setCoinCount(long value) {
        if (!isManaged()) {
            _coinCount = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__coinCount(this, 2, value));
    }

    @SuppressWarnings("deprecation")
    public BCollectCoinEvent() {
        _name = "";
    }

    @SuppressWarnings("deprecation")
    public BCollectCoinEvent(String _name_, long _coinCount_) {
        if (_name_ == null)
            throw new IllegalArgumentException();
        _name = _name_;
        _coinCount = _coinCount_;
    }

    public void assign(BCollectCoinEvent other) {
        setName(other.getName());
        setCoinCount(other.getCoinCount());
    }

    @Deprecated
    public void Assign(BCollectCoinEvent other) {
        assign(other);
    }

    public BCollectCoinEvent copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BCollectCoinEvent copy() {
        var copy = new BCollectCoinEvent();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BCollectCoinEvent Copy() {
        return copy();
    }

    public static void swap(BCollectCoinEvent a, BCollectCoinEvent b) {
        BCollectCoinEvent save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__name extends Zeze.Transaction.Logs.LogString {
        public Log__name(BCollectCoinEvent bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BCollectCoinEvent)getBelong())._name = value; }
    }

    private static final class Log__coinCount extends Zeze.Transaction.Logs.LogLong {
        public Log__coinCount(BCollectCoinEvent bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BCollectCoinEvent)getBelong())._coinCount = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.TaskBase.BCollectCoinEvent: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("name=").append(getName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("coinCount=").append(getCoinCount()).append(System.lineSeparator());
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
            String _x_ = getName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            long _x_ = getCoinCount();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setCoinCount(_o_.ReadLong(_t_));
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
        if (getCoinCount() < 0)
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
                case 1: _name = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 2: _coinCount = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
            }
        }
    }
}
