// auto-generated @formatter:off
package Zeze.Builtin.Game.Task;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BTaskPhase extends Zeze.Transaction.Bean implements BTaskPhaseReadOnly {
    public static final long TYPEID = -2081342941887981883L;

    private String _PhaseId;
    private String _PhaseName;
        final Zeze.Transaction.Collections.CollOne<Zeze.Builtin.Game.Task.BTaskCondition> _CurrentCondition;
    private final Zeze.Transaction.DynamicBean _TaskPhaseCustomData;

    public static Zeze.Transaction.DynamicBean newDynamicBean_TaskPhaseCustomData() {
        return new Zeze.Transaction.DynamicBean(4, Zeze.Game.TaskPhase::getSpecialTypeIdFromBean, Zeze.Game.Condition::createBeanFromSpecialTypeId);
    }

    public static long getSpecialTypeIdFromBean_TaskPhaseCustomData(Zeze.Transaction.Bean bean) {
        return Zeze.Game.TaskPhase.getSpecialTypeIdFromBean(bean);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_TaskPhaseCustomData(long typeId) {
        return Zeze.Game.Condition.createBeanFromSpecialTypeId(typeId);
    }

    @Override
    public String getPhaseId() {
        if (!isManaged())
            return _PhaseId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _PhaseId;
        var log = (Log__PhaseId)txn.getLog(objectId() + 1);
        return log != null ? log.value : _PhaseId;
    }

    public void setPhaseId(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _PhaseId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__PhaseId(this, 1, value));
    }

    @Override
    public String getPhaseName() {
        if (!isManaged())
            return _PhaseName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _PhaseName;
        var log = (Log__PhaseName)txn.getLog(objectId() + 2);
        return log != null ? log.value : _PhaseName;
    }

    public void setPhaseName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _PhaseName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__PhaseName(this, 2, value));
    }

    public Zeze.Builtin.Game.Task.BTaskCondition getCurrentCondition() {
        return _CurrentCondition.getValue();
    }

    public void setCurrentCondition(Zeze.Builtin.Game.Task.BTaskCondition value) {
        _CurrentCondition.setValue(value);
    }

    @Override
    public Zeze.Builtin.Game.Task.BTaskConditionReadOnly getCurrentConditionReadOnly() {
        return _CurrentCondition.getValue();
    }

    public Zeze.Transaction.DynamicBean getTaskPhaseCustomData() {
        return _TaskPhaseCustomData;
    }

    @Override
    public Zeze.Transaction.DynamicBeanReadOnly getTaskPhaseCustomDataReadOnly() {
        return _TaskPhaseCustomData;
    }

    @SuppressWarnings("deprecation")
    public BTaskPhase() {
        _PhaseId = "";
        _PhaseName = "";
        _CurrentCondition = new Zeze.Transaction.Collections.CollOne<>(new Zeze.Builtin.Game.Task.BTaskCondition(), Zeze.Builtin.Game.Task.BTaskCondition.class);
        _CurrentCondition.variableId(3);
        _TaskPhaseCustomData = newDynamicBean_TaskPhaseCustomData();
    }

    @SuppressWarnings("deprecation")
    public BTaskPhase(String _PhaseId_, String _PhaseName_) {
        if (_PhaseId_ == null)
            throw new IllegalArgumentException();
        _PhaseId = _PhaseId_;
        if (_PhaseName_ == null)
            throw new IllegalArgumentException();
        _PhaseName = _PhaseName_;
        _CurrentCondition = new Zeze.Transaction.Collections.CollOne<>(new Zeze.Builtin.Game.Task.BTaskCondition(), Zeze.Builtin.Game.Task.BTaskCondition.class);
        _CurrentCondition.variableId(3);
        _TaskPhaseCustomData = newDynamicBean_TaskPhaseCustomData();
    }

    public void assign(BTaskPhase other) {
        setPhaseId(other.getPhaseId());
        setPhaseName(other.getPhaseName());
        _CurrentCondition.assign(other._CurrentCondition);
        _TaskPhaseCustomData.assign(other._TaskPhaseCustomData);
    }

    @Deprecated
    public void Assign(BTaskPhase other) {
        assign(other);
    }

    public BTaskPhase copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTaskPhase copy() {
        var copy = new BTaskPhase();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BTaskPhase Copy() {
        return copy();
    }

    public static void swap(BTaskPhase a, BTaskPhase b) {
        BTaskPhase save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__PhaseId extends Zeze.Transaction.Logs.LogString {
        public Log__PhaseId(BTaskPhase bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTaskPhase)getBelong())._PhaseId = value; }
    }

    private static final class Log__PhaseName extends Zeze.Transaction.Logs.LogString {
        public Log__PhaseName(BTaskPhase bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTaskPhase)getBelong())._PhaseName = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.Task.BTaskPhase: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("PhaseId").append('=').append(getPhaseId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("PhaseName").append('=').append(getPhaseName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("CurrentCondition").append('=').append(System.lineSeparator());
        _CurrentCondition.buildString(sb, level + 4);
        sb.append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("TaskPhaseCustomData").append('=').append(System.lineSeparator());
        _TaskPhaseCustomData.getBean().buildString(sb, level + 4);
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
            String _x_ = getPhaseId();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getPhaseName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 3, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _CurrentCondition.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        {
            var _x_ = _TaskPhaseCustomData;
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
            setPhaseId(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setPhaseName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _o_.ReadBean(_CurrentCondition, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _o_.ReadDynamic(_TaskPhaseCustomData, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _CurrentCondition.initRootInfo(root, this);
        _TaskPhaseCustomData.initRootInfo(root, this);
    }

    @Override
    protected void resetChildrenRootInfo() {
        _CurrentCondition.resetRootInfo();
        _TaskPhaseCustomData.resetRootInfo();
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
                case 1: _PhaseId = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 2: _PhaseName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 3: _CurrentCondition.followerApply(vlog); break;
                case 4: _TaskPhaseCustomData.followerApply(vlog); break;
            }
        }
    }
}
