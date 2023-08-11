// auto-generated @formatter:off
package Zeze.Builtin.Game.Bag;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BItem extends Zeze.Transaction.Bean implements BItemReadOnly {
    public static final long TYPEID = 8937000213993683283L;

    private int _Id;
    private int _Number;
    private final Zeze.Transaction.DynamicBean _Item;

    public static Zeze.Transaction.DynamicBean newDynamicBean_Item() {
        return new Zeze.Transaction.DynamicBean(3, Zeze.Game.Bag::getSpecialTypeIdFromBean, Zeze.Game.Bag::createBeanFromSpecialTypeId);
    }

    public static long getSpecialTypeIdFromBean_3(Zeze.Transaction.Bean bean) {
        return Zeze.Game.Bag.getSpecialTypeIdFromBean(bean);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_3(long typeId) {
        return Zeze.Game.Bag.createBeanFromSpecialTypeId(typeId);
    }

    private transient Object __zeze_map_key__;

    @Override
    public Object mapKey() {
        return __zeze_map_key__;
    }

    @Override
    public void mapKey(Object value) {
        __zeze_map_key__ = value;
    }

    @Override
    public int getId() {
        if (!isManaged())
            return _Id;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Id;
        var log = (Log__Id)txn.getLog(objectId() + 1);
        return log != null ? log.value : _Id;
    }

    public void setId(int value) {
        if (!isManaged()) {
            _Id = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Id(this, 1, value));
    }

    @Override
    public int getNumber() {
        if (!isManaged())
            return _Number;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Number;
        var log = (Log__Number)txn.getLog(objectId() + 2);
        return log != null ? log.value : _Number;
    }

    public void setNumber(int value) {
        if (!isManaged()) {
            _Number = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Number(this, 2, value));
    }

    public Zeze.Transaction.DynamicBean getItem() {
        return _Item;
    }

    @Override
    public Zeze.Transaction.DynamicBeanReadOnly getItemReadOnly() {
        return _Item;
    }

    @SuppressWarnings("deprecation")
    public BItem() {
        _Item = newDynamicBean_Item();
    }

    @SuppressWarnings("deprecation")
    public BItem(int _Id_, int _Number_) {
        _Id = _Id_;
        _Number = _Number_;
        _Item = newDynamicBean_Item();
    }

    @Override
    public void reset() {
        setId(0);
        setNumber(0);
        _Item.reset();
        _unknown_ = null;
    }

    public void assign(BItem other) {
        setId(other.getId());
        setNumber(other.getNumber());
        _Item.assign(other._Item);
        _unknown_ = other._unknown_;
    }

    public BItem copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BItem copy() {
        var copy = new BItem();
        copy.assign(this);
        return copy;
    }

    public static void swap(BItem a, BItem b) {
        BItem save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Id extends Zeze.Transaction.Logs.LogInt {
        public Log__Id(BItem bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BItem)getBelong())._Id = value; }
    }

    private static final class Log__Number extends Zeze.Transaction.Logs.LogInt {
        public Log__Number(BItem bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BItem)getBelong())._Number = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.Bag.BItem: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Id=").append(getId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Number=").append(getNumber()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(System.lineSeparator());
        _Item.getBean().buildString(sb, level + 4);
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
            int _x_ = getId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getNumber();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = _Item;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.DYNAMIC);
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
            setId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setNumber(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _o_.ReadDynamic(_Item, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Item.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _Item.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getId() < 0)
            return true;
        if (getNumber() < 0)
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
                case 1: _Id = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 2: _Number = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 3: _Item.followerApply(vlog); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setId(rs.getInt(_parents_name_ + "Id"));
        setNumber(rs.getInt(_parents_name_ + "Number"));
        Zeze.Serialize.Helper.decodeJsonDynamic(_Item, rs.getString(_parents_name_ + "Item"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendInt(_parents_name_ + "Id", getId());
        st.appendInt(_parents_name_ + "Number", getNumber());
        st.appendString(_parents_name_ + "Item", Zeze.Serialize.Helper.encodeJson(_Item));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Id", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Number", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Item", "dynamic", "", ""));
        return vars;
    }
}
