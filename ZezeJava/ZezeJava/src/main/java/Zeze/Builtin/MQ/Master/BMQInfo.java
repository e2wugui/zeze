// auto-generated @formatter:off
package Zeze.Builtin.MQ.Master;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BMQInfo extends Zeze.Transaction.Bean implements BMQInfoReadOnly {
    public static final long TYPEID = 1467226550828193896L;

    private String _Topic;
    private int _Partition;
    private final Zeze.Transaction.Collections.CollOne<Zeze.Builtin.MQ.BOptions> _Options;

    private static final java.lang.invoke.VarHandle vh_Topic;
    private static final java.lang.invoke.VarHandle vh_Partition;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_Topic = _l_.findVarHandle(BMQInfo.class, "_Topic", String.class);
            vh_Partition = _l_.findVarHandle(BMQInfo.class, "_Partition", int.class);
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
        return log != null ? log.value : _Topic;
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

    @Override
    public int getPartition() {
        if (!isManaged())
            return _Partition;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Partition;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _Partition;
    }

    public void setPartition(int _v_) {
        if (!isManaged()) {
            _Partition = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 2, vh_Partition, _v_));
    }

    public Zeze.Builtin.MQ.BOptions getOptions() {
        return _Options.getValue();
    }

    public void setOptions(Zeze.Builtin.MQ.BOptions _v_) {
        _Options.setValue(_v_);
    }

    @Override
    public Zeze.Builtin.MQ.BOptionsReadOnly getOptionsReadOnly() {
        return _Options.getValue();
    }

    @SuppressWarnings("deprecation")
    public BMQInfo() {
        _Topic = "";
        _Options = new Zeze.Transaction.Collections.CollOne<>(new Zeze.Builtin.MQ.BOptions(), Zeze.Builtin.MQ.BOptions.class);
        _Options.variableId(3);
    }

    @SuppressWarnings("deprecation")
    public BMQInfo(String _Topic_, int _Partition_) {
        if (_Topic_ == null)
            _Topic_ = "";
        _Topic = _Topic_;
        _Partition = _Partition_;
        _Options = new Zeze.Transaction.Collections.CollOne<>(new Zeze.Builtin.MQ.BOptions(), Zeze.Builtin.MQ.BOptions.class);
        _Options.variableId(3);
    }

    @Override
    public void reset() {
        setTopic("");
        setPartition(0);
        _Options.reset();
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.MQ.Master.BMQInfo.Data toData() {
        var _d_ = new Zeze.Builtin.MQ.Master.BMQInfo.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.MQ.Master.BMQInfo.Data)_o_);
    }

    public void assign(BMQInfo.Data _o_) {
        setTopic(_o_._Topic);
        setPartition(_o_._Partition);
        var _d__Options = new Zeze.Builtin.MQ.BOptions();
        _d__Options.assign(_o_._Options);
        _Options.setValue(_d__Options);
        _unknown_ = null;
    }

    public void assign(BMQInfo _o_) {
        setTopic(_o_.getTopic());
        setPartition(_o_.getPartition());
        _Options.assign(_o_._Options);
        _unknown_ = _o_._unknown_;
    }

    public BMQInfo copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BMQInfo copy() {
        var _c_ = new BMQInfo();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BMQInfo _a_, BMQInfo _b_) {
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
        _s_.append("Zeze.Builtin.MQ.Master.BMQInfo: {\n");
        _s_.append(_i1_).append("Topic=").append(getTopic()).append(",\n");
        _s_.append(_i1_).append("Partition=").append(getPartition()).append(",\n");
        _s_.append(_i1_).append("Options=");
        _Options.buildString(_s_, _l_ + 8);
        _s_.append('\n');
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
            int _x_ = getPartition();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 3, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _Options.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
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
            setPartition(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _o_.ReadBean(_Options, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BMQInfo))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BMQInfo)_o_;
        if (!getTopic().equals(_b_.getTopic()))
            return false;
        if (getPartition() != _b_.getPartition())
            return false;
        if (!_Options.equals(_b_._Options))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _Options.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _Options.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getPartition() < 0)
            return true;
        if (_Options.negativeCheck())
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
                case 1: _Topic = _v_.stringValue(); break;
                case 2: _Partition = _v_.intValue(); break;
                case 3: _Options.followerApply(_v_); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setTopic(_r_.getString(_pn_ + "Topic"));
        if (getTopic() == null)
            setTopic("");
        setPartition(_r_.getInt(_pn_ + "Partition"));
        _p_.add("Options");
        _Options.decodeResultSet(_p_, _r_);
        _p_.remove(_p_.size() - 1);
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "Topic", getTopic());
        _s_.appendInt(_pn_ + "Partition", getPartition());
        _p_.add("Options");
        _Options.encodeSQLStatement(_p_, _s_);
        _p_.remove(_p_.size() - 1);
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Topic", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Partition", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Options", "Zeze.Builtin.MQ.BOptions", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 1467226550828193896L;

    private String _Topic;
    private int _Partition;
    private Zeze.Builtin.MQ.BOptions.Data _Options;

    public String getTopic() {
        return _Topic;
    }

    public void setTopic(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Topic = _v_;
    }

    public int getPartition() {
        return _Partition;
    }

    public void setPartition(int _v_) {
        _Partition = _v_;
    }

    public Zeze.Builtin.MQ.BOptions.Data getOptions() {
        return _Options;
    }

    public void setOptions(Zeze.Builtin.MQ.BOptions.Data _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Options = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Topic = "";
        _Options = new Zeze.Builtin.MQ.BOptions.Data();
    }

    @SuppressWarnings("deprecation")
    public Data(String _Topic_, int _Partition_, Zeze.Builtin.MQ.BOptions.Data _Options_) {
        if (_Topic_ == null)
            _Topic_ = "";
        _Topic = _Topic_;
        _Partition = _Partition_;
        if (_Options_ == null)
            _Options_ = new Zeze.Builtin.MQ.BOptions.Data();
        _Options = _Options_;
    }

    @Override
    public void reset() {
        _Topic = "";
        _Partition = 0;
        _Options.reset();
    }

    @Override
    public Zeze.Builtin.MQ.Master.BMQInfo toBean() {
        var _b_ = new Zeze.Builtin.MQ.Master.BMQInfo();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BMQInfo)_o_);
    }

    public void assign(BMQInfo _o_) {
        _Topic = _o_.getTopic();
        _Partition = _o_.getPartition();
        _Options.assign(_o_._Options.getValue());
    }

    public void assign(BMQInfo.Data _o_) {
        _Topic = _o_._Topic;
        _Partition = _o_._Partition;
        _Options.assign(_o_._Options);
    }

    @Override
    public BMQInfo.Data copy() {
        var _c_ = new BMQInfo.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BMQInfo.Data _a_, BMQInfo.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BMQInfo.Data clone() {
        return (BMQInfo.Data)super.clone();
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
        _s_.append("Zeze.Builtin.MQ.Master.BMQInfo: {\n");
        _s_.append(_i1_).append("Topic=").append(_Topic).append(",\n");
        _s_.append(_i1_).append("Partition=").append(_Partition).append(",\n");
        _s_.append(_i1_).append("Options=");
        _Options.buildString(_s_, _l_ + 8);
        _s_.append('\n');
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
            int _x_ = _Partition;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 3, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _Options.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
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
            _Partition = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _o_.ReadBean(_Options, _t_);
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
        if (!(_o_ instanceof BMQInfo.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BMQInfo.Data)_o_;
        if (!_Topic.equals(_b_._Topic))
            return false;
        if (_Partition != _b_._Partition)
            return false;
        if (!_Options.equals(_b_._Options))
            return false;
        return true;
    }
}
}
