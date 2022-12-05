// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

import Zeze.Serialize.ByteBuffer;

// 内置条件类型：CollectCoinTask
@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BCollectCoinTask extends Zeze.Transaction.Bean implements BCollectCoinTaskReadOnly {
    public static final long TYPEID = -5612956764852219684L;

    private String _name;
    private long _targetCoinCount;
    private long _currentCoinCount;

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
    public long getTargetCoinCount() {
        if (!isManaged())
            return _targetCoinCount;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _targetCoinCount;
        var log = (Log__targetCoinCount)txn.getLog(objectId() + 2);
        return log != null ? log.value : _targetCoinCount;
    }

    public void setTargetCoinCount(long value) {
        if (!isManaged()) {
            _targetCoinCount = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__targetCoinCount(this, 2, value));
    }

    @Override
    public long getCurrentCoinCount() {
        if (!isManaged())
            return _currentCoinCount;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _currentCoinCount;
        var log = (Log__currentCoinCount)txn.getLog(objectId() + 3);
        return log != null ? log.value : _currentCoinCount;
    }

    public void setCurrentCoinCount(long value) {
        if (!isManaged()) {
            _currentCoinCount = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__currentCoinCount(this, 3, value));
    }

    @SuppressWarnings("deprecation")
    public BCollectCoinTask() {
        _name = "";
    }

    @SuppressWarnings("deprecation")
    public BCollectCoinTask(String _name_, long _targetCoinCount_, long _currentCoinCount_) {
        if (_name_ == null)
            throw new IllegalArgumentException();
        _name = _name_;
        _targetCoinCount = _targetCoinCount_;
        _currentCoinCount = _currentCoinCount_;
    }

    public void assign(BCollectCoinTask other) {
        setName(other.getName());
        setTargetCoinCount(other.getTargetCoinCount());
        setCurrentCoinCount(other.getCurrentCoinCount());
    }

    @Deprecated
    public void Assign(BCollectCoinTask other) {
        assign(other);
    }

    public BCollectCoinTask copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BCollectCoinTask copy() {
        var copy = new BCollectCoinTask();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BCollectCoinTask Copy() {
        return copy();
    }

    public static void swap(BCollectCoinTask a, BCollectCoinTask b) {
        BCollectCoinTask save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__name extends Zeze.Transaction.Logs.LogString {
        public Log__name(BCollectCoinTask bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BCollectCoinTask)getBelong())._name = value; }
    }

    private static final class Log__targetCoinCount extends Zeze.Transaction.Logs.LogLong {
        public Log__targetCoinCount(BCollectCoinTask bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BCollectCoinTask)getBelong())._targetCoinCount = value; }
    }

    private static final class Log__currentCoinCount extends Zeze.Transaction.Logs.LogLong {
        public Log__currentCoinCount(BCollectCoinTask bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BCollectCoinTask)getBelong())._currentCoinCount = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.TaskBase.BCollectCoinTask: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("name=").append(getName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("targetCoinCount=").append(getTargetCoinCount()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("currentCoinCount=").append(getCurrentCoinCount()).append(System.lineSeparator());
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
            long _x_ = getTargetCoinCount();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getCurrentCoinCount();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
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
            setTargetCoinCount(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setCurrentCoinCount(_o_.ReadLong(_t_));
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
        if (getTargetCoinCount() < 0)
            return true;
        if (getCurrentCoinCount() < 0)
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
                case 2: _targetCoinCount = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 3: _currentCoinCount = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
            }
        }
    }
}
