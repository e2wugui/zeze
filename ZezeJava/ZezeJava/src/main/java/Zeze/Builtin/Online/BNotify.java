// auto-generated @formatter:off
package Zeze.Builtin.Online;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BNotify extends Zeze.Transaction.Bean {
    private Zeze.Net.Binary _FullEncodedProtocol;

    public Zeze.Net.Binary getFullEncodedProtocol() {
        if (!isManaged())
            return _FullEncodedProtocol;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _FullEncodedProtocol;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__FullEncodedProtocol)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.getValue() : _FullEncodedProtocol;
    }

    public void setFullEncodedProtocol(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _FullEncodedProtocol = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__FullEncodedProtocol(this, 1, value));
    }

    public BNotify() {
         this(0);
    }

    public BNotify(int _varId_) {
        super(_varId_);
        _FullEncodedProtocol = Zeze.Net.Binary.Empty;
    }

    public void Assign(BNotify other) {
        setFullEncodedProtocol(other.getFullEncodedProtocol());
    }

    public BNotify CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BNotify Copy() {
        var copy = new BNotify();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BNotify a, BNotify b) {
        BNotify save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public static final long TYPEID = -1042898139461326074L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private static final class Log__FullEncodedProtocol extends Zeze.Transaction.Log1<BNotify, Zeze.Net.Binary> {
        public Log__FullEncodedProtocol(BNotify bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void Commit() { getBeanTyped()._FullEncodedProtocol = this.getValue(); }
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Online.BNotify: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("FullEncodedProtocol").append('=').append(getFullEncodedProtocol()).append(System.lineSeparator());
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

    @Override
    public void Encode(ByteBuffer _o_) {
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
    public void Decode(ByteBuffer _o_) {
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
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
    }

    @Override
    public boolean NegativeCheck() {
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
                case 1: _FullEncodedProtocol = ((Zeze.Transaction.Logs.LogBinary)vlog).Value; break;
            }
        }
    }
}
