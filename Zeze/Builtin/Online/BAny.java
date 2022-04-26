// auto-generated @formatter:off
package Zeze.Builtin.Online;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BAny extends Zeze.Transaction.Bean {
    private final Zeze.Transaction.DynamicBean _Any;

    public Zeze.Transaction.DynamicBean getAny() {
        return _Any;
    }

    public BAny() {
         this(0);
    }

    public BAny(int _varId_) {
        super(_varId_);
        _Any = new Zeze.Transaction.DynamicBean(1, Zeze.Game.Online::GetSpecialTypeIdFromBean, Zeze.Game.Online::CreateBeanFromSpecialTypeId);
    }

    public void Assign(BAny other) {
        getAny().Assign(other.getAny());
    }

    public BAny CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BAny Copy() {
        var copy = new BAny();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BAny a, BAny b) {
        BAny save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public static final long TYPEID = 5253251427600819301L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    public static long GetSpecialTypeIdFromBean_Any(Zeze.Transaction.Bean bean) {
        var _typeId_ = bean.getTypeId();
        if (_typeId_ == Zeze.Transaction.EmptyBean.TYPEID)
            return Zeze.Transaction.EmptyBean.TYPEID;
        throw new RuntimeException("Unknown Bean! dynamic@Zeze.Builtin.Online.BAny:Any");
    }

    public static Zeze.Transaction.Bean CreateBeanFromSpecialTypeId_Any(long typeId) {
        return null;
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Online.BAny: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Any").append('=').append(System.lineSeparator());
        getAny().getBean().BuildString(sb, level + 4);
        sb.append(System.lineSeparator());
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

    @Override
    public void Encode(ByteBuffer _o_) {
        int _i_ = 0;
        {
            var _x_ = getAny();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.DYNAMIC);
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
            _o_.ReadDynamic(getAny(), _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Any.InitRootInfo(root, this);
    }

    @Override
    public boolean NegativeCheck() {
        return false;
    }
}
