// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BAny extends Zeze.Transaction.Bean {
    private final Zeze.Transaction.DynamicBean _Any;

    public static long getSpecialTypeIdFromBean_Any(Zeze.Transaction.Bean bean) {
        return Zeze.Game.Online.getSpecialTypeIdFromBean(bean);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_Any(long typeId) {
        return Zeze.Game.Online.createBeanFromSpecialTypeId(typeId);
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

    public Zeze.Transaction.DynamicBean getAny() {
        return _Any;
    }

    @SuppressWarnings("deprecation")
    public BAny() {
        _Any = new Zeze.Transaction.DynamicBean(1, Zeze.Game.Online::getSpecialTypeIdFromBean, Zeze.Game.Online::createBeanFromSpecialTypeId);
    }

    public void assign(BAny other) {
        getAny().assign(other.getAny());
    }

    @Deprecated
    public void Assign(BAny other) {
        assign(other);
    }

    public BAny copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    public BAny copy() {
        var copy = new BAny();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BAny Copy() {
        return copy();
    }

    public static void swap(BAny a, BAny b) {
        BAny save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public BAny copyBean() {
        return copy();
    }

    public static final long TYPEID = 5085416693215220301L;

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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.Online.BAny: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Any").append('=').append(System.lineSeparator());
        getAny().getBean().buildString(sb, level + 4);
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
            var _x_ = getAny();
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
            _o_.ReadDynamic(getAny(), _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Any.initRootInfo(root, this);
    }

    @Override
    protected void resetChildrenRootInfo() {
        _Any.resetRootInfo();
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
                case 1: _Any.followerApply(vlog); break;
            }
        }
    }
}
