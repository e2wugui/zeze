// auto-generated @formatter:off
package demo.Game;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"EqualsAndHashcode", "ConstantConditions", "ConstantValue", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BMail extends Zeze.Transaction.Bean implements BMailReadOnly {
    public static final long TYPEID = -3327136206549511679L;

    private long _From;
    private long _Money;
    private final Zeze.Transaction.Collections.PMap2<Integer, demo.Game.BItem> _Items;
    private static final Zeze.Transaction.Collections.Map2Meta<Integer, demo.Game.BItem> meta2_Items
            = Zeze.Transaction.Collections.Map2Meta.get(Integer.class, demo.Game.BItem.class);

    private boolean _Claimed;

    private static final java.lang.invoke.VarHandle vh_From;
    private static final java.lang.invoke.VarHandle vh_Money;
    private static final java.lang.invoke.VarHandle vh_Claimed;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_From = _l_.findVarHandle(BMail.class, "_From", long.class);
            vh_Money = _l_.findVarHandle(BMail.class, "_Money", long.class);
            vh_Claimed = _l_.findVarHandle(BMail.class, "_Claimed", boolean.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public long getFrom() {
        if (!isManaged())
            return _From;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _From;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _From;
    }

    public void setFrom(long _v_) {
        if (!isManaged()) {
            _From = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 1, vh_From, _v_));
    }

    @Override
    public long getMoney() {
        if (!isManaged())
            return _Money;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Money;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _Money;
    }

    public void setMoney(long _v_) {
        if (!isManaged()) {
            _Money = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 2, vh_Money, _v_));
    }

    public Zeze.Transaction.Collections.PMap2<Integer, demo.Game.BItem> getItems() {
        return _Items;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<Integer, demo.Game.BItem, demo.Game.BItemReadOnly> getItemsReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_Items);
    }

    @Override
    public boolean isClaimed() {
        if (!isManaged())
            return _Claimed;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Claimed;
        var log = (Zeze.Transaction.Logs.LogBool)_t_.getLog(objectId() + 4);
        return log != null ? log.value : _Claimed;
    }

    public void setClaimed(boolean _v_) {
        if (!isManaged()) {
            _Claimed = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBool(this, 4, vh_Claimed, _v_));
    }

    @SuppressWarnings("deprecation")
    public BMail() {
        _Items = new Zeze.Transaction.Collections.PMap2<>(meta2_Items);
        _Items.variableId(3);
    }

    @SuppressWarnings("deprecation")
    public BMail(long _From_, long _Money_, boolean _Claimed_) {
        _From = _From_;
        _Money = _Money_;
        _Items = new Zeze.Transaction.Collections.PMap2<>(meta2_Items);
        _Items.variableId(3);
        _Claimed = _Claimed_;
    }

    @Override
    public void reset() {
        setFrom(0);
        setMoney(0);
        _Items.clear();
        setClaimed(false);
        _unknown_ = null;
    }

    public void assign(BMail _o_) {
        setFrom(_o_.getFrom());
        setMoney(_o_.getMoney());
        _Items.clear();
        for (var _e_ : _o_._Items.entrySet())
            _Items.put(_e_.getKey(), _e_.getValue().copy());
        setClaimed(_o_.isClaimed());
        _unknown_ = _o_._unknown_;
    }

    public BMail copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BMail copy() {
        var _c_ = new BMail();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BMail _a_, BMail _b_) {
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
        _s_.append("demo.Game.BMail: {\n");
        _s_.append(_i1_).append("From=").append(getFrom()).append(",\n");
        _s_.append(_i1_).append("Money=").append(getMoney()).append(",\n");
        _s_.append(_i1_).append("Items={");
        if (!_Items.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _e_ : _Items.entrySet()) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_Items.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Key=").append(_e_.getKey()).append(",\n");
                _s_.append(_i2_).append("Value=");
                _e_.getValue().buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("Claimed=").append(isClaimed()).append('\n');
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
            long _x_ = getFrom();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getMoney();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = _Items;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.MAP);
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
            boolean _x_ = isClaimed();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
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
            setFrom(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setMoney(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            var _x_ = _Items;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUIntPositive(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadInt(_s_);
                    var _v_ = _o_.ReadBean(new demo.Game.BItem(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setClaimed(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(@Nullable Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BMail))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BMail)_o_;
        if (getFrom() != _b_.getFrom())
            return false;
        if (getMoney() != _b_.getMoney())
            return false;
        if (!_Items.equals(_b_._Items))
            return false;
        if (isClaimed() != _b_.isClaimed())
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
        if (getFrom() < 0)
            return true;
        if (getMoney() < 0)
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
                case 1: _From = _v_.longValue(); break;
                case 2: _Money = _v_.longValue(); break;
                case 3: _Items.followerApply(_v_); break;
                case 4: _Claimed = _v_.booleanValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setFrom(_r_.getLong(_pn_ + "From"));
        setMoney(_r_.getLong(_pn_ + "Money"));
        Zeze.Serialize.Helper.decodeJsonMap(this, "Items", _Items, _r_.getString(_pn_ + "Items"));
        setClaimed(_r_.getBoolean(_pn_ + "Claimed"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendLong(_pn_ + "From", getFrom());
        _s_.appendLong(_pn_ + "Money", getMoney());
        _s_.appendString(_pn_ + "Items", Zeze.Serialize.Helper.encodeJson(_Items));
        _s_.appendBoolean(_pn_ + "Claimed", isClaimed());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "From", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Money", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Items", "map", "int", "demo.Game.BItem"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "Claimed", "bool", "", ""));
        return _v_;
    }
}
