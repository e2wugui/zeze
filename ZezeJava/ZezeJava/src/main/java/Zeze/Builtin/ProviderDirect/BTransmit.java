// auto-generated @formatter:off
package Zeze.Builtin.ProviderDirect;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BTransmit extends Zeze.Transaction.Bean {
    private String _ActionName;
    private final Zeze.Transaction.Collections.PSet1<Long> _Roles; // 查询目标角色。
    private long _Sender; // 结果发送给Sender。
    private Zeze.Net.Binary _Parameter; // encoded bean

    public String getActionName() {
        if (!isManaged())
            return _ActionName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ActionName;
        var log = (Log__ActionName)txn.GetLog(objectId() + 1);
        return log != null ? log.Value : _ActionName;
    }

    public void setActionName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ActionName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.PutLog(new Log__ActionName(this, 1, value));
    }

    public Zeze.Transaction.Collections.PSet1<Long> getRoles() {
        return _Roles;
    }

    public long getSender() {
        if (!isManaged())
            return _Sender;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Sender;
        var log = (Log__Sender)txn.GetLog(objectId() + 3);
        return log != null ? log.Value : _Sender;
    }

    public void setSender(long value) {
        if (!isManaged()) {
            _Sender = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.PutLog(new Log__Sender(this, 3, value));
    }

    public Zeze.Net.Binary getParameter() {
        if (!isManaged())
            return _Parameter;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Parameter;
        var log = (Log__Parameter)txn.GetLog(objectId() + 4);
        return log != null ? log.Value : _Parameter;
    }

    public void setParameter(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Parameter = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.PutLog(new Log__Parameter(this, 4, value));
    }

    public BTransmit() {
        _ActionName = "";
        _Roles = new Zeze.Transaction.Collections.PSet1<>(Long.class);
        _Roles.VariableId = 2;
        _Parameter = Zeze.Net.Binary.Empty;
    }

    public BTransmit(String _ActionName_, long _Sender_, Zeze.Net.Binary _Parameter_) {
        if (_ActionName_ == null)
            throw new IllegalArgumentException();
        _ActionName = _ActionName_;
        _Roles = new Zeze.Transaction.Collections.PSet1<>(Long.class);
        _Roles.VariableId = 2;
        _Sender = _Sender_;
        if (_Parameter_ == null)
            throw new IllegalArgumentException();
        _Parameter = _Parameter_;
    }

    public void Assign(BTransmit other) {
        setActionName(other.getActionName());
        getRoles().clear();
        for (var e : other.getRoles())
            getRoles().add(e);
        setSender(other.getSender());
        setParameter(other.getParameter());
    }

    public BTransmit CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BTransmit Copy() {
        var copy = new BTransmit();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BTransmit a, BTransmit b) {
        BTransmit save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public BTransmit CopyBean() {
        return Copy();
    }

    public static final long TYPEID = 7395081565293443928L;

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ActionName extends Zeze.Transaction.Logs.LogString {
        public Log__ActionName(BTransmit bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BTransmit)getBelong())._ActionName = Value; }
    }

    private static final class Log__Sender extends Zeze.Transaction.Logs.LogLong {
        public Log__Sender(BTransmit bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BTransmit)getBelong())._Sender = Value; }
    }

    private static final class Log__Parameter extends Zeze.Transaction.Logs.LogBinary {
        public Log__Parameter(BTransmit bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BTransmit)getBelong())._Parameter = Value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        BuildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void BuildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.ProviderDirect.BTransmit: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ActionName").append('=').append(getActionName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Roles").append("=[").append(System.lineSeparator());
        level += 4;
        for (var _item_ : getRoles()) {
            sb.append(Zeze.Util.Str.indent(level)).append("Item").append('=').append(_item_).append(',').append(System.lineSeparator());
        }
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Sender").append('=').append(getSender()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Parameter").append('=').append(getParameter()).append(System.lineSeparator());
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
    public void Encode(ByteBuffer _o_) {
        int _i_ = 0;
        {
            String _x_ = getActionName();
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
        _o_.WriteByte(0);
    }

    @Override
    public void Decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setActionName(_o_.ReadString(_t_));
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
            setSender(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setParameter(_o_.ReadBinary(_t_));
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
    protected void ResetChildrenRootInfo() {
        _Roles.ResetRootInfo();
    }

    @Override
    public boolean NegativeCheck() {
        for (var _v_ : getRoles()) {
            if (_v_ < 0)
                return true;
        }
        if (getSender() < 0)
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
                case 1: _ActionName = ((Zeze.Transaction.Logs.LogString)vlog).Value; break;
                case 2: _Roles.FollowerApply(vlog); break;
                case 3: _Sender = ((Zeze.Transaction.Logs.LogLong)vlog).Value; break;
                case 4: _Parameter = ((Zeze.Transaction.Logs.LogBinary)vlog).Value; break;
            }
        }
    }
}
