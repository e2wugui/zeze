// auto-generated @formatter:off
package rocksdynamic.Fixture;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"EqualsAndHashcode", "ConstantConditions", "ConstantValue", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class LegacyDecodeParent extends Zeze.Transaction.Bean implements LegacyDecodeParentReadOnly {
    public static final long TYPEID = 5559241961456004390L;

    private final Zeze.Transaction.Collections.CollOne<rocksdynamic.Fixture.LegacyDecodeChild> _Child;
    private static final Zeze.Transaction.Collections.LogOneMeta<rocksdynamic.Fixture.LegacyDecodeChild> meta1_Child
            = Zeze.Transaction.Collections.LogOneMeta.get(rocksdynamic.Fixture.LegacyDecodeChild.class);

    public rocksdynamic.Fixture.LegacyDecodeChild getChild() {
        return _Child.getValue();
    }

    public void setChild(rocksdynamic.Fixture.LegacyDecodeChild _v_) {
        _Child.setValue(_v_);
    }

    @Override
    public rocksdynamic.Fixture.LegacyDecodeChildReadOnly getChildReadOnly() {
        return _Child.getValue();
    }

    @SuppressWarnings("deprecation")
    public LegacyDecodeParent() {
        _Child = new Zeze.Transaction.Collections.CollOne<>(new rocksdynamic.Fixture.LegacyDecodeChild(), meta1_Child);
        _Child.variableId(1);
    }

    @Override
    public void reset() {
        _Child.reset();
        _unknown_ = null;
    }

    @Override
    public rocksdynamic.Fixture.LegacyDecodeParent.Data toData() {
        var _d_ = new rocksdynamic.Fixture.LegacyDecodeParent.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((rocksdynamic.Fixture.LegacyDecodeParent.Data)_o_);
    }

    public void assign(LegacyDecodeParent.Data _o_) {
        var _d__Child = new rocksdynamic.Fixture.LegacyDecodeChild();
        _d__Child.assign(_o_._Child);
        _Child.setValue(_d__Child);
        _unknown_ = null;
    }

    public void assign(LegacyDecodeParent _o_) {
        _Child.assign(_o_._Child);
        _unknown_ = _o_._unknown_;
    }

    public LegacyDecodeParent copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public LegacyDecodeParent copy() {
        var _c_ = new LegacyDecodeParent();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(LegacyDecodeParent _a_, LegacyDecodeParent _b_) {
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
        _s_.append("rocksdynamic.Fixture.LegacyDecodeParent: {\n");
        _s_.append(_i1_).append("Child=");
        _Child.buildString(_s_, _l_ + 8);
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

    private byte @Nullable [] _unknown_;

    public byte @Nullable [] unknown() {
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
            _Child.encode(_o_);
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
            _o_.ReadBean(_Child, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        } else
            getChild().restoreOmittedFields(new rocksdynamic.Fixture.LegacyDecodeChild());
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(@Nullable Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof LegacyDecodeParent))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (LegacyDecodeParent)_o_;
        if (!_Child.equals(_b_._Child))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _Child.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _Child.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        if (_Child.negativeCheck())
            return true;
        return false;
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
                case 1: _Child.followerApply(_v_); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        _p_.add("Child");
        _Child.decodeResultSet(_p_, _r_);
        _p_.removeLast();
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        _p_.add("Child");
        _Child.encodeSQLStatement(_p_, _s_);
        _p_.removeLast();
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Child", "rocksdynamic.Fixture.LegacyDecodeChild", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 5559241961456004390L;

    private rocksdynamic.Fixture.LegacyDecodeChild.Data _Child;

    public rocksdynamic.Fixture.LegacyDecodeChild.Data getChild() {
        return _Child;
    }

    public void setChild(rocksdynamic.Fixture.LegacyDecodeChild.Data _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Child = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Child = new rocksdynamic.Fixture.LegacyDecodeChild.Data();
    }

    @SuppressWarnings("deprecation")
    public Data(rocksdynamic.Fixture.LegacyDecodeChild.@Nullable Data _Child_) {
        if (_Child_ == null)
            _Child_ = new rocksdynamic.Fixture.LegacyDecodeChild.Data();
        _Child = _Child_;
    }

    @Override
    public void reset() {
        _Child.reset();
    }

    @Override
    public rocksdynamic.Fixture.LegacyDecodeParent toBean() {
        var _b_ = new rocksdynamic.Fixture.LegacyDecodeParent();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((LegacyDecodeParent)_o_);
    }

    public void assign(LegacyDecodeParent _o_) {
        _Child.assign(_o_._Child.getValue());
    }

    public void assign(LegacyDecodeParent.Data _o_) {
        _Child.assign(_o_._Child);
    }

    @Override
    public LegacyDecodeParent.Data copy() {
        var _c_ = new LegacyDecodeParent.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(LegacyDecodeParent.Data _a_, LegacyDecodeParent.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public LegacyDecodeParent.Data clone() {
        return (LegacyDecodeParent.Data)super.clone();
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
        _s_.append("rocksdynamic.Fixture.LegacyDecodeParent: {\n");
        _s_.append(_i1_).append("Child=");
        _Child.buildString(_s_, _l_ + 8);
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
            _Child.encode(_o_);
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
            _o_.ReadBean(_Child, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        } else
            _Child.restoreOmittedFields(new rocksdynamic.Fixture.LegacyDecodeChild.Data());
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    public boolean equals(@Nullable Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof LegacyDecodeParent.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (LegacyDecodeParent.Data)_o_;
        if (!_Child.equals(_b_._Child))
            return false;
        return true;
    }
}
}
