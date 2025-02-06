// auto-generated @formatter:off
package Zeze.Builtin.MQ;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BSendMessage extends Zeze.Transaction.Bean implements BSendMessageReadOnly {
    public static final long TYPEID = -2718288657481798989L;

    private String _Topic; // 主题，用户不用填写
    private int _PartitionIndex; // 分区索引，用户不用填写
    private final Zeze.Transaction.Collections.CollOne<Zeze.Builtin.MQ.BMessage> _Message; // 消息内容

    private static final java.lang.invoke.VarHandle vh_Topic;
    private static final java.lang.invoke.VarHandle vh_PartitionIndex;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_Topic = _l_.findVarHandle(BSendMessage.class, "_Topic", String.class);
            vh_PartitionIndex = _l_.findVarHandle(BSendMessage.class, "_PartitionIndex", int.class);
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

    @Override
    public int getPartitionIndex() {
        if (!isManaged())
            return _PartitionIndex;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _PartitionIndex;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _PartitionIndex;
    }

    public void setPartitionIndex(int _v_) {
        if (!isManaged()) {
            _PartitionIndex = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 2, vh_PartitionIndex, _v_));
    }

    public Zeze.Builtin.MQ.BMessage getMessage() {
        return _Message.getValue();
    }

    public void setMessage(Zeze.Builtin.MQ.BMessage _v_) {
        _Message.setValue(_v_);
    }

    @Override
    public Zeze.Builtin.MQ.BMessageReadOnly getMessageReadOnly() {
        return _Message.getValue();
    }

    @SuppressWarnings("deprecation")
    public BSendMessage() {
        _Topic = "";
        _Message = new Zeze.Transaction.Collections.CollOne<>(new Zeze.Builtin.MQ.BMessage(), Zeze.Builtin.MQ.BMessage.class);
        _Message.variableId(3);
    }

    @SuppressWarnings("deprecation")
    public BSendMessage(String _Topic_, int _PartitionIndex_) {
        if (_Topic_ == null)
            _Topic_ = "";
        _Topic = _Topic_;
        _PartitionIndex = _PartitionIndex_;
        _Message = new Zeze.Transaction.Collections.CollOne<>(new Zeze.Builtin.MQ.BMessage(), Zeze.Builtin.MQ.BMessage.class);
        _Message.variableId(3);
    }

    @Override
    public void reset() {
        setTopic("");
        setPartitionIndex(0);
        _Message.reset();
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.MQ.BSendMessage.Data toData() {
        var _d_ = new Zeze.Builtin.MQ.BSendMessage.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.MQ.BSendMessage.Data)_o_);
    }

    public void assign(BSendMessage.Data _o_) {
        setTopic(_o_._Topic);
        setPartitionIndex(_o_._PartitionIndex);
        var _d__Message = new Zeze.Builtin.MQ.BMessage();
        _d__Message.assign(_o_._Message);
        _Message.setValue(_d__Message);
        _unknown_ = null;
    }

    public void assign(BSendMessage _o_) {
        setTopic(_o_.getTopic());
        setPartitionIndex(_o_.getPartitionIndex());
        _Message.assign(_o_._Message);
        _unknown_ = _o_._unknown_;
    }

    public BSendMessage copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BSendMessage copy() {
        var _c_ = new BSendMessage();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BSendMessage _a_, BSendMessage _b_) {
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
        _s_.append("Zeze.Builtin.MQ.BSendMessage: {\n");
        _s_.append(_i1_).append("Topic=").append(getTopic()).append(",\n");
        _s_.append(_i1_).append("PartitionIndex=").append(getPartitionIndex()).append(",\n");
        _s_.append(_i1_).append("Message=");
        _Message.buildString(_s_, _l_ + 8);
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
            int _x_ = getPartitionIndex();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 3, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _Message.encode(_o_);
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
            setPartitionIndex(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _o_.ReadBean(_Message, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BSendMessage))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BSendMessage)_o_;
        if (!getTopic().equals(_b_.getTopic()))
            return false;
        if (getPartitionIndex() != _b_.getPartitionIndex())
            return false;
        if (!_Message.equals(_b_._Message))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _Message.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _Message.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getPartitionIndex() < 0)
            return true;
        if (_Message.negativeCheck())
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
                case 2: _PartitionIndex = _v_.intValue(); break;
                case 3: _Message.followerApply(_v_); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setTopic(_r_.getString(_pn_ + "Topic"));
        if (getTopic() == null)
            setTopic("");
        setPartitionIndex(_r_.getInt(_pn_ + "PartitionIndex"));
        _p_.add("Message");
        _Message.decodeResultSet(_p_, _r_);
        _p_.remove(_p_.size() - 1);
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "Topic", getTopic());
        _s_.appendInt(_pn_ + "PartitionIndex", getPartitionIndex());
        _p_.add("Message");
        _Message.encodeSQLStatement(_p_, _s_);
        _p_.remove(_p_.size() - 1);
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Topic", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "PartitionIndex", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Message", "Zeze.Builtin.MQ.BMessage", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -2718288657481798989L;

    private String _Topic; // 主题，用户不用填写
    private int _PartitionIndex; // 分区索引，用户不用填写
    private Zeze.Builtin.MQ.BMessage.Data _Message; // 消息内容

    public String getTopic() {
        return _Topic;
    }

    public void setTopic(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Topic = _v_;
    }

    public int getPartitionIndex() {
        return _PartitionIndex;
    }

    public void setPartitionIndex(int _v_) {
        _PartitionIndex = _v_;
    }

    public Zeze.Builtin.MQ.BMessage.Data getMessage() {
        return _Message;
    }

    public void setMessage(Zeze.Builtin.MQ.BMessage.Data _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Message = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Topic = "";
        _Message = new Zeze.Builtin.MQ.BMessage.Data();
    }

    @SuppressWarnings("deprecation")
    public Data(String _Topic_, int _PartitionIndex_, Zeze.Builtin.MQ.BMessage.Data _Message_) {
        if (_Topic_ == null)
            _Topic_ = "";
        _Topic = _Topic_;
        _PartitionIndex = _PartitionIndex_;
        if (_Message_ == null)
            _Message_ = new Zeze.Builtin.MQ.BMessage.Data();
        _Message = _Message_;
    }

    @Override
    public void reset() {
        _Topic = "";
        _PartitionIndex = 0;
        _Message.reset();
    }

    @Override
    public Zeze.Builtin.MQ.BSendMessage toBean() {
        var _b_ = new Zeze.Builtin.MQ.BSendMessage();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BSendMessage)_o_);
    }

    public void assign(BSendMessage _o_) {
        _Topic = _o_.getTopic();
        _PartitionIndex = _o_.getPartitionIndex();
        _Message.assign(_o_._Message.getValue());
    }

    public void assign(BSendMessage.Data _o_) {
        _Topic = _o_._Topic;
        _PartitionIndex = _o_._PartitionIndex;
        _Message.assign(_o_._Message);
    }

    @Override
    public BSendMessage.Data copy() {
        var _c_ = new BSendMessage.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BSendMessage.Data _a_, BSendMessage.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BSendMessage.Data clone() {
        return (BSendMessage.Data)super.clone();
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
        _s_.append("Zeze.Builtin.MQ.BSendMessage: {\n");
        _s_.append(_i1_).append("Topic=").append(_Topic).append(",\n");
        _s_.append(_i1_).append("PartitionIndex=").append(_PartitionIndex).append(",\n");
        _s_.append(_i1_).append("Message=");
        _Message.buildString(_s_, _l_ + 8);
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
            int _x_ = _PartitionIndex;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 3, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _Message.encode(_o_);
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
            _PartitionIndex = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _o_.ReadBean(_Message, _t_);
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
        if (!(_o_ instanceof BSendMessage.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BSendMessage.Data)_o_;
        if (!_Topic.equals(_b_._Topic))
            return false;
        if (_PartitionIndex != _b_._PartitionIndex)
            return false;
        if (!_Message.equals(_b_._Message))
            return false;
        return true;
    }
}
}
