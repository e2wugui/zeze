// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskModule;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BPhase extends Zeze.Transaction.Bean implements BPhaseReadOnly {
    public static final long TYPEID = -4991109539831580788L;

    private final Zeze.Transaction.Collections.PList2<Zeze.Builtin.Game.TaskModule.BCondition> _Conditions;
    private final Zeze.Transaction.Collections.PSet1<Integer> _IndexSet;
    private String _Description;

    public Zeze.Transaction.Collections.PList2<Zeze.Builtin.Game.TaskModule.BCondition> getConditions() {
        return _Conditions;
    }

    @Override
    public Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.Game.TaskModule.BCondition, Zeze.Builtin.Game.TaskModule.BConditionReadOnly> getConditionsReadOnly() {
        return new Zeze.Transaction.Collections.PList2ReadOnly<>(_Conditions);
    }

    public Zeze.Transaction.Collections.PSet1<Integer> getIndexSet() {
        return _IndexSet;
    }

    @Override
    public Zeze.Transaction.Collections.PSet1ReadOnly<Integer> getIndexSetReadOnly() {
        return new Zeze.Transaction.Collections.PSet1ReadOnly<>(_IndexSet);
    }

    @Override
    public String getDescription() {
        if (!isManaged())
            return _Description;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Description;
        var log = (Log__Description)txn.getLog(objectId() + 3);
        return log != null ? log.value : _Description;
    }

    public void setDescription(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Description = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Description(this, 3, value));
    }

    @SuppressWarnings("deprecation")
    public BPhase() {
        _Conditions = new Zeze.Transaction.Collections.PList2<>(Zeze.Builtin.Game.TaskModule.BCondition.class);
        _Conditions.variableId(1);
        _IndexSet = new Zeze.Transaction.Collections.PSet1<>(Integer.class);
        _IndexSet.variableId(2);
        _Description = "";
    }

    @SuppressWarnings("deprecation")
    public BPhase(String _Description_) {
        _Conditions = new Zeze.Transaction.Collections.PList2<>(Zeze.Builtin.Game.TaskModule.BCondition.class);
        _Conditions.variableId(1);
        _IndexSet = new Zeze.Transaction.Collections.PSet1<>(Integer.class);
        _IndexSet.variableId(2);
        if (_Description_ == null)
            _Description_ = "";
        _Description = _Description_;
    }

    @Override
    public void reset() {
        _Conditions.clear();
        _IndexSet.clear();
        setDescription("");
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Game.TaskModule.BPhase.Data toData() {
        var data = new Zeze.Builtin.Game.TaskModule.BPhase.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Game.TaskModule.BPhase.Data)other);
    }

    public void assign(BPhase.Data other) {
        _Conditions.clear();
        for (var e : other._Conditions) {
            Zeze.Builtin.Game.TaskModule.BCondition data = new Zeze.Builtin.Game.TaskModule.BCondition();
            data.assign(e);
            _Conditions.add(data);
        }
        _IndexSet.clear();
        _IndexSet.addAll(other._IndexSet);
        setDescription(other._Description);
        _unknown_ = null;
    }

    public void assign(BPhase other) {
        _Conditions.clear();
        for (var e : other._Conditions)
            _Conditions.add(e.copy());
        _IndexSet.clear();
        _IndexSet.addAll(other._IndexSet);
        setDescription(other.getDescription());
        _unknown_ = other._unknown_;
    }

    public BPhase copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BPhase copy() {
        var copy = new BPhase();
        copy.assign(this);
        return copy;
    }

    public static void swap(BPhase a, BPhase b) {
        BPhase save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Description extends Zeze.Transaction.Logs.LogString {
        public Log__Description(BPhase bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BPhase)getBelong())._Description = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.TaskModule.BPhase: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Conditions=[");
        if (!_Conditions.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _Conditions) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(System.lineSeparator());
                _item_.buildString(sb, level + 4);
                sb.append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("IndexSet={");
        if (!_IndexSet.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _IndexSet) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(_item_).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append('}').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Description=").append(getDescription()).append(System.lineSeparator());
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
            var _x_ = _Conditions;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BEAN);
                for (var _v_ : _x_) {
                    _v_.encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _IndexSet;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
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
            String _x_ = getDescription();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
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
        if (_i_ == 1) {
            var _x_ = _Conditions;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new Zeze.Builtin.Game.TaskModule.BCondition(), _t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = _IndexSet;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadInt(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setDescription(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Conditions.initRootInfo(root, this);
        _IndexSet.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _Conditions.initRootInfoWithRedo(root, this);
        _IndexSet.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
        for (var _v_ : _IndexSet) {
            if (_v_ < 0)
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
                case 1: _Conditions.followerApply(vlog); break;
                case 2: _IndexSet.followerApply(vlog); break;
                case 3: _Description = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        Zeze.Serialize.Helper.decodeJsonList(_Conditions, Zeze.Builtin.Game.TaskModule.BCondition.class, rs.getString(_parents_name_ + "Conditions"));
        Zeze.Serialize.Helper.decodeJsonSet(_IndexSet, Integer.class, rs.getString(_parents_name_ + "IndexSet"));
        setDescription(rs.getString(_parents_name_ + "Description"));
        if (getDescription() == null)
            setDescription("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "Conditions", Zeze.Serialize.Helper.encodeJson(_Conditions));
        st.appendString(_parents_name_ + "IndexSet", Zeze.Serialize.Helper.encodeJson(_IndexSet));
        st.appendString(_parents_name_ + "Description", getDescription());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Conditions", "list", "", "BCondition"));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "IndexSet", "set", "", "int"));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Description", "string", "", ""));
        return vars;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -4991109539831580788L;

    private java.util.ArrayList<Zeze.Builtin.Game.TaskModule.BCondition.Data> _Conditions;
    private java.util.HashSet<Integer> _IndexSet;
    private String _Description;

    public java.util.ArrayList<Zeze.Builtin.Game.TaskModule.BCondition.Data> getConditions() {
        return _Conditions;
    }

    public void setConditions(java.util.ArrayList<Zeze.Builtin.Game.TaskModule.BCondition.Data> value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Conditions = value;
    }

    public java.util.HashSet<Integer> getIndexSet() {
        return _IndexSet;
    }

    public void setIndexSet(java.util.HashSet<Integer> value) {
        if (value == null)
            throw new IllegalArgumentException();
        _IndexSet = value;
    }

    public String getDescription() {
        return _Description;
    }

    public void setDescription(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Description = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Conditions = new java.util.ArrayList<>();
        _IndexSet = new java.util.HashSet<>();
        _Description = "";
    }

    @SuppressWarnings("deprecation")
    public Data(java.util.ArrayList<Zeze.Builtin.Game.TaskModule.BCondition.Data> _Conditions_, java.util.HashSet<Integer> _IndexSet_, String _Description_) {
        if (_Conditions_ == null)
            _Conditions_ = new java.util.ArrayList<>();
        _Conditions = _Conditions_;
        if (_IndexSet_ == null)
            _IndexSet_ = new java.util.HashSet<>();
        _IndexSet = _IndexSet_;
        if (_Description_ == null)
            _Description_ = "";
        _Description = _Description_;
    }

    @Override
    public void reset() {
        _Conditions.clear();
        _IndexSet.clear();
        _Description = "";
    }

    @Override
    public Zeze.Builtin.Game.TaskModule.BPhase toBean() {
        var bean = new Zeze.Builtin.Game.TaskModule.BPhase();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BPhase)other);
    }

    public void assign(BPhase other) {
        _Conditions.clear();
        for (var e : other._Conditions) {
            Zeze.Builtin.Game.TaskModule.BCondition.Data data = new Zeze.Builtin.Game.TaskModule.BCondition.Data();
            data.assign(e);
            _Conditions.add(data);
        }
        _IndexSet.clear();
        _IndexSet.addAll(other._IndexSet);
        _Description = other.getDescription();
    }

    public void assign(BPhase.Data other) {
        _Conditions.clear();
        for (var e : other._Conditions)
            _Conditions.add(e.copy());
        _IndexSet.clear();
        _IndexSet.addAll(other._IndexSet);
        _Description = other._Description;
    }

    @Override
    public BPhase.Data copy() {
        var copy = new BPhase.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BPhase.Data a, BPhase.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BPhase.Data clone() {
        return (BPhase.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.TaskModule.BPhase: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Conditions=[");
        if (!_Conditions.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _Conditions) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(System.lineSeparator());
                _item_.buildString(sb, level + 4);
                sb.append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("IndexSet={");
        if (!_IndexSet.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _IndexSet) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(_item_).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append('}').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Description=").append(_Description).append(System.lineSeparator());
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
            var _x_ = _Conditions;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BEAN);
                for (int _j_ = 0, _c_ = _x_.size(); _j_ < _c_; _j_++) {
                    var _v_ = _x_.get(_j_);
                    _v_.encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _IndexSet;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
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
            String _x_ = _Description;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            var _x_ = _Conditions;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new Zeze.Builtin.Game.TaskModule.BCondition.Data(), _t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = _IndexSet;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadInt(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _Description = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
