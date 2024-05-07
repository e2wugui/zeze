// auto-generated @formatter:off
package Zeze.Builtin.LogService;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BBrowse extends Zeze.Transaction.Bean implements BBrowseReadOnly {
    public static final long TYPEID = -5609078144289042953L;

    private long _Id;
    private int _Limit;
    private float _OffsetFactor;
    private boolean _Reset;
    private final Zeze.Transaction.Collections.CollOne<Zeze.Builtin.LogService.BCondition> _Condition;

    @Override
    public long getId() {
        if (!isManaged())
            return _Id;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Id;
        var log = (Log__Id)txn.getLog(objectId() + 1);
        return log != null ? log.value : _Id;
    }

    public void setId(long value) {
        if (!isManaged()) {
            _Id = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Id(this, 1, value));
    }

    @Override
    public int getLimit() {
        if (!isManaged())
            return _Limit;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Limit;
        var log = (Log__Limit)txn.getLog(objectId() + 2);
        return log != null ? log.value : _Limit;
    }

    public void setLimit(int value) {
        if (!isManaged()) {
            _Limit = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Limit(this, 2, value));
    }

    @Override
    public float getOffsetFactor() {
        if (!isManaged())
            return _OffsetFactor;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _OffsetFactor;
        var log = (Log__OffsetFactor)txn.getLog(objectId() + 3);
        return log != null ? log.value : _OffsetFactor;
    }

    public void setOffsetFactor(float value) {
        if (!isManaged()) {
            _OffsetFactor = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__OffsetFactor(this, 3, value));
    }

    @Override
    public boolean isReset() {
        if (!isManaged())
            return _Reset;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Reset;
        var log = (Log__Reset)txn.getLog(objectId() + 4);
        return log != null ? log.value : _Reset;
    }

    public void setReset(boolean value) {
        if (!isManaged()) {
            _Reset = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Reset(this, 4, value));
    }

    public Zeze.Builtin.LogService.BCondition getCondition() {
        return _Condition.getValue();
    }

    public void setCondition(Zeze.Builtin.LogService.BCondition value) {
        _Condition.setValue(value);
    }

    @Override
    public Zeze.Builtin.LogService.BConditionReadOnly getConditionReadOnly() {
        return _Condition.getValue();
    }

    @SuppressWarnings("deprecation")
    public BBrowse() {
        _Condition = new Zeze.Transaction.Collections.CollOne<>(new Zeze.Builtin.LogService.BCondition(), Zeze.Builtin.LogService.BCondition.class);
        _Condition.variableId(5);
    }

    @SuppressWarnings("deprecation")
    public BBrowse(long _Id_, int _Limit_, float _OffsetFactor_, boolean _Reset_) {
        _Id = _Id_;
        _Limit = _Limit_;
        _OffsetFactor = _OffsetFactor_;
        _Reset = _Reset_;
        _Condition = new Zeze.Transaction.Collections.CollOne<>(new Zeze.Builtin.LogService.BCondition(), Zeze.Builtin.LogService.BCondition.class);
        _Condition.variableId(5);
    }

    @Override
    public void reset() {
        setId(0);
        setLimit(0);
        setOffsetFactor(0);
        setReset(false);
        _Condition.reset();
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.LogService.BBrowse.Data toData() {
        var data = new Zeze.Builtin.LogService.BBrowse.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.LogService.BBrowse.Data)other);
    }

    public void assign(BBrowse.Data other) {
        setId(other._Id);
        setLimit(other._Limit);
        setOffsetFactor(other._OffsetFactor);
        setReset(other._Reset);
        Zeze.Builtin.LogService.BCondition data_Condition = new Zeze.Builtin.LogService.BCondition();
        data_Condition.assign(other._Condition);
        _Condition.setValue(data_Condition);
        _unknown_ = null;
    }

    public void assign(BBrowse other) {
        setId(other.getId());
        setLimit(other.getLimit());
        setOffsetFactor(other.getOffsetFactor());
        setReset(other.isReset());
        _Condition.assign(other._Condition);
        _unknown_ = other._unknown_;
    }

    public BBrowse copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BBrowse copy() {
        var copy = new BBrowse();
        copy.assign(this);
        return copy;
    }

    public static void swap(BBrowse a, BBrowse b) {
        BBrowse save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Id extends Zeze.Transaction.Logs.LogLong {
        public Log__Id(BBrowse bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BBrowse)getBelong())._Id = value; }
    }

    private static final class Log__Limit extends Zeze.Transaction.Logs.LogInt {
        public Log__Limit(BBrowse bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BBrowse)getBelong())._Limit = value; }
    }

    private static final class Log__OffsetFactor extends Zeze.Transaction.Logs.LogFloat {
        public Log__OffsetFactor(BBrowse bean, int varId, float value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BBrowse)getBelong())._OffsetFactor = value; }
    }

    private static final class Log__Reset extends Zeze.Transaction.Logs.LogBool {
        public Log__Reset(BBrowse bean, int varId, boolean value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BBrowse)getBelong())._Reset = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.LogService.BBrowse: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Id=").append(getId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Limit=").append(getLimit()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("OffsetFactor=").append(getOffsetFactor()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Reset=").append(isReset()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Condition=").append(System.lineSeparator());
        _Condition.buildString(sb, level + 4);
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
        {
            long _x_ = getId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            int _x_ = getLimit();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            float _x_ = getOffsetFactor();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.FLOAT);
                _o_.WriteFloat(_x_);
            }
        }
        {
            boolean _x_ = isReset();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 5, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _Condition.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        _o_.writeAllUnknownFields(_i_, _ui_, _u_);
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        ByteBuffer _u_ = null;
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setLimit(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setOffsetFactor(_o_.ReadFloat(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setReset(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            _o_.ReadBean(_Condition, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Condition.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _Condition.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getId() < 0)
            return true;
        if (getLimit() < 0)
            return true;
        if (_Condition.negativeCheck())
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
                case 1: _Id = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 2: _Limit = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 3: _OffsetFactor = ((Zeze.Transaction.Logs.LogFloat)vlog).value; break;
                case 4: _Reset = ((Zeze.Transaction.Logs.LogBool)vlog).value; break;
                case 5: _Condition.followerApply(vlog); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setId(rs.getLong(_parents_name_ + "Id"));
        setLimit(rs.getInt(_parents_name_ + "Limit"));
        setOffsetFactor(rs.getFloat(_parents_name_ + "OffsetFactor"));
        setReset(rs.getBoolean(_parents_name_ + "Reset"));
        parents.add("Condition");
        _Condition.decodeResultSet(parents, rs);
        parents.remove(parents.size() - 1);
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendLong(_parents_name_ + "Id", getId());
        st.appendInt(_parents_name_ + "Limit", getLimit());
        st.appendFloat(_parents_name_ + "OffsetFactor", getOffsetFactor());
        st.appendBoolean(_parents_name_ + "Reset", isReset());
        parents.add("Condition");
        _Condition.encodeSQLStatement(parents, st);
        parents.remove(parents.size() - 1);
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Id", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Limit", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "OffsetFactor", "float", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "Reset", "bool", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "Condition", "Zeze.Builtin.LogService.BCondition", "", ""));
        return vars;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -5609078144289042953L;

    private long _Id;
    private int _Limit;
    private float _OffsetFactor;
    private boolean _Reset;
    private Zeze.Builtin.LogService.BCondition.Data _Condition;

    public long getId() {
        return _Id;
    }

    public void setId(long value) {
        _Id = value;
    }

    public int getLimit() {
        return _Limit;
    }

    public void setLimit(int value) {
        _Limit = value;
    }

    public float getOffsetFactor() {
        return _OffsetFactor;
    }

    public void setOffsetFactor(float value) {
        _OffsetFactor = value;
    }

    public boolean isReset() {
        return _Reset;
    }

    public void setReset(boolean value) {
        _Reset = value;
    }

    public Zeze.Builtin.LogService.BCondition.Data getCondition() {
        return _Condition;
    }

    public void setCondition(Zeze.Builtin.LogService.BCondition.Data value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Condition = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Condition = new Zeze.Builtin.LogService.BCondition.Data();
    }

    @SuppressWarnings("deprecation")
    public Data(long _Id_, int _Limit_, float _OffsetFactor_, boolean _Reset_, Zeze.Builtin.LogService.BCondition.Data _Condition_) {
        _Id = _Id_;
        _Limit = _Limit_;
        _OffsetFactor = _OffsetFactor_;
        _Reset = _Reset_;
        if (_Condition_ == null)
            _Condition_ = new Zeze.Builtin.LogService.BCondition.Data();
        _Condition = _Condition_;
    }

    @Override
    public void reset() {
        _Id = 0;
        _Limit = 0;
        _OffsetFactor = 0;
        _Reset = false;
        _Condition.reset();
    }

    @Override
    public Zeze.Builtin.LogService.BBrowse toBean() {
        var bean = new Zeze.Builtin.LogService.BBrowse();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BBrowse)other);
    }

    public void assign(BBrowse other) {
        _Id = other.getId();
        _Limit = other.getLimit();
        _OffsetFactor = other.getOffsetFactor();
        _Reset = other.isReset();
        _Condition.assign(other._Condition.getValue());
    }

    public void assign(BBrowse.Data other) {
        _Id = other._Id;
        _Limit = other._Limit;
        _OffsetFactor = other._OffsetFactor;
        _Reset = other._Reset;
        _Condition.assign(other._Condition);
    }

    @Override
    public BBrowse.Data copy() {
        var copy = new BBrowse.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BBrowse.Data a, BBrowse.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BBrowse.Data clone() {
        return (BBrowse.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.LogService.BBrowse: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Id=").append(_Id).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Limit=").append(_Limit).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("OffsetFactor=").append(_OffsetFactor).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Reset=").append(_Reset).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Condition=").append(System.lineSeparator());
        _Condition.buildString(sb, level + 4);
        sb.append(System.lineSeparator());
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append('}');
    }

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
            long _x_ = _Id;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            int _x_ = _Limit;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            float _x_ = _OffsetFactor;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.FLOAT);
                _o_.WriteFloat(_x_);
            }
        }
        {
            boolean _x_ = _Reset;
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 5, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _Condition.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _Id = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _Limit = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _OffsetFactor = _o_.ReadFloat(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _Reset = _o_.ReadBool(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            _o_.ReadBean(_Condition, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
