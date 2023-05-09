// auto-generated @formatter:off
package Zeze.Builtin.Provider;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BSetUserState extends Zeze.Transaction.Bean implements BSetUserStateReadOnly {
    public static final long TYPEID = -4860388989628287875L;

    private long _linkSid;
    private final Zeze.Transaction.Collections.CollOne<Zeze.Builtin.Provider.BUserState> _userState;

    @Override
    public long getLinkSid() {
        if (!isManaged())
            return _linkSid;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _linkSid;
        var log = (Log__linkSid)txn.getLog(objectId() + 1);
        return log != null ? log.value : _linkSid;
    }

    public void setLinkSid(long value) {
        if (!isManaged()) {
            _linkSid = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__linkSid(this, 1, value));
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
    public BSetUserState() {
        _userState = new Zeze.Transaction.Collections.CollOne<>(new Zeze.Builtin.Provider.BUserState(), Zeze.Builtin.Provider.BUserState.class);
        _userState.variableId(2);
    }

    @SuppressWarnings("deprecation")
    public BSetUserState(long _linkSid_) {
        _linkSid = _linkSid_;
        _userState = new Zeze.Transaction.Collections.CollOne<>(new Zeze.Builtin.Provider.BUserState(), Zeze.Builtin.Provider.BUserState.class);
        _userState.variableId(2);
    }

    public void assign(BSetUserState other) {
        setLinkSid(other.getLinkSid());
        _userState.assign(other._userState);
    }

    public BSetUserState copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BSetUserState copy() {
        var copy = new BSetUserState();
        copy.assign(this);
        return copy;
    }

    public static void swap(BSetUserState a, BSetUserState b) {
        BSetUserState save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__linkSid extends Zeze.Transaction.Logs.LogLong {
        public Log__linkSid(BSetUserState bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BSetUserState)getBelong())._linkSid = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Provider.BSetUserState: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("linkSid=").append(getLinkSid()).append(',').append(System.lineSeparator());
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
            long _x_ = getLinkSid();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 2, ByteBuffer.BEAN);
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
            setLinkSid(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
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
        if (_userState.negativeCheck())
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
                case 1: _linkSid = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 2: _userState.followerApply(vlog); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setLinkSid(rs.getLong(_parents_name_ + "linkSid"));
        parents.add("userState");
        _userState.decodeResultSet(parents, rs);
        parents.remove(parents.size() - 1);
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendLong(_parents_name_ + "linkSid", getLinkSid());
        parents.add("userState");
        _userState.encodeSQLStatement(parents, st);
        parents.remove(parents.size() - 1);
    }
}
