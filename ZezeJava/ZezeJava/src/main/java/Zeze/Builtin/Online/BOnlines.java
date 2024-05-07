// auto-generated @formatter:off
package Zeze.Builtin.Online;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BOnlines extends Zeze.Transaction.Bean implements BOnlinesReadOnly {
    public static final long TYPEID = -725348871039859823L;

    private final Zeze.Transaction.Collections.PMap2<String, Zeze.Builtin.Online.BOnline> _Logins; // key is ClientId
    private long _LastLoginVersion; // 用来生成 account 登录版本号。每次递增。
    private String _Account; // 所属账号,用于登录验证

    public Zeze.Transaction.Collections.PMap2<String, Zeze.Builtin.Online.BOnline> getLogins() {
        return _Logins;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<String, Zeze.Builtin.Online.BOnline, Zeze.Builtin.Online.BOnlineReadOnly> getLoginsReadOnly() {
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

    @Override
    public String getAccount() {
        if (!isManaged())
            return _Account;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Account;
        var log = (Log__Account)txn.getLog(objectId() + 3);
        return log != null ? log.value : _Account;
    }

    public void setAccount(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Account = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Account(this, 3, value));
    }

    @SuppressWarnings("deprecation")
    public BOnlines() {
        _Logins = new Zeze.Transaction.Collections.PMap2<>(String.class, Zeze.Builtin.Online.BOnline.class);
        _Logins.variableId(1);
        _Account = "";
    }

    @SuppressWarnings("deprecation")
    public BOnlines(long _LastLoginVersion_, String _Account_) {
        _Logins = new Zeze.Transaction.Collections.PMap2<>(String.class, Zeze.Builtin.Online.BOnline.class);
        _Logins.variableId(1);
        _LastLoginVersion = _LastLoginVersion_;
        if (_Account_ == null)
            _Account_ = "";
        _Account = _Account_;
    }

    @Override
    public void reset() {
        _Logins.clear();
        setLastLoginVersion(0);
        setAccount("");
        _unknown_ = null;
    }

    public void assign(BOnlines other) {
        _Logins.clear();
        for (var e : other._Logins.entrySet())
            _Logins.put(e.getKey(), e.getValue().copy());
        setLastLoginVersion(other.getLastLoginVersion());
        setAccount(other.getAccount());
        _unknown_ = other._unknown_;
    }

    public BOnlines copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BOnlines copy() {
        var copy = new BOnlines();
        copy.assign(this);
        return copy;
    }

    public static void swap(BOnlines a, BOnlines b) {
        BOnlines save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__LastLoginVersion extends Zeze.Transaction.Logs.LogLong {
        public Log__LastLoginVersion(BOnlines bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BOnlines)getBelong())._LastLoginVersion = value; }
    }

    private static final class Log__Account extends Zeze.Transaction.Logs.LogString {
        public Log__Account(BOnlines bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BOnlines)getBelong())._Account = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Online.BOnlines: {").append(System.lineSeparator());
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
        sb.append(Zeze.Util.Str.indent(level)).append("LastLoginVersion=").append(getLastLoginVersion()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Account=").append(getAccount()).append(System.lineSeparator());
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
            var _x_ = _Logins;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BYTES, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteString(_e_.getKey());
                    _e_.getValue().encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            long _x_ = getLastLoginVersion();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            String _x_ = getAccount();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        _o_.writeAllUnknownFields(_i_, _ui_, _u_);
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        ByteBuffer _u_ = null;
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            var _x_ = _Logins;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadString(_s_);
                    var _v_ = _o_.ReadBean(new Zeze.Builtin.Online.BOnline(), _t_);
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
        if (_i_ == 3) {
            setAccount(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
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
                case 3: _Account = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        Zeze.Serialize.Helper.decodeJsonMap(this, "Logins", _Logins, rs.getString(_parents_name_ + "Logins"));
        setLastLoginVersion(rs.getLong(_parents_name_ + "LastLoginVersion"));
        setAccount(rs.getString(_parents_name_ + "Account"));
        if (getAccount() == null)
            setAccount("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "Logins", Zeze.Serialize.Helper.encodeJson(_Logins));
        st.appendLong(_parents_name_ + "LastLoginVersion", getLastLoginVersion());
        st.appendString(_parents_name_ + "Account", getAccount());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Logins", "map", "string", "Zeze.Builtin.Online.BOnline"));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "LastLoginVersion", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Account", "string", "", ""));
        return vars;
    }
}
