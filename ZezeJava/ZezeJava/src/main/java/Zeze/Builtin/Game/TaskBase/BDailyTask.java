// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BDailyTask extends Zeze.Transaction.Bean implements BDailyTaskReadOnly {
    public static final long TYPEID = -374261286132962226L;

    private int _everydayTaskCount; // 每天任务刷新的个数
    private long _flushTime; // 刷新时间，按照UTC+0存储，自动翻译到各个机子上
    private final Zeze.Transaction.Collections.PList1<Long> _todayTaskPhaseIds; // 今天的所有的每日任务的Id（实际上是PhaseId）

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

    public Zeze.Transaction.Collections.PList1<Long> getTodayTaskPhaseIds() {
        return _todayTaskPhaseIds;
    }

    @Override
    public Zeze.Transaction.Collections.PList1ReadOnly<Long> getTodayTaskPhaseIdsReadOnly() {
        return new Zeze.Transaction.Collections.PList1ReadOnly<>(_todayTaskPhaseIds);
    }

    @SuppressWarnings("deprecation")
    public BDailyTask() {
        _todayTaskPhaseIds = new Zeze.Transaction.Collections.PList1<>(Long.class);
        _todayTaskPhaseIds.variableId(3);
    }

    @SuppressWarnings("deprecation")
    public BDailyTask(int _everydayTaskCount_, long _flushTime_) {
        _everydayTaskCount = _everydayTaskCount_;
        _flushTime = _flushTime_;
        _todayTaskPhaseIds = new Zeze.Transaction.Collections.PList1<>(Long.class);
        _todayTaskPhaseIds.variableId(3);
    }

    @Override
    public void reset() {
        setEverydayTaskCount(0);
        setFlushTime(0);
        _todayTaskPhaseIds.clear();
        _unknown_ = null;
    }

    public void assign(BDailyTask other) {
        setEverydayTaskCount(other.getEverydayTaskCount());
        setFlushTime(other.getFlushTime());
        _todayTaskPhaseIds.clear();
        _todayTaskPhaseIds.addAll(other._todayTaskPhaseIds);
        _unknown_ = other._unknown_;
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
        sb.append(Zeze.Util.Str.indent(level)).append("flushTime=").append(getFlushTime()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("todayTaskPhaseIds=[");
        if (!_todayTaskPhaseIds.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _todayTaskPhaseIds) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(_item_).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append(']').append(System.lineSeparator());
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

    private byte[] _unknown_;

    public byte[] unknown() {
        return _unknown_;
    }

    public void clearUnknown() {
        _unknown_ = null;
    }

    @Override
    public void encode(ByteBuffer _o_) {
        ByteBuffer _u_ = null;
        var _ua_ = _unknown_;
        var _ui_ = _ua_ != null ? (_u_ = ByteBuffer.Wrap(_ua_)).readUnknownIndex() : Long.MAX_VALUE;
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
        {
            var _x_ = _todayTaskPhaseIds;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                for (var _v_ : _x_) {
                    _o_.WriteLong(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        _o_.writeAllUnknownFields(_i_, _ui_, _u_);
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        ByteBuffer _u_ = null;
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
        if (_i_ == 3) {
            var _x_ = _todayTaskPhaseIds;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadLong(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _todayTaskPhaseIds.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _todayTaskPhaseIds.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getEverydayTaskCount() < 0)
            return true;
        if (getFlushTime() < 0)
            return true;
        for (var _v_ : _todayTaskPhaseIds) {
            if (_v_ < 0)
                return true;
        }
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
                case 3: _todayTaskPhaseIds.followerApply(vlog); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setEverydayTaskCount(rs.getInt(_parents_name_ + "everydayTaskCount"));
        setFlushTime(rs.getLong(_parents_name_ + "flushTime"));
        Zeze.Serialize.Helper.decodeJsonList(_todayTaskPhaseIds, Long.class, rs.getString(_parents_name_ + "todayTaskPhaseIds"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendInt(_parents_name_ + "everydayTaskCount", getEverydayTaskCount());
        st.appendLong(_parents_name_ + "flushTime", getFlushTime());
        st.appendString(_parents_name_ + "todayTaskPhaseIds", Zeze.Serialize.Helper.encodeJson(_todayTaskPhaseIds));
    }

    @Override
    public Zeze.Transaction.Bean toPrevious() {
        return null; // todo BDailyTask
    }
}
