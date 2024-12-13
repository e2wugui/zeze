// auto-generated @formatter:off
package Zeze.Builtin.MQ;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BMessage extends Zeze.Transaction.Bean implements BMessageReadOnly {
    public static final long TYPEID = -6688505362992437637L;

    private int _PartitionIndex; // 用户不用填写
    private final Zeze.Transaction.Collections.PMap1<String, String> _Properties; // 属性，用户自定义
    private Zeze.Net.Binary _Body; // 消息体，用户自定义
    private long _Timestamp;

    private static final java.lang.invoke.VarHandle vh_PartitionIndex;
    private static final java.lang.invoke.VarHandle vh_Body;
    private static final java.lang.invoke.VarHandle vh_Timestamp;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_PartitionIndex = _l_.findVarHandle(BMessage.class, "_PartitionIndex", int.class);
            vh_Body = _l_.findVarHandle(BMessage.class, "_Body", Zeze.Net.Binary.class);
            vh_Timestamp = _l_.findVarHandle(BMessage.class, "_Timestamp", long.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public int getPartitionIndex() {
        if (!isManaged())
            return _PartitionIndex;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _PartitionIndex;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _PartitionIndex;
    }

    public void setPartitionIndex(int _v_) {
        if (!isManaged()) {
            _PartitionIndex = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 1, vh_PartitionIndex, _v_));
    }

    public Zeze.Transaction.Collections.PMap1<String, String> getProperties() {
        return _Properties;
    }

    @Override
    public Zeze.Transaction.Collections.PMap1ReadOnly<String, String> getPropertiesReadOnly() {
        return new Zeze.Transaction.Collections.PMap1ReadOnly<>(_Properties);
    }

    @Override
    public Zeze.Net.Binary getBody() {
        if (!isManaged())
            return _Body;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Body;
        var log = (Zeze.Transaction.Logs.LogBinary)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _Body;
    }

    public void setBody(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Body = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBinary(this, 3, vh_Body, _v_));
    }

    @Override
    public long getTimestamp() {
        if (!isManaged())
            return _Timestamp;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Timestamp;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 4);
        return log != null ? log.value : _Timestamp;
    }

    public void setTimestamp(long _v_) {
        if (!isManaged()) {
            _Timestamp = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 4, vh_Timestamp, _v_));
    }

    @SuppressWarnings("deprecation")
    public BMessage() {
        _Properties = new Zeze.Transaction.Collections.PMap1<>(String.class, String.class);
        _Properties.variableId(2);
        _Body = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BMessage(int _PartitionIndex_, Zeze.Net.Binary _Body_, long _Timestamp_) {
        _PartitionIndex = _PartitionIndex_;
        _Properties = new Zeze.Transaction.Collections.PMap1<>(String.class, String.class);
        _Properties.variableId(2);
        if (_Body_ == null)
            _Body_ = Zeze.Net.Binary.Empty;
        _Body = _Body_;
        _Timestamp = _Timestamp_;
    }

    @Override
    public void reset() {
        setPartitionIndex(0);
        _Properties.clear();
        setBody(Zeze.Net.Binary.Empty);
        setTimestamp(0);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.MQ.BMessage.Data toData() {
        var _d_ = new Zeze.Builtin.MQ.BMessage.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.MQ.BMessage.Data)_o_);
    }

    public void assign(BMessage.Data _o_) {
        setPartitionIndex(_o_._PartitionIndex);
        _Properties.clear();
        _Properties.putAll(_o_._Properties);
        setBody(_o_._Body);
        setTimestamp(_o_._Timestamp);
        _unknown_ = null;
    }

    public void assign(BMessage _o_) {
        setPartitionIndex(_o_.getPartitionIndex());
        _Properties.assign(_o_._Properties);
        setBody(_o_.getBody());
        setTimestamp(_o_.getTimestamp());
        _unknown_ = _o_._unknown_;
    }

    public BMessage copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BMessage copy() {
        var _c_ = new BMessage();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BMessage _a_, BMessage _b_) {
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
        _s_.append("Zeze.Builtin.MQ.BMessage: {\n");
        _s_.append(_i1_).append("PartitionIndex=").append(getPartitionIndex()).append(",\n");
        _s_.append(_i1_).append("Properties={");
        if (!_Properties.isEmpty()) {
            _s_.append('\n');
            for (var _e_ : _Properties.entrySet()) {
                _s_.append(_i2_).append("Key=").append(_e_.getKey()).append(",\n");
                _s_.append(_i2_).append("Value=").append(_e_.getValue()).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("Body=").append(getBody()).append(",\n");
        _s_.append(_i1_).append("Timestamp=").append(getTimestamp()).append('\n');
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
            int _x_ = getPartitionIndex();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = _Properties;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BYTES, ByteBuffer.BYTES);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteString(_e_.getKey());
                    _o_.WriteString(_e_.getValue());
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = getBody();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            long _x_ = getTimestamp();
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
            setPartitionIndex(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = _Properties;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadString(_s_);
                    var _v_ = _o_.ReadString(_t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setBody(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setTimestamp(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BMessage))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BMessage)_o_;
        if (getPartitionIndex() != _b_.getPartitionIndex())
            return false;
        if (!_Properties.equals(_b_._Properties))
            return false;
        if (!getBody().equals(_b_.getBody()))
            return false;
        if (getTimestamp() != _b_.getTimestamp())
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _Properties.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _Properties.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getPartitionIndex() < 0)
            return true;
        if (getTimestamp() < 0)
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
                case 1: _PartitionIndex = _v_.intValue(); break;
                case 2: _Properties.followerApply(_v_); break;
                case 3: _Body = _v_.binaryValue(); break;
                case 4: _Timestamp = _v_.longValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setPartitionIndex(_r_.getInt(_pn_ + "PartitionIndex"));
        Zeze.Serialize.Helper.decodeJsonMap(this, "Properties", _Properties, _r_.getString(_pn_ + "Properties"));
        setBody(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "Body")));
        setTimestamp(_r_.getLong(_pn_ + "Timestamp"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendInt(_pn_ + "PartitionIndex", getPartitionIndex());
        _s_.appendString(_pn_ + "Properties", Zeze.Serialize.Helper.encodeJson(_Properties));
        _s_.appendBinary(_pn_ + "Body", getBody());
        _s_.appendLong(_pn_ + "Timestamp", getTimestamp());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "PartitionIndex", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Properties", "map", "string", "string"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Body", "binary", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "Timestamp", "long", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -6688505362992437637L;

    private int _PartitionIndex; // 用户不用填写
    private java.util.HashMap<String, String> _Properties; // 属性，用户自定义
    private Zeze.Net.Binary _Body; // 消息体，用户自定义
    private long _Timestamp;

    public int getPartitionIndex() {
        return _PartitionIndex;
    }

    public void setPartitionIndex(int _v_) {
        _PartitionIndex = _v_;
    }

    public java.util.HashMap<String, String> getProperties() {
        return _Properties;
    }

    public void setProperties(java.util.HashMap<String, String> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Properties = _v_;
    }

    public Zeze.Net.Binary getBody() {
        return _Body;
    }

    public void setBody(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Body = _v_;
    }

    public long getTimestamp() {
        return _Timestamp;
    }

    public void setTimestamp(long _v_) {
        _Timestamp = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Properties = new java.util.HashMap<>();
        _Body = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public Data(int _PartitionIndex_, java.util.HashMap<String, String> _Properties_, Zeze.Net.Binary _Body_, long _Timestamp_) {
        _PartitionIndex = _PartitionIndex_;
        if (_Properties_ == null)
            _Properties_ = new java.util.HashMap<>();
        _Properties = _Properties_;
        if (_Body_ == null)
            _Body_ = Zeze.Net.Binary.Empty;
        _Body = _Body_;
        _Timestamp = _Timestamp_;
    }

    @Override
    public void reset() {
        _PartitionIndex = 0;
        _Properties.clear();
        _Body = Zeze.Net.Binary.Empty;
        _Timestamp = 0;
    }

    @Override
    public Zeze.Builtin.MQ.BMessage toBean() {
        var _b_ = new Zeze.Builtin.MQ.BMessage();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BMessage)_o_);
    }

    public void assign(BMessage _o_) {
        _PartitionIndex = _o_.getPartitionIndex();
        _Properties.clear();
        _Properties.putAll(_o_._Properties);
        _Body = _o_.getBody();
        _Timestamp = _o_.getTimestamp();
    }

    public void assign(BMessage.Data _o_) {
        _PartitionIndex = _o_._PartitionIndex;
        _Properties.clear();
        _Properties.putAll(_o_._Properties);
        _Body = _o_._Body;
        _Timestamp = _o_._Timestamp;
    }

    @Override
    public BMessage.Data copy() {
        var _c_ = new BMessage.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BMessage.Data _a_, BMessage.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BMessage.Data clone() {
        return (BMessage.Data)super.clone();
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
        _s_.append("Zeze.Builtin.MQ.BMessage: {\n");
        _s_.append(_i1_).append("PartitionIndex=").append(_PartitionIndex).append(",\n");
        _s_.append(_i1_).append("Properties={");
        if (!_Properties.isEmpty()) {
            _s_.append('\n');
            for (var _e_ : _Properties.entrySet()) {
                _s_.append(_i2_).append("Key=").append(_e_.getKey()).append(",\n");
                _s_.append(_i2_).append("Value=").append(_e_.getValue()).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("Body=").append(_Body).append(",\n");
        _s_.append(_i1_).append("Timestamp=").append(_Timestamp).append('\n');
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
            int _x_ = _PartitionIndex;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = _Properties;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BYTES, ByteBuffer.BYTES);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteString(_e_.getKey());
                    _o_.WriteString(_e_.getValue());
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _Body;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            long _x_ = _Timestamp;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _PartitionIndex = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = _Properties;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadString(_s_);
                    var _v_ = _o_.ReadString(_t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _Body = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _Timestamp = _o_.ReadLong(_t_);
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
        if (!(_o_ instanceof BMessage.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BMessage.Data)_o_;
        if (_PartitionIndex != _b_._PartitionIndex)
            return false;
        if (!_Properties.equals(_b_._Properties))
            return false;
        if (!_Body.equals(_b_._Body))
            return false;
        if (_Timestamp != _b_._Timestamp)
            return false;
        return true;
    }
}
}
