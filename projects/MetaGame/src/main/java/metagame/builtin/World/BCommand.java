// auto-generated @formatter:off
package metagame.builtin.World;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

// 一个具体的操作。
@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BCommand extends Zeze.Transaction.Bean implements BCommandReadOnly {
    public static final long TYPEID = -4392130331074426542L;

    public static final int eReserveCommandId = 2000; // 保留Id给组件内部用。自定义的必须大于这个值。
    public static final int eMoveMmo = 0; // handle=server,client 位置同步命令。
    public static final int eEnterWorld = 2; // handle=client
    public static final int eEnterConfirm = 3; // handle=server
    public static final int eAoiOperate = 4; // handle=client，需要同步的其他任意操作，完全抽象。
    public static final int eAoiEnter = 5; // handle=client
    public static final int eAoiLeave = 6; // handle=client

    private long _MapInstanceId;
    private int _CommandId;
    private Zeze.Net.Binary _Param;

    private static final java.lang.invoke.VarHandle vh_MapInstanceId;
    private static final java.lang.invoke.VarHandle vh_CommandId;
    private static final java.lang.invoke.VarHandle vh_Param;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_MapInstanceId = _l_.findVarHandle(BCommand.class, "_MapInstanceId", long.class);
            vh_CommandId = _l_.findVarHandle(BCommand.class, "_CommandId", int.class);
            vh_Param = _l_.findVarHandle(BCommand.class, "_Param", Zeze.Net.Binary.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public long getMapInstanceId() {
        if (!isManaged())
            return _MapInstanceId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _MapInstanceId;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _MapInstanceId;
    }

    public void setMapInstanceId(long _v_) {
        if (!isManaged()) {
            _MapInstanceId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 1, vh_MapInstanceId, _v_));
    }

    @Override
    public int getCommandId() {
        if (!isManaged())
            return _CommandId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _CommandId;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _CommandId;
    }

    public void setCommandId(int _v_) {
        if (!isManaged()) {
            _CommandId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 2, vh_CommandId, _v_));
    }

    @Override
    public Zeze.Net.Binary getParam() {
        if (!isManaged())
            return _Param;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Param;
        var log = (Zeze.Transaction.Logs.LogBinary)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _Param;
    }

    public void setParam(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Param = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBinary(this, 3, vh_Param, _v_));
    }

    @SuppressWarnings("deprecation")
    public BCommand() {
        _Param = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BCommand(long _MapInstanceId_, int _CommandId_, Zeze.Net.Binary _Param_) {
        _MapInstanceId = _MapInstanceId_;
        _CommandId = _CommandId_;
        if (_Param_ == null)
            _Param_ = Zeze.Net.Binary.Empty;
        _Param = _Param_;
    }

    @Override
    public void reset() {
        setMapInstanceId(0);
        setCommandId(0);
        setParam(Zeze.Net.Binary.Empty);
        _unknown_ = null;
    }

    @Override
    public metagame.builtin.World.BCommand.Data toData() {
        var _d_ = new metagame.builtin.World.BCommand.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((metagame.builtin.World.BCommand.Data)_o_);
    }

    public void assign(BCommand.Data _o_) {
        setMapInstanceId(_o_._MapInstanceId);
        setCommandId(_o_._CommandId);
        setParam(_o_._Param);
        _unknown_ = null;
    }

    public void assign(BCommand _o_) {
        setMapInstanceId(_o_.getMapInstanceId());
        setCommandId(_o_.getCommandId());
        setParam(_o_.getParam());
        _unknown_ = _o_._unknown_;
    }

    public BCommand copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BCommand copy() {
        var _c_ = new BCommand();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BCommand _a_, BCommand _b_) {
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
        _s_.append("metagame.builtin.World.BCommand: {\n");
        _s_.append(_i1_).append("MapInstanceId=").append(getMapInstanceId()).append(",\n");
        _s_.append(_i1_).append("CommandId=").append(getCommandId()).append(",\n");
        _s_.append(_i1_).append("Param=").append(getParam()).append('\n');
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
            long _x_ = getMapInstanceId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            int _x_ = getCommandId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = getParam();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
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
            setMapInstanceId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setCommandId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setParam(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BCommand))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BCommand)_o_;
        if (getMapInstanceId() != _b_.getMapInstanceId())
            return false;
        if (getCommandId() != _b_.getCommandId())
            return false;
        if (!getParam().equals(_b_.getParam()))
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getMapInstanceId() < 0)
            return true;
        if (getCommandId() < 0)
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
                case 1: _MapInstanceId = _v_.longValue(); break;
                case 2: _CommandId = _v_.intValue(); break;
                case 3: _Param = _v_.binaryValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setMapInstanceId(_r_.getLong(_pn_ + "MapInstanceId"));
        setCommandId(_r_.getInt(_pn_ + "CommandId"));
        setParam(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "Param")));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendLong(_pn_ + "MapInstanceId", getMapInstanceId());
        _s_.appendInt(_pn_ + "CommandId", getCommandId());
        _s_.appendBinary(_pn_ + "Param", getParam());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "MapInstanceId", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "CommandId", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Param", "binary", "", ""));
        return _v_;
    }

// 一个具体的操作。
@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -4392130331074426542L;

    public static final int eReserveCommandId = 2000; // 保留Id给组件内部用。自定义的必须大于这个值。
    public static final int eMoveMmo = 0; // handle=server,client 位置同步命令。
    public static final int eEnterWorld = 2; // handle=client
    public static final int eEnterConfirm = 3; // handle=server
    public static final int eAoiOperate = 4; // handle=client，需要同步的其他任意操作，完全抽象。
    public static final int eAoiEnter = 5; // handle=client
    public static final int eAoiLeave = 6; // handle=client

    private long _MapInstanceId;
    private int _CommandId;
    private Zeze.Net.Binary _Param;

    public long getMapInstanceId() {
        return _MapInstanceId;
    }

    public void setMapInstanceId(long _v_) {
        _MapInstanceId = _v_;
    }

    public int getCommandId() {
        return _CommandId;
    }

    public void setCommandId(int _v_) {
        _CommandId = _v_;
    }

    public Zeze.Net.Binary getParam() {
        return _Param;
    }

    public void setParam(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Param = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Param = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public Data(long _MapInstanceId_, int _CommandId_, Zeze.Net.Binary _Param_) {
        _MapInstanceId = _MapInstanceId_;
        _CommandId = _CommandId_;
        if (_Param_ == null)
            _Param_ = Zeze.Net.Binary.Empty;
        _Param = _Param_;
    }

    @Override
    public void reset() {
        _MapInstanceId = 0;
        _CommandId = 0;
        _Param = Zeze.Net.Binary.Empty;
    }

    @Override
    public metagame.builtin.World.BCommand toBean() {
        var _b_ = new metagame.builtin.World.BCommand();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BCommand)_o_);
    }

    public void assign(BCommand _o_) {
        _MapInstanceId = _o_.getMapInstanceId();
        _CommandId = _o_.getCommandId();
        _Param = _o_.getParam();
    }

    public void assign(BCommand.Data _o_) {
        _MapInstanceId = _o_._MapInstanceId;
        _CommandId = _o_._CommandId;
        _Param = _o_._Param;
    }

    @Override
    public BCommand.Data copy() {
        var _c_ = new BCommand.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BCommand.Data _a_, BCommand.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BCommand.Data clone() {
        return (BCommand.Data)super.clone();
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
        _s_.append("metagame.builtin.World.BCommand: {\n");
        _s_.append(_i1_).append("MapInstanceId=").append(_MapInstanceId).append(",\n");
        _s_.append(_i1_).append("CommandId=").append(_CommandId).append(",\n");
        _s_.append(_i1_).append("Param=").append(_Param).append('\n');
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
            long _x_ = _MapInstanceId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            int _x_ = _CommandId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = _Param;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _MapInstanceId = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _CommandId = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _Param = _o_.ReadBinary(_t_);
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
        if (!(_o_ instanceof BCommand.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BCommand.Data)_o_;
        if (_MapInstanceId != _b_._MapInstanceId)
            return false;
        if (_CommandId != _b_._CommandId)
            return false;
        if (!_Param.equals(_b_._Param))
            return false;
        return true;
    }
}
}
