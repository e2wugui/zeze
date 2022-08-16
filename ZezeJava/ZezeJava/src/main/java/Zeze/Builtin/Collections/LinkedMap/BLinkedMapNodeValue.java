// auto-generated @formatter:off
package Zeze.Builtin.Collections.LinkedMap;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BLinkedMapNodeValue extends Zeze.Transaction.Bean {
    private String _Id; // LinkedMap的Key转成字符串类型
    private final Zeze.Transaction.DynamicBean _Value;
        public static long GetSpecialTypeIdFromBean_Value(Zeze.Transaction.Bean bean) {
            var _typeId_ = bean.typeId();
            if (_typeId_ == Zeze.Transaction.EmptyBean.TYPEID)
                return Zeze.Transaction.EmptyBean.TYPEID;
            throw new RuntimeException("Unknown Bean! dynamic@Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeValue:Value");
        }

        public static Zeze.Transaction.Bean CreateBeanFromSpecialTypeId_Value(long typeId) {
            return null;
        }


    public String getId() {
        if (!isManaged())
            return _Id;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Id;
        var log = (Log__Id)txn.GetLog(objectId() + 1);
        return log != null ? log.Value : _Id;
    }

    public void setId(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Id = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.PutLog(new Log__Id(this, 1, value));
    }

    public Zeze.Transaction.DynamicBean getValue() {
        return _Value;
    }

    public BLinkedMapNodeValue() {
        _Id = "";
        _Value = new Zeze.Transaction.DynamicBean(2, Zeze.Collections.LinkedMap::GetSpecialTypeIdFromBean, Zeze.Collections.LinkedMap::CreateBeanFromSpecialTypeId);
    }

    public BLinkedMapNodeValue(String _Id_) {
        _Id = _Id_;
        _Value = new Zeze.Transaction.DynamicBean(2, Zeze.Collections.LinkedMap::GetSpecialTypeIdFromBean, Zeze.Collections.LinkedMap::CreateBeanFromSpecialTypeId);
    }

    public void Assign(BLinkedMapNodeValue other) {
        setId(other.getId());
        getValue().Assign(other.getValue());
    }

    public BLinkedMapNodeValue CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BLinkedMapNodeValue Copy() {
        var copy = new BLinkedMapNodeValue();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BLinkedMapNodeValue a, BLinkedMapNodeValue b) {
        BLinkedMapNodeValue save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public BLinkedMapNodeValue CopyBean() {
        return Copy();
    }

    public static final long TYPEID = -6110801358414370128L;

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Id extends Zeze.Transaction.Logs.LogString {
        public Log__Id(BLinkedMapNodeValue bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BLinkedMapNodeValue)getBelong())._Id = Value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        BuildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void BuildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeValue: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Id").append('=').append(getId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Value").append('=').append(System.lineSeparator());
        getValue().getBean().BuildString(sb, level + 4);
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
    public void Encode(ByteBuffer _o_) {
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
                _x_.Encode(_o_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void Decode(ByteBuffer _o_) {
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
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Value.InitRootInfo(root, this);
    }

    @Override
    protected void ResetChildrenRootInfo() {
        _Value.ResetRootInfo();
    }

    @Override
    public boolean NegativeCheck() {
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void FollowerApply(Zeze.Transaction.Log log) {
        var vars = ((Zeze.Transaction.Collections.LogBean)log).getVariables();
        if (vars == null)
            return;
        for (var it = vars.iterator(); it.moveToNext(); ) {
            var vlog = it.value();
            switch (vlog.getVariableId()) {
                case 1: _Id = ((Zeze.Transaction.Logs.LogString)vlog).Value; break;
                case 2: _Value.FollowerApply(vlog); break;
            }
        }
    }
}
