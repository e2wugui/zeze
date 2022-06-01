// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BAccount extends Zeze.Transaction.Bean {
    private String _Name;
    private final Zeze.Transaction.Collections.PList1<Long> _Roles; // roleid list
    private long _LastLoginRoleId;
    private long _LastLoginVersion; // 用来生成 role 登录版本号。每次递增。

    public String getName() {
        if (!isManaged())
            return _Name;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _Name;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__Name)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.getValue() : _Name;
    }

    public void setName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Name = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__Name(this, 1, value));
    }

    public Zeze.Transaction.Collections.PList1<Long> getRoles() {
        return _Roles;
    }

    public long getLastLoginRoleId() {
        if (!isManaged())
            return _LastLoginRoleId;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _LastLoginRoleId;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__LastLoginRoleId)txn.GetLog(this.getObjectId() + 3);
        return log != null ? log.getValue() : _LastLoginRoleId;
    }

    public void setLastLoginRoleId(long value) {
        if (!isManaged()) {
            _LastLoginRoleId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__LastLoginRoleId(this, 3, value));
    }

    public long getLastLoginVersion() {
        if (!isManaged())
            return _LastLoginVersion;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _LastLoginVersion;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__LastLoginVersion)txn.GetLog(this.getObjectId() + 4);
        return log != null ? log.getValue() : _LastLoginVersion;
    }

    public void setLastLoginVersion(long value) {
        if (!isManaged()) {
            _LastLoginVersion = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__LastLoginVersion(this, 4, value));
    }

    public BAccount() {
         this(0);
    }

    public BAccount(int _varId_) {
        super(_varId_);
        _Name = "";
        _Roles = new Zeze.Transaction.Collections.PList1<>(Long.class);
        _Roles.VariableId = 2;
    }

    public void Assign(BAccount other) {
        setName(other.getName());
        getRoles().clear();
        for (var e : other.getRoles())
            getRoles().add(e);
        setLastLoginRoleId(other.getLastLoginRoleId());
        setLastLoginVersion(other.getLastLoginVersion());
    }

    public BAccount CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BAccount Copy() {
        var copy = new BAccount();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BAccount a, BAccount b) {
        BAccount save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public static final long TYPEID = -6071732171172452068L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private static final class Log__Name extends Zeze.Transaction.Log1<BAccount, String> {
        public Log__Name(BAccount bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void Commit() { getBeanTyped()._Name = this.getValue(); }
    }

    private static final class Log__LastLoginRoleId extends Zeze.Transaction.Log1<BAccount, Long> {
        public Log__LastLoginRoleId(BAccount bean, int varId, Long value) { super(bean, varId, value); }

        @Override
        public void Commit() { getBeanTyped()._LastLoginRoleId = this.getValue(); }
    }

    private static final class Log__LastLoginVersion extends Zeze.Transaction.Log1<BAccount, Long> {
        public Log__LastLoginVersion(BAccount bean, int varId, Long value) { super(bean, varId, value); }

        @Override
        public void Commit() { getBeanTyped()._LastLoginVersion = this.getValue(); }
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.Online.BAccount: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Name").append('=').append(getName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Roles").append("=[").append(System.lineSeparator());
        level += 4;
        for (var _item_ : getRoles()) {
            sb.append(Zeze.Util.Str.indent(level)).append("Item").append('=').append(_item_).append(',').append(System.lineSeparator());
        }
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("LastLoginRoleId").append('=').append(getLastLoginRoleId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("LastLoginVersion").append('=').append(getLastLoginVersion()).append(System.lineSeparator());
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
            String _x_ = getName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = getRoles();
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                for (var _v_ : _x_)
                    _o_.WriteLong(_v_);
            }
        }
        {
            long _x_ = getLastLoginRoleId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getLastLoginVersion();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
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
            setName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = getRoles();
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadLong(_t_));
            } else
                _o_.SkipUnknownField(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setLastLoginRoleId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setLastLoginVersion(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Roles.InitRootInfo(root, this);
    }

    @Override
    public boolean NegativeCheck() {
        for (var _v_ : getRoles()) {
            if (_v_ < 0)
                return true;
        }
        if (getLastLoginRoleId() < 0)
            return true;
        if (getLastLoginVersion() < 0)
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
                case 1: _Name = ((Zeze.Transaction.Logs.LogString)vlog).Value; break;
                case 2: _Roles.FollowerApply(vlog); break;
                case 3: _LastLoginRoleId = ((Zeze.Transaction.Logs.LogLong)vlog).Value; break;
                case 4: _LastLoginVersion = ((Zeze.Transaction.Logs.LogLong)vlog).Value; break;
            }
        }
    }
}
