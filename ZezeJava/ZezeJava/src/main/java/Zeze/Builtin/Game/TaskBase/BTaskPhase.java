// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

import Zeze.Serialize.ByteBuffer;

// <enum name="ConditionCompleteBranch" value="24"/> 通过不同的完成条件实现进入不同的分支Phase
@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BTaskPhase extends Zeze.Transaction.Bean implements BTaskPhaseReadOnly {
    public static final long TYPEID = -3008758867375693466L;

    private long _phaseId; // Phase的Id
    private int _phaseType; // Phase的提交类型：commitAuto | commitNPCTalk
    private String _phaseName; // Phase的名字
    private String _phaseDescription; // Phase的描述
    private final Zeze.Transaction.Collections.PList1<Long> _afterPhaseIds; // 后置PhaseId
    private long _nextPhaseId; // 下一个PhaseId（允许通过不同的conditions完成情况动态变化）
    private final Zeze.Transaction.Collections.PMap2<Long, Zeze.Builtin.Game.TaskBase.BTaskCondition> _conditions; // 该Phase包含的所有的Condition
    private int _conditionsCompleteType; // 该Phase所有Condition的完成类型：完成所有completeAll | 完成任一completeAny | 顺序完成completeSequence
    private final Zeze.Transaction.DynamicBean _extendedData;
    public static final long DynamicTypeId_ExtendedData_Zeze_Builtin_Game_TaskBase_BTaskPhaseCommitNPCTalk = 4346965359965455652L;

    public static Zeze.Transaction.DynamicBean newDynamicBean_ExtendedData() {
        return new Zeze.Transaction.DynamicBean(10, BTaskPhase::getSpecialTypeIdFromBean_10, BTaskPhase::createBeanFromSpecialTypeId_10);
    }

    public static long getSpecialTypeIdFromBean_10(Zeze.Transaction.Bean bean) {
        var _typeId_ = bean.typeId();
        if (_typeId_ == Zeze.Transaction.EmptyBean.TYPEID)
            return Zeze.Transaction.EmptyBean.TYPEID;
        if (_typeId_ == 4346965359965455652L)
            return 4346965359965455652L; // Zeze.Builtin.Game.TaskBase.BTaskPhaseCommitNPCTalk
        throw new RuntimeException("Unknown Bean! dynamic@Zeze.Builtin.Game.TaskBase.BTaskPhase:extendedData");
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_10(long typeId) {
        if (typeId == 4346965359965455652L)
            return new Zeze.Builtin.Game.TaskBase.BTaskPhaseCommitNPCTalk();
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

    public Zeze.Transaction.Collections.PList1<Long> getAfterPhaseIds() {
        return _afterPhaseIds;
    }

    @Override
    public Zeze.Transaction.Collections.PList1ReadOnly<Long> getAfterPhaseIdsReadOnly() {
        return new Zeze.Transaction.Collections.PList1ReadOnly<>(_afterPhaseIds);
    }

    @Override
    public long getNextPhaseId() {
        if (!isManaged())
            return _nextPhaseId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _nextPhaseId;
        var log = (Log__nextPhaseId)txn.getLog(objectId() + 7);
        return log != null ? log.value : _nextPhaseId;
    }

    public void setNextPhaseId(long value) {
        if (!isManaged()) {
            _nextPhaseId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__nextPhaseId(this, 7, value));
    }

    public Zeze.Transaction.Collections.PMap2<Long, Zeze.Builtin.Game.TaskBase.BTaskCondition> getConditions() {
        return _conditions;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<Long, Zeze.Builtin.Game.TaskBase.BTaskCondition, Zeze.Builtin.Game.TaskBase.BTaskConditionReadOnly> getConditionsReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_conditions);
    }

    @Override
    public int getConditionsCompleteType() {
        if (!isManaged())
            return _conditionsCompleteType;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _conditionsCompleteType;
        var log = (Log__conditionsCompleteType)txn.getLog(objectId() + 9);
        return log != null ? log.value : _conditionsCompleteType;
    }

    public void setConditionsCompleteType(int value) {
        if (!isManaged()) {
            _conditionsCompleteType = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__conditionsCompleteType(this, 9, value));
    }

    public Zeze.Transaction.DynamicBean getExtendedData() {
        return _extendedData;
    }

    public Zeze.Builtin.Game.TaskBase.BTaskPhaseCommitNPCTalk getExtendedData_Zeze_Builtin_Game_TaskBase_BTaskPhaseCommitNPCTalk() {
        return (Zeze.Builtin.Game.TaskBase.BTaskPhaseCommitNPCTalk)_extendedData.getBean();
    }

    public void setExtendedData(Zeze.Builtin.Game.TaskBase.BTaskPhaseCommitNPCTalk value) {
        _extendedData.setBean(value);
    }

    @Override
    public Zeze.Transaction.DynamicBeanReadOnly getExtendedDataReadOnly() {
        return _extendedData;
    }

    @Override
    public Zeze.Builtin.Game.TaskBase.BTaskPhaseCommitNPCTalkReadOnly getExtendedData_Zeze_Builtin_Game_TaskBase_BTaskPhaseCommitNPCTalkReadOnly() {
        return (Zeze.Builtin.Game.TaskBase.BTaskPhaseCommitNPCTalk)_extendedData.getBean();
    }

    @SuppressWarnings("deprecation")
    public BTaskPhase() {
        _phaseName = "";
        _phaseDescription = "";
        _afterPhaseIds = new Zeze.Transaction.Collections.PList1<>(Long.class);
        _afterPhaseIds.variableId(6);
        _conditions = new Zeze.Transaction.Collections.PMap2<>(Long.class, Zeze.Builtin.Game.TaskBase.BTaskCondition.class);
        _conditions.variableId(8);
        _extendedData = newDynamicBean_ExtendedData();
    }

    @SuppressWarnings("deprecation")
    public BTaskPhase(long _phaseId_, int _phaseType_, String _phaseName_, String _phaseDescription_, long _nextPhaseId_, int _conditionsCompleteType_) {
        _phaseId = _phaseId_;
        _phaseType = _phaseType_;
        if (_phaseName_ == null)
            throw new IllegalArgumentException();
        _phaseName = _phaseName_;
        if (_phaseDescription_ == null)
            throw new IllegalArgumentException();
        _phaseDescription = _phaseDescription_;
        _afterPhaseIds = new Zeze.Transaction.Collections.PList1<>(Long.class);
        _afterPhaseIds.variableId(6);
        _nextPhaseId = _nextPhaseId_;
        _conditions = new Zeze.Transaction.Collections.PMap2<>(Long.class, Zeze.Builtin.Game.TaskBase.BTaskCondition.class);
        _conditions.variableId(8);
        _conditionsCompleteType = _conditionsCompleteType_;
        _extendedData = newDynamicBean_ExtendedData();
    }

    public void assign(BTaskPhase other) {
        setPhaseId(other.getPhaseId());
        setPhaseType(other.getPhaseType());
        setPhaseName(other.getPhaseName());
        setPhaseDescription(other.getPhaseDescription());
        _afterPhaseIds.clear();
        _afterPhaseIds.addAll(other._afterPhaseIds);
        setNextPhaseId(other.getNextPhaseId());
        _conditions.clear();
        for (var e : other._conditions.entrySet())
            _conditions.put(e.getKey(), e.getValue().copy());
        setConditionsCompleteType(other.getConditionsCompleteType());
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

    private static final class Log__nextPhaseId extends Zeze.Transaction.Logs.LogLong {
        public Log__nextPhaseId(BTaskPhase bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTaskPhase)getBelong())._nextPhaseId = value; }
    }

    private static final class Log__conditionsCompleteType extends Zeze.Transaction.Logs.LogInt {
        public Log__conditionsCompleteType(BTaskPhase bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTaskPhase)getBelong())._conditionsCompleteType = value; }
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
        sb.append(Zeze.Util.Str.indent(level)).append("afterPhaseIds=[");
        if (!_afterPhaseIds.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _afterPhaseIds) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(_item_).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("nextPhaseId=").append(getNextPhaseId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("conditions={");
        if (!_conditions.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _kv_ : _conditions.entrySet()) {
                sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(_kv_.getKey()).append(',').append(System.lineSeparator());
                sb.append(Zeze.Util.Str.indent(level)).append("Value=").append(System.lineSeparator());
                _kv_.getValue().buildString(sb, level + 4);
                sb.append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append('}').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("conditionsCompleteType=").append(getConditionsCompleteType()).append(',').append(System.lineSeparator());
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
            var _x_ = _afterPhaseIds;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                for (var _v_ : _x_)
                    _o_.WriteLong(_v_);
            }
        }
        {
            long _x_ = getNextPhaseId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 7, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = _conditions;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 8, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _e_.getValue().encode(_o_);
                }
            }
        }
        {
            int _x_ = getConditionsCompleteType();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 9, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = _extendedData;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 10, ByteBuffer.DYNAMIC);
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
        while ((_t_ & 0xff) > 1 && _i_ < 6) {
            _o_.SkipUnknownField(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            var _x_ = _afterPhaseIds;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadLong(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 7) {
            setNextPhaseId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 8) {
            var _x_ = _conditions;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadLong(_s_);
                    var _v_ = _o_.ReadBean(new Zeze.Builtin.Game.TaskBase.BTaskCondition(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 9) {
            setConditionsCompleteType(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 10) {
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
        _afterPhaseIds.initRootInfo(root, this);
        _conditions.initRootInfo(root, this);
        _extendedData.initRootInfo(root, this);
    }

    @Override
    protected void resetChildrenRootInfo() {
        _afterPhaseIds.resetRootInfo();
        _conditions.resetRootInfo();
        _extendedData.resetRootInfo();
    }

    @Override
    public boolean negativeCheck() {
        if (getPhaseId() < 0)
            return true;
        if (getPhaseType() < 0)
            return true;
        for (var _v_ : _afterPhaseIds) {
            if (_v_ < 0)
                return true;
        }
        if (getNextPhaseId() < 0)
            return true;
        for (var _v_ : _conditions.values()) {
            if (_v_.negativeCheck())
                return true;
        }
        if (getConditionsCompleteType() < 0)
            return true;
        if (_extendedData.negativeCheck())
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
                case 1: _phaseId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 2: _phaseType = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 3: _phaseName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 4: _phaseDescription = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 6: _afterPhaseIds.followerApply(vlog); break;
                case 7: _nextPhaseId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 8: _conditions.followerApply(vlog); break;
                case 9: _conditionsCompleteType = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 10: _extendedData.followerApply(vlog); break;
            }
        }
    }
}
