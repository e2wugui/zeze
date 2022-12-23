// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BDailyTask extends Zeze.Transaction.Bean implements BDailyTaskReadOnly {
    public static final long TYPEID = -374261286132962226L;

    private int _everydayTaskCount; // 每天任务刷新的个数
    private long _flushTime; // 刷新时间，按照UTC+0存储，自动翻译到各个机子上

    @Override
    public int getEverydayTaskCount() {
        if (!isManaged())
            return _everydayTaskCount;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _everydayTaskCount;
        var log = (Log__everydayTaskCount)txn.getLog(objectId() + 1);
        return log != null ? log.value : _everydayTaskCount;
    }

    public void setEverydayTaskCount(int value) {
        if (!isManaged()) {
            _everydayTaskCount = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__everydayTaskCount(this, 1, value));
    }

    @Override
    public long getFlushTime() {
        if (!isManaged())
            return _flushTime;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _flushTime;
        var log = (Log__flushTime)txn.getLog(objectId() + 2);
        return log != null ? log.value : _flushTime;
    }

    public void setFlushTime(long value) {
        if (!isManaged()) {
            _flushTime = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__flushTime(this, 2, value));
    }

    @SuppressWarnings("deprecation")
    public BDailyTask() {
    }

    @SuppressWarnings("deprecation")
    public BDailyTask(int _everydayTaskCount_, long _flushTime_) {
        _everydayTaskCount = _everydayTaskCount_;
        _flushTime = _flushTime_;
    }

    public void assign(BDailyTask other) {
        setEverydayTaskCount(other.getEverydayTaskCount());
        setFlushTime(other.getFlushTime());
    }

    @Deprecated
    public void Assign(BDailyTask other) {
        assign(other);
    }

    public BDailyTask copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BDailyTask copy() {
        var copy = new BDailyTask();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BDailyTask Copy() {
        return copy();
    }

    public static void swap(BDailyTask a, BDailyTask b) {
        BDailyTask save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__everydayTaskCount extends Zeze.Transaction.Logs.LogInt {
        public Log__everydayTaskCount(BDailyTask bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BDailyTask)getBelong())._everydayTaskCount = value; }
    }

    private static final class Log__flushTime extends Zeze.Transaction.Logs.LogLong {
        public Log__flushTime(BDailyTask bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BDailyTask)getBelong())._flushTime = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.TaskBase.BDailyTask: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("everydayTaskCount=").append(getEverydayTaskCount()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("flushTime=").append(getFlushTime()).append(System.lineSeparator());
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
            int _x_ = getEverydayTaskCount();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            long _x_ = getFlushTime();
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
            setEverydayTaskCount(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setFlushTime(_o_.ReadLong(_t_));
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
        if (getEverydayTaskCount() < 0)
            return true;
        if (getFlushTime() < 0)
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
                case 1: _everydayTaskCount = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 2: _flushTime = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
            }
        }
    }
}
