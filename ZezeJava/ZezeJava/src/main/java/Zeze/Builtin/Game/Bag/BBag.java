// auto-generated @formatter:off
package Zeze.Builtin.Game.Bag;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BBag extends Zeze.Transaction.Bean implements BBagReadOnly {
    public static final long TYPEID = -5051317137860806350L;

    private int _Capacity;
    private final Zeze.Transaction.Collections.PMap2<Integer, Zeze.Builtin.Game.Bag.BItem> _Items; // key is bag position

    private static final java.lang.invoke.VarHandle vh_Capacity;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_Capacity = _l_.findVarHandle(BBag.class, "_Capacity", int.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public int getCapacity() {
        if (!isManaged())
            return _Capacity;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Capacity;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _Capacity;
    }

    public void setCapacity(int _v_) {
        if (!isManaged()) {
            _Capacity = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 1, vh_Capacity, _v_));
    }

    public Zeze.Transaction.Collections.PMap2<Integer, Zeze.Builtin.Game.Bag.BItem> getItems() {
        return _Items;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<Integer, Zeze.Builtin.Game.Bag.BItem, Zeze.Builtin.Game.Bag.BItemReadOnly> getItemsReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_Items);
    }

    @SuppressWarnings("deprecation")
    public BBag() {
        _Items = new Zeze.Transaction.Collections.PMap2<>(Integer.class, Zeze.Builtin.Game.Bag.BItem.class);
        _Items.variableId(2);
    }

    @SuppressWarnings("deprecation")
    public BBag(int _Capacity_) {
        _Capacity = _Capacity_;
        _Items = new Zeze.Transaction.Collections.PMap2<>(Integer.class, Zeze.Builtin.Game.Bag.BItem.class);
        _Items.variableId(2);
    }

    @Override
    public void reset() {
        setCapacity(0);
        _Items.clear();
        _unknown_ = null;
    }

    public void assign(BBag _o_) {
        setCapacity(_o_.getCapacity());
        _Items.clear();
        for (var _e_ : _o_._Items.entrySet())
            _Items.put(_e_.getKey(), _e_.getValue().copy());
        _unknown_ = _o_._unknown_;
    }

    public BBag copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BBag copy() {
        var _c_ = new BBag();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BBag _a_, BBag _b_) {
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
        _s_.append("Zeze.Builtin.Game.Bag.BBag: {\n");
        _s_.append(_i1_).append("Capacity=").append(getCapacity()).append(",\n");
        _s_.append(_i1_).append("Items={");
        if (!_Items.isEmpty()) {
            _s_.append('\n');
            for (var _e_ : _Items.entrySet()) {
                _s_.append(_i2_).append("Key=").append(_e_.getKey()).append(",\n");
                _s_.append(_i2_).append("Value=");
                _e_.getValue().buildString(_s_, _l_ + 12);
                _s_.append(",\n");
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
            int _x_ = getCapacity();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = _Items;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.MAP);
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
        _o_.writeAllUnknownFields(_i_, _ui_, _u_);
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        ByteBuffer _u_ = null;
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setCapacity(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = _Items;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadInt(_s_);
                    var _v_ = _o_.ReadBean(new Zeze.Builtin.Game.Bag.BItem(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BBag))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BBag)_o_;
        if (getCapacity() != _b_.getCapacity())
            return false;
        if (!_Items.equals(_b_._Items))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _Items.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _Items.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getCapacity() < 0)
            return true;
        for (var _v_ : _Items.values()) {
            if (_v_.negativeCheck())
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
                case 1: _Capacity = _v_.intValue(); break;
                case 2: _Items.followerApply(_v_); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setCapacity(_r_.getInt(_pn_ + "Capacity"));
        Zeze.Serialize.Helper.decodeJsonMap(this, "Items", _Items, _r_.getString(_pn_ + "Items"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendInt(_pn_ + "Capacity", getCapacity());
        _s_.appendString(_pn_ + "Items", Zeze.Serialize.Helper.encodeJson(_Items));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Capacity", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Items", "map", "int", "Zeze.Builtin.Game.Bag.BItem"));
        return _v_;
    }
}
