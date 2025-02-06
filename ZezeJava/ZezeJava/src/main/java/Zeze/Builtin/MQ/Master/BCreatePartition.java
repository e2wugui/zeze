// auto-generated @formatter:off
package Zeze.Builtin.MQ.Master;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BCreatePartition extends Zeze.Transaction.Bean implements BCreatePartitionReadOnly {
    public static final long TYPEID = -6151628565091328456L;

    private String _Topic; // 主题
    private final Zeze.Transaction.Collections.PSet1<Integer> _PartitionIndexes; // 分区索引集合

    private static final java.lang.invoke.VarHandle vh_Topic;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_Topic = _l_.findVarHandle(BCreatePartition.class, "_Topic", String.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public String getTopic() {
        if (!isManaged())
            return _Topic;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Topic;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 1);
        return log != null ? log.stringValue() : _Topic;
    }

    public void setTopic(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Topic = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 1, vh_Topic, _v_));
    }

    public Zeze.Transaction.Collections.PSet1<Integer> getPartitionIndexes() {
        return _PartitionIndexes;
    }

    @Override
    public Zeze.Transaction.Collections.PSet1ReadOnly<Integer> getPartitionIndexesReadOnly() {
        return new Zeze.Transaction.Collections.PSet1ReadOnly<>(_PartitionIndexes);
    }

    @SuppressWarnings("deprecation")
    public BCreatePartition() {
        _Topic = "";
        _PartitionIndexes = new Zeze.Transaction.Collections.PSet1<>(Integer.class);
        _PartitionIndexes.variableId(2);
    }

    @SuppressWarnings("deprecation")
    public BCreatePartition(String _Topic_) {
        if (_Topic_ == null)
            _Topic_ = "";
        _Topic = _Topic_;
        _PartitionIndexes = new Zeze.Transaction.Collections.PSet1<>(Integer.class);
        _PartitionIndexes.variableId(2);
    }

    @Override
    public void reset() {
        setTopic("");
        _PartitionIndexes.clear();
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.MQ.Master.BCreatePartition.Data toData() {
        var _d_ = new Zeze.Builtin.MQ.Master.BCreatePartition.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.MQ.Master.BCreatePartition.Data)_o_);
    }

    public void assign(BCreatePartition.Data _o_) {
        setTopic(_o_._Topic);
        _PartitionIndexes.clear();
        _PartitionIndexes.addAll(_o_._PartitionIndexes);
        _unknown_ = null;
    }

    public void assign(BCreatePartition _o_) {
        setTopic(_o_.getTopic());
        _PartitionIndexes.assign(_o_._PartitionIndexes);
        _unknown_ = _o_._unknown_;
    }

    public BCreatePartition copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BCreatePartition copy() {
        var _c_ = new BCreatePartition();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BCreatePartition _a_, BCreatePartition _b_) {
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
        _s_.append("Zeze.Builtin.MQ.Master.BCreatePartition: {\n");
        _s_.append(_i1_).append("Topic=").append(getTopic()).append(",\n");
        _s_.append(_i1_).append("PartitionIndexes={");
        if (!_PartitionIndexes.isEmpty()) {
            _s_.append('\n');
            for (var _v_ : _PartitionIndexes) {
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
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
            String _x_ = getTopic();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = _PartitionIndexes;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                for (var _v_ : _x_) {
                    _o_.WriteLong(_v_);
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
            setTopic(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = _PartitionIndexes;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadInt(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BCreatePartition))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BCreatePartition)_o_;
        if (!getTopic().equals(_b_.getTopic()))
            return false;
        if (!_PartitionIndexes.equals(_b_._PartitionIndexes))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _PartitionIndexes.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _PartitionIndexes.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        for (var _v_ : _PartitionIndexes) {
            if (_v_ < 0)
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
                case 1: _Topic = _v_.stringValue(); break;
                case 2: _PartitionIndexes.followerApply(_v_); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setTopic(_r_.getString(_pn_ + "Topic"));
        if (getTopic() == null)
            setTopic("");
        Zeze.Serialize.Helper.decodeJsonSet(_PartitionIndexes, Integer.class, _r_.getString(_pn_ + "PartitionIndexes"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "Topic", getTopic());
        _s_.appendString(_pn_ + "PartitionIndexes", Zeze.Serialize.Helper.encodeJson(_PartitionIndexes));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Topic", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "PartitionIndexes", "set", "", "int"));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -6151628565091328456L;

    private String _Topic; // 主题
    private java.util.HashSet<Integer> _PartitionIndexes; // 分区索引集合

    public String getTopic() {
        return _Topic;
    }

    public void setTopic(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Topic = _v_;
    }

    public java.util.HashSet<Integer> getPartitionIndexes() {
        return _PartitionIndexes;
    }

    public void setPartitionIndexes(java.util.HashSet<Integer> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _PartitionIndexes = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Topic = "";
        _PartitionIndexes = new java.util.HashSet<>();
    }

    @SuppressWarnings("deprecation")
    public Data(String _Topic_, java.util.HashSet<Integer> _PartitionIndexes_) {
        if (_Topic_ == null)
            _Topic_ = "";
        _Topic = _Topic_;
        if (_PartitionIndexes_ == null)
            _PartitionIndexes_ = new java.util.HashSet<>();
        _PartitionIndexes = _PartitionIndexes_;
    }

    @Override
    public void reset() {
        _Topic = "";
        _PartitionIndexes.clear();
    }

    @Override
    public Zeze.Builtin.MQ.Master.BCreatePartition toBean() {
        var _b_ = new Zeze.Builtin.MQ.Master.BCreatePartition();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BCreatePartition)_o_);
    }

    public void assign(BCreatePartition _o_) {
        _Topic = _o_.getTopic();
        _PartitionIndexes.clear();
        _PartitionIndexes.addAll(_o_._PartitionIndexes);
    }

    public void assign(BCreatePartition.Data _o_) {
        _Topic = _o_._Topic;
        _PartitionIndexes.clear();
        _PartitionIndexes.addAll(_o_._PartitionIndexes);
    }

    @Override
    public BCreatePartition.Data copy() {
        var _c_ = new BCreatePartition.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BCreatePartition.Data _a_, BCreatePartition.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BCreatePartition.Data clone() {
        return (BCreatePartition.Data)super.clone();
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
        _s_.append("Zeze.Builtin.MQ.Master.BCreatePartition: {\n");
        _s_.append(_i1_).append("Topic=").append(_Topic).append(",\n");
        _s_.append(_i1_).append("PartitionIndexes={");
        if (!_PartitionIndexes.isEmpty()) {
            _s_.append('\n');
            for (var _v_ : _PartitionIndexes) {
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
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
            String _x_ = _Topic;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = _PartitionIndexes;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                for (var _v_ : _x_) {
                    _o_.WriteLong(_v_);
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
            _Topic = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = _PartitionIndexes;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadInt(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
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
        if (!(_o_ instanceof BCreatePartition.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BCreatePartition.Data)_o_;
        if (!_Topic.equals(_b_._Topic))
            return false;
        if (!_PartitionIndexes.equals(_b_._PartitionIndexes))
            return false;
        return true;
    }
}
}
