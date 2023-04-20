// auto-generated @formatter:off
package Zeze.Builtin.ProviderDirect;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BTransmit extends Zeze.Transaction.Bean implements BTransmitReadOnly {
    public static final long TYPEID = 7395081565293443928L;

    private String _ActionName;
    private final Zeze.Transaction.Collections.PSet1<Long> _Roles; // 查询目标角色。
    private long _Sender; // 结果发送给Sender。
    private Zeze.Net.Binary _Parameter; // encoded bean
    private String _OnlineSetName;

    @Override
    public String getActionName() {
        if (!isManaged())
            return _ActionName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ActionName;
        var log = (Log__ActionName)txn.getLog(objectId() + 1);
        return log != null ? log.value : _ActionName;
    }

    public void setActionName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ActionName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ActionName(this, 1, value));
    }

    public Zeze.Transaction.Collections.PSet1<Long> getRoles() {
        return _Roles;
    }

    @Override
    public Zeze.Transaction.Collections.PSet1ReadOnly<Long> getRolesReadOnly() {
        return new Zeze.Transaction.Collections.PSet1ReadOnly<>(_Roles);
    }

    @Override
    public long getSender() {
        if (!isManaged())
            return _Sender;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Sender;
        var log = (Log__Sender)txn.getLog(objectId() + 3);
        return log != null ? log.value : _Sender;
    }

    public void setSender(long value) {
        if (!isManaged()) {
            _Sender = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Sender(this, 3, value));
    }

    @Override
    public Zeze.Net.Binary getParameter() {
        if (!isManaged())
            return _Parameter;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Parameter;
        var log = (Log__Parameter)txn.getLog(objectId() + 4);
        return log != null ? log.value : _Parameter;
    }

    public void setParameter(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Parameter = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Parameter(this, 4, value));
    }

    @Override
    public String getOnlineSetName() {
        if (!isManaged())
            return _OnlineSetName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _OnlineSetName;
        var log = (Log__OnlineSetName)txn.getLog(objectId() + 5);
        return log != null ? log.value : _OnlineSetName;
    }

    public void setOnlineSetName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _OnlineSetName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__OnlineSetName(this, 5, value));
    }

    @SuppressWarnings("deprecation")
    public BTransmit() {
        _ActionName = "";
        _Roles = new Zeze.Transaction.Collections.PSet1<>(Long.class);
        _Roles.variableId(2);
        _Parameter = Zeze.Net.Binary.Empty;
        _OnlineSetName = "";
    }

    @SuppressWarnings("deprecation")
    public BTransmit(String _ActionName_, long _Sender_, Zeze.Net.Binary _Parameter_, String _OnlineSetName_) {
        if (_ActionName_ == null)
            throw new IllegalArgumentException();
        _ActionName = _ActionName_;
        _Roles = new Zeze.Transaction.Collections.PSet1<>(Long.class);
        _Roles.variableId(2);
        _Sender = _Sender_;
        if (_Parameter_ == null)
            throw new IllegalArgumentException();
        _Parameter = _Parameter_;
        if (_OnlineSetName_ == null)
            throw new IllegalArgumentException();
        _OnlineSetName = _OnlineSetName_;
    }

    public void assign(BTransmit other) {
        setActionName(other.getActionName());
        _Roles.clear();
        _Roles.addAll(other.getRoles());
        setSender(other.getSender());
        setParameter(other.getParameter());
        setOnlineSetName(other.getOnlineSetName());
    }

    public BTransmit copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTransmit copy() {
        var copy = new BTransmit();
        copy.assign(this);
        return copy;
    }

    public static void swap(BTransmit a, BTransmit b) {
        BTransmit save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ActionName extends Zeze.Transaction.Logs.LogString {
        public Log__ActionName(BTransmit bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTransmit)getBelong())._ActionName = value; }
    }

    private static final class Log__Sender extends Zeze.Transaction.Logs.LogLong {
        public Log__Sender(BTransmit bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTransmit)getBelong())._Sender = value; }
    }

    private static final class Log__Parameter extends Zeze.Transaction.Logs.LogBinary {
        public Log__Parameter(BTransmit bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTransmit)getBelong())._Parameter = value; }
    }

    private static final class Log__OnlineSetName extends Zeze.Transaction.Logs.LogString {
        public Log__OnlineSetName(BTransmit bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTransmit)getBelong())._OnlineSetName = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.ProviderDirect.BTransmit: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ActionName=").append(getActionName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Roles={");
        if (!_Roles.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _Roles) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(_item_).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append('}').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Sender=").append(getSender()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Parameter=").append(getParameter()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("OnlineSetName=").append(getOnlineSetName()).append(System.lineSeparator());
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
            String _x_ = getActionName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = _Roles;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                for (var _v_ : _x_)
                    _o_.WriteLong(_v_);
            }
        }
        {
            long _x_ = getSender();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = getParameter();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            String _x_ = getOnlineSetName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setActionName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = _Roles;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadLong(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setSender(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setParameter(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setOnlineSetName(_o_.ReadString(_t_));
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
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _Roles.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
        for (var _v_ : _Roles) {
            if (_v_ < 0)
                return true;
        }
        if (getSender() < 0)
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
                case 1: _ActionName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 2: _Roles.followerApply(vlog); break;
                case 3: _Sender = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 4: _Parameter = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
                case 5: _OnlineSetName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setActionName(rs.getString(_parents_name_ + "ActionName"));
        if (getActionName() == null)
            setActionName("");
        Zeze.Serialize.Helper.decodeJsonSet(getRoles(), Long.class, rs.getString(_parents_name_ + "Roles"));
        setSender(rs.getLong(_parents_name_ + "Sender"));
        setParameter(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "Parameter")));
        if (getParameter() == null)
            setParameter(Zeze.Net.Binary.Empty);
        setOnlineSetName(rs.getString(_parents_name_ + "OnlineSetName"));
        if (getOnlineSetName() == null)
            setOnlineSetName("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "ActionName", getActionName());
        st.appendString(_parents_name_ + "Roles", Zeze.Serialize.Helper.encodeJson(getRoles()));
        st.appendLong(_parents_name_ + "Sender", getSender());
        st.appendBinary(_parents_name_ + "Parameter", getParameter());
        st.appendString(_parents_name_ + "OnlineSetName", getOnlineSetName());
    }
}
