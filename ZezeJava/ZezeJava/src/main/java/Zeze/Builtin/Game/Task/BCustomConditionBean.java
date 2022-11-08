// auto-generated @formatter:off
package Zeze.Builtin.Game.Task;

import Zeze.Serialize.ByteBuffer;

// 只供测试的Custom Bean
@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BCustomConditionBean extends Zeze.Transaction.Bean implements BCustomConditionBeanReadOnly {
    public static final long TYPEID = 8552022057186377633L;

    private int _CoinSum; // 玩家吃到的金币数量
    private int _TargetCoinSum; // 任务需要的金币数量

    @Override
    public int getCoinSum() {
        if (!isManaged())
            return _CoinSum;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _CoinSum;
        var log = (Log__CoinSum)txn.getLog(objectId() + 1);
        return log != null ? log.value : _CoinSum;
    }

    public void setCoinSum(int value) {
        if (!isManaged()) {
            _CoinSum = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__CoinSum(this, 1, value));
    }

    @Override
    public int getTargetCoinSum() {
        if (!isManaged())
            return _TargetCoinSum;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _TargetCoinSum;
        var log = (Log__TargetCoinSum)txn.getLog(objectId() + 2);
        return log != null ? log.value : _TargetCoinSum;
    }

    public void setTargetCoinSum(int value) {
        if (!isManaged()) {
            _TargetCoinSum = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__TargetCoinSum(this, 2, value));
    }

    @SuppressWarnings("deprecation")
    public BCustomConditionBean() {
    }

    @SuppressWarnings("deprecation")
    public BCustomConditionBean(int _CoinSum_, int _TargetCoinSum_) {
        _CoinSum = _CoinSum_;
        _TargetCoinSum = _TargetCoinSum_;
    }

    public void assign(BCustomConditionBean other) {
        setCoinSum(other.getCoinSum());
        setTargetCoinSum(other.getTargetCoinSum());
    }

    @Deprecated
    public void Assign(BCustomConditionBean other) {
        assign(other);
    }

    public BCustomConditionBean copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BCustomConditionBean copy() {
        var copy = new BCustomConditionBean();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BCustomConditionBean Copy() {
        return copy();
    }

    public static void swap(BCustomConditionBean a, BCustomConditionBean b) {
        BCustomConditionBean save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__CoinSum extends Zeze.Transaction.Logs.LogInt {
        public Log__CoinSum(BCustomConditionBean bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BCustomConditionBean)getBelong())._CoinSum = value; }
    }

    private static final class Log__TargetCoinSum extends Zeze.Transaction.Logs.LogInt {
        public Log__TargetCoinSum(BCustomConditionBean bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BCustomConditionBean)getBelong())._TargetCoinSum = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.Task.BCustomConditionBean: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("CoinSum").append('=').append(getCoinSum()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("TargetCoinSum").append('=').append(getTargetCoinSum()).append(System.lineSeparator());
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
            int _x_ = getCoinSum();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getTargetCoinSum();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
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
            setCoinSum(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setTargetCoinSum(_o_.ReadInt(_t_));
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
        if (getCoinSum() < 0)
            return true;
        if (getTargetCoinSum() < 0)
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
                case 1: _CoinSum = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 2: _TargetCoinSum = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
            }
        }
    }
}
