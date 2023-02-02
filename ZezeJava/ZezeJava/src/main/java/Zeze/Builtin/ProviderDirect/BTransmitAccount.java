// auto-generated @formatter:off
package Zeze.Builtin.ProviderDirect;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BTransmitAccount extends Zeze.Transaction.Bean implements BTransmitAccountReadOnly {
    public static final long TYPEID = 2637210793748287339L;

    private String _ActionName;
    private Zeze.Net.Binary _Parameter; // encoded bean
    private final Zeze.Transaction.Collections.PSet1<String> _TargetAccounts; // 查询目标角色。
    private String _SenderAccount; // 结果发送给Sender。
    private String _SenderClientId; // 结果发送给Sender。

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

    @Override
    public Zeze.Net.Binary getParameter() {
        if (!isManaged())
            return _Parameter;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Parameter;
        var log = (Log__Parameter)txn.getLog(objectId() + 2);
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
        txn.putLog(new Log__Parameter(this, 2, value));
    }

    public Zeze.Transaction.Collections.PSet1<String> getTargetAccounts() {
        return _TargetAccounts;
    }

    @Override
    public Zeze.Transaction.Collections.PSet1ReadOnly<String> getTargetAccountsReadOnly() {
        return new Zeze.Transaction.Collections.PSet1ReadOnly<>(_TargetAccounts);
    }

    @Override
    public String getSenderAccount() {
        if (!isManaged())
            return _SenderAccount;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _SenderAccount;
        var log = (Log__SenderAccount)txn.getLog(objectId() + 4);
        return log != null ? log.value : _SenderAccount;
    }

    public void setSenderAccount(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _SenderAccount = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__SenderAccount(this, 4, value));
    }

    @Override
    public String getSenderClientId() {
        if (!isManaged())
            return _SenderClientId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _SenderClientId;
        var log = (Log__SenderClientId)txn.getLog(objectId() + 5);
        return log != null ? log.value : _SenderClientId;
    }

    public void setSenderClientId(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _SenderClientId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__SenderClientId(this, 5, value));
    }

    @SuppressWarnings("deprecation")
    public BTransmitAccount() {
        _ActionName = "";
        _Parameter = Zeze.Net.Binary.Empty;
        _TargetAccounts = new Zeze.Transaction.Collections.PSet1<>(String.class);
        _TargetAccounts.variableId(3);
        _SenderAccount = "";
        _SenderClientId = "";
    }

    @SuppressWarnings("deprecation")
    public BTransmitAccount(String _ActionName_, Zeze.Net.Binary _Parameter_, String _SenderAccount_, String _SenderClientId_) {
        if (_ActionName_ == null)
            throw new IllegalArgumentException();
        _ActionName = _ActionName_;
        if (_Parameter_ == null)
            throw new IllegalArgumentException();
        _Parameter = _Parameter_;
        _TargetAccounts = new Zeze.Transaction.Collections.PSet1<>(String.class);
        _TargetAccounts.variableId(3);
        if (_SenderAccount_ == null)
            throw new IllegalArgumentException();
        _SenderAccount = _SenderAccount_;
        if (_SenderClientId_ == null)
            throw new IllegalArgumentException();
        _SenderClientId = _SenderClientId_;
    }

    public void assign(BTransmitAccount other) {
        setActionName(other.getActionName());
        setParameter(other.getParameter());
        _TargetAccounts.clear();
        _TargetAccounts.addAll(other._TargetAccounts);
        setSenderAccount(other.getSenderAccount());
        setSenderClientId(other.getSenderClientId());
    }

    @Deprecated
    public void Assign(BTransmitAccount other) {
        assign(other);
    }

    public BTransmitAccount copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTransmitAccount copy() {
        var copy = new BTransmitAccount();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BTransmitAccount Copy() {
        return copy();
    }

    public static void swap(BTransmitAccount a, BTransmitAccount b) {
        BTransmitAccount save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ActionName extends Zeze.Transaction.Logs.LogString {
        public Log__ActionName(BTransmitAccount bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTransmitAccount)getBelong())._ActionName = value; }
    }

    private static final class Log__Parameter extends Zeze.Transaction.Logs.LogBinary {
        public Log__Parameter(BTransmitAccount bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTransmitAccount)getBelong())._Parameter = value; }
    }

    private static final class Log__SenderAccount extends Zeze.Transaction.Logs.LogString {
        public Log__SenderAccount(BTransmitAccount bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTransmitAccount)getBelong())._SenderAccount = value; }
    }

    private static final class Log__SenderClientId extends Zeze.Transaction.Logs.LogString {
        public Log__SenderClientId(BTransmitAccount bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTransmitAccount)getBelong())._SenderClientId = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.ProviderDirect.BTransmitAccount: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ActionName=").append(getActionName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Parameter=").append(getParameter()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("TargetAccounts={");
        if (!_TargetAccounts.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _TargetAccounts) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(_item_).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append('}').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("SenderAccount=").append(getSenderAccount()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("SenderClientId=").append(getSenderClientId()).append(System.lineSeparator());
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
            var _x_ = getParameter();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            var _x_ = _TargetAccounts;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BYTES);
                for (var _v_ : _x_)
                    _o_.WriteString(_v_);
            }
        }
        {
            String _x_ = getSenderAccount();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getSenderClientId();
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
            setParameter(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            var _x_ = _TargetAccounts;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadString(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setSenderAccount(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setSenderClientId(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _TargetAccounts.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _TargetAccounts.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
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
                case 2: _Parameter = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
                case 3: _TargetAccounts.followerApply(vlog); break;
                case 4: _SenderAccount = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 5: _SenderClientId = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
            }
        }
    }
}
