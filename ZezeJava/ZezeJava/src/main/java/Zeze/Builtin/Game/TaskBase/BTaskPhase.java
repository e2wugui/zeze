// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BTaskPhase extends Zeze.Transaction.Bean implements BTaskPhaseReadOnly {
    public static final long TYPEID = -3008758867375693466L;

    private long _phaseId; // Phase的Id
    private int _phaseType; // Phase的提交类型
    private String _phaseName; // Phase的名字
    private String _phaseDescription; // Phase的描述
    private final Zeze.Transaction.Collections.PList1<Long> _prePhasesId; // 前置PhaseId
    private final Zeze.Transaction.Collections.PList2<Zeze.Builtin.Game.TaskBase.BTaskCondition> _conditions; // 该Phase所有的Condition
    private final Zeze.Transaction.DynamicBean _extendedData;

    public static Zeze.Transaction.DynamicBean newDynamicBean_ExtendedData() {
        return new Zeze.Transaction.DynamicBean(7, Zeze.Game.TaskBase::getSpecialTypeIdFromBean, Zeze.Game.TaskBase::createBeanFromSpecialTypeId);
    }

    public static long getSpecialTypeIdFromBean_7(Zeze.Transaction.Bean bean) {
        return Zeze.Game.TaskBase.getSpecialTypeIdFromBean(bean);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_7(long typeId) {
        return Zeze.Game.TaskBase.createBeanFromSpecialTypeId(typeId);
    }

    @Override
    public long getPhaseId() {
        if (!isManaged())
            return _phaseId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _phaseId;
        var log = (Log__phaseId)txn.getLog(objectId() + 1);
        return log != null ? log.value : _phaseId;
    }

    public void setPhaseId(long value) {
        if (!isManaged()) {
            _phaseId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__phaseId(this, 1, value));
    }

    @Override
    public int getPhaseType() {
        if (!isManaged())
            return _phaseType;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _phaseType;
        var log = (Log__phaseType)txn.getLog(objectId() + 2);
        return log != null ? log.value : _phaseType;
    }

    public void setPhaseType(int value) {
        if (!isManaged()) {
            _phaseType = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__phaseType(this, 2, value));
    }

    @Override
    public String getPhaseName() {
        if (!isManaged())
            return _phaseName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _phaseName;
        var log = (Log__phaseName)txn.getLog(objectId() + 3);
        return log != null ? log.value : _phaseName;
    }

    public void setPhaseName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _phaseName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__phaseName(this, 3, value));
    }

    @Override
    public String getPhaseDescription() {
        if (!isManaged())
            return _phaseDescription;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _phaseDescription;
        var log = (Log__phaseDescription)txn.getLog(objectId() + 4);
        return log != null ? log.value : _phaseDescription;
    }

    public void setPhaseDescription(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _phaseDescription = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__phaseDescription(this, 4, value));
    }

    public Zeze.Transaction.Collections.PList1<Long> getPrePhasesId() {
        return _prePhasesId;
    }

    @Override
    public Zeze.Transaction.Collections.PList1ReadOnly<Long> getPrePhasesIdReadOnly() {
        return new Zeze.Transaction.Collections.PList1ReadOnly<>(_prePhasesId);
    }

    public Zeze.Transaction.Collections.PList2<Zeze.Builtin.Game.TaskBase.BTaskCondition> getConditions() {
        return _conditions;
    }

    @Override
    public Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.Game.TaskBase.BTaskCondition, Zeze.Builtin.Game.TaskBase.BTaskConditionReadOnly> getConditionsReadOnly() {
        return new Zeze.Transaction.Collections.PList2ReadOnly<>(_conditions);
    }

    public Zeze.Transaction.DynamicBean getExtendedData() {
        return _extendedData;
    }

    @Override
    public Zeze.Transaction.DynamicBeanReadOnly getExtendedDataReadOnly() {
        return _extendedData;
    }

    @SuppressWarnings("deprecation")
    public BTaskPhase() {
        _phaseName = "";
        _phaseDescription = "";
        _prePhasesId = new Zeze.Transaction.Collections.PList1<>(Long.class);
        _prePhasesId.variableId(5);
        _conditions = new Zeze.Transaction.Collections.PList2<>(Zeze.Builtin.Game.TaskBase.BTaskCondition.class);
        _conditions.variableId(6);
        _extendedData = newDynamicBean_ExtendedData();
    }

    @SuppressWarnings("deprecation")
    public BTaskPhase(long _phaseId_, int _phaseType_, String _phaseName_, String _phaseDescription_) {
        _phaseId = _phaseId_;
        _phaseType = _phaseType_;
        if (_phaseName_ == null)
            throw new IllegalArgumentException();
        _phaseName = _phaseName_;
        if (_phaseDescription_ == null)
            throw new IllegalArgumentException();
        _phaseDescription = _phaseDescription_;
        _prePhasesId = new Zeze.Transaction.Collections.PList1<>(Long.class);
        _prePhasesId.variableId(5);
        _conditions = new Zeze.Transaction.Collections.PList2<>(Zeze.Builtin.Game.TaskBase.BTaskCondition.class);
        _conditions.variableId(6);
        _extendedData = newDynamicBean_ExtendedData();
    }

    public void assign(BTaskPhase other) {
        setPhaseId(other.getPhaseId());
        setPhaseType(other.getPhaseType());
        setPhaseName(other.getPhaseName());
        setPhaseDescription(other.getPhaseDescription());
        _prePhasesId.clear();
        _prePhasesId.addAll(other._prePhasesId);
        _conditions.clear();
        for (var e : other._conditions)
            _conditions.add(e.copy());
        _extendedData.assign(other._extendedData);
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

    private static final class Log__phaseId extends Zeze.Transaction.Logs.LogLong {
        public Log__phaseId(BTaskPhase bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTaskPhase)getBelong())._phaseId = value; }
    }

    private static final class Log__phaseType extends Zeze.Transaction.Logs.LogInt {
        public Log__phaseType(BTaskPhase bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTaskPhase)getBelong())._phaseType = value; }
    }

    private static final class Log__phaseName extends Zeze.Transaction.Logs.LogString {
        public Log__phaseName(BTaskPhase bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTaskPhase)getBelong())._phaseName = value; }
    }

    private static final class Log__phaseDescription extends Zeze.Transaction.Logs.LogString {
        public Log__phaseDescription(BTaskPhase bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTaskPhase)getBelong())._phaseDescription = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.TaskBase.BTaskPhase: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("phaseId=").append(getPhaseId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("phaseType=").append(getPhaseType()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("phaseName=").append(getPhaseName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("phaseDescription=").append(getPhaseDescription()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("prePhasesId=[");
        if (!_prePhasesId.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _prePhasesId) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(_item_).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("conditions=[");
        if (!_conditions.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _conditions) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(System.lineSeparator());
                _item_.buildString(sb, level + 4);
                sb.append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("extendedData=").append(System.lineSeparator());
        _extendedData.getBean().buildString(sb, level + 4);
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
            long _x_ = getPhaseId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            int _x_ = getPhaseType();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            String _x_ = getPhaseName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getPhaseDescription();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = _prePhasesId;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                for (var _v_ : _x_)
                    _o_.WriteLong(_v_);
            }
        }
        {
            var _x_ = _conditions;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BEAN);
                for (var _v_ : _x_)
                    _v_.encode(_o_);
            }
        }
        {
            var _x_ = _extendedData;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 7, ByteBuffer.DYNAMIC);
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
            setPhaseId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setPhaseType(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setPhaseName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setPhaseDescription(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            var _x_ = _prePhasesId;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadLong(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            var _x_ = _conditions;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new Zeze.Builtin.Game.TaskBase.BTaskCondition(), _t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 7) {
            _o_.ReadDynamic(_extendedData, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _prePhasesId.initRootInfo(root, this);
        _conditions.initRootInfo(root, this);
        _extendedData.initRootInfo(root, this);
    }

    @Override
    protected void resetChildrenRootInfo() {
        _prePhasesId.resetRootInfo();
        _conditions.resetRootInfo();
        _extendedData.resetRootInfo();
    }

    @Override
    public boolean negativeCheck() {
        if (getPhaseId() < 0)
            return true;
        if (getPhaseType() < 0)
            return true;
        for (var _v_ : _prePhasesId) {
            if (_v_ < 0)
                return true;
        }
        for (var _v_ : _conditions) {
            if (_v_.negativeCheck())
                return true;
        }
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
                case 1: _phaseId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 2: _phaseType = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 3: _phaseName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 4: _phaseDescription = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 5: _prePhasesId.followerApply(vlog); break;
                case 6: _conditions.followerApply(vlog); break;
                case 7: _extendedData.followerApply(vlog); break;
            }
        }
    }
}
