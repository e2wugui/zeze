// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

// ======================================== TaskPhase的Bean数据 ========================================
@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BTaskPhase extends Zeze.Transaction.Bean implements BTaskPhaseReadOnly {
    public static final long TYPEID = -3008758867375693466L;

    private long _phaseId; // Phase的Id
    private String _phaseName; // Phase的名字
    private String _phaseDescription; // Phase的描述
    private final Zeze.Transaction.Collections.PList1<Long> _prePhaseIds; // 前置PhaseId
    private long _nextPhaseId; // 下一个PhaseId（允许通过不同的conditions完成情况动态变化）
    private final Zeze.Transaction.Collections.PMap2<Long, Zeze.Builtin.Game.TaskBase.BSubPhase> _subPhases; // 该Phase包含的所有的SubPhase
    private long _currentSubPhaseId; // 当前的SubPhaseId

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
        var log = (Log__phaseId)txn.getLog(objectId() + 2);
        return log != null ? log.value : _phaseId;
    }

    public void setPhaseId(long value) {
        if (!isManaged()) {
            _phaseId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__phaseId(this, 2, value));
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

    public Zeze.Transaction.Collections.PList1<Long> getPrePhaseIds() {
        return _prePhaseIds;
    }

    @Override
    public Zeze.Transaction.Collections.PList1ReadOnly<Long> getPrePhaseIdsReadOnly() {
        return new Zeze.Transaction.Collections.PList1ReadOnly<>(_prePhaseIds);
    }

    @Override
    public long getNextPhaseId() {
        if (!isManaged())
            return _nextPhaseId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _nextPhaseId;
        var log = (Log__nextPhaseId)txn.getLog(objectId() + 6);
        return log != null ? log.value : _nextPhaseId;
    }

    public void setNextPhaseId(long value) {
        if (!isManaged()) {
            _nextPhaseId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__nextPhaseId(this, 6, value));
    }

    public Zeze.Transaction.Collections.PMap2<Long, Zeze.Builtin.Game.TaskBase.BSubPhase> getSubPhases() {
        return _subPhases;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<Long, Zeze.Builtin.Game.TaskBase.BSubPhase, Zeze.Builtin.Game.TaskBase.BSubPhaseReadOnly> getSubPhasesReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_subPhases);
    }

    @Override
    public long getCurrentSubPhaseId() {
        if (!isManaged())
            return _currentSubPhaseId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _currentSubPhaseId;
        var log = (Log__currentSubPhaseId)txn.getLog(objectId() + 8);
        return log != null ? log.value : _currentSubPhaseId;
    }

    public void setCurrentSubPhaseId(long value) {
        if (!isManaged()) {
            _currentSubPhaseId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__currentSubPhaseId(this, 8, value));
    }

    @SuppressWarnings("deprecation")
    public BTaskPhase() {
        _phaseName = "";
        _phaseDescription = "";
        _prePhaseIds = new Zeze.Transaction.Collections.PList1<>(Long.class);
        _prePhaseIds.variableId(5);
        _subPhases = new Zeze.Transaction.Collections.PMap2<>(Long.class, Zeze.Builtin.Game.TaskBase.BSubPhase.class);
        _subPhases.variableId(7);
    }

    @SuppressWarnings("deprecation")
    public BTaskPhase(long _phaseId_, String _phaseName_, String _phaseDescription_, long _nextPhaseId_, long _currentSubPhaseId_) {
        _phaseId = _phaseId_;
        if (_phaseName_ == null)
            _phaseName_ = "";
        _phaseName = _phaseName_;
        if (_phaseDescription_ == null)
            _phaseDescription_ = "";
        _phaseDescription = _phaseDescription_;
        _prePhaseIds = new Zeze.Transaction.Collections.PList1<>(Long.class);
        _prePhaseIds.variableId(5);
        _nextPhaseId = _nextPhaseId_;
        _subPhases = new Zeze.Transaction.Collections.PMap2<>(Long.class, Zeze.Builtin.Game.TaskBase.BSubPhase.class);
        _subPhases.variableId(7);
        _currentSubPhaseId = _currentSubPhaseId_;
    }

    @Override
    public void reset() {
        setPhaseId(0);
        setPhaseName("");
        setPhaseDescription("");
        _prePhaseIds.clear();
        setNextPhaseId(0);
        _subPhases.clear();
        setCurrentSubPhaseId(0);
        _unknown_ = null;
    }

    public void assign(BTaskPhase other) {
        setPhaseId(other.getPhaseId());
        setPhaseName(other.getPhaseName());
        setPhaseDescription(other.getPhaseDescription());
        _prePhaseIds.clear();
        _prePhaseIds.addAll(other._prePhaseIds);
        setNextPhaseId(other.getNextPhaseId());
        _subPhases.clear();
        for (var e : other._subPhases.entrySet())
            _subPhases.put(e.getKey(), e.getValue().copy());
        setCurrentSubPhaseId(other.getCurrentSubPhaseId());
        _unknown_ = other._unknown_;
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

    private static final class Log__currentSubPhaseId extends Zeze.Transaction.Logs.LogLong {
        public Log__currentSubPhaseId(BTaskPhase bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTaskPhase)getBelong())._currentSubPhaseId = value; }
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
        sb.append(Zeze.Util.Str.indent(level)).append("phaseName=").append(getPhaseName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("phaseDescription=").append(getPhaseDescription()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("prePhaseIds=[");
        if (!_prePhaseIds.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _prePhaseIds) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(_item_).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("nextPhaseId=").append(getNextPhaseId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("subPhases={");
        if (!_subPhases.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _kv_ : _subPhases.entrySet()) {
                sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(_kv_.getKey()).append(',').append(System.lineSeparator());
                sb.append(Zeze.Util.Str.indent(level)).append("Value=").append(System.lineSeparator());
                _kv_.getValue().buildString(sb, level + 4);
                sb.append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append('}').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("currentSubPhaseId=").append(getCurrentSubPhaseId()).append(System.lineSeparator());
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

    private byte[] _unknown_;

    public byte[] unknown() {
        return _unknown_;
    }

    public void clearUnknown() {
        _unknown_ = null;
    }

    @Override
    public void encode(ByteBuffer _o_) {
        ByteBuffer _u_ = null;
        var _ua_ = _unknown_;
        var _ui_ = _ua_ != null ? (_u_ = ByteBuffer.Wrap(_ua_)).readUnknownIndex() : Long.MAX_VALUE;
        int _i_ = 0;
        while (_ui_ < 2) {
            _i_ = _o_.writeUnknownField(_i_, _ui_, _u_);
            _ui_ = _u_.readUnknownIndex();
        }
        {
            long _x_ = getPhaseId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
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
            var _x_ = _prePhaseIds;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                for (var _v_ : _x_) {
                    _o_.WriteLong(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            long _x_ = getNextPhaseId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = _subPhases;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 7, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _e_.getValue().encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            long _x_ = getCurrentSubPhaseId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 8, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        _o_.writeAllUnknownFields(_i_, _ui_, _u_);
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        ByteBuffer _u_ = null;
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        while ((_t_ & 0xff) > 1 && _i_ < 2) {
            _u_ = _o_.readUnknownField(_i_, _t_, _u_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setPhaseId(_o_.ReadLong(_t_));
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
            var _x_ = _prePhaseIds;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadLong(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            setNextPhaseId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 7) {
            var _x_ = _subPhases;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadLong(_s_);
                    var _v_ = _o_.ReadBean(new Zeze.Builtin.Game.TaskBase.BSubPhase(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 8) {
            setCurrentSubPhaseId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _prePhaseIds.initRootInfo(root, this);
        _subPhases.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _prePhaseIds.initRootInfoWithRedo(root, this);
        _subPhases.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getPhaseId() < 0)
            return true;
        for (var _v_ : _prePhaseIds) {
            if (_v_ < 0)
                return true;
        }
        if (getNextPhaseId() < 0)
            return true;
        for (var _v_ : _subPhases.values()) {
            if (_v_.negativeCheck())
                return true;
        }
        if (getCurrentSubPhaseId() < 0)
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
                case 2: _phaseId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 3: _phaseName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 4: _phaseDescription = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 5: _prePhaseIds.followerApply(vlog); break;
                case 6: _nextPhaseId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 7: _subPhases.followerApply(vlog); break;
                case 8: _currentSubPhaseId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setPhaseId(rs.getLong(_parents_name_ + "phaseId"));
        setPhaseName(rs.getString(_parents_name_ + "phaseName"));
        if (getPhaseName() == null)
            setPhaseName("");
        setPhaseDescription(rs.getString(_parents_name_ + "phaseDescription"));
        if (getPhaseDescription() == null)
            setPhaseDescription("");
        Zeze.Serialize.Helper.decodeJsonList(_prePhaseIds, Long.class, rs.getString(_parents_name_ + "prePhaseIds"));
        setNextPhaseId(rs.getLong(_parents_name_ + "nextPhaseId"));
        Zeze.Serialize.Helper.decodeJsonMap(this, "subPhases", _subPhases, rs.getString(_parents_name_ + "subPhases"));
        setCurrentSubPhaseId(rs.getLong(_parents_name_ + "currentSubPhaseId"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendLong(_parents_name_ + "phaseId", getPhaseId());
        st.appendString(_parents_name_ + "phaseName", getPhaseName());
        st.appendString(_parents_name_ + "phaseDescription", getPhaseDescription());
        st.appendString(_parents_name_ + "prePhaseIds", Zeze.Serialize.Helper.encodeJson(_prePhaseIds));
        st.appendLong(_parents_name_ + "nextPhaseId", getNextPhaseId());
        st.appendString(_parents_name_ + "subPhases", Zeze.Serialize.Helper.encodeJson(_subPhases));
        st.appendLong(_parents_name_ + "currentSubPhaseId", getCurrentSubPhaseId());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "phaseId", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "phaseName", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "phaseDescription", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "prePhaseIds", "list", "", "long"));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(6, "nextPhaseId", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(7, "subPhases", "map", "long", "BSubPhase"));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(8, "currentSubPhaseId", "long", "", ""));
        return vars;
    }
}
