// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BOnlineCustom extends Zeze.Transaction.Bean {
    public static final long TYPEID = 4804037996721232525L;

    private final Zeze.Transaction.DynamicBean _CustomData;

    public static Zeze.Transaction.DynamicBean newDynamicBean_CustomData() {
        return new Zeze.Transaction.DynamicBean(1, BOnlineCustom::getSpecialTypeIdFromBean_CustomData, BOnlineCustom::createBeanFromSpecialTypeId_CustomData);
    }

    public static long getSpecialTypeIdFromBean_CustomData(Zeze.Transaction.Bean bean) {
        var _typeId_ = bean.typeId();
        if (_typeId_ == Zeze.Transaction.EmptyBean.TYPEID)
            return Zeze.Transaction.EmptyBean.TYPEID;
        throw new RuntimeException("Unknown Bean! dynamic@Zeze.Builtin.Timer.BOnlineCustom:CustomData");
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_CustomData(long typeId) {
        return null;
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

    public Zeze.Transaction.DynamicBean getCustomData() {
        return _CustomData;
    }


    @SuppressWarnings("deprecation")
    public BOnlineCustom() {
        _CustomData = newDynamicBean_CustomData();
    }

    public void assign(BOnlineCustom other) {
        _CustomData.assign(other._CustomData);
    }

    @Deprecated
    public void Assign(BOnlineCustom other) {
        assign(other);
    }

    public BOnlineCustom copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BOnlineCustom copy() {
        var copy = new BOnlineCustom();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BOnlineCustom Copy() {
        return copy();
    }

    public static void swap(BOnlineCustom a, BOnlineCustom b) {
        BOnlineCustom save = a.copy();
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Timer.BOnlineCustom: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("CustomData").append('=').append(System.lineSeparator());
        _CustomData.getBean().buildString(sb, level + 4);
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
            var _x_ = _CustomData;
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
            _o_.ReadDynamic(_CustomData, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _CustomData.initRootInfo(root, this);
    }

    @Override
    protected void resetChildrenRootInfo() {
        _CustomData.resetRootInfo();
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
                case 1: _CustomData.followerApply(vlog); break;
            }
        }
    }
}
