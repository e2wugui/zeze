// auto-generated @formatter:off
package Zeze.Builtin.MQ;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BSubscribeConsumer extends Zeze.Transaction.Bean implements BSubscribeConsumerReadOnly {
    public static final long TYPEID = 4653262047441464626L;

    private String _Topic; // 主题
    private long _SessionId;

    private static final java.lang.invoke.VarHandle vh_Topic;
    private static final java.lang.invoke.VarHandle vh_SessionId;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_Topic = _l_.findVarHandle(BSubscribeConsumer.class, "_Topic", String.class);
            vh_SessionId = _l_.findVarHandle(BSubscribeConsumer.class, "_SessionId", long.class);
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
    public long getSessionId() {
        if (!isManaged())
            return _SessionId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _SessionId;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _SessionId;
    }

    public void setSessionId(long _v_) {
        if (!isManaged()) {
            _SessionId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 2, vh_SessionId, _v_));
    }

    @SuppressWarnings("deprecation")
    public BSubscribeConsumer() {
        _Topic = "";
    }

    @SuppressWarnings("deprecation")
    public BSubscribeConsumer(String _Topic_, long _SessionId_) {
        if (_Topic_ == null)
            _Topic_ = "";
        _Topic = _Topic_;
        _SessionId = _SessionId_;
    }

    @Override
    public void reset() {
        setTopic("");
        setSessionId(0);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.MQ.BSubscribeConsumer.Data toData() {
        var _d_ = new Zeze.Builtin.MQ.BSubscribeConsumer.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.MQ.BSubscribeConsumer.Data)_o_);
    }

    public void assign(BSubscribeConsumer.Data _o_) {
        setTopic(_o_._Topic);
        setSessionId(_o_._SessionId);
        _unknown_ = null;
    }

    public void assign(BSubscribeConsumer _o_) {
        setTopic(_o_.getTopic());
        setSessionId(_o_.getSessionId());
        _unknown_ = _o_._unknown_;
    }

    public BSubscribeConsumer copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BSubscribeConsumer copy() {
        var _c_ = new BSubscribeConsumer();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BSubscribeConsumer _a_, BSubscribeConsumer _b_) {
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
        _s_.append("Zeze.Builtin.MQ.BSubscribeConsumer: {\n");
        _s_.append(_i1_).append("Topic=").append(getTopic()).append(",\n");
        _s_.append(_i1_).append("SessionId=").append(getSessionId()).append('\n');
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
            long _x_ = getSessionId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
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
            setTopic(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setSessionId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BSubscribeConsumer))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BSubscribeConsumer)_o_;
        if (!getTopic().equals(_b_.getTopic()))
            return false;
        if (getSessionId() != _b_.getSessionId())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getSessionId() < 0)
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
                case 2: _SessionId = _v_.longValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setTopic(_r_.getString(_pn_ + "Topic"));
        if (getTopic() == null)
            setTopic("");
        setSessionId(_r_.getLong(_pn_ + "SessionId"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "Topic", getTopic());
        _s_.appendLong(_pn_ + "SessionId", getSessionId());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Topic", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "SessionId", "long", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 4653262047441464626L;

    private String _Topic; // 主题
    private long _SessionId;

    public String getTopic() {
        return _Topic;
    }

    public void setTopic(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Topic = _v_;
    }

    public long getSessionId() {
        return _SessionId;
    }

    public void setSessionId(long _v_) {
        _SessionId = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Topic = "";
    }

    @SuppressWarnings("deprecation")
    public Data(String _Topic_, long _SessionId_) {
        if (_Topic_ == null)
            _Topic_ = "";
        _Topic = _Topic_;
        _SessionId = _SessionId_;
    }

    @Override
    public void reset() {
        _Topic = "";
        _SessionId = 0;
    }

    @Override
    public Zeze.Builtin.MQ.BSubscribeConsumer toBean() {
        var _b_ = new Zeze.Builtin.MQ.BSubscribeConsumer();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BSubscribeConsumer)_o_);
    }

    public void assign(BSubscribeConsumer _o_) {
        _Topic = _o_.getTopic();
        _SessionId = _o_.getSessionId();
    }

    public void assign(BSubscribeConsumer.Data _o_) {
        _Topic = _o_._Topic;
        _SessionId = _o_._SessionId;
    }

    @Override
    public BSubscribeConsumer.Data copy() {
        var _c_ = new BSubscribeConsumer.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BSubscribeConsumer.Data _a_, BSubscribeConsumer.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BSubscribeConsumer.Data clone() {
        return (BSubscribeConsumer.Data)super.clone();
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
        _s_.append("Zeze.Builtin.MQ.BSubscribeConsumer: {\n");
        _s_.append(_i1_).append("Topic=").append(_Topic).append(",\n");
        _s_.append(_i1_).append("SessionId=").append(_SessionId).append('\n');
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
            long _x_ = _SessionId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
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
            _Topic = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _SessionId = _o_.ReadLong(_t_);
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
        if (!(_o_ instanceof BSubscribeConsumer.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BSubscribeConsumer.Data)_o_;
        if (!_Topic.equals(_b_._Topic))
            return false;
        if (_SessionId != _b_._SessionId)
            return false;
        return true;
    }
}
}
