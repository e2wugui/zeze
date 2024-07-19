// auto-generated @formatter:off
package Zeze.Builtin.Provider;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BAnnounceLinkInfo extends Zeze.Transaction.Bean implements BAnnounceLinkInfoReadOnly {
    public static final long TYPEID = 6291432069805514560L;

    @SuppressWarnings("deprecation")
    public BAnnounceLinkInfo() {
    }

    @Override
    public Zeze.Builtin.Provider.BAnnounceLinkInfo.Data toData() {
        var _d_ = new Zeze.Builtin.Provider.BAnnounceLinkInfo.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Provider.BAnnounceLinkInfo.Data)_o_);
    }

    public void assign(BAnnounceLinkInfo.Data _o_) {
        _unknown_ = null;
    }

    public void assign(BAnnounceLinkInfo _o_) {
        _unknown_ = _o_._unknown_;
    }

    public BAnnounceLinkInfo copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BAnnounceLinkInfo copy() {
        var _c_ = new BAnnounceLinkInfo();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BAnnounceLinkInfo _a_, BAnnounceLinkInfo _b_) {
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
        return _s_.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Zeze.Builtin.Provider.BAnnounceLinkInfo: {").append(System.lineSeparator());
        _l_ += 4;
        _l_ -= 4;
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
        return _o_ instanceof BAnnounceLinkInfo;
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
    public static final long TYPEID = 6291432069805514560L;

    @SuppressWarnings("deprecation")
    public Data() {
    }

    @Override
    public void reset() {
    }

    @Override
    public Zeze.Builtin.Provider.BAnnounceLinkInfo toBean() {
        var _b_ = new Zeze.Builtin.Provider.BAnnounceLinkInfo();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BAnnounceLinkInfo)_o_);
    }

    public void assign(BAnnounceLinkInfo _o_) {
    }

    public void assign(BAnnounceLinkInfo.Data _o_) {
    }

    @Override
    public BAnnounceLinkInfo.Data copy() {
        var _c_ = new BAnnounceLinkInfo.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BAnnounceLinkInfo.Data _a_, BAnnounceLinkInfo.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BAnnounceLinkInfo.Data clone() {
        return (BAnnounceLinkInfo.Data)super.clone();
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Zeze.Builtin.Provider.BAnnounceLinkInfo: {").append(System.lineSeparator());
        _l_ += 4;
        _l_ -= 4;
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
