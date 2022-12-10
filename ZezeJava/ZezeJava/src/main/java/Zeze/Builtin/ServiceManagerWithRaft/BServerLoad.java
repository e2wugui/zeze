// auto-generated @formatter:off
package Zeze.Builtin.ServiceManagerWithRaft;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BServerLoad extends Zeze.Transaction.Bean implements BServerLoadReadOnly {
    public static final long TYPEID = 5255345221471747886L;

    private String _Ip;
    private int _Port;
    private Zeze.Net.Binary _Param;

    @Override
    public String getIp() {
        if (!isManaged())
            return _Ip;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Ip;
        var log = (Log__Ip)txn.getLog(objectId() + 1);
        return log != null ? log.value : _Ip;
    }

    public void setIp(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Ip = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Ip(this, 1, value));
    }

    @Override
    public int getPort() {
        if (!isManaged())
            return _Port;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Port;
        var log = (Log__Port)txn.getLog(objectId() + 2);
        return log != null ? log.value : _Port;
    }

    public void setPort(int value) {
        if (!isManaged()) {
            _Port = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Port(this, 2, value));
    }

    @Override
    public Zeze.Net.Binary getParam() {
        if (!isManaged())
            return _Param;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Param;
        var log = (Log__Param)txn.getLog(objectId() + 3);
        return log != null ? log.value : _Param;
    }

    public void setParam(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Param = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Param(this, 3, value));
    }

    @SuppressWarnings("deprecation")
    public BServerLoad() {
        _Ip = "";
        _Param = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BServerLoad(String _Ip_, int _Port_, Zeze.Net.Binary _Param_) {
        if (_Ip_ == null)
            throw new IllegalArgumentException();
        _Ip = _Ip_;
        _Port = _Port_;
        if (_Param_ == null)
            throw new IllegalArgumentException();
        _Param = _Param_;
    }

    public void assign(BServerLoad other) {
        setIp(other.getIp());
        setPort(other.getPort());
        setParam(other.getParam());
    }

    @Deprecated
    public void Assign(BServerLoad other) {
        assign(other);
    }

    public BServerLoad copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BServerLoad copy() {
        var copy = new BServerLoad();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BServerLoad Copy() {
        return copy();
    }

    public static void swap(BServerLoad a, BServerLoad b) {
        BServerLoad save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Ip extends Zeze.Transaction.Logs.LogString {
        public Log__Ip(BServerLoad bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BServerLoad)getBelong())._Ip = value; }
    }

    private static final class Log__Port extends Zeze.Transaction.Logs.LogInt {
        public Log__Port(BServerLoad bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BServerLoad)getBelong())._Port = value; }
    }

    private static final class Log__Param extends Zeze.Transaction.Logs.LogBinary {
        public Log__Param(BServerLoad bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BServerLoad)getBelong())._Param = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.ServiceManagerWithRaft.BServerLoad: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Ip=").append(getIp()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Port=").append(getPort()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Param=").append(getParam()).append(System.lineSeparator());
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
            String _x_ = getIp();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            int _x_ = getPort();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = getParam();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
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
            setIp(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setPort(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setParam(_o_.ReadBinary(_t_));
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
        if (getPort() < 0)
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
                case 1: _Ip = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 2: _Port = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 3: _Param = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
            }
        }
    }
}
