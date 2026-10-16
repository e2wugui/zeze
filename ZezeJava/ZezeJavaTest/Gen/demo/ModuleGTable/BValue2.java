// auto-generated @formatter:off
package demo.ModuleGTable;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"EqualsAndHashcode", "ConstantConditions", "ConstantValue", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BValue2 extends Zeze.Transaction.Bean implements BValue2ReadOnly {
    public static final long TYPEID = 4226048367927192742L;

    private final Zeze.Transaction.GTable.GTable2<Integer, Integer, demo.ModuleGTable.Bean1, demo.ModuleGTable.Bean1ReadOnly> _gTable;
    private static final Zeze.Transaction.GTable.GTable2.Factory<Integer, Integer, demo.ModuleGTable.Bean1, demo.ModuleGTable.Bean1ReadOnly> factory_gTable
            = Zeze.Transaction.GTable.GTable2.getFactory(Integer.class, Integer.class, demo.ModuleGTable.Bean1.class);

    public Zeze.Transaction.GTable.GTable2<Integer, Integer, demo.ModuleGTable.Bean1, demo.ModuleGTable.Bean1ReadOnly> getGTable() {
        return _gTable;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<Integer, Zeze.Transaction.GTable.BeanMap2<Integer, demo.ModuleGTable.Bean1, demo.ModuleGTable.Bean1ReadOnly>, Zeze.Transaction.GTable.BeanMap2ReadOnly<Integer, demo.ModuleGTable.Bean1, demo.ModuleGTable.Bean1ReadOnly>> getGTableReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_gTable.getPMap2());
    }

    @SuppressWarnings("deprecation")
    public BValue2() {
        _gTable = new Zeze.Transaction.GTable.GTable2<>(factory_gTable);
        _gTable.variableId(1);
    }

    @Override
    public void reset() {
        _gTable.clear();
        _unknown_ = null;
    }

    public void assign(BValue2 _o_) {
        _gTable.assign(_o_._gTable);
        _unknown_ = _o_._unknown_;
    }

    public BValue2 copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BValue2 copy() {
        var _c_ = new BValue2();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BValue2 _a_, BValue2 _b_) {
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
        _s_.append("demo.ModuleGTable.BValue2: {\n");
        _s_.append(_i1_).append("gTable={");
        if (!_gTable.isEmpty()) {
            _s_.append('\n');
            _gTable.buildString(_s_, _l_ + 4);
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

    private byte @Nullable [] _unknown_;

    public byte @Nullable [] unknown() {
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
            var _x_ = _gTable;
            int _n_ = _x_.getPMap2().size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.BEAN);
                for (var _e_ : _x_.getPMap2().entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _e_.getValue().encode(_o_);
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
            var _x_ = _gTable;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUIntPositive(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadInt(_s_);
                    var _v_ = _o_.ReadBean(_gTable.getPMap2().createValue(), _t_);
                    _x_.getPMap2().put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(@Nullable Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BValue2))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BValue2)_o_;
        if (!_gTable.equals(_b_._gTable))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _gTable.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _gTable.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        for (var _v_ : _gTable.getPMap2().values()) {
            if (_v_.negativeCheck()) return true;
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
                case 1: _gTable.getPMap2().followerApply(_v_); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        Zeze.Serialize.Helper.decodeJsonGTable2(this, "gTable", _gTable, _r_.getString(_pn_ + "gTable"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "gTable", Zeze.Serialize.Helper.encodeJson(_gTable));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "gTable", "gtable", "int,int", "demo.ModuleGTable.Bean1"));
        return _v_;
    }
}
