// auto-generated @formatter:off
package Zeze.Builtin.Token;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BTopic extends Zeze.Transaction.Bean implements BTopicReadOnly {
    public static final long TYPEID = -8399522464290840620L;

    private String _topic; // 主题

    private static final java.lang.invoke.VarHandle vh_topic;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_topic = _l_.findVarHandle(BTopic.class, "_topic", String.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public String getTopic() {
        if (!isManaged())
            return _topic;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _topic;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 1);
        return log != null ? log.stringValue() : _topic;
    }

    public void setTopic(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _topic = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 1, vh_topic, _v_));
    }

    @SuppressWarnings("deprecation")
    public BTopic() {
        _topic = "";
    }

    @SuppressWarnings("deprecation")
    public BTopic(String _topic_) {
        if (_topic_ == null)
            _topic_ = "";
        _topic = _topic_;
    }

    @Override
    public void reset() {
        setTopic("");
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Token.BTopic.Data toData() {
        var _d_ = new Zeze.Builtin.Token.BTopic.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Token.BTopic.Data)_o_);
    }

    public void assign(BTopic.Data _o_) {
        setTopic(_o_._topic);
        _unknown_ = null;
    }

    public void assign(BTopic _o_) {
        setTopic(_o_.getTopic());
        _unknown_ = _o_._unknown_;
    }

    public BTopic copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTopic copy() {
        var _c_ = new BTopic();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BTopic _a_, BTopic _b_) {
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
        _s_.append("Zeze.Builtin.Token.BTopic: {\n");
        _s_.append(_i1_).append("topic=").append(getTopic()).append('\n');
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
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BTopic))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BTopic)_o_;
        if (!getTopic().equals(_b_.getTopic()))
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
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setTopic(_r_.getString(_pn_ + "topic"));
        if (getTopic() == null)
            setTopic("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "topic", getTopic());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "topic", "string", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -8399522464290840620L;

    private String _topic; // 主题

    public String getTopic() {
        return _topic;
    }

    public void setTopic(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _topic = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _topic = "";
    }

    @SuppressWarnings("deprecation")
    public Data(String _topic_) {
        if (_topic_ == null)
            _topic_ = "";
        _topic = _topic_;
    }

    @Override
    public void reset() {
        _topic = "";
    }

    @Override
    public Zeze.Builtin.Token.BTopic toBean() {
        var _b_ = new Zeze.Builtin.Token.BTopic();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BTopic)_o_);
    }

    public void assign(BTopic _o_) {
        _topic = _o_.getTopic();
    }

    public void assign(BTopic.Data _o_) {
        _topic = _o_._topic;
    }

    @Override
    public BTopic.Data copy() {
        var _c_ = new BTopic.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BTopic.Data _a_, BTopic.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BTopic.Data clone() {
        return (BTopic.Data)super.clone();
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
        _s_.append("Zeze.Builtin.Token.BTopic: {\n");
        _s_.append(_i1_).append("topic=").append(_topic).append('\n');
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
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BTopic.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BTopic.Data)_o_;
        if (!_topic.equals(_b_._topic))
            return false;
        return true;
    }
}
}
