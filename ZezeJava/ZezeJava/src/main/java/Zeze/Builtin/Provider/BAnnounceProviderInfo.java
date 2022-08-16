// auto-generated @formatter:off
package Zeze.Builtin.Provider;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BAnnounceProviderInfo extends Zeze.Transaction.Bean {
    private String _ServiceNamePrefix;
    private String _ServiceIndentity;
    private String _ProviderDirectIp;
    private int _ProviderDirectPort;

    public String getServiceNamePrefix() {
        if (!isManaged())
            return _ServiceNamePrefix;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ServiceNamePrefix;
        var log = (Log__ServiceNamePrefix)txn.GetLog(objectId() + 1);
        return log != null ? log.Value : _ServiceNamePrefix;
    }

    public void setServiceNamePrefix(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ServiceNamePrefix = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.PutLog(new Log__ServiceNamePrefix(this, 1, value));
    }

    public String getServiceIndentity() {
        if (!isManaged())
            return _ServiceIndentity;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ServiceIndentity;
        var log = (Log__ServiceIndentity)txn.GetLog(objectId() + 2);
        return log != null ? log.Value : _ServiceIndentity;
    }

    public void setServiceIndentity(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ServiceIndentity = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.PutLog(new Log__ServiceIndentity(this, 2, value));
    }

    public String getProviderDirectIp() {
        if (!isManaged())
            return _ProviderDirectIp;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ProviderDirectIp;
        var log = (Log__ProviderDirectIp)txn.GetLog(objectId() + 3);
        return log != null ? log.Value : _ProviderDirectIp;
    }

    public void setProviderDirectIp(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ProviderDirectIp = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.PutLog(new Log__ProviderDirectIp(this, 3, value));
    }

    public int getProviderDirectPort() {
        if (!isManaged())
            return _ProviderDirectPort;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ProviderDirectPort;
        var log = (Log__ProviderDirectPort)txn.GetLog(objectId() + 4);
        return log != null ? log.Value : _ProviderDirectPort;
    }

    public void setProviderDirectPort(int value) {
        if (!isManaged()) {
            _ProviderDirectPort = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.PutLog(new Log__ProviderDirectPort(this, 4, value));
    }

    public BAnnounceProviderInfo() {
        _ServiceNamePrefix = "";
        _ServiceIndentity = "";
        _ProviderDirectIp = "";
    }

    public BAnnounceProviderInfo(String _ServiceNamePrefix_, String _ServiceIndentity_, String _ProviderDirectIp_, int _ProviderDirectPort_) {
        if (_ServiceNamePrefix_ == null)
            throw new IllegalArgumentException();
        _ServiceNamePrefix = _ServiceNamePrefix_;
        if (_ServiceIndentity_ == null)
            throw new IllegalArgumentException();
        _ServiceIndentity = _ServiceIndentity_;
        if (_ProviderDirectIp_ == null)
            throw new IllegalArgumentException();
        _ProviderDirectIp = _ProviderDirectIp_;
        _ProviderDirectPort = _ProviderDirectPort_;
    }

    public void Assign(BAnnounceProviderInfo other) {
        setServiceNamePrefix(other.getServiceNamePrefix());
        setServiceIndentity(other.getServiceIndentity());
        setProviderDirectIp(other.getProviderDirectIp());
        setProviderDirectPort(other.getProviderDirectPort());
    }

    public BAnnounceProviderInfo CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BAnnounceProviderInfo Copy() {
        var copy = new BAnnounceProviderInfo();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BAnnounceProviderInfo a, BAnnounceProviderInfo b) {
        BAnnounceProviderInfo save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public BAnnounceProviderInfo CopyBean() {
        return Copy();
    }

    public static final long TYPEID = 4964769950995033065L;

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ServiceNamePrefix extends Zeze.Transaction.Logs.LogString {
        public Log__ServiceNamePrefix(BAnnounceProviderInfo bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BAnnounceProviderInfo)getBelong())._ServiceNamePrefix = Value; }
    }

    private static final class Log__ServiceIndentity extends Zeze.Transaction.Logs.LogString {
        public Log__ServiceIndentity(BAnnounceProviderInfo bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BAnnounceProviderInfo)getBelong())._ServiceIndentity = Value; }
    }

    private static final class Log__ProviderDirectIp extends Zeze.Transaction.Logs.LogString {
        public Log__ProviderDirectIp(BAnnounceProviderInfo bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BAnnounceProviderInfo)getBelong())._ProviderDirectIp = Value; }
    }

    private static final class Log__ProviderDirectPort extends Zeze.Transaction.Logs.LogInt {
        public Log__ProviderDirectPort(BAnnounceProviderInfo bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BAnnounceProviderInfo)getBelong())._ProviderDirectPort = Value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        BuildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void BuildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Provider.BAnnounceProviderInfo: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ServiceNamePrefix").append('=').append(getServiceNamePrefix()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ServiceIndentity").append('=').append(getServiceIndentity()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ProviderDirectIp").append('=').append(getProviderDirectIp()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ProviderDirectPort").append('=').append(getProviderDirectPort()).append(System.lineSeparator());
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
            String _x_ = getServiceNamePrefix();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getServiceIndentity();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getProviderDirectIp();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            int _x_ = getProviderDirectPort();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void Decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setServiceNamePrefix(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setServiceIndentity(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setProviderDirectIp(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setProviderDirectPort(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
    }

    @Override
    protected void ResetChildrenRootInfo() {
    }

    @Override
    public boolean NegativeCheck() {
        if (getProviderDirectPort() < 0)
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
                case 1: _ServiceNamePrefix = ((Zeze.Transaction.Logs.LogString)vlog).Value; break;
                case 2: _ServiceIndentity = ((Zeze.Transaction.Logs.LogString)vlog).Value; break;
                case 3: _ProviderDirectIp = ((Zeze.Transaction.Logs.LogString)vlog).Value; break;
                case 4: _ProviderDirectPort = ((Zeze.Transaction.Logs.LogInt)vlog).Value; break;
            }
        }
    }
}
