// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

// 一个timer的信息
@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BIndex extends Zeze.Transaction.Bean implements BIndexReadOnly {
    public static final long TYPEID = 8921847554177605341L;

    private int _ServerId; // 所属的serverId, 被其它timer接管后会更新
    private long _NodeId; // 所属的节点ID
    private long _SerialId; // 创建时从AutoKey("Zeze.Component.Timer.SerialId")分配, 用于触发时验证是否一致,并在触发后验证是否重置了该定时器
    private long _Version; // 创建时记下当前timer所属server的版本

    private static final java.lang.invoke.VarHandle vh_ServerId;
    private static final java.lang.invoke.VarHandle vh_NodeId;
    private static final java.lang.invoke.VarHandle vh_SerialId;
    private static final java.lang.invoke.VarHandle vh_Version;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_ServerId = _l_.findVarHandle(BIndex.class, "_ServerId", int.class);
            vh_NodeId = _l_.findVarHandle(BIndex.class, "_NodeId", long.class);
            vh_SerialId = _l_.findVarHandle(BIndex.class, "_SerialId", long.class);
            vh_Version = _l_.findVarHandle(BIndex.class, "_Version", long.class);
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
    public long getNodeId() {
        if (!isManaged())
            return _NodeId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _NodeId;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _NodeId;
    }

    public void setNodeId(long _v_) {
        if (!isManaged()) {
            _NodeId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 2, vh_NodeId, _v_));
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
    public long getVersion() {
        if (!isManaged())
            return _Version;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Version;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 4);
        return log != null ? log.value : _Version;
    }

    public void setVersion(long _v_) {
        if (!isManaged()) {
            _Version = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 4, vh_Version, _v_));
    }

    @SuppressWarnings("deprecation")
    public BIndex() {
    }

    @SuppressWarnings("deprecation")
    public BIndex(int _ServerId_, long _NodeId_, long _SerialId_, long _Version_) {
        _ServerId = _ServerId_;
        _NodeId = _NodeId_;
        _SerialId = _SerialId_;
        _Version = _Version_;
    }

    @Override
    public void reset() {
        setServerId(0);
        setNodeId(0);
        setSerialId(0);
        setVersion(0);
        _unknown_ = null;
    }

    public void assign(BIndex _o_) {
        setServerId(_o_.getServerId());
        setNodeId(_o_.getNodeId());
        setSerialId(_o_.getSerialId());
        setVersion(_o_.getVersion());
        _unknown_ = _o_._unknown_;
    }

    public BIndex copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BIndex copy() {
        var _c_ = new BIndex();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BIndex _a_, BIndex _b_) {
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
        _s_.append("Zeze.Builtin.Timer.BIndex: {\n");
        _s_.append(_i1_).append("ServerId=").append(getServerId()).append(",\n");
        _s_.append(_i1_).append("NodeId=").append(getNodeId()).append(",\n");
        _s_.append(_i1_).append("SerialId=").append(getSerialId()).append(",\n");
        _s_.append(_i1_).append("Version=").append(getVersion()).append('\n');
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
            long _x_ = getNodeId();
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
            long _x_ = getVersion();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
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
            setServerId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setNodeId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setSerialId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setVersion(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BIndex))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BIndex)_o_;
        if (getServerId() != _b_.getServerId())
            return false;
        if (getNodeId() != _b_.getNodeId())
            return false;
        if (getSerialId() != _b_.getSerialId())
            return false;
        if (getVersion() != _b_.getVersion())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getServerId() < 0)
            return true;
        if (getNodeId() < 0)
            return true;
        if (getSerialId() < 0)
            return true;
        if (getVersion() < 0)
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
                case 2: _NodeId = _v_.longValue(); break;
                case 3: _SerialId = _v_.longValue(); break;
                case 4: _Version = _v_.longValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setServerId(_r_.getInt(_pn_ + "ServerId"));
        setNodeId(_r_.getLong(_pn_ + "NodeId"));
        setSerialId(_r_.getLong(_pn_ + "SerialId"));
        setVersion(_r_.getLong(_pn_ + "Version"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendInt(_pn_ + "ServerId", getServerId());
        _s_.appendLong(_pn_ + "NodeId", getNodeId());
        _s_.appendLong(_pn_ + "SerialId", getSerialId());
        _s_.appendLong(_pn_ + "Version", getVersion());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "ServerId", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "NodeId", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "SerialId", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "Version", "long", "", ""));
        return _v_;
    }
}
