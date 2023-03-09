// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BDeleteResult extends Zeze.Transaction.Bean implements BDeleteResultReadOnly {
    public static final long TYPEID = 8209931211572490098L;

    private String _RaftConfig;

    @Override
    public String getRaftConfig() {
        if (!isManaged())
            return _RaftConfig;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _RaftConfig;
        var log = (Log__RaftConfig)txn.getLog(objectId() + 1);
        return log != null ? log.value : _RaftConfig;
    }

    public void setRaftConfig(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _RaftConfig = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__RaftConfig(this, 1, value));
    }

    @SuppressWarnings("deprecation")
    public BDeleteResult() {
        _RaftConfig = "";
    }

    @SuppressWarnings("deprecation")
    public BDeleteResult(String _RaftConfig_) {
        if (_RaftConfig_ == null)
            throw new IllegalArgumentException();
        _RaftConfig = _RaftConfig_;
    }

    @Override
    public Zeze.Builtin.Dbh2.BDeleteResultDaTa toData() {
        var data = new Zeze.Builtin.Dbh2.BDeleteResultDaTa();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Dbh2.BDeleteResultDaTa)other);
    }

    public void assign(BDeleteResultDaTa other) {
        setRaftConfig(other.getRaftConfig());
    }

    public void assign(BDeleteResult other) {
        setRaftConfig(other.getRaftConfig());
    }

    @Deprecated
    public void Assign(BDeleteResult other) {
        assign(other);
    }

    public BDeleteResult copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BDeleteResult copy() {
        var copy = new BDeleteResult();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BDeleteResult Copy() {
        return copy();
    }

    public static void swap(BDeleteResult a, BDeleteResult b) {
        BDeleteResult save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__RaftConfig extends Zeze.Transaction.Logs.LogString {
        public Log__RaftConfig(BDeleteResult bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BDeleteResult)getBelong())._RaftConfig = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.BDeleteResult: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("RaftConfig=").append(getRaftConfig()).append(System.lineSeparator());
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
            String _x_ = getRaftConfig();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
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
            setRaftConfig(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
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
                case 1: _RaftConfig = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
            }
        }
    }
}
