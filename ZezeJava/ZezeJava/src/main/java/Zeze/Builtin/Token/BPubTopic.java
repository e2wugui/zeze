// auto-generated @formatter:off
package Zeze.Builtin.Token;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BPubTopic extends Zeze.Transaction.Bean implements BPubTopicReadOnly {
    public static final long TYPEID = -1295819062894155861L;

    private String _topic; // 主题
    private Zeze.Net.Binary _content; // 内容
    private boolean _broadcast; // false:只随机通知一个订阅者; true:通知所有订阅者

    @Override
    public String getTopic() {
        if (!isManaged())
            return _topic;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _topic;
        var log = (Log__topic)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _topic;
    }

    public void setTopic(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _topic = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__topic(this, 1, _v_));
    }

    @Override
    public Zeze.Net.Binary getContent() {
        if (!isManaged())
            return _content;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _content;
        var log = (Log__content)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _content;
    }

    public void setContent(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _content = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__content(this, 2, _v_));
    }

    @Override
    public boolean isBroadcast() {
        if (!isManaged())
            return _broadcast;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _broadcast;
        var log = (Log__broadcast)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _broadcast;
    }

    public void setBroadcast(boolean _v_) {
        if (!isManaged()) {
            _broadcast = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__broadcast(this, 3, _v_));
    }

    @SuppressWarnings("deprecation")
    public BPubTopic() {
        _topic = "";
        _content = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BPubTopic(String _topic_, Zeze.Net.Binary _content_, boolean _broadcast_) {
        if (_topic_ == null)
            _topic_ = "";
        _topic = _topic_;
        if (_content_ == null)
            _content_ = Zeze.Net.Binary.Empty;
        _content = _content_;
        _broadcast = _broadcast_;
    }

    @Override
    public void reset() {
        setTopic("");
        setContent(Zeze.Net.Binary.Empty);
        setBroadcast(false);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Token.BPubTopic.Data toData() {
        var _d_ = new Zeze.Builtin.Token.BPubTopic.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Token.BPubTopic.Data)_o_);
    }

    public void assign(BPubTopic.Data _o_) {
        setTopic(_o_._topic);
        setContent(_o_._content);
        setBroadcast(_o_._broadcast);
        _unknown_ = null;
    }

    public void assign(BPubTopic _o_) {
        setTopic(_o_.getTopic());
        setContent(_o_.getContent());
        setBroadcast(_o_.isBroadcast());
        _unknown_ = _o_._unknown_;
    }

    public BPubTopic copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BPubTopic copy() {
        var _c_ = new BPubTopic();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BPubTopic _a_, BPubTopic _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__topic extends Zeze.Transaction.Logs.LogString {
        public Log__topic(BPubTopic _b_, int _i_, String _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BPubTopic)getBelong())._topic = value; }
    }

    private static final class Log__content extends Zeze.Transaction.Logs.LogBinary {
        public Log__content(BPubTopic _b_, int _i_, Zeze.Net.Binary _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BPubTopic)getBelong())._content = value; }
    }

    private static final class Log__broadcast extends Zeze.Transaction.Logs.LogBool {
        public Log__broadcast(BPubTopic _b_, int _i_, boolean _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BPubTopic)getBelong())._broadcast = value; }
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
        _s_.append("Zeze.Builtin.Token.BPubTopic: {\n");
        _s_.append(_i1_).append("topic=").append(getTopic()).append(",\n");
        _s_.append(_i1_).append("content=").append(getContent()).append(",\n");
        _s_.append(_i1_).append("broadcast=").append(isBroadcast()).append('\n');
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
            var _x_ = getContent();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            boolean _x_ = isBroadcast();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
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
            setContent(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setBroadcast(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BPubTopic))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BPubTopic)_o_;
        if (!getTopic().equals(_b_.getTopic()))
            return false;
        if (!getContent().equals(_b_.getContent()))
            return false;
        if (isBroadcast() != _b_.isBroadcast())
            return false;
        return true;
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
                case 1: _topic = _v_.stringValue(); break;
                case 2: _content = _v_.binaryValue(); break;
                case 3: _broadcast = _v_.booleanValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setTopic(_r_.getString(_pn_ + "topic"));
        if (getTopic() == null)
            setTopic("");
        setContent(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "content")));
        setBroadcast(_r_.getBoolean(_pn_ + "broadcast"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "topic", getTopic());
        _s_.appendBinary(_pn_ + "content", getContent());
        _s_.appendBoolean(_pn_ + "broadcast", isBroadcast());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "topic", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "content", "binary", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "broadcast", "bool", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -1295819062894155861L;

    private String _topic; // 主题
    private Zeze.Net.Binary _content; // 内容
    private boolean _broadcast; // false:只随机通知一个订阅者; true:通知所有订阅者

    public String getTopic() {
        return _topic;
    }

    public void setTopic(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _topic = _v_;
    }

    public Zeze.Net.Binary getContent() {
        return _content;
    }

    public void setContent(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _content = _v_;
    }

    public boolean isBroadcast() {
        return _broadcast;
    }

    public void setBroadcast(boolean _v_) {
        _broadcast = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _topic = "";
        _content = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public Data(String _topic_, Zeze.Net.Binary _content_, boolean _broadcast_) {
        if (_topic_ == null)
            _topic_ = "";
        _topic = _topic_;
        if (_content_ == null)
            _content_ = Zeze.Net.Binary.Empty;
        _content = _content_;
        _broadcast = _broadcast_;
    }

    @Override
    public void reset() {
        _topic = "";
        _content = Zeze.Net.Binary.Empty;
        _broadcast = false;
    }

    @Override
    public Zeze.Builtin.Token.BPubTopic toBean() {
        var _b_ = new Zeze.Builtin.Token.BPubTopic();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BPubTopic)_o_);
    }

    public void assign(BPubTopic _o_) {
        _topic = _o_.getTopic();
        _content = _o_.getContent();
        _broadcast = _o_.isBroadcast();
    }

    public void assign(BPubTopic.Data _o_) {
        _topic = _o_._topic;
        _content = _o_._content;
        _broadcast = _o_._broadcast;
    }

    @Override
    public BPubTopic.Data copy() {
        var _c_ = new BPubTopic.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BPubTopic.Data _a_, BPubTopic.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BPubTopic.Data clone() {
        return (BPubTopic.Data)super.clone();
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
        _s_.append("Zeze.Builtin.Token.BPubTopic: {\n");
        _s_.append(_i1_).append("topic=").append(_topic).append(",\n");
        _s_.append(_i1_).append("content=").append(_content).append(",\n");
        _s_.append(_i1_).append("broadcast=").append(_broadcast).append('\n');
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
            String _x_ = _topic;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = _content;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            boolean _x_ = _broadcast;
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _topic = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _content = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _broadcast = _o_.ReadBool(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
