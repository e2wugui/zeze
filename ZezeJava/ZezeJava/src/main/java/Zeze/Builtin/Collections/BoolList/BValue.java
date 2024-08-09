// auto-generated @formatter:off
package Zeze.Builtin.Collections.BoolList;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BValue extends Zeze.Transaction.Bean implements BValueReadOnly {
    public static final long TYPEID = -6830223910089940882L;

    private long _Item0;
    private long _Item1;
    private long _Item2;
    private long _Item3;
    private long _Item4;
    private long _Item5;
    private long _Item6;
    private long _Item7;

    private static final java.lang.invoke.VarHandle vh_Item0;
    private static final java.lang.invoke.VarHandle vh_Item1;
    private static final java.lang.invoke.VarHandle vh_Item2;
    private static final java.lang.invoke.VarHandle vh_Item3;
    private static final java.lang.invoke.VarHandle vh_Item4;
    private static final java.lang.invoke.VarHandle vh_Item5;
    private static final java.lang.invoke.VarHandle vh_Item6;
    private static final java.lang.invoke.VarHandle vh_Item7;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_Item0 = _l_.findVarHandle(BValue.class, "_Item0", long.class);
            vh_Item1 = _l_.findVarHandle(BValue.class, "_Item1", long.class);
            vh_Item2 = _l_.findVarHandle(BValue.class, "_Item2", long.class);
            vh_Item3 = _l_.findVarHandle(BValue.class, "_Item3", long.class);
            vh_Item4 = _l_.findVarHandle(BValue.class, "_Item4", long.class);
            vh_Item5 = _l_.findVarHandle(BValue.class, "_Item5", long.class);
            vh_Item6 = _l_.findVarHandle(BValue.class, "_Item6", long.class);
            vh_Item7 = _l_.findVarHandle(BValue.class, "_Item7", long.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public long getItem0() {
        if (!isManaged())
            return _Item0;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Item0;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _Item0;
    }

    public void setItem0(long _v_) {
        if (!isManaged()) {
            _Item0 = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 1, vh_Item0, _v_));
    }

    @Override
    public long getItem1() {
        if (!isManaged())
            return _Item1;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Item1;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _Item1;
    }

    public void setItem1(long _v_) {
        if (!isManaged()) {
            _Item1 = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 2, vh_Item1, _v_));
    }

    @Override
    public long getItem2() {
        if (!isManaged())
            return _Item2;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Item2;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _Item2;
    }

    public void setItem2(long _v_) {
        if (!isManaged()) {
            _Item2 = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 3, vh_Item2, _v_));
    }

    @Override
    public long getItem3() {
        if (!isManaged())
            return _Item3;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Item3;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 4);
        return log != null ? log.value : _Item3;
    }

    public void setItem3(long _v_) {
        if (!isManaged()) {
            _Item3 = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 4, vh_Item3, _v_));
    }

    @Override
    public long getItem4() {
        if (!isManaged())
            return _Item4;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Item4;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 5);
        return log != null ? log.value : _Item4;
    }

    public void setItem4(long _v_) {
        if (!isManaged()) {
            _Item4 = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 5, vh_Item4, _v_));
    }

    @Override
    public long getItem5() {
        if (!isManaged())
            return _Item5;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Item5;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 6);
        return log != null ? log.value : _Item5;
    }

    public void setItem5(long _v_) {
        if (!isManaged()) {
            _Item5 = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 6, vh_Item5, _v_));
    }

    @Override
    public long getItem6() {
        if (!isManaged())
            return _Item6;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Item6;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 7);
        return log != null ? log.value : _Item6;
    }

    public void setItem6(long _v_) {
        if (!isManaged()) {
            _Item6 = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 7, vh_Item6, _v_));
    }

    @Override
    public long getItem7() {
        if (!isManaged())
            return _Item7;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Item7;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 8);
        return log != null ? log.value : _Item7;
    }

    public void setItem7(long _v_) {
        if (!isManaged()) {
            _Item7 = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 8, vh_Item7, _v_));
    }

    @SuppressWarnings("deprecation")
    public BValue() {
    }

    @SuppressWarnings("deprecation")
    public BValue(long _Item0_, long _Item1_, long _Item2_, long _Item3_, long _Item4_, long _Item5_, long _Item6_, long _Item7_) {
        _Item0 = _Item0_;
        _Item1 = _Item1_;
        _Item2 = _Item2_;
        _Item3 = _Item3_;
        _Item4 = _Item4_;
        _Item5 = _Item5_;
        _Item6 = _Item6_;
        _Item7 = _Item7_;
    }

    @Override
    public void reset() {
        setItem0(0);
        setItem1(0);
        setItem2(0);
        setItem3(0);
        setItem4(0);
        setItem5(0);
        setItem6(0);
        setItem7(0);
        _unknown_ = null;
    }

    public void assign(BValue _o_) {
        setItem0(_o_.getItem0());
        setItem1(_o_.getItem1());
        setItem2(_o_.getItem2());
        setItem3(_o_.getItem3());
        setItem4(_o_.getItem4());
        setItem5(_o_.getItem5());
        setItem6(_o_.getItem6());
        setItem7(_o_.getItem7());
        _unknown_ = _o_._unknown_;
    }

    public BValue copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BValue copy() {
        var _c_ = new BValue();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BValue _a_, BValue _b_) {
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
        _s_.append("Zeze.Builtin.Collections.BoolList.BValue: {\n");
        _s_.append(_i1_).append("Item0=").append(getItem0()).append(",\n");
        _s_.append(_i1_).append("Item1=").append(getItem1()).append(",\n");
        _s_.append(_i1_).append("Item2=").append(getItem2()).append(",\n");
        _s_.append(_i1_).append("Item3=").append(getItem3()).append(",\n");
        _s_.append(_i1_).append("Item4=").append(getItem4()).append(",\n");
        _s_.append(_i1_).append("Item5=").append(getItem5()).append(",\n");
        _s_.append(_i1_).append("Item6=").append(getItem6()).append(",\n");
        _s_.append(_i1_).append("Item7=").append(getItem7()).append('\n');
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
            long _x_ = getItem0();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getItem1();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getItem2();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getItem3();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getItem4();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getItem5();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getItem6();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 7, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getItem7();
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
        if (_i_ == 1) {
            setItem0(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setItem1(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setItem2(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setItem3(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setItem4(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            setItem5(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 7) {
            setItem6(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 8) {
            setItem7(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BValue))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BValue)_o_;
        if (getItem0() != _b_.getItem0())
            return false;
        if (getItem1() != _b_.getItem1())
            return false;
        if (getItem2() != _b_.getItem2())
            return false;
        if (getItem3() != _b_.getItem3())
            return false;
        if (getItem4() != _b_.getItem4())
            return false;
        if (getItem5() != _b_.getItem5())
            return false;
        if (getItem6() != _b_.getItem6())
            return false;
        if (getItem7() != _b_.getItem7())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getItem0() < 0)
            return true;
        if (getItem1() < 0)
            return true;
        if (getItem2() < 0)
            return true;
        if (getItem3() < 0)
            return true;
        if (getItem4() < 0)
            return true;
        if (getItem5() < 0)
            return true;
        if (getItem6() < 0)
            return true;
        if (getItem7() < 0)
            return true;
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
                case 1: _Item0 = _v_.longValue(); break;
                case 2: _Item1 = _v_.longValue(); break;
                case 3: _Item2 = _v_.longValue(); break;
                case 4: _Item3 = _v_.longValue(); break;
                case 5: _Item4 = _v_.longValue(); break;
                case 6: _Item5 = _v_.longValue(); break;
                case 7: _Item6 = _v_.longValue(); break;
                case 8: _Item7 = _v_.longValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setItem0(_r_.getLong(_pn_ + "Item0"));
        setItem1(_r_.getLong(_pn_ + "Item1"));
        setItem2(_r_.getLong(_pn_ + "Item2"));
        setItem3(_r_.getLong(_pn_ + "Item3"));
        setItem4(_r_.getLong(_pn_ + "Item4"));
        setItem5(_r_.getLong(_pn_ + "Item5"));
        setItem6(_r_.getLong(_pn_ + "Item6"));
        setItem7(_r_.getLong(_pn_ + "Item7"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendLong(_pn_ + "Item0", getItem0());
        _s_.appendLong(_pn_ + "Item1", getItem1());
        _s_.appendLong(_pn_ + "Item2", getItem2());
        _s_.appendLong(_pn_ + "Item3", getItem3());
        _s_.appendLong(_pn_ + "Item4", getItem4());
        _s_.appendLong(_pn_ + "Item5", getItem5());
        _s_.appendLong(_pn_ + "Item6", getItem6());
        _s_.appendLong(_pn_ + "Item7", getItem7());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Item0", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Item1", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Item2", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "Item3", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "Item4", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(6, "Item5", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(7, "Item6", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(8, "Item7", "long", "", ""));
        return _v_;
    }
}
