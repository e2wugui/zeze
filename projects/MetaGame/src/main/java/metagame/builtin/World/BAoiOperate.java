// auto-generated @formatter:off
package metagame.builtin.World;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

// 一个具体的操作。
@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BAoiOperate extends Zeze.Transaction.Bean implements BAoiOperateReadOnly {
    public static final long TYPEID = -1047237435540817782L;

    private int _OperateId;
    private Zeze.Net.Binary _Param;
    private final Zeze.Transaction.Collections.PMap2<Long, metagame.builtin.World.BAoiOperate> _Children;

    private transient Object __zeze_map_key__;

    @Override
    public Object mapKey() {
        return __zeze_map_key__;
    }

    @Override
    public void mapKey(Object _v_) {
        __zeze_map_key__ = _v_;
    }

    private static final java.lang.invoke.VarHandle vh_OperateId;
    private static final java.lang.invoke.VarHandle vh_Param;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_OperateId = _l_.findVarHandle(BAoiOperate.class, "_OperateId", int.class);
            vh_Param = _l_.findVarHandle(BAoiOperate.class, "_Param", Zeze.Net.Binary.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public int getOperateId() {
        if (!isManaged())
            return _OperateId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _OperateId;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _OperateId;
    }

    public void setOperateId(int _v_) {
        if (!isManaged()) {
            _OperateId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 1, vh_OperateId, _v_));
    }

    @Override
    public Zeze.Net.Binary getParam() {
        if (!isManaged())
            return _Param;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Param;
        var log = (Zeze.Transaction.Logs.LogBinary)_t_.getLog(objectId() + 2);
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
        _t_.putLog(new Zeze.Transaction.Logs.LogBinary(this, 2, vh_Param, _v_));
    }

    public Zeze.Transaction.Collections.PMap2<Long, metagame.builtin.World.BAoiOperate> getChildren() {
        return _Children;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<Long, metagame.builtin.World.BAoiOperate, metagame.builtin.World.BAoiOperateReadOnly> getChildrenReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_Children);
    }

    @SuppressWarnings("deprecation")
    public BAoiOperate() {
        _Param = Zeze.Net.Binary.Empty;
        _Children = new Zeze.Transaction.Collections.PMap2<>(Long.class, metagame.builtin.World.BAoiOperate.class);
        _Children.variableId(3);
    }

    @SuppressWarnings("deprecation")
    public BAoiOperate(int _OperateId_, Zeze.Net.Binary _Param_) {
        _OperateId = _OperateId_;
        if (_Param_ == null)
            _Param_ = Zeze.Net.Binary.Empty;
        _Param = _Param_;
        _Children = new Zeze.Transaction.Collections.PMap2<>(Long.class, metagame.builtin.World.BAoiOperate.class);
        _Children.variableId(3);
    }

    @Override
    public void reset() {
        setOperateId(0);
        setParam(Zeze.Net.Binary.Empty);
        _Children.clear();
        _unknown_ = null;
    }

    @Override
    public metagame.builtin.World.BAoiOperate.Data toData() {
        var _d_ = new metagame.builtin.World.BAoiOperate.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((metagame.builtin.World.BAoiOperate.Data)_o_);
    }

    public void assign(BAoiOperate.Data _o_) {
        setOperateId(_o_._OperateId);
        setParam(_o_._Param);
        _Children.clear();
        for (var _e_ : _o_._Children.entrySet()) {
            var _v_ = new metagame.builtin.World.BAoiOperate();
            _v_.assign(_e_.getValue());
            _Children.put(_e_.getKey(), _v_);
        }
        _unknown_ = null;
    }

    public void assign(BAoiOperate _o_) {
        setOperateId(_o_.getOperateId());
        setParam(_o_.getParam());
        _Children.clear();
        for (var _e_ : _o_._Children.entrySet())
            _Children.put(_e_.getKey(), _e_.getValue().copy());
        _unknown_ = _o_._unknown_;
    }

    public BAoiOperate copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BAoiOperate copy() {
        var _c_ = new BAoiOperate();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BAoiOperate _a_, BAoiOperate _b_) {
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
        _s_.append("metagame.builtin.World.BAoiOperate: {\n");
        _s_.append(_i1_).append("OperateId=").append(getOperateId()).append(",\n");
        _s_.append(_i1_).append("Param=").append(getParam()).append(",\n");
        _s_.append(_i1_).append("Children={");
        if (!_Children.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _e_ : _Children.entrySet()) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_Children.size()).append("]\n");
                    break;
                }
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
            int _x_ = getOperateId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = getParam();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            var _x_ = _Children;
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
        _o_.writeAllUnknownFields(_i_, _ui_, _u_);
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        ByteBuffer _u_ = null;
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setOperateId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setParam(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            var _x_ = _Children;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadLong(_s_);
                    var _v_ = _o_.ReadBean(new metagame.builtin.World.BAoiOperate(), _t_);
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
        if (!(_o_ instanceof BAoiOperate))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BAoiOperate)_o_;
        if (getOperateId() != _b_.getOperateId())
            return false;
        if (!getParam().equals(_b_.getParam()))
            return false;
        if (!_Children.equals(_b_._Children))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _Children.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _Children.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getOperateId() < 0)
            return true;
        for (var _v_ : _Children.values()) {
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
                case 1: _OperateId = _v_.intValue(); break;
                case 2: _Param = _v_.binaryValue(); break;
                case 3: _Children.followerApply(_v_); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setOperateId(_r_.getInt(_pn_ + "OperateId"));
        setParam(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "Param")));
        Zeze.Serialize.Helper.decodeJsonMap(this, "Children", _Children, _r_.getString(_pn_ + "Children"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendInt(_pn_ + "OperateId", getOperateId());
        _s_.appendBinary(_pn_ + "Param", getParam());
        _s_.appendString(_pn_ + "Children", Zeze.Serialize.Helper.encodeJson(_Children));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "OperateId", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Param", "binary", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Children", "map", "long", "metagame.builtin.World.BAoiOperate"));
        return _v_;
    }

// 一个具体的操作。
@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -1047237435540817782L;

    private int _OperateId;
    private Zeze.Net.Binary _Param;
    private java.util.HashMap<Long, metagame.builtin.World.BAoiOperate.Data> _Children;

    public int getOperateId() {
        return _OperateId;
    }

    public void setOperateId(int _v_) {
        _OperateId = _v_;
    }

    public Zeze.Net.Binary getParam() {
        return _Param;
    }

    public void setParam(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Param = _v_;
    }

    public java.util.HashMap<Long, metagame.builtin.World.BAoiOperate.Data> getChildren() {
        return _Children;
    }

    public void setChildren(java.util.HashMap<Long, metagame.builtin.World.BAoiOperate.Data> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Children = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Param = Zeze.Net.Binary.Empty;
        _Children = new java.util.HashMap<>();
    }

    @SuppressWarnings("deprecation")
    public Data(int _OperateId_, Zeze.Net.Binary _Param_, java.util.HashMap<Long, metagame.builtin.World.BAoiOperate.Data> _Children_) {
        _OperateId = _OperateId_;
        if (_Param_ == null)
            _Param_ = Zeze.Net.Binary.Empty;
        _Param = _Param_;
        if (_Children_ == null)
            _Children_ = new java.util.HashMap<>();
        _Children = _Children_;
    }

    @Override
    public void reset() {
        _OperateId = 0;
        _Param = Zeze.Net.Binary.Empty;
        _Children.clear();
    }

    @Override
    public metagame.builtin.World.BAoiOperate toBean() {
        var _b_ = new metagame.builtin.World.BAoiOperate();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BAoiOperate)_o_);
    }

    public void assign(BAoiOperate _o_) {
        _OperateId = _o_.getOperateId();
        _Param = _o_.getParam();
        _Children.clear();
        for (var _e_ : _o_._Children.entrySet()) {
            var _v_ = new metagame.builtin.World.BAoiOperate.Data();
            _v_.assign(_e_.getValue());
            _Children.put(_e_.getKey(), _v_);
        }
    }

    public void assign(BAoiOperate.Data _o_) {
        _OperateId = _o_._OperateId;
        _Param = _o_._Param;
        _Children.clear();
        for (var _e_ : _o_._Children.entrySet())
            _Children.put(_e_.getKey(), _e_.getValue().copy());
    }

    @Override
    public BAoiOperate.Data copy() {
        var _c_ = new BAoiOperate.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BAoiOperate.Data _a_, BAoiOperate.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BAoiOperate.Data clone() {
        return (BAoiOperate.Data)super.clone();
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
        _s_.append("metagame.builtin.World.BAoiOperate: {\n");
        _s_.append(_i1_).append("OperateId=").append(_OperateId).append(",\n");
        _s_.append(_i1_).append("Param=").append(_Param).append(",\n");
        _s_.append(_i1_).append("Children={");
        if (!_Children.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _e_ : _Children.entrySet()) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_Children.size()).append("]\n");
                    break;
                }
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
            int _x_ = _OperateId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = _Param;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            var _x_ = _Children;
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
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _OperateId = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _Param = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            var _x_ = _Children;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadLong(_s_);
                    var _v_ = _o_.ReadBean(new metagame.builtin.World.BAoiOperate.Data(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
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
        if (!(_o_ instanceof BAoiOperate.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BAoiOperate.Data)_o_;
        if (_OperateId != _b_._OperateId)
            return false;
        if (!_Param.equals(_b_._Param))
            return false;
        if (!_Children.equals(_b_._Children))
            return false;
        return true;
    }
}
}
