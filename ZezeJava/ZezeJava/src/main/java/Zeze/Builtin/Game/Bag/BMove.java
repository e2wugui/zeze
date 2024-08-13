// auto-generated @formatter:off
package Zeze.Builtin.Game.Bag;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BMove extends Zeze.Transaction.Bean implements BMoveReadOnly {
    public static final long TYPEID = -7346236832819011963L;

    private String _BagName;
    private int _PositionFrom;
    private int _PositionTo;
    private int _number; // -1 表示全部

    private static final java.lang.invoke.VarHandle vh_BagName;
    private static final java.lang.invoke.VarHandle vh_PositionFrom;
    private static final java.lang.invoke.VarHandle vh_PositionTo;
    private static final java.lang.invoke.VarHandle vh_number;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_BagName = _l_.findVarHandle(BMove.class, "_BagName", String.class);
            vh_PositionFrom = _l_.findVarHandle(BMove.class, "_PositionFrom", int.class);
            vh_PositionTo = _l_.findVarHandle(BMove.class, "_PositionTo", int.class);
            vh_number = _l_.findVarHandle(BMove.class, "_number", int.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public String getBagName() {
        if (!isManaged())
            return _BagName;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _BagName;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _BagName;
    }

    public void setBagName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _BagName = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 1, vh_BagName, _v_));
    }

    @Override
    public int getPositionFrom() {
        if (!isManaged())
            return _PositionFrom;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _PositionFrom;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _PositionFrom;
    }

    public void setPositionFrom(int _v_) {
        if (!isManaged()) {
            _PositionFrom = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 2, vh_PositionFrom, _v_));
    }

    @Override
    public int getPositionTo() {
        if (!isManaged())
            return _PositionTo;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _PositionTo;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _PositionTo;
    }

    public void setPositionTo(int _v_) {
        if (!isManaged()) {
            _PositionTo = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 3, vh_PositionTo, _v_));
    }

    @Override
    public int getNumber() {
        if (!isManaged())
            return _number;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _number;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 4);
        return log != null ? log.value : _number;
    }

    public void setNumber(int _v_) {
        if (!isManaged()) {
            _number = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 4, vh_number, _v_));
    }

    @SuppressWarnings("deprecation")
    public BMove() {
        _BagName = "";
    }

    @SuppressWarnings("deprecation")
    public BMove(String _BagName_, int _PositionFrom_, int _PositionTo_, int _number_) {
        if (_BagName_ == null)
            _BagName_ = "";
        _BagName = _BagName_;
        _PositionFrom = _PositionFrom_;
        _PositionTo = _PositionTo_;
        _number = _number_;
    }

    @Override
    public void reset() {
        setBagName("");
        setPositionFrom(0);
        setPositionTo(0);
        setNumber(0);
        _unknown_ = null;
    }

    public void assign(BMove _o_) {
        setBagName(_o_.getBagName());
        setPositionFrom(_o_.getPositionFrom());
        setPositionTo(_o_.getPositionTo());
        setNumber(_o_.getNumber());
        _unknown_ = _o_._unknown_;
    }

    public BMove copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BMove copy() {
        var _c_ = new BMove();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BMove _a_, BMove _b_) {
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
        _s_.append("Zeze.Builtin.Game.Bag.BMove: {\n");
        _s_.append(_i1_).append("BagName=").append(getBagName()).append(",\n");
        _s_.append(_i1_).append("PositionFrom=").append(getPositionFrom()).append(",\n");
        _s_.append(_i1_).append("PositionTo=").append(getPositionTo()).append(",\n");
        _s_.append(_i1_).append("number=").append(getNumber()).append('\n');
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
            String _x_ = getBagName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            int _x_ = getPositionFrom();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getPositionTo();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getNumber();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
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
            setBagName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setPositionFrom(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setPositionTo(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setNumber(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BMove))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BMove)_o_;
        if (!getBagName().equals(_b_.getBagName()))
            return false;
        if (getPositionFrom() != _b_.getPositionFrom())
            return false;
        if (getPositionTo() != _b_.getPositionTo())
            return false;
        if (getNumber() != _b_.getNumber())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getPositionFrom() < 0)
            return true;
        if (getPositionTo() < 0)
            return true;
        if (getNumber() < 0)
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
                case 1: _BagName = _v_.stringValue(); break;
                case 2: _PositionFrom = _v_.intValue(); break;
                case 3: _PositionTo = _v_.intValue(); break;
                case 4: _number = _v_.intValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setBagName(_r_.getString(_pn_ + "BagName"));
        if (getBagName() == null)
            setBagName("");
        setPositionFrom(_r_.getInt(_pn_ + "PositionFrom"));
        setPositionTo(_r_.getInt(_pn_ + "PositionTo"));
        setNumber(_r_.getInt(_pn_ + "number"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "BagName", getBagName());
        _s_.appendInt(_pn_ + "PositionFrom", getPositionFrom());
        _s_.appendInt(_pn_ + "PositionTo", getPositionTo());
        _s_.appendInt(_pn_ + "number", getNumber());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "BagName", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "PositionFrom", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "PositionTo", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "number", "int", "", ""));
        return _v_;
    }
}
