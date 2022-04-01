// auto-generated @formatter:off
package Zeze.Beans.Provider2;

import Zeze.Serialize.ByteBuffer;

public final class BTransmit extends Zeze.Transaction.Bean {
    private String _ActionName;
    private final Zeze.Transaction.Collections.PMap2<Long, Zeze.Beans.Provider2.BTransmitContext> _Roles; // 查询目标角色。
    private long _Sender; // 结果发送给Sender。
    private String _ServiceNamePrefix;
    private String _ParameterBeanName; // fullname
    private Zeze.Net.Binary _ParameterBeanValue; // encoded bean

    public String getActionName() {
        if (!isManaged())
            return _ActionName;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _ActionName;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ActionName)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.getValue() : _ActionName;
    }

    public void setActionName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ActionName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__ActionName(this, value));
    }

    public Zeze.Transaction.Collections.PMap2<Long, Zeze.Beans.Provider2.BTransmitContext> getRoles() {
        return _Roles;
    }

    public long getSender() {
        if (!isManaged())
            return _Sender;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _Sender;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__Sender)txn.GetLog(this.getObjectId() + 3);
        return log != null ? log.getValue() : _Sender;
    }

    public void setSender(long value) {
        if (!isManaged()) {
            _Sender = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__Sender(this, value));
    }

    public String getServiceNamePrefix() {
        if (!isManaged())
            return _ServiceNamePrefix;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _ServiceNamePrefix;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ServiceNamePrefix)txn.GetLog(this.getObjectId() + 4);
        return log != null ? log.getValue() : _ServiceNamePrefix;
    }

    public void setServiceNamePrefix(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ServiceNamePrefix = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__ServiceNamePrefix(this, value));
    }

    public String getParameterBeanName() {
        if (!isManaged())
            return _ParameterBeanName;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _ParameterBeanName;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ParameterBeanName)txn.GetLog(this.getObjectId() + 5);
        return log != null ? log.getValue() : _ParameterBeanName;
    }

    public void setParameterBeanName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ParameterBeanName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__ParameterBeanName(this, value));
    }

    public Zeze.Net.Binary getParameterBeanValue() {
        if (!isManaged())
            return _ParameterBeanValue;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _ParameterBeanValue;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ParameterBeanValue)txn.GetLog(this.getObjectId() + 6);
        return log != null ? log.getValue() : _ParameterBeanValue;
    }

    public void setParameterBeanValue(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ParameterBeanValue = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__ParameterBeanValue(this, value));
    }

    public BTransmit() {
         this(0);
    }

    public BTransmit(int _varId_) {
        super(_varId_);
        _ActionName = "";
        _Roles = new Zeze.Transaction.Collections.PMap2<>(getObjectId() + 2, (_v) -> new Log__Roles(this, _v));
        _ServiceNamePrefix = "";
        _ParameterBeanName = "";
        _ParameterBeanValue = Zeze.Net.Binary.Empty;
    }

    public void Assign(BTransmit other) {
        setActionName(other.getActionName());
        getRoles().clear();
        for (var e : other.getRoles().entrySet())
            getRoles().put(e.getKey(), e.getValue().Copy());
        setSender(other.getSender());
        setServiceNamePrefix(other.getServiceNamePrefix());
        setParameterBeanName(other.getParameterBeanName());
        setParameterBeanValue(other.getParameterBeanValue());
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
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public static final long TYPEID = 945069410197099269L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private static final class Log__ActionName extends Zeze.Transaction.Log1<BTransmit, String> {
        public Log__ActionName(BTransmit self, String value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 1; }
        @Override
        public void Commit() { this.getBeanTyped()._ActionName = this.getValue(); }
    }

    private static final class Log__Roles extends Zeze.Transaction.Collections.PMap.LogV<Long, Zeze.Beans.Provider2.BTransmitContext> {
        public Log__Roles(BTransmit host, org.pcollections.PMap<Long, Zeze.Beans.Provider2.BTransmitContext> value) { super(host, value); }
        @Override
        public long getLogKey() { return getBean().getObjectId() + 2; }
        public BTransmit getBeanTyped() { return (BTransmit)getBean(); }
        @Override
        public void Commit() { Commit(getBeanTyped()._Roles); }
    }

    private static final class Log__Sender extends Zeze.Transaction.Log1<BTransmit, Long> {
        public Log__Sender(BTransmit self, Long value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 3; }
        @Override
        public void Commit() { this.getBeanTyped()._Sender = this.getValue(); }
    }

    private static final class Log__ServiceNamePrefix extends Zeze.Transaction.Log1<BTransmit, String> {
        public Log__ServiceNamePrefix(BTransmit self, String value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 4; }
        @Override
        public void Commit() { this.getBeanTyped()._ServiceNamePrefix = this.getValue(); }
    }

    private static final class Log__ParameterBeanName extends Zeze.Transaction.Log1<BTransmit, String> {
        public Log__ParameterBeanName(BTransmit self, String value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 5; }
        @Override
        public void Commit() { this.getBeanTyped()._ParameterBeanName = this.getValue(); }
    }

    private static final class Log__ParameterBeanValue extends Zeze.Transaction.Log1<BTransmit, Zeze.Net.Binary> {
        public Log__ParameterBeanValue(BTransmit self, Zeze.Net.Binary value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 6; }
        @Override
        public void Commit() { this.getBeanTyped()._ParameterBeanValue = this.getValue(); }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        BuildString(sb, 0);
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    @Override
    public void BuildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Beans.Provider2.BTransmit: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ActionName").append('=').append(getActionName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Roles").append("=[").append(System.lineSeparator());
        level += 4;
        for (var _kv_ : getRoles().entrySet()) {
            sb.append(Zeze.Util.Str.indent(level)).append('(').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append("Key").append('=').append(_kv_.getKey()).append(',').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append("Value").append('=').append(System.lineSeparator());
            _kv_.getValue().BuildString(sb, level + 4);
            sb.append(',').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append(')').append(System.lineSeparator());
        }
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Sender").append('=').append(getSender()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ServiceNamePrefix").append('=').append(getServiceNamePrefix()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ParameterBeanName").append('=').append(getParameterBeanName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ParameterBeanValue").append('=').append(getParameterBeanValue()).append(System.lineSeparator());
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append('}');
    }

    private static int _PRE_ALLOC_SIZE_ = 16;

    @Override
    public int getPreAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void setPreAllocSize(int size) {
        _PRE_ALLOC_SIZE_ = size;
    }

    @SuppressWarnings("UnusedAssignment")
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
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _e_.getValue().Encode(_o_);
                }
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
            String _x_ = getServiceNamePrefix();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getParameterBeanName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = getParameterBeanValue();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @SuppressWarnings("UnusedAssignment")
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
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadLong(_s_);
                    var _v_ = _o_.ReadBean(new Zeze.Beans.Provider2.BTransmitContext(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownField(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setSender(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setServiceNamePrefix(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setParameterBeanName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            setParameterBeanValue(_o_.ReadBinary(_t_));
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

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean NegativeCheck() {
        for (var _v_ : getRoles().values()) {
            if (_v_.NegativeCheck())
                return true;
        }
        if (getSender() < 0)
            return true;
        return false;
    }
}
