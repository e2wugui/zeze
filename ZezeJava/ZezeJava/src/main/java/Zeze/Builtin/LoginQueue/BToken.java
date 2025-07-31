// auto-generated @formatter:off
package Zeze.Builtin.LoginQueue;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BToken extends Zeze.Transaction.Bean implements BTokenReadOnly {
    public static final long TYPEID = -3906186370562466947L;

    private int _ServerId;
    private long _ExpireTime;
    private long _SerialId;
    private int _LinkServerId; // 一般是负数，从-1往后分配，避免和gs的serverId重复。

    private static final java.lang.invoke.VarHandle vh_ServerId;
    private static final java.lang.invoke.VarHandle vh_ExpireTime;
    private static final java.lang.invoke.VarHandle vh_SerialId;
    private static final java.lang.invoke.VarHandle vh_LinkServerId;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_ServerId = _l_.findVarHandle(BToken.class, "_ServerId", int.class);
            vh_ExpireTime = _l_.findVarHandle(BToken.class, "_ExpireTime", long.class);
            vh_SerialId = _l_.findVarHandle(BToken.class, "_SerialId", long.class);
            vh_LinkServerId = _l_.findVarHandle(BToken.class, "_LinkServerId", int.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public int getServerId() {
        if (!isManaged())
            return _ServerId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ServerId;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _ServerId;
    }

    public void setServerId(int _v_) {
        if (!isManaged()) {
            _ServerId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 1, vh_ServerId, _v_));
    }

    @Override
    public long getExpireTime() {
        if (!isManaged())
            return _ExpireTime;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ExpireTime;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _ExpireTime;
    }

    public void setExpireTime(long _v_) {
        if (!isManaged()) {
            _ExpireTime = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 2, vh_ExpireTime, _v_));
    }

    @Override
    public long getSerialId() {
        if (!isManaged())
            return _SerialId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _SerialId;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _SerialId;
    }

    public void setSerialId(long _v_) {
        if (!isManaged()) {
            _SerialId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 3, vh_SerialId, _v_));
    }

    @Override
    public int getLinkServerId() {
        if (!isManaged())
            return _LinkServerId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _LinkServerId;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 4);
        return log != null ? log.value : _LinkServerId;
    }

    public void setLinkServerId(int _v_) {
        if (!isManaged()) {
            _LinkServerId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 4, vh_LinkServerId, _v_));
    }

    @SuppressWarnings("deprecation")
    public BToken() {
    }

    @SuppressWarnings("deprecation")
    public BToken(int _ServerId_, long _ExpireTime_, long _SerialId_, int _LinkServerId_) {
        _ServerId = _ServerId_;
        _ExpireTime = _ExpireTime_;
        _SerialId = _SerialId_;
        _LinkServerId = _LinkServerId_;
    }

    @Override
    public void reset() {
        setServerId(0);
        setExpireTime(0);
        setSerialId(0);
        setLinkServerId(0);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.LoginQueue.BToken.Data toData() {
        var _d_ = new Zeze.Builtin.LoginQueue.BToken.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.LoginQueue.BToken.Data)_o_);
    }

    public void assign(BToken.Data _o_) {
        setServerId(_o_._ServerId);
        setExpireTime(_o_._ExpireTime);
        setSerialId(_o_._SerialId);
        setLinkServerId(_o_._LinkServerId);
        _unknown_ = null;
    }

    public void assign(BToken _o_) {
        setServerId(_o_.getServerId());
        setExpireTime(_o_.getExpireTime());
        setSerialId(_o_.getSerialId());
        setLinkServerId(_o_.getLinkServerId());
        _unknown_ = _o_._unknown_;
    }

    public BToken copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BToken copy() {
        var _c_ = new BToken();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BToken _a_, BToken _b_) {
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
        _s_.append("Zeze.Builtin.LoginQueue.BToken: {\n");
        _s_.append(_i1_).append("ServerId=").append(getServerId()).append(",\n");
        _s_.append(_i1_).append("ExpireTime=").append(getExpireTime()).append(",\n");
        _s_.append(_i1_).append("SerialId=").append(getSerialId()).append(",\n");
        _s_.append(_i1_).append("LinkServerId=").append(getLinkServerId()).append('\n');
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
            int _x_ = getServerId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            long _x_ = getExpireTime();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getSerialId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            int _x_ = getLinkServerId();
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
            setServerId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setExpireTime(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setSerialId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setLinkServerId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BToken))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BToken)_o_;
        if (getServerId() != _b_.getServerId())
            return false;
        if (getExpireTime() != _b_.getExpireTime())
            return false;
        if (getSerialId() != _b_.getSerialId())
            return false;
        if (getLinkServerId() != _b_.getLinkServerId())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getServerId() < 0)
            return true;
        if (getExpireTime() < 0)
            return true;
        if (getSerialId() < 0)
            return true;
        if (getLinkServerId() < 0)
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
                case 1: _ServerId = _v_.intValue(); break;
                case 2: _ExpireTime = _v_.longValue(); break;
                case 3: _SerialId = _v_.longValue(); break;
                case 4: _LinkServerId = _v_.intValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setServerId(_r_.getInt(_pn_ + "ServerId"));
        setExpireTime(_r_.getLong(_pn_ + "ExpireTime"));
        setSerialId(_r_.getLong(_pn_ + "SerialId"));
        setLinkServerId(_r_.getInt(_pn_ + "LinkServerId"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendInt(_pn_ + "ServerId", getServerId());
        _s_.appendLong(_pn_ + "ExpireTime", getExpireTime());
        _s_.appendLong(_pn_ + "SerialId", getSerialId());
        _s_.appendInt(_pn_ + "LinkServerId", getLinkServerId());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "ServerId", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "ExpireTime", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "SerialId", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "LinkServerId", "int", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -3906186370562466947L;

    private int _ServerId;
    private long _ExpireTime;
    private long _SerialId;
    private int _LinkServerId; // 一般是负数，从-1往后分配，避免和gs的serverId重复。

    public int getServerId() {
        return _ServerId;
    }

    public void setServerId(int _v_) {
        _ServerId = _v_;
    }

    public long getExpireTime() {
        return _ExpireTime;
    }

    public void setExpireTime(long _v_) {
        _ExpireTime = _v_;
    }

    public long getSerialId() {
        return _SerialId;
    }

    public void setSerialId(long _v_) {
        _SerialId = _v_;
    }

    public int getLinkServerId() {
        return _LinkServerId;
    }

    public void setLinkServerId(int _v_) {
        _LinkServerId = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
    }

    @SuppressWarnings("deprecation")
    public Data(int _ServerId_, long _ExpireTime_, long _SerialId_, int _LinkServerId_) {
        _ServerId = _ServerId_;
        _ExpireTime = _ExpireTime_;
        _SerialId = _SerialId_;
        _LinkServerId = _LinkServerId_;
    }

    @Override
    public void reset() {
        _ServerId = 0;
        _ExpireTime = 0;
        _SerialId = 0;
        _LinkServerId = 0;
    }

    @Override
    public Zeze.Builtin.LoginQueue.BToken toBean() {
        var _b_ = new Zeze.Builtin.LoginQueue.BToken();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BToken)_o_);
    }

    public void assign(BToken _o_) {
        _ServerId = _o_.getServerId();
        _ExpireTime = _o_.getExpireTime();
        _SerialId = _o_.getSerialId();
        _LinkServerId = _o_.getLinkServerId();
    }

    public void assign(BToken.Data _o_) {
        _ServerId = _o_._ServerId;
        _ExpireTime = _o_._ExpireTime;
        _SerialId = _o_._SerialId;
        _LinkServerId = _o_._LinkServerId;
    }

    @Override
    public BToken.Data copy() {
        var _c_ = new BToken.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BToken.Data _a_, BToken.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BToken.Data clone() {
        return (BToken.Data)super.clone();
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
        _s_.append("Zeze.Builtin.LoginQueue.BToken: {\n");
        _s_.append(_i1_).append("ServerId=").append(_ServerId).append(",\n");
        _s_.append(_i1_).append("ExpireTime=").append(_ExpireTime).append(",\n");
        _s_.append(_i1_).append("SerialId=").append(_SerialId).append(",\n");
        _s_.append(_i1_).append("LinkServerId=").append(_LinkServerId).append('\n');
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
            int _x_ = _ServerId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            long _x_ = _ExpireTime;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = _SerialId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            int _x_ = _LinkServerId;
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
            _ServerId = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _ExpireTime = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _SerialId = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _LinkServerId = _o_.ReadInt(_t_);
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
        if (!(_o_ instanceof BToken.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BToken.Data)_o_;
        if (_ServerId != _b_._ServerId)
            return false;
        if (_ExpireTime != _b_._ExpireTime)
            return false;
        if (_SerialId != _b_._SerialId)
            return false;
        if (_LinkServerId != _b_._LinkServerId)
            return false;
        return true;
    }
}
}
