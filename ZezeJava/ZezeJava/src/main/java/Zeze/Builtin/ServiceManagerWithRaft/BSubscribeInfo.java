// auto-generated @formatter:off
package Zeze.Builtin.ServiceManagerWithRaft;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BSubscribeInfo extends Zeze.Transaction.Bean implements BSubscribeInfoReadOnly {
    public static final long TYPEID = 6777856993253287025L;

    private String _ServiceName;
    private int _SubscribeType;

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
    public int getSubscribeType() {
        if (!isManaged())
            return _SubscribeType;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _SubscribeType;
        var log = (Log__SubscribeType)txn.getLog(objectId() + 2);
        return log != null ? log.value : _SubscribeType;
    }

    public void setSubscribeType(int value) {
        if (!isManaged()) {
            _SubscribeType = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__SubscribeType(this, 2, value));
    }

    @SuppressWarnings("deprecation")
    public BSubscribeInfo() {
        _ServiceName = "";
    }

    @SuppressWarnings("deprecation")
    public BSubscribeInfo(String _ServiceName_, int _SubscribeType_) {
        if (_ServiceName_ == null)
            throw new IllegalArgumentException();
        _ServiceName = _ServiceName_;
        _SubscribeType = _SubscribeType_;
    }

    public void assign(BSubscribeInfo other) {
        setServiceName(other.getServiceName());
        setSubscribeType(other.getSubscribeType());
    }

    @Deprecated
    public void Assign(BSubscribeInfo other) {
        assign(other);
    }

    public BSubscribeInfo copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BSubscribeInfo copy() {
        var copy = new BSubscribeInfo();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BSubscribeInfo Copy() {
        return copy();
    }

    public static void swap(BSubscribeInfo a, BSubscribeInfo b) {
        BSubscribeInfo save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ServiceName extends Zeze.Transaction.Logs.LogString {
        public Log__ServiceName(BSubscribeInfo bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BSubscribeInfo)getBelong())._ServiceName = value; }
    }

    private static final class Log__SubscribeType extends Zeze.Transaction.Logs.LogInt {
        public Log__SubscribeType(BSubscribeInfo bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BSubscribeInfo)getBelong())._SubscribeType = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.ServiceManagerWithRaft.BSubscribeInfo: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ServiceName=").append(getServiceName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("SubscribeType=").append(getSubscribeType()).append(System.lineSeparator());
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
            int _x_ = getSubscribeType();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
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
            setSubscribeType(_o_.ReadInt(_t_));
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
        if (getSubscribeType() < 0)
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
                case 2: _SubscribeType = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
            }
        }
    }
}
