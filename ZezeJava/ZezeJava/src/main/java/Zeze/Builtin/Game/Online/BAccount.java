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
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Name;
        var log = (Log__Name)txn.getLog(objectId() + 1);
        return log != null ? log.value : _Name;
    }

    public void setName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Name = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Name(this, 1, value));
    }

    public Zeze.Transaction.Collections.PList1<Long> getRoles() {
        return _Roles;
    }

    public long getLastLoginRoleId() {
        if (!isManaged())
            return _LastLoginRoleId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _LastLoginRoleId;
        var log = (Log__LastLoginRoleId)txn.getLog(objectId() + 3);
        return log != null ? log.value : _LastLoginRoleId;
    }

    public void setLastLoginRoleId(long value) {
        if (!isManaged()) {
            _LastLoginRoleId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__LastLoginRoleId(this, 3, value));
    }

    public long getLastLoginVersion() {
        if (!isManaged())
            return _LastLoginVersion;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _LastLoginVersion;
        var log = (Log__LastLoginVersion)txn.getLog(objectId() + 4);
        return log != null ? log.value : _LastLoginVersion;
    }

    public void setLastLoginVersion(long value) {
        if (!isManaged()) {
            _LastLoginVersion = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__LastLoginVersion(this, 4, value));
    }

    @SuppressWarnings("deprecation")
    public BAccount() {
        _Name = "";
        _Roles = new Zeze.Transaction.Collections.PList1<>(Long.class);
        _Roles.variableId(2);
    }

    @SuppressWarnings("deprecation")
    public BAccount(String _Name_, long _LastLoginRoleId_, long _LastLoginVersion_) {
        if (_Name_ == null)
            throw new IllegalArgumentException();
        _Name = _Name_;
        _Roles = new Zeze.Transaction.Collections.PList1<>(Long.class);
        _Roles.variableId(2);
        _LastLoginRoleId = _LastLoginRoleId_;
        _LastLoginVersion = _LastLoginVersion_;
    }

    public void assign(BAccount other) {
        setName(other.getName());
        getRoles().clear();
        for (var e : other.getRoles())
            getRoles().add(e);
        setLastLoginRoleId(other.getLastLoginRoleId());
        setLastLoginVersion(other.getLastLoginVersion());
    }

    @Deprecated
    public void Assign(BAccount other) {
        assign(other);
    }

    public BAccount copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    public BAccount copy() {
        var copy = new BAccount();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BAccount Copy() {
        return copy();
    }

    public static void swap(BAccount a, BAccount b) {
        BAccount save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public BAccount copyBean() {
        return copy();
    }

    public static final long TYPEID = -6071732171172452068L;

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Name extends Zeze.Transaction.Logs.LogString {
        public Log__Name(BAccount bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BAccount)getBelong())._Name = value; }
    }

    private static final class Log__LastLoginRoleId extends Zeze.Transaction.Logs.LogLong {
        public Log__LastLoginRoleId(BAccount bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BAccount)getBelong())._LastLoginRoleId = value; }
    }

    private static final class Log__LastLoginVersion extends Zeze.Transaction.Logs.LogLong {
        public Log__LastLoginVersion(BAccount bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BAccount)getBelong())._LastLoginVersion = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
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
    public void decode(ByteBuffer _o_) {
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
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
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
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Roles.initRootInfo(root, this);
    }

    @Override
    protected void resetChildrenRootInfo() {
        _Roles.resetRootInfo();
    }

    @Override
    public boolean negativeCheck() {
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
    public void followerApply(Zeze.Transaction.Log log) {
        var vars = ((Zeze.Transaction.Collections.LogBean)log).getVariables();
        if (vars == null)
            return;
        for (var it = vars.iterator(); it.moveToNext(); ) {
            var vlog = it.value();
            switch (vlog.getVariableId()) {
                case 1: _Name = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 2: _Roles.followerApply(vlog); break;
                case 3: _LastLoginRoleId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 4: _LastLoginVersion = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
            }
        }
    }
}
