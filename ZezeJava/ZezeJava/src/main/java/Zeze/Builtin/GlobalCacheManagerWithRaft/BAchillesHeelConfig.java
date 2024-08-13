// auto-generated @formatter:off
package Zeze.Builtin.GlobalCacheManagerWithRaft;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BAchillesHeelConfig extends Zeze.Transaction.Bean implements BAchillesHeelConfigReadOnly {
    public static final long TYPEID = 6351123425648255834L;

    private int _MaxNetPing;
    private int _ServerProcessTime;
    private int _ServerReleaseTimeout;

    private static final java.lang.invoke.VarHandle vh_MaxNetPing;
    private static final java.lang.invoke.VarHandle vh_ServerProcessTime;
    private static final java.lang.invoke.VarHandle vh_ServerReleaseTimeout;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_MaxNetPing = _l_.findVarHandle(BAchillesHeelConfig.class, "_MaxNetPing", int.class);
            vh_ServerProcessTime = _l_.findVarHandle(BAchillesHeelConfig.class, "_ServerProcessTime", int.class);
            vh_ServerReleaseTimeout = _l_.findVarHandle(BAchillesHeelConfig.class, "_ServerReleaseTimeout", int.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public int getMaxNetPing() {
        if (!isManaged())
            return _MaxNetPing;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _MaxNetPing;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _MaxNetPing;
    }

    public void setMaxNetPing(int _v_) {
        if (!isManaged()) {
            _MaxNetPing = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 1, vh_MaxNetPing, _v_));
    }

    @Override
    public int getServerProcessTime() {
        if (!isManaged())
            return _ServerProcessTime;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ServerProcessTime;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _ServerProcessTime;
    }

    public void setServerProcessTime(int _v_) {
        if (!isManaged()) {
            _ServerProcessTime = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 2, vh_ServerProcessTime, _v_));
    }

    @Override
    public int getServerReleaseTimeout() {
        if (!isManaged())
            return _ServerReleaseTimeout;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ServerReleaseTimeout;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _ServerReleaseTimeout;
    }

    public void setServerReleaseTimeout(int _v_) {
        if (!isManaged()) {
            _ServerReleaseTimeout = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 3, vh_ServerReleaseTimeout, _v_));
    }

    @SuppressWarnings("deprecation")
    public BAchillesHeelConfig() {
    }

    @SuppressWarnings("deprecation")
    public BAchillesHeelConfig(int _MaxNetPing_, int _ServerProcessTime_, int _ServerReleaseTimeout_) {
        _MaxNetPing = _MaxNetPing_;
        _ServerProcessTime = _ServerProcessTime_;
        _ServerReleaseTimeout = _ServerReleaseTimeout_;
    }

    @Override
    public void reset() {
        setMaxNetPing(0);
        setServerProcessTime(0);
        setServerReleaseTimeout(0);
        _unknown_ = null;
    }

    public void assign(BAchillesHeelConfig _o_) {
        setMaxNetPing(_o_.getMaxNetPing());
        setServerProcessTime(_o_.getServerProcessTime());
        setServerReleaseTimeout(_o_.getServerReleaseTimeout());
        _unknown_ = _o_._unknown_;
    }

    public BAchillesHeelConfig copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BAchillesHeelConfig copy() {
        var _c_ = new BAchillesHeelConfig();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BAchillesHeelConfig _a_, BAchillesHeelConfig _b_) {
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
        _s_.append("Zeze.Builtin.GlobalCacheManagerWithRaft.BAchillesHeelConfig: {\n");
        _s_.append(_i1_).append("MaxNetPing=").append(getMaxNetPing()).append(",\n");
        _s_.append(_i1_).append("ServerProcessTime=").append(getServerProcessTime()).append(",\n");
        _s_.append(_i1_).append("ServerReleaseTimeout=").append(getServerReleaseTimeout()).append('\n');
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
            int _x_ = getMaxNetPing();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getServerProcessTime();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getServerReleaseTimeout();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
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
            setMaxNetPing(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setServerProcessTime(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setServerReleaseTimeout(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BAchillesHeelConfig))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BAchillesHeelConfig)_o_;
        if (getMaxNetPing() != _b_.getMaxNetPing())
            return false;
        if (getServerProcessTime() != _b_.getServerProcessTime())
            return false;
        if (getServerReleaseTimeout() != _b_.getServerReleaseTimeout())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getMaxNetPing() < 0)
            return true;
        if (getServerProcessTime() < 0)
            return true;
        if (getServerReleaseTimeout() < 0)
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
                case 1: _MaxNetPing = _v_.intValue(); break;
                case 2: _ServerProcessTime = _v_.intValue(); break;
                case 3: _ServerReleaseTimeout = _v_.intValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setMaxNetPing(_r_.getInt(_pn_ + "MaxNetPing"));
        setServerProcessTime(_r_.getInt(_pn_ + "ServerProcessTime"));
        setServerReleaseTimeout(_r_.getInt(_pn_ + "ServerReleaseTimeout"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendInt(_pn_ + "MaxNetPing", getMaxNetPing());
        _s_.appendInt(_pn_ + "ServerProcessTime", getServerProcessTime());
        _s_.appendInt(_pn_ + "ServerReleaseTimeout", getServerReleaseTimeout());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "MaxNetPing", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "ServerProcessTime", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "ServerReleaseTimeout", "int", "", ""));
        return _v_;
    }
}
