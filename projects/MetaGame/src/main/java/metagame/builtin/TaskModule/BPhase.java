// auto-generated @formatter:off
package metagame.builtin.TaskModule;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BPhase extends Zeze.Transaction.Bean implements BPhaseReadOnly {
    public static final long TYPEID = -5963783578031677021L;

    private final Zeze.Transaction.Collections.PList2<metagame.builtin.TaskModule.BCondition> _Conditions;
    private final Zeze.Transaction.Collections.PSet1<Integer> _IndexSet;
    private String _Description;

    private static final java.lang.invoke.VarHandle vh_Description;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_Description = _l_.findVarHandle(BPhase.class, "_Description", String.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    public Zeze.Transaction.Collections.PList2<metagame.builtin.TaskModule.BCondition> getConditions() {
        return _Conditions;
    }

    @Override
    public Zeze.Transaction.Collections.PList2ReadOnly<metagame.builtin.TaskModule.BCondition, metagame.builtin.TaskModule.BConditionReadOnly> getConditionsReadOnly() {
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
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Description;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 3);
        return log != null ? log.stringValue() : _Description;
    }

    public void setDescription(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Description = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 3, vh_Description, _v_));
    }

    @SuppressWarnings("deprecation")
    public BPhase() {
        _Conditions = new Zeze.Transaction.Collections.PList2<>(metagame.builtin.TaskModule.BCondition.class);
        _Conditions.variableId(1);
        _IndexSet = new Zeze.Transaction.Collections.PSet1<>(Integer.class);
        _IndexSet.variableId(2);
        _Description = "";
    }

    @SuppressWarnings("deprecation")
    public BPhase(String _Description_) {
        _Conditions = new Zeze.Transaction.Collections.PList2<>(metagame.builtin.TaskModule.BCondition.class);
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
    public metagame.builtin.TaskModule.BPhase.Data toData() {
        var _d_ = new metagame.builtin.TaskModule.BPhase.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((metagame.builtin.TaskModule.BPhase.Data)_o_);
    }

    public void assign(BPhase.Data _o_) {
        _Conditions.clear();
        for (var _e_ : _o_._Conditions) {
            var _v_ = new metagame.builtin.TaskModule.BCondition();
            _v_.assign(_e_);
            _Conditions.add(_v_);
        }
        _IndexSet.clear();
        _IndexSet.addAll(_o_._IndexSet);
        setDescription(_o_._Description);
        _unknown_ = null;
    }

    public void assign(BPhase _o_) {
        _Conditions.clear();
        for (var _e_ : _o_._Conditions)
            _Conditions.add(_e_.copy());
        _IndexSet.assign(_o_._IndexSet);
        setDescription(_o_.getDescription());
        _unknown_ = _o_._unknown_;
    }

    public BPhase copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BPhase copy() {
        var _c_ = new BPhase();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BPhase _a_, BPhase _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        var _i1_ = Zeze.Util.Str.indent(_l_ + 4);
        var _i2_ = Zeze.Util.Str.indent(_l_ + 8);
        _s_.append("metagame.builtin.TaskModule.BPhase: {\n");
        _s_.append(_i1_).append("Conditions=[");
        if (!_Conditions.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _Conditions) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_Conditions.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=");
                _v_.buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("],\n");
        _s_.append(_i1_).append("IndexSet={");
        if (!_IndexSet.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _IndexSet) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_IndexSet.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("Description=").append(getDescription()).append('\n');
        _s_.append(Zeze.Util.Str.indent(_l_)).append('}');
    }

    private static int _PRE_ALLOC_SIZE_ = 16;

    @Override
    public int preAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void preAllocSize(int _s_) {
        _PRE_ALLOC_SIZE_ = _s_;
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
                    _x_.add(_o_.ReadBean(new metagame.builtin.TaskModule.BCondition(), _t_));
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
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BPhase))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BPhase)_o_;
        if (!_Conditions.equals(_b_._Conditions))
            return false;
        if (!_IndexSet.equals(_b_._IndexSet))
            return false;
        if (!getDescription().equals(_b_.getDescription()))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _Conditions.initRootInfo(_r_, this);
        _IndexSet.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _Conditions.initRootInfoWithRedo(_r_, this);
        _IndexSet.initRootInfoWithRedo(_r_, this);
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
    public void followerApply(Zeze.Transaction.Log _l_) {
        var _vs_ = ((Zeze.Transaction.Collections.LogBean)_l_).getVariables();
        if (_vs_ == null)
            return;
        for (var _i_ = _vs_.iterator(); _i_.moveToNext(); ) {
            var _v_ = _i_.value();
            switch (_v_.getVariableId()) {
                case 1: _Conditions.followerApply(_v_); break;
                case 2: _IndexSet.followerApply(_v_); break;
                case 3: _Description = _v_.stringValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        Zeze.Serialize.Helper.decodeJsonList(_Conditions, metagame.builtin.TaskModule.BCondition.class, _r_.getString(_pn_ + "Conditions"));
        Zeze.Serialize.Helper.decodeJsonSet(_IndexSet, Integer.class, _r_.getString(_pn_ + "IndexSet"));
        setDescription(_r_.getString(_pn_ + "Description"));
        if (getDescription() == null)
            setDescription("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "Conditions", Zeze.Serialize.Helper.encodeJson(_Conditions));
        _s_.appendString(_pn_ + "IndexSet", Zeze.Serialize.Helper.encodeJson(_IndexSet));
        _s_.appendString(_pn_ + "Description", getDescription());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Conditions", "list", "", "metagame.builtin.TaskModule.BCondition"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "IndexSet", "set", "", "int"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Description", "string", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -5963783578031677021L;

    private java.util.ArrayList<metagame.builtin.TaskModule.BCondition.Data> _Conditions;
    private java.util.HashSet<Integer> _IndexSet;
    private String _Description;

    public java.util.ArrayList<metagame.builtin.TaskModule.BCondition.Data> getConditions() {
        return _Conditions;
    }

    public void setConditions(java.util.ArrayList<metagame.builtin.TaskModule.BCondition.Data> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Conditions = _v_;
    }

    public java.util.HashSet<Integer> getIndexSet() {
        return _IndexSet;
    }

    public void setIndexSet(java.util.HashSet<Integer> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _IndexSet = _v_;
    }

    public String getDescription() {
        return _Description;
    }

    public void setDescription(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Description = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Conditions = new java.util.ArrayList<>();
        _IndexSet = new java.util.HashSet<>();
        _Description = "";
    }

    @SuppressWarnings("deprecation")
    public Data(java.util.ArrayList<metagame.builtin.TaskModule.BCondition.Data> _Conditions_, java.util.HashSet<Integer> _IndexSet_, String _Description_) {
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
    public metagame.builtin.TaskModule.BPhase toBean() {
        var _b_ = new metagame.builtin.TaskModule.BPhase();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BPhase)_o_);
    }

    public void assign(BPhase _o_) {
        _Conditions.clear();
        for (var _e_ : _o_._Conditions) {
            var _v_ = new metagame.builtin.TaskModule.BCondition.Data();
            _v_.assign(_e_);
            _Conditions.add(_v_);
        }
        _IndexSet.clear();
        _IndexSet.addAll(_o_._IndexSet);
        _Description = _o_.getDescription();
    }

    public void assign(BPhase.Data _o_) {
        _Conditions.clear();
        for (var _e_ : _o_._Conditions)
            _Conditions.add(_e_.copy());
        _IndexSet.clear();
        _IndexSet.addAll(_o_._IndexSet);
        _Description = _o_._Description;
    }

    @Override
    public BPhase.Data copy() {
        var _c_ = new BPhase.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BPhase.Data _a_, BPhase.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
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
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        var _i1_ = Zeze.Util.Str.indent(_l_ + 4);
        var _i2_ = Zeze.Util.Str.indent(_l_ + 8);
        _s_.append("metagame.builtin.TaskModule.BPhase: {\n");
        _s_.append(_i1_).append("Conditions=[");
        if (!_Conditions.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _Conditions) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_Conditions.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=");
                _v_.buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("],\n");
        _s_.append(_i1_).append("IndexSet={");
        if (!_IndexSet.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _IndexSet) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_IndexSet.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("Description=").append(_Description).append('\n');
        _s_.append(Zeze.Util.Str.indent(_l_)).append('}');
    }

    @Override
    public int preAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void preAllocSize(int _s_) {
        _PRE_ALLOC_SIZE_ = _s_;
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
                    _x_.add(_o_.ReadBean(new metagame.builtin.TaskModule.BCondition.Data(), _t_));
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

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BPhase.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BPhase.Data)_o_;
        if (!_Conditions.equals(_b_._Conditions))
            return false;
        if (!_IndexSet.equals(_b_._IndexSet))
            return false;
        if (!_Description.equals(_b_._Description))
            return false;
        return true;
    }
}
}
