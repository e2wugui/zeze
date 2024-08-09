// auto-generated @formatter:off
package Zeze.Builtin.Provider;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BLoad extends Zeze.Transaction.Bean implements BLoadReadOnly {
    public static final long TYPEID = 8972064501607813483L;

    public static final int eWorkFine = 0;
    public static final int eThreshold = 1;
    public static final int eOverload = 2;

    private int _Online; // 用户数量
    private int _ProposeMaxOnline; // 建议最大用户数量
    private int _OnlineNew; // 最近上线用户数量，一般是一秒内的。用来防止短时间内给同一个gs分配太多用户。
    private int _Overload; // 过载保护类型。参见上面的枚举定义。

    private static final java.lang.invoke.VarHandle vh_Online;
    private static final java.lang.invoke.VarHandle vh_ProposeMaxOnline;
    private static final java.lang.invoke.VarHandle vh_OnlineNew;
    private static final java.lang.invoke.VarHandle vh_Overload;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_Online = _l_.findVarHandle(BLoad.class, "_Online", int.class);
            vh_ProposeMaxOnline = _l_.findVarHandle(BLoad.class, "_ProposeMaxOnline", int.class);
            vh_OnlineNew = _l_.findVarHandle(BLoad.class, "_OnlineNew", int.class);
            vh_Overload = _l_.findVarHandle(BLoad.class, "_Overload", int.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public int getOnline() {
        if (!isManaged())
            return _Online;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Online;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _Online;
    }

    public void setOnline(int _v_) {
        if (!isManaged()) {
            _Online = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 1, vh_Online, _v_));
    }

    @Override
    public int getProposeMaxOnline() {
        if (!isManaged())
            return _ProposeMaxOnline;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ProposeMaxOnline;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _ProposeMaxOnline;
    }

    public void setProposeMaxOnline(int _v_) {
        if (!isManaged()) {
            _ProposeMaxOnline = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 2, vh_ProposeMaxOnline, _v_));
    }

    @Override
    public int getOnlineNew() {
        if (!isManaged())
            return _OnlineNew;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _OnlineNew;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _OnlineNew;
    }

    public void setOnlineNew(int _v_) {
        if (!isManaged()) {
            _OnlineNew = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 3, vh_OnlineNew, _v_));
    }

    @Override
    public int getOverload() {
        if (!isManaged())
            return _Overload;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Overload;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 4);
        return log != null ? log.value : _Overload;
    }

    public void setOverload(int _v_) {
        if (!isManaged()) {
            _Overload = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 4, vh_Overload, _v_));
    }

    @SuppressWarnings("deprecation")
    public BLoad() {
    }

    @SuppressWarnings("deprecation")
    public BLoad(int _Online_, int _ProposeMaxOnline_, int _OnlineNew_, int _Overload_) {
        _Online = _Online_;
        _ProposeMaxOnline = _ProposeMaxOnline_;
        _OnlineNew = _OnlineNew_;
        _Overload = _Overload_;
    }

    @Override
    public void reset() {
        setOnline(0);
        setProposeMaxOnline(0);
        setOnlineNew(0);
        setOverload(0);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Provider.BLoad.Data toData() {
        var _d_ = new Zeze.Builtin.Provider.BLoad.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Provider.BLoad.Data)_o_);
    }

    public void assign(BLoad.Data _o_) {
        setOnline(_o_._Online);
        setProposeMaxOnline(_o_._ProposeMaxOnline);
        setOnlineNew(_o_._OnlineNew);
        setOverload(_o_._Overload);
        _unknown_ = null;
    }

    public void assign(BLoad _o_) {
        setOnline(_o_.getOnline());
        setProposeMaxOnline(_o_.getProposeMaxOnline());
        setOnlineNew(_o_.getOnlineNew());
        setOverload(_o_.getOverload());
        _unknown_ = _o_._unknown_;
    }

    public BLoad copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BLoad copy() {
        var _c_ = new BLoad();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BLoad _a_, BLoad _b_) {
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
        _s_.append("Zeze.Builtin.Provider.BLoad: {\n");
        _s_.append(_i1_).append("Online=").append(getOnline()).append(",\n");
        _s_.append(_i1_).append("ProposeMaxOnline=").append(getProposeMaxOnline()).append(",\n");
        _s_.append(_i1_).append("OnlineNew=").append(getOnlineNew()).append(",\n");
        _s_.append(_i1_).append("Overload=").append(getOverload()).append('\n');
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
            int _x_ = getOnline();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getProposeMaxOnline();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getOnlineNew();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getOverload();
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
            setOnline(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setProposeMaxOnline(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setOnlineNew(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setOverload(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BLoad))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BLoad)_o_;
        if (getOnline() != _b_.getOnline())
            return false;
        if (getProposeMaxOnline() != _b_.getProposeMaxOnline())
            return false;
        if (getOnlineNew() != _b_.getOnlineNew())
            return false;
        if (getOverload() != _b_.getOverload())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getOnline() < 0)
            return true;
        if (getProposeMaxOnline() < 0)
            return true;
        if (getOnlineNew() < 0)
            return true;
        if (getOverload() < 0)
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
                case 1: _Online = _v_.intValue(); break;
                case 2: _ProposeMaxOnline = _v_.intValue(); break;
                case 3: _OnlineNew = _v_.intValue(); break;
                case 4: _Overload = _v_.intValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setOnline(_r_.getInt(_pn_ + "Online"));
        setProposeMaxOnline(_r_.getInt(_pn_ + "ProposeMaxOnline"));
        setOnlineNew(_r_.getInt(_pn_ + "OnlineNew"));
        setOverload(_r_.getInt(_pn_ + "Overload"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendInt(_pn_ + "Online", getOnline());
        _s_.appendInt(_pn_ + "ProposeMaxOnline", getProposeMaxOnline());
        _s_.appendInt(_pn_ + "OnlineNew", getOnlineNew());
        _s_.appendInt(_pn_ + "Overload", getOverload());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Online", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "ProposeMaxOnline", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "OnlineNew", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "Overload", "int", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 8972064501607813483L;

    public static final int eWorkFine = 0;
    public static final int eThreshold = 1;
    public static final int eOverload = 2;

    private int _Online; // 用户数量
    private int _ProposeMaxOnline; // 建议最大用户数量
    private int _OnlineNew; // 最近上线用户数量，一般是一秒内的。用来防止短时间内给同一个gs分配太多用户。
    private int _Overload; // 过载保护类型。参见上面的枚举定义。

    public int getOnline() {
        return _Online;
    }

    public void setOnline(int _v_) {
        _Online = _v_;
    }

    public int getProposeMaxOnline() {
        return _ProposeMaxOnline;
    }

    public void setProposeMaxOnline(int _v_) {
        _ProposeMaxOnline = _v_;
    }

    public int getOnlineNew() {
        return _OnlineNew;
    }

    public void setOnlineNew(int _v_) {
        _OnlineNew = _v_;
    }

    public int getOverload() {
        return _Overload;
    }

    public void setOverload(int _v_) {
        _Overload = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
    }

    @SuppressWarnings("deprecation")
    public Data(int _Online_, int _ProposeMaxOnline_, int _OnlineNew_, int _Overload_) {
        _Online = _Online_;
        _ProposeMaxOnline = _ProposeMaxOnline_;
        _OnlineNew = _OnlineNew_;
        _Overload = _Overload_;
    }

    @Override
    public void reset() {
        _Online = 0;
        _ProposeMaxOnline = 0;
        _OnlineNew = 0;
        _Overload = 0;
    }

    @Override
    public Zeze.Builtin.Provider.BLoad toBean() {
        var _b_ = new Zeze.Builtin.Provider.BLoad();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BLoad)_o_);
    }

    public void assign(BLoad _o_) {
        _Online = _o_.getOnline();
        _ProposeMaxOnline = _o_.getProposeMaxOnline();
        _OnlineNew = _o_.getOnlineNew();
        _Overload = _o_.getOverload();
    }

    public void assign(BLoad.Data _o_) {
        _Online = _o_._Online;
        _ProposeMaxOnline = _o_._ProposeMaxOnline;
        _OnlineNew = _o_._OnlineNew;
        _Overload = _o_._Overload;
    }

    @Override
    public BLoad.Data copy() {
        var _c_ = new BLoad.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BLoad.Data _a_, BLoad.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BLoad.Data clone() {
        return (BLoad.Data)super.clone();
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
        _s_.append("Zeze.Builtin.Provider.BLoad: {\n");
        _s_.append(_i1_).append("Online=").append(_Online).append(",\n");
        _s_.append(_i1_).append("ProposeMaxOnline=").append(_ProposeMaxOnline).append(",\n");
        _s_.append(_i1_).append("OnlineNew=").append(_OnlineNew).append(",\n");
        _s_.append(_i1_).append("Overload=").append(_Overload).append('\n');
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
            int _x_ = _Online;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = _ProposeMaxOnline;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = _OnlineNew;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = _Overload;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _Online = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _ProposeMaxOnline = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _OnlineNew = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _Overload = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
