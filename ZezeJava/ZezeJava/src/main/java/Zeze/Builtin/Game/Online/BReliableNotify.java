// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BReliableNotify extends Zeze.Transaction.Bean implements BReliableNotifyReadOnly {
    public static final long TYPEID = -6166834646872658332L;

    private final Zeze.Transaction.Collections.PList1<Zeze.Net.Binary> _Notifies; // full encoded protocol list
    private long _ReliableNotifyIndex; // Notify的计数开始。客户端收到的总计数为：start + Notifies.Count

    public Zeze.Transaction.Collections.PList1<Zeze.Net.Binary> getNotifies() {
        return _Notifies;
    }

    @Override
    public Zeze.Transaction.Collections.PList1ReadOnly<Zeze.Net.Binary> getNotifiesReadOnly() {
        return new Zeze.Transaction.Collections.PList1ReadOnly<>(_Notifies);
    }

    @Override
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

    @Override
    public void reset() {
        _Notifies.clear();
        setReliableNotifyIndex(0);
        _unknown_ = null;
    }

    public void assign(BReliableNotify other) {
        _Notifies.assign(other._Notifies);
        setReliableNotifyIndex(other.getReliableNotifyIndex());
        _unknown_ = other._unknown_;
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.Online.BReliableNotify: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Notifies=[");
        if (!_Notifies.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _Notifies) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(_item_).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ReliableNotifyIndex=").append(getReliableNotifyIndex()).append(System.lineSeparator());
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

    private byte[] _unknown_;

    public byte[] unknown() {
        return _unknown_;
    }

    public void clearUnknown() {
        _unknown_ = null;
    }

    @Override
    public void encode(ByteBuffer _o_) {
        ByteBuffer _u_ = null;
        var _ua_ = _unknown_;
        var _ui_ = _ua_ != null ? (_u_ = ByteBuffer.Wrap(_ua_)).readUnknownIndex() : Long.MAX_VALUE;
        int _i_ = 0;
        {
            var _x_ = _Notifies;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BYTES);
                for (var _v_ : _x_) {
                    _o_.WriteBinary(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            long _x_ = getReliableNotifyIndex();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        _o_.writeAllUnknownFields(_i_, _ui_, _u_);
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        ByteBuffer _u_ = null;
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            var _x_ = _Notifies;
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
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BReliableNotify))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BReliableNotify)_o_;
        if (!_Notifies.equals(_b_._Notifies))
            return false;
        if (getReliableNotifyIndex() != _b_.getReliableNotifyIndex())
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Notifies.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _Notifies.initRootInfoWithRedo(root, this);
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
                case 2: _ReliableNotifyIndex = vlog.longValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        Zeze.Serialize.Helper.decodeJsonList(_Notifies, Zeze.Net.Binary.class, rs.getString(_parents_name_ + "Notifies"));
        setReliableNotifyIndex(rs.getLong(_parents_name_ + "ReliableNotifyIndex"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "Notifies", Zeze.Serialize.Helper.encodeJson(_Notifies));
        st.appendLong(_parents_name_ + "ReliableNotifyIndex", getReliableNotifyIndex());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Notifies", "list", "", "binary"));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "ReliableNotifyIndex", "long", "", ""));
        return vars;
    }
}
