// auto-generated @formatter:off
package Zeze.Builtin.ServiceManagerWithRaft;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BServiceInfo extends Zeze.Transaction.Bean implements BServiceInfoReadOnly {
    public static final long TYPEID = -8052613410984017450L;

    private String _ServiceName;
    private String _ServiceIdentity;
    private String _PassiveIp;
    private int _PassivePort;
    private Zeze.Net.Binary _ExtraInfo;

    @Override
    public String getServiceName() {
        if (!isManaged())
            return _ServiceName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ServiceName;
        var log = (Log__ServiceName)txn.getLog(objectId() + 1);
        return log != null ? log.value : _ServiceName;
    }

    public void setServiceName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ServiceName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ServiceName(this, 1, value));
    }

    @Override
    public String getServiceIdentity() {
        if (!isManaged())
            return _ServiceIdentity;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ServiceIdentity;
        var log = (Log__ServiceIdentity)txn.getLog(objectId() + 2);
        return log != null ? log.value : _ServiceIdentity;
    }

    public void setServiceIdentity(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ServiceIdentity = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ServiceIdentity(this, 2, value));
    }

    @Override
    public String getPassiveIp() {
        if (!isManaged())
            return _PassiveIp;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _PassiveIp;
        var log = (Log__PassiveIp)txn.getLog(objectId() + 3);
        return log != null ? log.value : _PassiveIp;
    }

    public void setPassiveIp(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _PassiveIp = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__PassiveIp(this, 3, value));
    }

    @Override
    public int getPassivePort() {
        if (!isManaged())
            return _PassivePort;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _PassivePort;
        var log = (Log__PassivePort)txn.getLog(objectId() + 4);
        return log != null ? log.value : _PassivePort;
    }

    public void setPassivePort(int value) {
        if (!isManaged()) {
            _PassivePort = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__PassivePort(this, 4, value));
    }

    @Override
    public Zeze.Net.Binary getExtraInfo() {
        if (!isManaged())
            return _ExtraInfo;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ExtraInfo;
        var log = (Log__ExtraInfo)txn.getLog(objectId() + 5);
        return log != null ? log.value : _ExtraInfo;
    }

    public void setExtraInfo(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ExtraInfo = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ExtraInfo(this, 5, value));
    }

    @SuppressWarnings("deprecation")
    public BServiceInfo() {
        _ServiceName = "";
        _ServiceIdentity = "";
        _PassiveIp = "";
        _ExtraInfo = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BServiceInfo(String _ServiceName_, String _ServiceIdentity_, String _PassiveIp_, int _PassivePort_, Zeze.Net.Binary _ExtraInfo_) {
        if (_ServiceName_ == null)
            throw new IllegalArgumentException();
        _ServiceName = _ServiceName_;
        if (_ServiceIdentity_ == null)
            throw new IllegalArgumentException();
        _ServiceIdentity = _ServiceIdentity_;
        if (_PassiveIp_ == null)
            throw new IllegalArgumentException();
        _PassiveIp = _PassiveIp_;
        _PassivePort = _PassivePort_;
        if (_ExtraInfo_ == null)
            throw new IllegalArgumentException();
        _ExtraInfo = _ExtraInfo_;
    }

    public void assign(BServiceInfo other) {
        setServiceName(other.getServiceName());
        setServiceIdentity(other.getServiceIdentity());
        setPassiveIp(other.getPassiveIp());
        setPassivePort(other.getPassivePort());
        setExtraInfo(other.getExtraInfo());
    }

    @Deprecated
    public void Assign(BServiceInfo other) {
        assign(other);
    }

    public BServiceInfo copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BServiceInfo copy() {
        var copy = new BServiceInfo();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BServiceInfo Copy() {
        return copy();
    }

    public static void swap(BServiceInfo a, BServiceInfo b) {
        BServiceInfo save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ServiceName extends Zeze.Transaction.Logs.LogString {
        public Log__ServiceName(BServiceInfo bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BServiceInfo)getBelong())._ServiceName = value; }
    }

    private static final class Log__ServiceIdentity extends Zeze.Transaction.Logs.LogString {
        public Log__ServiceIdentity(BServiceInfo bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BServiceInfo)getBelong())._ServiceIdentity = value; }
    }

    private static final class Log__PassiveIp extends Zeze.Transaction.Logs.LogString {
        public Log__PassiveIp(BServiceInfo bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BServiceInfo)getBelong())._PassiveIp = value; }
    }

    private static final class Log__PassivePort extends Zeze.Transaction.Logs.LogInt {
        public Log__PassivePort(BServiceInfo bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BServiceInfo)getBelong())._PassivePort = value; }
    }

    private static final class Log__ExtraInfo extends Zeze.Transaction.Logs.LogBinary {
        public Log__ExtraInfo(BServiceInfo bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BServiceInfo)getBelong())._ExtraInfo = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.ServiceManagerWithRaft.BServiceInfo: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ServiceName=").append(getServiceName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ServiceIdentity=").append(getServiceIdentity()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("PassiveIp=").append(getPassiveIp()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("PassivePort=").append(getPassivePort()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ExtraInfo=").append(getExtraInfo()).append(System.lineSeparator());
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
            String _x_ = getServiceName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getServiceIdentity();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getPassiveIp();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            int _x_ = getPassivePort();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = getExtraInfo();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setServiceName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setServiceIdentity(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setPassiveIp(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setPassivePort(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setExtraInfo(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
    }

    @Override
    protected void resetChildrenRootInfo() {
    }

    @Override
    public boolean negativeCheck() {
        if (getPassivePort() < 0)
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
                case 1: _ServiceName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 2: _ServiceIdentity = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 3: _PassiveIp = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 4: _PassivePort = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 5: _ExtraInfo = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
            }
        }
    }
}
