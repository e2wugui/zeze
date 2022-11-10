// auto-generated @formatter:off
package Zeze.Builtin.Game.Task;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BNamedCountConditionComplete extends Zeze.Transaction.Bean implements BNamedCountConditionCompleteReadOnly {
    public static final long TYPEID = -6101317934632331571L;

    private long _CoinSum; // 玩家吃到的金币数量

    @Override
    public long getCoinSum() {
        if (!isManaged())
            return _CoinSum;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _CoinSum;
        var log = (Log__CoinSum)txn.getLog(objectId() + 1);
        return log != null ? log.value : _CoinSum;
    }

    public void setCoinSum(long value) {
        if (!isManaged()) {
            _CoinSum = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__CoinSum(this, 1, value));
    }

    @SuppressWarnings("deprecation")
    public BNamedCountConditionComplete() {
    }

    @SuppressWarnings("deprecation")
    public BNamedCountConditionComplete(long _CoinSum_) {
        _CoinSum = _CoinSum_;
    }

    public void assign(BNamedCountConditionComplete other) {
        setCoinSum(other.getCoinSum());
    }

    @Deprecated
    public void Assign(BNamedCountConditionComplete other) {
        assign(other);
    }

    public BNamedCountConditionComplete copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BNamedCountConditionComplete copy() {
        var copy = new BNamedCountConditionComplete();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BNamedCountConditionComplete Copy() {
        return copy();
    }

    public static void swap(BNamedCountConditionComplete a, BNamedCountConditionComplete b) {
        BNamedCountConditionComplete save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__CoinSum extends Zeze.Transaction.Logs.LogLong {
        public Log__CoinSum(BNamedCountConditionComplete bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BNamedCountConditionComplete)getBelong())._CoinSum = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.Task.BNamedCountConditionComplete: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("CoinSum").append('=').append(getCoinSum()).append(System.lineSeparator());
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
            long _x_ = getCoinSum();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
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
            setCoinSum(_o_.ReadLong(_t_));
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
                case 1: _CoinSum = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
            }
        }
    }
}
