// auto-generated @formatter:off
package Zeze.Builtin.Online;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BReliableNotify extends Zeze.Transaction.Bean {
    public static final long TYPEID = -8784206618120085556L;

    private final Zeze.Transaction.Collections.PList1<Zeze.Net.Binary> _Notifies; // full encoded protocol list
    private long _ReliableNotifyIndex; // Notify的计数开始。客户端收到的总计数为：start + Notifies.Count

    public Zeze.Transaction.Collections.PList1<Zeze.Net.Binary> getNotifies() {
        return _Notifies;
    }

    public long getReliableNotifyIndex() {
        if (!isManaged())
            return _ReliableNotifyIndex;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ReliableNotifyIndex;
        var log = (Log__ReliableNotifyIndex)txn.getLog(objectId() + 2);
        return log != null ? log.value : _ReliableNotifyIndex;
    }

    public void setReliableNotifyIndex(long value) {
        if (!isManaged()) {
            _ReliableNotifyIndex = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ReliableNotifyIndex(this, 2, value));
    }

    @SuppressWarnings("deprecation")
    public BReliableNotify() {
        _Notifies = new Zeze.Transaction.Collections.PList1<>(Zeze.Net.Binary.class);
        _Notifies.variableId(1);
    }

    @SuppressWarnings("deprecation")
    public BReliableNotify(long _ReliableNotifyIndex_) {
        _Notifies = new Zeze.Transaction.Collections.PList1<>(Zeze.Net.Binary.class);
        _Notifies.variableId(1);
        _ReliableNotifyIndex = _ReliableNotifyIndex_;
    }

    public void assign(BReliableNotify other) {
        getNotifies().clear();
        for (var e : other.getNotifies())
            getNotifies().add(e);
        setReliableNotifyIndex(other.getReliableNotifyIndex());
    }

    @Deprecated
    public void Assign(BReliableNotify other) {
        assign(other);
    }

    public BReliableNotify copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BReliableNotify copy() {
        var copy = new BReliableNotify();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BReliableNotify Copy() {
        return copy();
    }

    public static void swap(BReliableNotify a, BReliableNotify b) {
        BReliableNotify save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ReliableNotifyIndex extends Zeze.Transaction.Logs.LogLong {
        public Log__ReliableNotifyIndex(BReliableNotify bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BReliableNotify)getBelong())._ReliableNotifyIndex = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Online.BReliableNotify: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Notifies").append("=[").append(System.lineSeparator());
        level += 4;
        for (var _item_ : getNotifies()) {
            sb.append(Zeze.Util.Str.indent(level)).append("Item").append('=').append(_item_).append(',').append(System.lineSeparator());
        }
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ReliableNotifyIndex").append('=').append(getReliableNotifyIndex()).append(System.lineSeparator());
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
            var _x_ = getNotifies();
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BYTES);
                for (var _v_ : _x_)
                    _o_.WriteBinary(_v_);
            }
        }
        {
            long _x_ = getReliableNotifyIndex();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
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
            var _x_ = getNotifies();
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBinary(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setReliableNotifyIndex(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Notifies.initRootInfo(root, this);
    }

    @Override
    protected void resetChildrenRootInfo() {
        _Notifies.resetRootInfo();
    }

    @Override
    public boolean negativeCheck() {
        if (getReliableNotifyIndex() < 0)
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
                case 1: _Notifies.followerApply(vlog); break;
                case 2: _ReliableNotifyIndex = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
            }
        }
    }
}
