// auto-generated @formatter:off
package Zeze.Builtin.Online;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BVersions extends Zeze.Transaction.Bean implements BVersionsReadOnly {
    public static final long TYPEID = 3480529937760660740L;

    private final Zeze.Transaction.Collections.PMap2<String, Zeze.Builtin.Online.BVersion> _Logins; // key is ClientId
    private long _LastLoginVersion; // 用来生成 account 登录版本号。每次递增。

    public Zeze.Transaction.Collections.PMap2<String, Zeze.Builtin.Online.BVersion> getLogins() {
        return _Logins;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<String, Zeze.Builtin.Online.BVersion, Zeze.Builtin.Online.BVersionReadOnly> getLoginsReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_Logins);
    }

    @Override
    public long getLastLoginVersion() {
        if (!isManaged())
            return _LastLoginVersion;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _LastLoginVersion;
        var log = (Log__LastLoginVersion)txn.getLog(objectId() + 2);
        return log != null ? log.value : _LastLoginVersion;
    }

    public void setLastLoginVersion(long value) {
        if (!isManaged()) {
            _LastLoginVersion = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__LastLoginVersion(this, 2, value));
    }

    @SuppressWarnings("deprecation")
    public BVersions() {
        _Logins = new Zeze.Transaction.Collections.PMap2<>(String.class, Zeze.Builtin.Online.BVersion.class);
        _Logins.variableId(1);
    }

    @SuppressWarnings("deprecation")
    public BVersions(long _LastLoginVersion_) {
        _Logins = new Zeze.Transaction.Collections.PMap2<>(String.class, Zeze.Builtin.Online.BVersion.class);
        _Logins.variableId(1);
        _LastLoginVersion = _LastLoginVersion_;
    }

    public void assign(BVersions other) {
        _Logins.clear();
        for (var e : other.getLogins().entrySet())
            _Logins.put(e.getKey(), e.getValue().copy());
        setLastLoginVersion(other.getLastLoginVersion());
    }

    public BVersions copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BVersions copy() {
        var copy = new BVersions();
        copy.assign(this);
        return copy;
    }

    public static void swap(BVersions a, BVersions b) {
        BVersions save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__LastLoginVersion extends Zeze.Transaction.Logs.LogLong {
        public Log__LastLoginVersion(BVersions bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BVersions)getBelong())._LastLoginVersion = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Online.BVersions: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Logins={");
        if (!_Logins.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _kv_ : _Logins.entrySet()) {
                sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(_kv_.getKey()).append(',').append(System.lineSeparator());
                sb.append(Zeze.Util.Str.indent(level)).append("Value=").append(System.lineSeparator());
                _kv_.getValue().buildString(sb, level + 4);
                sb.append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append('}').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("LastLoginVersion=").append(getLastLoginVersion()).append(System.lineSeparator());
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
            var _x_ = _Logins;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BYTES, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteString(_e_.getKey());
                    _e_.getValue().encode(_o_);
                }
            }
        }
        {
            long _x_ = getLastLoginVersion();
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
            var _x_ = _Logins;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadString(_s_);
                    var _v_ = _o_.ReadBean(new Zeze.Builtin.Online.BVersion(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
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
        _Logins.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _Logins.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
        for (var _v_ : _Logins.values()) {
            if (_v_.negativeCheck())
                return true;
        }
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
                case 1: _Logins.followerApply(vlog); break;
                case 2: _LastLoginVersion = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
            }
        }
    }
}
