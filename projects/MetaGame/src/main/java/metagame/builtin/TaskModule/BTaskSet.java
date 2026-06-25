// auto-generated @formatter:off
package metagame.builtin.TaskModule;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BTaskSet extends Zeze.Transaction.Bean implements BTaskSetReadOnly {
    public static final long TYPEID = -6036686140257775455L;

    private final Zeze.Transaction.Collections.PSet1<Integer> _TaskIds;

    public Zeze.Transaction.Collections.PSet1<Integer> getTaskIds() {
        return _TaskIds;
    }

    @Override
    public Zeze.Transaction.Collections.PSet1ReadOnly<Integer> getTaskIdsReadOnly() {
        return new Zeze.Transaction.Collections.PSet1ReadOnly<>(_TaskIds);
    }

    @SuppressWarnings("deprecation")
    public BTaskSet() {
        _TaskIds = new Zeze.Transaction.Collections.PSet1<>(Integer.class);
        _TaskIds.variableId(1);
    }

    @Override
    public void reset() {
        _TaskIds.clear();
        _unknown_ = null;
    }

    @Override
    public metagame.builtin.TaskModule.BTaskSet.Data toData() {
        var _d_ = new metagame.builtin.TaskModule.BTaskSet.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((metagame.builtin.TaskModule.BTaskSet.Data)_o_);
    }

    public void assign(BTaskSet.Data _o_) {
        _TaskIds.clear();
        _TaskIds.addAll(_o_._TaskIds);
        _unknown_ = null;
    }

    public void assign(BTaskSet _o_) {
        _TaskIds.assign(_o_._TaskIds);
        _unknown_ = _o_._unknown_;
    }

    public BTaskSet copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTaskSet copy() {
        var _c_ = new BTaskSet();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BTaskSet _a_, BTaskSet _b_) {
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
        _s_.append("metagame.builtin.TaskModule.BTaskSet: {\n");
        _s_.append(_i1_).append("TaskIds={");
        if (!_TaskIds.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _TaskIds) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_TaskIds.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("}\n");
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
            var _x_ = _TaskIds;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                for (var _v_ : _x_) {
                    _o_.WriteLong(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
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
            var _x_ = _TaskIds;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadInt(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BTaskSet))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BTaskSet)_o_;
        if (!_TaskIds.equals(_b_._TaskIds))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _TaskIds.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _TaskIds.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        for (var _v_ : _TaskIds) {
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
                case 1: _TaskIds.followerApply(_v_); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        Zeze.Serialize.Helper.decodeJsonSet(_TaskIds, Integer.class, _r_.getString(_pn_ + "TaskIds"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "TaskIds", Zeze.Serialize.Helper.encodeJson(_TaskIds));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "TaskIds", "set", "", "int"));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -6036686140257775455L;

    private java.util.HashSet<Integer> _TaskIds;

    public java.util.HashSet<Integer> getTaskIds() {
        return _TaskIds;
    }

    public void setTaskIds(java.util.HashSet<Integer> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _TaskIds = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _TaskIds = new java.util.HashSet<>();
    }

    @SuppressWarnings("deprecation")
    public Data(java.util.HashSet<Integer> _TaskIds_) {
        if (_TaskIds_ == null)
            _TaskIds_ = new java.util.HashSet<>();
        _TaskIds = _TaskIds_;
    }

    @Override
    public void reset() {
        _TaskIds.clear();
    }

    @Override
    public metagame.builtin.TaskModule.BTaskSet toBean() {
        var _b_ = new metagame.builtin.TaskModule.BTaskSet();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BTaskSet)_o_);
    }

    public void assign(BTaskSet _o_) {
        _TaskIds.clear();
        _TaskIds.addAll(_o_._TaskIds);
    }

    public void assign(BTaskSet.Data _o_) {
        _TaskIds.clear();
        _TaskIds.addAll(_o_._TaskIds);
    }

    @Override
    public BTaskSet.Data copy() {
        var _c_ = new BTaskSet.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BTaskSet.Data _a_, BTaskSet.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BTaskSet.Data clone() {
        return (BTaskSet.Data)super.clone();
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
        _s_.append("metagame.builtin.TaskModule.BTaskSet: {\n");
        _s_.append(_i1_).append("TaskIds={");
        if (!_TaskIds.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _TaskIds) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_TaskIds.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("}\n");
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
            var _x_ = _TaskIds;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                for (var _v_ : _x_) {
                    _o_.WriteLong(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            var _x_ = _TaskIds;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadInt(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
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
        if (!(_o_ instanceof BTaskSet.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BTaskSet.Data)_o_;
        if (!_TaskIds.equals(_b_._TaskIds))
            return false;
        return true;
    }
}
}
