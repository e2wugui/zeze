// auto-generated @formatter:off
package Zeze.Builtin.Provider;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BLinkBroken extends Zeze.Transaction.Bean implements BLinkBrokenReadOnly {
    public static final long TYPEID = 1424702393060691138L;

    public static final int REASON_PEERCLOSE = 0;

    private String _account;
    private long _linkSid;
    private int _reason;
    private final Zeze.Transaction.Collections.CollOne<Zeze.Builtin.Provider.BUserState> _userState;

    @Override
    public String getAccount() {
        if (!isManaged())
            return _account;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _account;
        var log = (Log__account)txn.getLog(objectId() + 1);
        return log != null ? log.value : _account;
    }

    public void setAccount(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _account = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__account(this, 1, value));
    }

    @Override
    public long getLinkSid() {
        if (!isManaged())
            return _linkSid;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _linkSid;
        var log = (Log__linkSid)txn.getLog(objectId() + 2);
        return log != null ? log.value : _linkSid;
    }

    public void setLinkSid(long value) {
        if (!isManaged()) {
            _linkSid = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__linkSid(this, 2, value));
    }

    @Override
    public int getReason() {
        if (!isManaged())
            return _reason;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _reason;
        var log = (Log__reason)txn.getLog(objectId() + 3);
        return log != null ? log.value : _reason;
    }

    public void setReason(int value) {
        if (!isManaged()) {
            _reason = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__reason(this, 3, value));
    }

    public Zeze.Builtin.Provider.BUserState getUserState() {
        return _userState.getValue();
    }

    public void setUserState(Zeze.Builtin.Provider.BUserState value) {
        _userState.setValue(value);
    }

    @Override
    public Zeze.Builtin.Provider.BUserStateReadOnly getUserStateReadOnly() {
        return _userState.getValue();
    }

    @SuppressWarnings("deprecation")
    public BLinkBroken() {
        _account = "";
        _userState = new Zeze.Transaction.Collections.CollOne<>(new Zeze.Builtin.Provider.BUserState(), Zeze.Builtin.Provider.BUserState.class);
        _userState.variableId(4);
    }

    @SuppressWarnings("deprecation")
    public BLinkBroken(String _account_, long _linkSid_, int _reason_) {
        if (_account_ == null)
            throw new IllegalArgumentException();
        _account = _account_;
        _linkSid = _linkSid_;
        _reason = _reason_;
        _userState = new Zeze.Transaction.Collections.CollOne<>(new Zeze.Builtin.Provider.BUserState(), Zeze.Builtin.Provider.BUserState.class);
        _userState.variableId(4);
    }

    public void assign(BLinkBroken other) {
        setAccount(other.getAccount());
        setLinkSid(other.getLinkSid());
        setReason(other.getReason());
        _userState.assign(other._userState);
    }

    public BLinkBroken copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BLinkBroken copy() {
        var copy = new BLinkBroken();
        copy.assign(this);
        return copy;
    }

    public static void swap(BLinkBroken a, BLinkBroken b) {
        BLinkBroken save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__account extends Zeze.Transaction.Logs.LogString {
        public Log__account(BLinkBroken bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BLinkBroken)getBelong())._account = value; }
    }

    private static final class Log__linkSid extends Zeze.Transaction.Logs.LogLong {
        public Log__linkSid(BLinkBroken bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BLinkBroken)getBelong())._linkSid = value; }
    }

    private static final class Log__reason extends Zeze.Transaction.Logs.LogInt {
        public Log__reason(BLinkBroken bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BLinkBroken)getBelong())._reason = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Provider.BLinkBroken: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("account=").append(getAccount()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("linkSid=").append(getLinkSid()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("reason=").append(getReason()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("userState=").append(System.lineSeparator());
        _userState.buildString(sb, level + 4);
        sb.append(System.lineSeparator());
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
            String _x_ = getAccount();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            long _x_ = getLinkSid();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            int _x_ = getReason();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 4, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _userState.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setAccount(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setLinkSid(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setReason(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _o_.ReadBean(_userState, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _userState.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _userState.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getLinkSid() < 0)
            return true;
        if (getReason() < 0)
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
                case 1: _account = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 2: _linkSid = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 3: _reason = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 4: _userState.followerApply(vlog); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setAccount(rs.getString(_parents_name_ + "account"));
        if (getAccount() == null)
            setAccount("");
        setLinkSid(rs.getLong(_parents_name_ + "linkSid"));
        setReason(rs.getInt(_parents_name_ + "reason"));
        parents.add("userState");
        _userState.decodeResultSet(parents, rs);
        parents.remove(parents.size() - 1);
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "account", getAccount());
        st.appendLong(_parents_name_ + "linkSid", getLinkSid());
        st.appendInt(_parents_name_ + "reason", getReason());
        parents.add("userState");
        _userState.encodeSQLStatement(parents, st);
        parents.remove(parents.size() - 1);
    }
}
