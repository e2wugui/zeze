// auto-generated @formatter:off
package Zeze.Builtin.ServiceManagerWithRaft;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BServiceInfos extends Zeze.Transaction.Bean implements BServiceInfosReadOnly {
    public static final long TYPEID = 2349099970207776935L;

    private String _ServiceName;
    private final Zeze.Transaction.Collections.PList2<Zeze.Builtin.ServiceManagerWithRaft.BServiceInfo> _ServiceInfoListSortedByIdentity;
    private long _SerialId;

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

    public Zeze.Transaction.Collections.PList2<Zeze.Builtin.ServiceManagerWithRaft.BServiceInfo> getServiceInfoListSortedByIdentity() {
        return _ServiceInfoListSortedByIdentity;
    }

    @Override
    public Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.ServiceManagerWithRaft.BServiceInfo, Zeze.Builtin.ServiceManagerWithRaft.BServiceInfoReadOnly> getServiceInfoListSortedByIdentityReadOnly() {
        return new Zeze.Transaction.Collections.PList2ReadOnly<>(_ServiceInfoListSortedByIdentity);
    }

    @Override
    public long getSerialId() {
        if (!isManaged())
            return _SerialId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _SerialId;
        var log = (Log__SerialId)txn.getLog(objectId() + 3);
        return log != null ? log.value : _SerialId;
    }

    public void setSerialId(long value) {
        if (!isManaged()) {
            _SerialId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__SerialId(this, 3, value));
    }

    @SuppressWarnings("deprecation")
    public BServiceInfos() {
        _ServiceName = "";
        _ServiceInfoListSortedByIdentity = new Zeze.Transaction.Collections.PList2<>(Zeze.Builtin.ServiceManagerWithRaft.BServiceInfo.class);
        _ServiceInfoListSortedByIdentity.variableId(2);
    }

    @SuppressWarnings("deprecation")
    public BServiceInfos(String _ServiceName_, long _SerialId_) {
        if (_ServiceName_ == null)
            throw new IllegalArgumentException();
        _ServiceName = _ServiceName_;
        _ServiceInfoListSortedByIdentity = new Zeze.Transaction.Collections.PList2<>(Zeze.Builtin.ServiceManagerWithRaft.BServiceInfo.class);
        _ServiceInfoListSortedByIdentity.variableId(2);
        _SerialId = _SerialId_;
    }

    public void assign(BServiceInfos other) {
        setServiceName(other.getServiceName());
        _ServiceInfoListSortedByIdentity.clear();
        for (var e : other._ServiceInfoListSortedByIdentity)
            _ServiceInfoListSortedByIdentity.add(e.copy());
        setSerialId(other.getSerialId());
    }

    @Deprecated
    public void Assign(BServiceInfos other) {
        assign(other);
    }

    public BServiceInfos copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BServiceInfos copy() {
        var copy = new BServiceInfos();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BServiceInfos Copy() {
        return copy();
    }

    public static void swap(BServiceInfos a, BServiceInfos b) {
        BServiceInfos save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ServiceName extends Zeze.Transaction.Logs.LogString {
        public Log__ServiceName(BServiceInfos bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BServiceInfos)getBelong())._ServiceName = value; }
    }

    private static final class Log__SerialId extends Zeze.Transaction.Logs.LogLong {
        public Log__SerialId(BServiceInfos bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BServiceInfos)getBelong())._SerialId = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.ServiceManagerWithRaft.BServiceInfos: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ServiceName=").append(getServiceName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ServiceInfoListSortedByIdentity=[");
        if (!_ServiceInfoListSortedByIdentity.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _ServiceInfoListSortedByIdentity) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(System.lineSeparator());
                _item_.buildString(sb, level + 4);
                sb.append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("SerialId=").append(getSerialId()).append(System.lineSeparator());
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
            var _x_ = _ServiceInfoListSortedByIdentity;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BEAN);
                for (var _v_ : _x_)
                    _v_.encode(_o_);
            }
        }
        {
            long _x_ = getSerialId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
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
            var _x_ = _ServiceInfoListSortedByIdentity;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new Zeze.Builtin.ServiceManagerWithRaft.BServiceInfo(), _t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setSerialId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _ServiceInfoListSortedByIdentity.initRootInfo(root, this);
    }

    @Override
    protected void resetChildrenRootInfo() {
        _ServiceInfoListSortedByIdentity.resetRootInfo();
    }

    @Override
    public boolean negativeCheck() {
        for (var _v_ : _ServiceInfoListSortedByIdentity) {
            if (_v_.negativeCheck())
                return true;
        }
        if (getSerialId() < 0)
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
                case 2: _ServiceInfoListSortedByIdentity.followerApply(vlog); break;
                case 3: _SerialId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
            }
        }
    }
}
