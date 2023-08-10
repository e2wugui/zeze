// auto-generated @formatter:off
package Zeze.Builtin.Collections.DAG;

import Zeze.Serialize.ByteBuffer;

// 有向图的结点类型（如：一个任务Task）
@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BDAGNode extends Zeze.Transaction.Bean implements BDAGNodeReadOnly {
    public static final long TYPEID = -4514731368266918897L;

    private final Zeze.Transaction.DynamicBean _Value;

    public static Zeze.Transaction.DynamicBean newDynamicBean_Value() {
        return new Zeze.Transaction.DynamicBean(1, Zeze.Collections.DAG::getSpecialTypeIdFromBean, Zeze.Collections.DAG::createBeanFromSpecialTypeId);
    }

    public static long getSpecialTypeIdFromBean_1(Zeze.Transaction.Bean bean) {
        return Zeze.Collections.DAG.getSpecialTypeIdFromBean(bean);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_1(long typeId) {
        return Zeze.Collections.DAG.createBeanFromSpecialTypeId(typeId);
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

    public void assign(BDAGNode other) {
        _Value.assign(other._Value);
        _unknown_ = other._unknown_;
    }

    public BDAGNode copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BDAGNode copy() {
        var copy = new BDAGNode();
        copy.assign(this);
        return copy;
    }

    public static void swap(BDAGNode a, BDAGNode b) {
        BDAGNode save = a.copy();
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Collections.DAG.BDAGNode: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Value=").append(System.lineSeparator());
        _Value.getBean().buildString(sb, level + 4);
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
    public void decode(ByteBuffer _o_) {
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
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Value.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _Value.initRootInfoWithRedo(root, this);
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
                case 1: _Value.followerApply(vlog); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        Zeze.Serialize.Helper.decodeJsonDynamic(_Value, rs.getString(_parents_name_ + "Value"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "Value", Zeze.Serialize.Helper.encodeJson(_Value));
    }

    @Override
    public Zeze.Transaction.Bean toPrevious() {
        return null; // todo BDAGNode
    }
}
