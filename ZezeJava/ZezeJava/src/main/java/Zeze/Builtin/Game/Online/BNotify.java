// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BNotify extends Zeze.Transaction.Bean implements BNotifyReadOnly {
    public static final long TYPEID = 663625160021568926L;

    private Zeze.Net.Binary _FullEncodedProtocol;

    @Override
    public Zeze.Net.Binary getFullEncodedProtocol() {
        if (!isManaged())
            return _FullEncodedProtocol;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _FullEncodedProtocol;
        var log = (Log__FullEncodedProtocol)txn.getLog(objectId() + 1);
        return log != null ? log.value : _FullEncodedProtocol;
    }

    public void setFullEncodedProtocol(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _FullEncodedProtocol = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__FullEncodedProtocol(this, 1, value));
    }

    @SuppressWarnings("deprecation")
    public BNotify() {
        _FullEncodedProtocol = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BNotify(Zeze.Net.Binary _FullEncodedProtocol_) {
        if (_FullEncodedProtocol_ == null)
            throw new IllegalArgumentException();
        _FullEncodedProtocol = _FullEncodedProtocol_;
    }

    public void assign(BNotify other) {
        setFullEncodedProtocol(other.getFullEncodedProtocol());
    }

    @Deprecated
    public void Assign(BNotify other) {
        assign(other);
    }

    public BNotify copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BNotify copy() {
        var copy = new BNotify();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BNotify Copy() {
        return copy();
    }

    public static void swap(BNotify a, BNotify b) {
        BNotify save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__FullEncodedProtocol extends Zeze.Transaction.Logs.LogBinary {
        public Log__FullEncodedProtocol(BNotify bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BNotify)getBelong())._FullEncodedProtocol = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.Online.BNotify: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("FullEncodedProtocol").append('=').append(getFullEncodedProtocol()).append(System.lineSeparator());
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
            var _x_ = getFullEncodedProtocol();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
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
            setFullEncodedProtocol(_o_.ReadBinary(_t_));
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
                case 1: _FullEncodedProtocol = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
            }
        }
    }
}
