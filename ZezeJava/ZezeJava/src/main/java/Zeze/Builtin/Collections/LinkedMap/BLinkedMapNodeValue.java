// auto-generated @formatter:off
package Zeze.Builtin.Collections.LinkedMap;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BLinkedMapNodeValue extends Zeze.Transaction.Bean implements BLinkedMapNodeValueReadOnly {
    public static final long TYPEID = -6110801358414370128L;

    private String _Id; // LinkedMap的Key转成字符串类型
    private final Zeze.Transaction.DynamicBean _Value;

    public static Zeze.Transaction.DynamicBean newDynamicBean_Value() {
        return new Zeze.Transaction.DynamicBean(2, Zeze.Collections.LinkedMap::getSpecialTypeIdFromBean, Zeze.Collections.LinkedMap::createBeanFromSpecialTypeId);
    }

    public static long getSpecialTypeIdFromBean_2(Zeze.Transaction.Bean bean) {
        return Zeze.Collections.LinkedMap.getSpecialTypeIdFromBean(bean);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_2(long typeId) {
        return Zeze.Collections.LinkedMap.createBeanFromSpecialTypeId(typeId);
    }

    @Override
    public String getId() {
        if (!isManaged())
            return _Id;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Id;
        var log = (Log__Id)txn.getLog(objectId() + 1);
        return log != null ? log.value : _Id;
    }

    public void setId(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Id = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Id(this, 1, value));
    }

    public Zeze.Transaction.DynamicBean getValue() {
        return _Value;
    }

    @Override
    public Zeze.Transaction.DynamicBeanReadOnly getValueReadOnly() {
        return _Value;
    }

    @SuppressWarnings("deprecation")
    public BLinkedMapNodeValue() {
        _Id = "";
        _Value = newDynamicBean_Value();
    }

    @SuppressWarnings("deprecation")
    public BLinkedMapNodeValue(String _Id_) {
        if (_Id_ == null)
            _Id_ = "";
        _Id = _Id_;
        _Value = newDynamicBean_Value();
    }

    @Override
    public void reset() {
        setId("");
        _Value.reset();
        _unknown_ = null;
    }

    public void assign(BLinkedMapNodeValue other) {
        setId(other.getId());
        _Value.assign(other._Value);
        _unknown_ = other._unknown_;
    }

    public BLinkedMapNodeValue copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BLinkedMapNodeValue copy() {
        var copy = new BLinkedMapNodeValue();
        copy.assign(this);
        return copy;
    }

    public static void swap(BLinkedMapNodeValue a, BLinkedMapNodeValue b) {
        BLinkedMapNodeValue save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Id extends Zeze.Transaction.Logs.LogString {
        public Log__Id(BLinkedMapNodeValue bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BLinkedMapNodeValue)getBelong())._Id = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeValue: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Id=").append(getId()).append(',').append(System.lineSeparator());
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
            String _x_ = getId();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = _Value;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.DYNAMIC);
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
            setId(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
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
                case 1: _Id = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 2: _Value.followerApply(vlog); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setId(rs.getString(_parents_name_ + "Id"));
        if (getId() == null)
            setId("");
        Zeze.Serialize.Helper.decodeJsonDynamic(_Value, rs.getString(_parents_name_ + "Value"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "Id", getId());
        st.appendString(_parents_name_ + "Value", Zeze.Serialize.Helper.encodeJson(_Value));
    }

    @Override
    public java.util.List<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Id", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Value", "dynamic", "", ""));
        return vars;
    }

    @Override
    public Zeze.Transaction.Bean toPrevious() {
        return null; // todo BLinkedMapNodeValue
    }
}
