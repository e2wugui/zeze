// auto-generated @formatter:off
package Zeze.Builtin.Online;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BReliableNotify extends Zeze.Transaction.Bean {
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
        var log = (Log__ReliableNotifyIndex)txn.GetLog(objectId() + 2);
        return log != null ? log.Value : _ReliableNotifyIndex;
    }

    public void setReliableNotifyIndex(long value) {
        if (!isManaged()) {
            _ReliableNotifyIndex = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.PutLog(new Log__ReliableNotifyIndex(this, 2, value));
    }

    public BReliableNotify() {
        _Notifies = new Zeze.Transaction.Collections.PList1<>(Zeze.Net.Binary.class);
        _Notifies.VariableId = 1;
    }

    public BReliableNotify(long _ReliableNotifyIndex_) {
        _Notifies = new Zeze.Transaction.Collections.PList1<>(Zeze.Net.Binary.class);
        _Notifies.VariableId = 1;
        _ReliableNotifyIndex = _ReliableNotifyIndex_;
    }

    public void Assign(BReliableNotify other) {
        getNotifies().clear();
        for (var e : other.getNotifies())
            getNotifies().add(e);
        setReliableNotifyIndex(other.getReliableNotifyIndex());
    }

    public BReliableNotify CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BReliableNotify Copy() {
        var copy = new BReliableNotify();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BReliableNotify a, BReliableNotify b) {
        BReliableNotify save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public BReliableNotify CopyBean() {
        return Copy();
    }

    public static final long TYPEID = -8784206618120085556L;

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ReliableNotifyIndex extends Zeze.Transaction.Logs.LogLong {
        public Log__ReliableNotifyIndex(BReliableNotify bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BReliableNotify)getBelong())._ReliableNotifyIndex = Value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        BuildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void BuildString(StringBuilder sb, int level) {
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
    public void Encode(ByteBuffer _o_) {
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
    public void Decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            var _x_ = getNotifies();
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBinary(_t_));
            } else
                _o_.SkipUnknownField(_t_);
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
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Notifies.InitRootInfo(root, this);
    }

    @Override
    protected void ResetChildrenRootInfo() {
        _Notifies.ResetRootInfo();
    }

    @Override
    public boolean NegativeCheck() {
        if (getReliableNotifyIndex() < 0)
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
                case 1: _Notifies.FollowerApply(vlog); break;
                case 2: _ReliableNotifyIndex = ((Zeze.Transaction.Logs.LogLong)vlog).Value; break;
            }
        }
    }
}
