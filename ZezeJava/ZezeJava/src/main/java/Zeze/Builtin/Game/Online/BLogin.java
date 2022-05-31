// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BLogin extends Zeze.Transaction.Bean {
    private long _RoleId;

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

    public BLogin() {
         this(0);
    }

    public BLogin(int _varId_) {
        super(_varId_);
    }

    public void Assign(BLogin other) {
        setRoleId(other.getRoleId());
    }

    public BLogin CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BLogin Copy() {
        var copy = new BLogin();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BLogin a, BLogin b) {
        BLogin save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public static final long TYPEID = 4454573042979027680L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private static final class Log__RoleId extends Zeze.Transaction.Log1<BLogin, Long> {
       public Log__RoleId(BLogin bean, int varId, Long value) { super(bean, varId, value); }
        @Override
        public void Commit() { getBeanTyped()._RoleId = this.getValue(); }
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.Online.BLogin: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("RoleId").append('=').append(getRoleId()).append(System.lineSeparator());
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
            }
        }
    }
}
