// auto-generated @formatter:off
package Zeze.Builtin.World;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BObject extends Zeze.Transaction.Bean implements BObjectReadOnly {
    public static final long TYPEID = -2457457472033861643L;

    private final Zeze.Transaction.DynamicBean _Custom;

    public static Zeze.Transaction.DynamicBean newDynamicBean_Custom() {
        return new Zeze.Transaction.DynamicBean(1, Zeze.World.World::getSpecialTypeIdFromBean, Zeze.World.World::createBeanFromSpecialTypeId);
    }

    public static long getSpecialTypeIdFromBean_1(Zeze.Transaction.Bean bean) {
        return Zeze.World.World.getSpecialTypeIdFromBean(bean);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_1(long typeId) {
        return Zeze.World.World.createBeanFromSpecialTypeId(typeId);
    }

    public Zeze.Transaction.DynamicBean getCustom() {
        return _Custom;
    }

    @Override
    public Zeze.Transaction.DynamicBeanReadOnly getCustomReadOnly() {
        return _Custom;
    }

    @SuppressWarnings("deprecation")
    public BObject() {
        _Custom = newDynamicBean_Custom();
    }

    @Override
    public Zeze.Builtin.World.BObject.Data toData() {
        var data = new Zeze.Builtin.World.BObject.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.World.BObject.Data)other);
    }

    public void assign(BObject.Data other) {
        _Custom.assign(other._Custom);
    }

    public void assign(BObject other) {
        _Custom.assign(other._Custom);
    }

    public BObject copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BObject copy() {
        var copy = new BObject();
        copy.assign(this);
        return copy;
    }

    public static void swap(BObject a, BObject b) {
        BObject save = a.copy();
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.World.BObject: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Custom=").append(System.lineSeparator());
        _Custom.getBean().buildString(sb, level + 4);
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
            var _x_ = _Custom;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.DYNAMIC);
                _x_.encode(_o_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _o_.ReadDynamic(_Custom, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Custom.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _Custom.initRootInfoWithRedo(root, this);
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
                case 1: _Custom.followerApply(vlog); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        Zeze.Serialize.Helper.decodeJsonDynamic(_Custom, rs.getString(_parents_name_ + "Custom"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "Custom", Zeze.Serialize.Helper.encodeJson(_Custom));
    }

public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -2457457472033861643L;

    private final DynamicData_Custom _Custom;

    public static final class DynamicData_Custom extends Zeze.Transaction.DynamicData {
        static {
            registerJsonParser(DynamicData_Custom.class);
        }

        @Override
        public long toTypeId(Zeze.Transaction.Data data) {
            return Zeze.World.World.getSpecialTypeIdFromBean(data);
        }

        @Override
        public Zeze.Transaction.Data toData(long typeId) {
            return Zeze.World.World.createDataFromSpecialTypeId(typeId);
        }

        @Override
        public DynamicData_Custom copy() {
            return (DynamicData_Custom)super.copy();
        }
    }

    public DynamicData_Custom getCustom() {
        return _Custom;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Custom = new DynamicData_Custom();
    }

    @SuppressWarnings("deprecation")
    public Data(DynamicData_Custom _Custom_) {
        if (_Custom_ == null)
            _Custom_ = new DynamicData_Custom();
        _Custom = _Custom_;
    }

    @Override
    public Zeze.Builtin.World.BObject toBean() {
        var bean = new Zeze.Builtin.World.BObject();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BObject)other);
    }

    public void assign(BObject other) {
        _Custom.assign(other._Custom);
    }

    public void assign(BObject.Data other) {
        _Custom.assign(other._Custom);
    }

    @Override
    public BObject.Data copy() {
        var copy = new BObject.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BObject.Data a, BObject.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BObject.Data clone() {
        return (BObject.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.World.BObject: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Custom=").append(System.lineSeparator());
        _Custom.getData().buildString(sb, level + 4);
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
            var _x_ = _Custom;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.DYNAMIC);
                _x_.encode(_o_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _o_.ReadDynamic(_Custom, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
