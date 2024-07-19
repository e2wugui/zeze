// auto-generated @formatter:off
package Zeze.Builtin.Collections.DAG;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

// 有向图的结点类型（如：一个任务Task）
@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BDAGNode extends Zeze.Transaction.Bean implements BDAGNodeReadOnly {
    public static final long TYPEID = -4514731368266918897L;

    private final Zeze.Transaction.DynamicBean _Value;

    public static Zeze.Transaction.DynamicBean newDynamicBean_Value() {
        return new Zeze.Transaction.DynamicBean(1, Zeze.Collections.DAG::getSpecialTypeIdFromBean, Zeze.Collections.DAG::createBeanFromSpecialTypeId);
    }

    public static long getSpecialTypeIdFromBean_1(Zeze.Transaction.Bean _b_) {
        return Zeze.Collections.DAG.getSpecialTypeIdFromBean(_b_);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_1(long _t_) {
        return Zeze.Collections.DAG.createBeanFromSpecialTypeId(_t_);
    }

    public Zeze.Transaction.DynamicBean getValue() {
        return _Value;
    }

    @Override
    public Zeze.Transaction.DynamicBeanReadOnly getValueReadOnly() {
        return _Value;
    }

    @SuppressWarnings("deprecation")
    public BDAGNode() {
        _Value = newDynamicBean_Value();
    }

    @Override
    public void reset() {
        _Value.reset();
        _unknown_ = null;
    }

    public void assign(BDAGNode _o_) {
        _Value.assign(_o_._Value);
        _unknown_ = _o_._unknown_;
    }

    public BDAGNode copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BDAGNode copy() {
        var _c_ = new BDAGNode();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BDAGNode _a_, BDAGNode _b_) {
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
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Zeze.Builtin.Collections.DAG.BDAGNode: {").append(System.lineSeparator());
        _l_ += 4;
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Value=").append(System.lineSeparator());
        _Value.getBean().buildString(_s_, _l_ + 4);
        _s_.append(System.lineSeparator());
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
        {
            var _x_ = _Value;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.DYNAMIC);
                _x_.encode(_o_);
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
            _o_.ReadDynamic(_Value, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BDAGNode))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BDAGNode)_o_;
        if (!_Value.equals(_b_._Value))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _Value.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _Value.initRootInfoWithRedo(_r_, this);
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
                case 1: _Value.followerApply(_v_); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        Zeze.Serialize.Helper.decodeJsonDynamic(_Value, _r_.getString(_pn_ + "Value"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "Value", Zeze.Serialize.Helper.encodeJson(_Value));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Value", "dynamic", "", ""));
        return _v_;
    }
}
