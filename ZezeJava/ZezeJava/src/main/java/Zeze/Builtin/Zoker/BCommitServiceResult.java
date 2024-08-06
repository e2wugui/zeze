// auto-generated @formatter:off
package Zeze.Builtin.Zoker;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BCommitServiceResult extends Zeze.Transaction.Bean implements BCommitServiceResultReadOnly {
    public static final long TYPEID = 7236706023884749986L;

    @SuppressWarnings("deprecation")
    public BCommitServiceResult() {
    }

    @Override
    public Zeze.Builtin.Zoker.BCommitServiceResult.Data toData() {
        var _d_ = new Zeze.Builtin.Zoker.BCommitServiceResult.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Zoker.BCommitServiceResult.Data)_o_);
    }

    public void assign(BCommitServiceResult.Data _o_) {
        _unknown_ = null;
    }

    public void assign(BCommitServiceResult _o_) {
        _unknown_ = _o_._unknown_;
    }

    public BCommitServiceResult copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BCommitServiceResult copy() {
        var _c_ = new BCommitServiceResult();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BCommitServiceResult _a_, BCommitServiceResult _b_) {
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
        _s_.append("Zeze.Builtin.Zoker.BCommitServiceResult: {\n");
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
        _o_.writeAllUnknownFields(_i_, _ui_, _u_);
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        ByteBuffer _u_ = null;
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        return _o_ instanceof BCommitServiceResult;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void followerApply(Zeze.Transaction.Log _l_) {
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 7236706023884749986L;

    @SuppressWarnings("deprecation")
    public Data() {
    }

    @Override
    public void reset() {
    }

    @Override
    public Zeze.Builtin.Zoker.BCommitServiceResult toBean() {
        var _b_ = new Zeze.Builtin.Zoker.BCommitServiceResult();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BCommitServiceResult)_o_);
    }

    public void assign(BCommitServiceResult _o_) {
    }

    public void assign(BCommitServiceResult.Data _o_) {
    }

    @Override
    public BCommitServiceResult.Data copy() {
        var _c_ = new BCommitServiceResult.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BCommitServiceResult.Data _a_, BCommitServiceResult.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BCommitServiceResult.Data clone() {
        return (BCommitServiceResult.Data)super.clone();
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        _s_.append("Zeze.Builtin.Zoker.BCommitServiceResult: {\n");
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
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        _o_.ReadTagSize(_t_);
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
