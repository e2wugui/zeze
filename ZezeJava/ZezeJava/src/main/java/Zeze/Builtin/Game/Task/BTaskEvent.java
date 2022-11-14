// auto-generated @formatter:off
package Zeze.Builtin.Game.Task;

import Zeze.Serialize.ByteBuffer;

// Task rpc的Bean
@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BTaskEvent extends Zeze.Transaction.Bean implements BTaskEventReadOnly {
    public static final long TYPEID = -4434634468626847386L;

    private String _TaskName;
    private final Zeze.Transaction.DynamicBean _DynamicData;

    public static Zeze.Transaction.DynamicBean newDynamicBean_DynamicData() {
        return new Zeze.Transaction.DynamicBean(4, Zeze.Game.Task::getSpecialTypeIdFromBean, Zeze.Game.Task::createBeanFromSpecialTypeId);
    }

    public static long getSpecialTypeIdFromBean_4(Zeze.Transaction.Bean bean) {
        return Zeze.Game.Task.getSpecialTypeIdFromBean(bean);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_4(long typeId) {
        return Zeze.Game.Task.createBeanFromSpecialTypeId(typeId);
    }

    @Override
    public String getTaskName() {
        if (!isManaged())
            return _TaskName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _TaskName;
        var log = (Log__TaskName)txn.getLog(objectId() + 1);
        return log != null ? log.value : _TaskName;
    }

    public void setTaskName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _TaskName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__TaskName(this, 1, value));
    }

    public Zeze.Transaction.DynamicBean getDynamicData() {
        return _DynamicData;
    }

    @Override
    public Zeze.Transaction.DynamicBeanReadOnly getDynamicDataReadOnly() {
        return _DynamicData;
    }

    @SuppressWarnings("deprecation")
    public BTaskEvent() {
        _TaskName = "";
        _DynamicData = newDynamicBean_DynamicData();
    }

    @SuppressWarnings("deprecation")
    public BTaskEvent(String _TaskName_) {
        if (_TaskName_ == null)
            throw new IllegalArgumentException();
        _TaskName = _TaskName_;
        _DynamicData = newDynamicBean_DynamicData();
    }

    public void assign(BTaskEvent other) {
        setTaskName(other.getTaskName());
        _DynamicData.assign(other._DynamicData);
    }

    @Deprecated
    public void Assign(BTaskEvent other) {
        assign(other);
    }

    public BTaskEvent copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTaskEvent copy() {
        var copy = new BTaskEvent();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BTaskEvent Copy() {
        return copy();
    }

    public static void swap(BTaskEvent a, BTaskEvent b) {
        BTaskEvent save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__TaskName extends Zeze.Transaction.Logs.LogString {
        public Log__TaskName(BTaskEvent bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTaskEvent)getBelong())._TaskName = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.Task.BTaskEvent: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("TaskName").append('=').append(getTaskName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("DynamicData").append('=').append(System.lineSeparator());
        _DynamicData.getBean().buildString(sb, level + 4);
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
            String _x_ = getTaskName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = _DynamicData;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.DYNAMIC);
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
            setTaskName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while ((_t_ & 0xff) > 1 && _i_ < 4) {
            _o_.SkipUnknownField(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _o_.ReadDynamic(_DynamicData, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _DynamicData.initRootInfo(root, this);
    }

    @Override
    protected void resetChildrenRootInfo() {
        _DynamicData.resetRootInfo();
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
                case 1: _TaskName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 4: _DynamicData.followerApply(vlog); break;
            }
        }
    }
}
