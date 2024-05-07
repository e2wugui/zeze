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
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _topic;
        var log = (Log__topic)txn.getLog(objectId() + 1);
        return log != null ? log.value : _topic;
    }

    public void setTopic(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _topic = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__topic(this, 1, value));
    }

    @Override
    public Zeze.Net.Binary getContent() {
        if (!isManaged())
            return _content;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _content;
        var log = (Log__content)txn.getLog(objectId() + 2);
        return log != null ? log.value : _content;
    }

    public void setContent(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _content = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__content(this, 2, value));
    }

    @Override
    public boolean isBroadcast() {
        if (!isManaged())
            return _broadcast;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _broadcast;
        var log = (Log__broadcast)txn.getLog(objectId() + 3);
        return log != null ? log.value : _broadcast;
    }

    public void setBroadcast(boolean value) {
        if (!isManaged()) {
            _broadcast = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__broadcast(this, 3, value));
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
        var data = new Zeze.Builtin.Token.BPubTopic.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Token.BPubTopic.Data)other);
    }

    public void assign(BPubTopic.Data other) {
        setTopic(other._topic);
        setContent(other._content);
        setBroadcast(other._broadcast);
        _unknown_ = null;
    }

    public void assign(BPubTopic other) {
        setTopic(other.getTopic());
        setContent(other.getContent());
        setBroadcast(other.isBroadcast());
        _unknown_ = other._unknown_;
    }

    public BPubTopic copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BPubTopic copy() {
        var copy = new BPubTopic();
        copy.assign(this);
        return copy;
    }

    public static void swap(BPubTopic a, BPubTopic b) {
        BPubTopic save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__topic extends Zeze.Transaction.Logs.LogString {
        public Log__topic(BPubTopic bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BPubTopic)getBelong())._topic = value; }
    }

    private static final class Log__content extends Zeze.Transaction.Logs.LogBinary {
        public Log__content(BPubTopic bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BPubTopic)getBelong())._content = value; }
    }

    private static final class Log__broadcast extends Zeze.Transaction.Logs.LogBool {
        public Log__broadcast(BPubTopic bean, int varId, boolean value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BPubTopic)getBelong())._broadcast = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Token.BPubTopic: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("topic=").append(getTopic()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("content=").append(getContent()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("broadcast=").append(isBroadcast()).append(System.lineSeparator());
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append('}');
    }

    private static int _PRE_ALLOC_SIZE_ = 16;

    @Override
    public int preAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void preAllocSize(int size) {
        _PRE_ALLOC_SIZE_ = size;
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
    public void followerApply(Zeze.Transaction.Log log) {
        var vars = ((Zeze.Transaction.Collections.LogBean)log).getVariables();
        if (vars == null)
            return;
        for (var it = vars.iterator(); it.moveToNext(); ) {
            var vlog = it.value();
            switch (vlog.getVariableId()) {
                case 1: _topic = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 2: _content = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
                case 3: _broadcast = ((Zeze.Transaction.Logs.LogBool)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setTopic(rs.getString(_parents_name_ + "topic"));
        if (getTopic() == null)
            setTopic("");
        setContent(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "content")));
        setBroadcast(rs.getBoolean(_parents_name_ + "broadcast"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "topic", getTopic());
        st.appendBinary(_parents_name_ + "content", getContent());
        st.appendBoolean(_parents_name_ + "broadcast", isBroadcast());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "topic", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "content", "binary", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "broadcast", "bool", "", ""));
        return vars;
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

    public void setTopic(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _topic = value;
    }

    public Zeze.Net.Binary getContent() {
        return _content;
    }

    public void setContent(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        _content = value;
    }

    public boolean isBroadcast() {
        return _broadcast;
    }

    public void setBroadcast(boolean value) {
        _broadcast = value;
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
        var bean = new Zeze.Builtin.Token.BPubTopic();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BPubTopic)other);
    }

    public void assign(BPubTopic other) {
        _topic = other.getTopic();
        _content = other.getContent();
        _broadcast = other.isBroadcast();
    }

    public void assign(BPubTopic.Data other) {
        _topic = other._topic;
        _content = other._content;
        _broadcast = other._broadcast;
    }

    @Override
    public BPubTopic.Data copy() {
        var copy = new BPubTopic.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BPubTopic.Data a, BPubTopic.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
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
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Token.BPubTopic: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("topic=").append(_topic).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("content=").append(_content).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("broadcast=").append(_broadcast).append(System.lineSeparator());
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append('}');
    }

    @Override
    public int preAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void preAllocSize(int size) {
        _PRE_ALLOC_SIZE_ = size;
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
