// auto-generated @formatter:off
package Zeze.Beans.Collections.LinkedMap;

import Zeze.Serialize.ByteBuffer;

public final class BLinkedMapNodeValue extends Zeze.Transaction.Bean {
    private String _Id;
    private final Zeze.Transaction.DynamicBean _Value;
    private boolean _Pin;

    public String getId() {
        if (!isManaged())
            return _Id;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _Id;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__Id)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.getValue() : _Id;
    }

    public void setId(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Id = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__Id(this, value));
    }

    public Zeze.Transaction.DynamicBean getValue() {
        return _Value;
    }

    public boolean isPin() {
        if (!isManaged())
            return _Pin;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _Pin;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__Pin)txn.GetLog(this.getObjectId() + 3);
        return log != null ? log.getValue() : _Pin;
    }

    public void setPin(boolean value) {
        if (!isManaged()) {
            _Pin = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__Pin(this, value));
    }

    public BLinkedMapNodeValue() {
         this(0);
    }

    public BLinkedMapNodeValue(int _varId_) {
        super(_varId_);
        _Id = "";
        _Value = new Zeze.Transaction.DynamicBean(2, BLinkedMapNodeValue::GetSpecialTypeIdFromBean_Value, BLinkedMapNodeValue::CreateBeanFromSpecialTypeId_Value);
    }

    public void Assign(BLinkedMapNodeValue other) {
        setId(other.getId());
        getValue().Assign(other.getValue());
        setPin(other.isPin());
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
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public static final long TYPEID = 8765045186332684704L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private static final class Log__Id extends Zeze.Transaction.Log1<BLinkedMapNodeValue, String> {
        public Log__Id(BLinkedMapNodeValue self, String value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 1; }
        @Override
        public void Commit() { this.getBeanTyped()._Id = this.getValue(); }
    }

    public static long GetSpecialTypeIdFromBean_Value(Zeze.Transaction.Bean bean) {
        var _typeId_ = bean.getTypeId();
        if (_typeId_ == Zeze.Transaction.EmptyBean.TYPEID)
            return Zeze.Transaction.EmptyBean.TYPEID;
        throw new RuntimeException("Unknown Bean! dynamic@Zeze.Beans.Collections.LinkedMap.BLinkedMapNodeValue:Value");
    }

    public static Zeze.Transaction.Bean CreateBeanFromSpecialTypeId_Value(long typeId) {
        return null;
    }


    private static final class Log__Pin extends Zeze.Transaction.Log1<BLinkedMapNodeValue, Boolean> {
        public Log__Pin(BLinkedMapNodeValue self, Boolean value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 3; }
        @Override
        public void Commit() { this.getBeanTyped()._Pin = this.getValue(); }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        BuildString(sb, 0);
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    @Override
    public void BuildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Beans.Collections.LinkedMap.BLinkedMapNodeValue: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Id").append('=').append(getId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Value").append('=').append(System.lineSeparator());
        getValue().getBean().BuildString(sb, level + 4);
        sb.append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Pin").append('=').append(isPin()).append(System.lineSeparator());
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append('}');
    }

    private static int _PRE_ALLOC_SIZE_ = 16;

    @Override
    public int getPreAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void setPreAllocSize(int size) {
        _PRE_ALLOC_SIZE_ = size;
    }

    @SuppressWarnings("UnusedAssignment")
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
        {
            boolean _x_ = isPin();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        _o_.WriteByte(0);
    }

    @SuppressWarnings("UnusedAssignment")
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
        if (_i_ == 3) {
            setPin(_o_.ReadBool(_t_));
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

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean NegativeCheck() {
        return false;
    }
}
