// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BEndMove extends Zeze.Transaction.Bean implements BEndMoveReadOnly {
    public static final long TYPEID = 1744858924397766646L;

    private final Zeze.Transaction.Collections.CollOne<Zeze.Builtin.Dbh2.BBucketMeta> _To;

    public Zeze.Builtin.Dbh2.BBucketMeta getTo() {
        return _To.getValue();
    }

    public void setTo(Zeze.Builtin.Dbh2.BBucketMeta _v_) {
        _To.setValue(_v_);
    }

    @Override
    public Zeze.Builtin.Dbh2.BBucketMetaReadOnly getToReadOnly() {
        return _To.getValue();
    }

    @SuppressWarnings("deprecation")
    public BEndMove() {
        _To = new Zeze.Transaction.Collections.CollOne<>(new Zeze.Builtin.Dbh2.BBucketMeta(), Zeze.Builtin.Dbh2.BBucketMeta.class);
        _To.variableId(1);
    }

    @Override
    public void reset() {
        _To.reset();
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Dbh2.Master.BEndMove.Data toData() {
        var _d_ = new Zeze.Builtin.Dbh2.Master.BEndMove.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Dbh2.Master.BEndMove.Data)_o_);
    }

    public void assign(BEndMove.Data _o_) {
        var _d__To = new Zeze.Builtin.Dbh2.BBucketMeta();
        _d__To.assign(_o_._To);
        _To.setValue(_d__To);
        _unknown_ = null;
    }

    public void assign(BEndMove _o_) {
        _To.assign(_o_._To);
        _unknown_ = _o_._unknown_;
    }

    public BEndMove copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BEndMove copy() {
        var _c_ = new BEndMove();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BEndMove _a_, BEndMove _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        var _i1_ = Zeze.Util.Str.indent(_l_ + 4);
        _s_.append("Zeze.Builtin.Dbh2.Master.BEndMove: {\n");
        _s_.append(_i1_).append("To=");
        _To.buildString(_s_, _l_ + 8);
        _s_.append('\n');
        _s_.append(Zeze.Util.Str.indent(_l_)).append('}');
    }

    private static int _PRE_ALLOC_SIZE_ = 16;

    @Override
    public int preAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void preAllocSize(int _s_) {
        _PRE_ALLOC_SIZE_ = _s_;
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
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 1, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _To.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
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
            _o_.ReadBean(_To, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BEndMove))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BEndMove)_o_;
        if (!_To.equals(_b_._To))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _To.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _To.initRootInfoWithRedo(_r_, this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void followerApply(Zeze.Transaction.Log _l_) {
        var _vs_ = ((Zeze.Transaction.Collections.LogBean)_l_).getVariables();
        if (_vs_ == null)
            return;
        for (var _i_ = _vs_.iterator(); _i_.moveToNext(); ) {
            var _v_ = _i_.value();
            switch (_v_.getVariableId()) {
                case 1: _To.followerApply(_v_); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        _p_.add("To");
        _To.decodeResultSet(_p_, _r_);
        _p_.remove(_p_.size() - 1);
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        _p_.add("To");
        _To.encodeSQLStatement(_p_, _s_);
        _p_.remove(_p_.size() - 1);
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "To", "Zeze.Builtin.Dbh2.BBucketMeta", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 1744858924397766646L;

    private Zeze.Builtin.Dbh2.BBucketMeta.Data _To;

    public Zeze.Builtin.Dbh2.BBucketMeta.Data getTo() {
        return _To;
    }

    public void setTo(Zeze.Builtin.Dbh2.BBucketMeta.Data _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _To = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _To = new Zeze.Builtin.Dbh2.BBucketMeta.Data();
    }

    @SuppressWarnings("deprecation")
    public Data(Zeze.Builtin.Dbh2.BBucketMeta.Data _To_) {
        if (_To_ == null)
            _To_ = new Zeze.Builtin.Dbh2.BBucketMeta.Data();
        _To = _To_;
    }

    @Override
    public void reset() {
        _To.reset();
    }

    @Override
    public Zeze.Builtin.Dbh2.Master.BEndMove toBean() {
        var _b_ = new Zeze.Builtin.Dbh2.Master.BEndMove();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BEndMove)_o_);
    }

    public void assign(BEndMove _o_) {
        _To.assign(_o_._To.getValue());
    }

    public void assign(BEndMove.Data _o_) {
        _To.assign(_o_._To);
    }

    @Override
    public BEndMove.Data copy() {
        var _c_ = new BEndMove.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BEndMove.Data _a_, BEndMove.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BEndMove.Data clone() {
        return (BEndMove.Data)super.clone();
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        var _i1_ = Zeze.Util.Str.indent(_l_ + 4);
        _s_.append("Zeze.Builtin.Dbh2.Master.BEndMove: {\n");
        _s_.append(_i1_).append("To=");
        _To.buildString(_s_, _l_ + 8);
        _s_.append('\n');
        _s_.append(Zeze.Util.Str.indent(_l_)).append('}');
    }

    @Override
    public int preAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void preAllocSize(int _s_) {
        _PRE_ALLOC_SIZE_ = _s_;
    }

    @Override
    public void encode(ByteBuffer _o_) {
        int _i_ = 0;
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 1, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _To.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _o_.ReadBean(_To, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BEndMove.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BEndMove.Data)_o_;
        if (!_To.equals(_b_._To))
            return false;
        return true;
    }
}
}
