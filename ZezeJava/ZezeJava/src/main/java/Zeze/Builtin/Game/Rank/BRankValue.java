// auto-generated @formatter:off
package Zeze.Builtin.Game.Rank;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BRankValue extends Zeze.Transaction.Bean {
    private long _RoleId;
    private long _Value; // 含义由 BConcurrentKey.RankType 决定
    private Zeze.Net.Binary _ValueEx; // 自定义数据。

    public long getRoleId() {
        if (!isManaged())
            return _RoleId;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _RoleId;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__RoleId)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.getValue() : _RoleId;
    }

    public void setRoleId(long value) {
        if (!isManaged()) {
            _RoleId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__RoleId(this, 1, value));
    }

    public long getValue() {
        if (!isManaged())
            return _Value;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _Value;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__Value)txn.GetLog(this.getObjectId() + 2);
        return log != null ? log.getValue() : _Value;
    }

    public void setValue(long value) {
        if (!isManaged()) {
            _Value = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__Value(this, 2, value));
    }

    public Zeze.Net.Binary getValueEx() {
        if (!isManaged())
            return _ValueEx;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _ValueEx;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ValueEx)txn.GetLog(this.getObjectId() + 3);
        return log != null ? log.getValue() : _ValueEx;
    }

    public void setValueEx(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ValueEx = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__ValueEx(this, 3, value));
    }

    public BRankValue() {
         this(0);
    }

    public BRankValue(int _varId_) {
        super(_varId_);
        _ValueEx = Zeze.Net.Binary.Empty;
    }

    public void Assign(BRankValue other) {
        setRoleId(other.getRoleId());
        setValue(other.getValue());
        setValueEx(other.getValueEx());
    }

    public BRankValue CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BRankValue Copy() {
        var copy = new BRankValue();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BRankValue a, BRankValue b) {
        BRankValue save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public static final long TYPEID = 2276228832088785165L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private static final class Log__RoleId extends Zeze.Transaction.Log1<BRankValue, Long> {
       public Log__RoleId(BRankValue bean, int varId, Long value) { super(bean, varId, value); }
        @Override
        public void Commit() { getBeanTyped()._RoleId = this.getValue(); }
    }

    private static final class Log__Value extends Zeze.Transaction.Log1<BRankValue, Long> {
       public Log__Value(BRankValue bean, int varId, Long value) { super(bean, varId, value); }
        @Override
        public void Commit() { getBeanTyped()._Value = this.getValue(); }
    }

    private static final class Log__ValueEx extends Zeze.Transaction.Log1<BRankValue, Zeze.Net.Binary> {
       public Log__ValueEx(BRankValue bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }
        @Override
        public void Commit() { getBeanTyped()._ValueEx = this.getValue(); }
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.Rank.BRankValue: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("RoleId").append('=').append(getRoleId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Value").append('=').append(getValue()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ValueEx").append('=').append(getValueEx()).append(System.lineSeparator());
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
            long _x_ = getRoleId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getValue();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = getValueEx();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void Decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setRoleId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setValue(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setValueEx(_o_.ReadBinary(_t_));
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
        if (getRoleId() < 0)
            return true;
        if (getValue() < 0)
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
                case 1: _RoleId = ((Zeze.Transaction.Logs.LogLong)vlog).Value; break;
                case 2: _Value = ((Zeze.Transaction.Logs.LogLong)vlog).Value; break;
                case 3: _ValueEx = ((Zeze.Transaction.Logs.LogBinary)vlog).Value; break;
            }
        }
    }
}
