// auto-generated @formatter:off
package Zeze.Builtin.Provider;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
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

    @Override
    public void reset() {
        setLinkSid(0);
        _userState.reset();
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Provider.BSetUserState.Data toData() {
        var data = new Zeze.Builtin.Provider.BSetUserState.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Provider.BSetUserState.Data)other);
    }

    public void assign(BSetUserState.Data other) {
        setLinkSid(other._linkSid);
        Zeze.Builtin.Provider.BUserState data_userState = new Zeze.Builtin.Provider.BUserState();
        data_userState.assign(other._userState);
        _userState.setValue(data_userState);
        _unknown_ = null;
    }

    public void assign(BSetUserState other) {
        setLinkSid(other.getLinkSid());
        _userState.assign(other._userState);
        _unknown_ = other._unknown_;
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
        _o_.writeAllUnknownFields(_i_, _ui_, _u_);
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        ByteBuffer _u_ = null;
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
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
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

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "linkSid", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "userState", "Zeze.Builtin.Provider.BUserState", "", ""));
        return vars;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -4860388989628287875L;

    private long _linkSid;
    private Zeze.Builtin.Provider.BUserState.Data _userState;

    public long getLinkSid() {
        return _linkSid;
    }

    public void setLinkSid(long value) {
        _linkSid = value;
    }

    public Zeze.Builtin.Provider.BUserState.Data getUserState() {
        return _userState;
    }

    public void setUserState(Zeze.Builtin.Provider.BUserState.Data value) {
        if (value == null)
            throw new IllegalArgumentException();
        _userState = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _userState = new Zeze.Builtin.Provider.BUserState.Data();
    }

    @SuppressWarnings("deprecation")
    public Data(long _linkSid_, Zeze.Builtin.Provider.BUserState.Data _userState_) {
        _linkSid = _linkSid_;
        if (_userState_ == null)
            _userState_ = new Zeze.Builtin.Provider.BUserState.Data();
        _userState = _userState_;
    }

    @Override
    public void reset() {
        _linkSid = 0;
        _userState.reset();
    }

    @Override
    public Zeze.Builtin.Provider.BSetUserState toBean() {
        var bean = new Zeze.Builtin.Provider.BSetUserState();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BSetUserState)other);
    }

    public void assign(BSetUserState other) {
        _linkSid = other.getLinkSid();
        _userState.assign(other._userState.getValue());
    }

    public void assign(BSetUserState.Data other) {
        _linkSid = other._linkSid;
        _userState.assign(other._userState);
    }

    @Override
    public BSetUserState.Data copy() {
        var copy = new BSetUserState.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BSetUserState.Data a, BSetUserState.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BSetUserState.Data clone() {
        return (BSetUserState.Data)super.clone();
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
        sb.append(Zeze.Util.Str.indent(level)).append("linkSid=").append(_linkSid).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("userState=").append(System.lineSeparator());
        _userState.buildString(sb, level + 4);
        sb.append(System.lineSeparator());
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append('}');
    }

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
            long _x_ = _linkSid;
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
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _linkSid = _o_.ReadLong(_t_);
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
}
}
