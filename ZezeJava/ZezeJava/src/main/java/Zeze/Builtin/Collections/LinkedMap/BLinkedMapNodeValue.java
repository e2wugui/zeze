// auto-generated @formatter:off
package Zeze.Builtin.Collections.LinkedMap;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BLinkedMapNodeValue extends Zeze.Transaction.Bean {
    private String _Id; // LinkedMap的Key转成字符串类型
    private final Zeze.Transaction.DynamicBean _Value;

    public static long getSpecialTypeIdFromBean_Value(Zeze.Transaction.Bean bean) {
        return Zeze.Collections.LinkedMap.getSpecialTypeIdFromBean(bean);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_Value(long typeId) {
        return Zeze.Collections.LinkedMap.createBeanFromSpecialTypeId(typeId);
    }

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

    @SuppressWarnings("deprecation")
    public BLinkedMapNodeValue() {
        _Id = "";
        _Value = new Zeze.Transaction.DynamicBean(2, Zeze.Collections.LinkedMap::getSpecialTypeIdFromBean, Zeze.Collections.LinkedMap::createBeanFromSpecialTypeId);
    }

    @SuppressWarnings("deprecation")
    public BLinkedMapNodeValue(String _Id_) {
        if (_Id_ == null)
            throw new IllegalArgumentException();
        _Id = _Id_;
        _Value = new Zeze.Transaction.DynamicBean(2, Zeze.Collections.LinkedMap::getSpecialTypeIdFromBean, Zeze.Collections.LinkedMap::createBeanFromSpecialTypeId);
    }

    public void assign(BLinkedMapNodeValue other) {
        setId(other.getId());
        getValue().assign(other.getValue());
    }

    @Deprecated
    public void Assign(BLinkedMapNodeValue other) {
        assign(other);
    }

    public BLinkedMapNodeValue copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    public BLinkedMapNodeValue copy() {
        var copy = new BLinkedMapNodeValue();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BLinkedMapNodeValue Copy() {
        return copy();
    }

    public static void swap(BLinkedMapNodeValue a, BLinkedMapNodeValue b) {
        BLinkedMapNodeValue save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public BLinkedMapNodeValue copyBean() {
        return copy();
    }

    public static final long TYPEID = -6110801358414370128L;

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
        sb.append(Zeze.Util.Str.indent(level)).append("Id").append('=').append(getId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Value").append('=').append(System.lineSeparator());
        getValue().getBean().buildString(sb, level + 4);
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
            String _x_ = getId();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = getValue();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.DYNAMIC);
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
            setId(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _o_.ReadDynamic(getValue(), _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Value.initRootInfo(root, this);
    }

    @Override
    protected void resetChildrenRootInfo() {
        _Value.resetRootInfo();
    }

    @Override
    public boolean negativeCheck() {
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
                case 1: _Id = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 2: _Value.followerApply(vlog); break;
            }
        }
    }
}
