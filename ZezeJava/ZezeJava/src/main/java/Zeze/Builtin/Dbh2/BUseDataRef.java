// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

import Zeze.Serialize.ByteBuffer;

// 用来生成Data数据结构，没有实际功能
@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BUseDataRef extends Zeze.Transaction.Bean implements BUseDataRefReadOnly {
    public static final long TYPEID = -8788076056876784776L;

    private final Zeze.Transaction.Collections.CollOne<Zeze.Builtin.Dbh2.BLogBeginTransaction> _Ref1;

    public Zeze.Builtin.Dbh2.BLogBeginTransaction getRef1() {
        return _Ref1.getValue();
    }

    public void setRef1(Zeze.Builtin.Dbh2.BLogBeginTransaction value) {
        _Ref1.setValue(value);
    }

    @Override
    public Zeze.Builtin.Dbh2.BLogBeginTransactionReadOnly getRef1ReadOnly() {
        return _Ref1.getValue();
    }

    @SuppressWarnings("deprecation")
    public BUseDataRef() {
        _Ref1 = new Zeze.Transaction.Collections.CollOne<>(new Zeze.Builtin.Dbh2.BLogBeginTransaction(), Zeze.Builtin.Dbh2.BLogBeginTransaction.class);
        _Ref1.variableId(1);
    }

    @Override
    public Zeze.Builtin.Dbh2.BUseDataRefData toData() {
        var data = new Zeze.Builtin.Dbh2.BUseDataRefData();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Dbh2.BUseDataRefData)other);
    }

    public void assign(BUseDataRefData other) {
        Zeze.Builtin.Dbh2.BLogBeginTransaction data_Ref1 = new Zeze.Builtin.Dbh2.BLogBeginTransaction();
        data_Ref1.assign(other.getRef1());
        _Ref1.setValue(data_Ref1);
    }

    public void assign(BUseDataRef other) {
        _Ref1.assign(other._Ref1);
    }

    @Deprecated
    public void Assign(BUseDataRef other) {
        assign(other);
    }

    public BUseDataRef copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BUseDataRef copy() {
        var copy = new BUseDataRef();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BUseDataRef Copy() {
        return copy();
    }

    public static void swap(BUseDataRef a, BUseDataRef b) {
        BUseDataRef save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.BUseDataRef: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Ref1=").append(System.lineSeparator());
        _Ref1.buildString(sb, level + 4);
        sb.append(System.lineSeparator());
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
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 1, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _Ref1.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _o_.ReadBean(_Ref1, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Ref1.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _Ref1.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
        if (_Ref1.negativeCheck())
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
                case 1: _Ref1.followerApply(vlog); break;
            }
        }
    }
}
