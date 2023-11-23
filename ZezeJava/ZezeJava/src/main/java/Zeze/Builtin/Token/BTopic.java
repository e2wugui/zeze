// auto-generated @formatter:off
package Zeze.Builtin.Token;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BTopic extends Zeze.Transaction.Bean implements BTopicReadOnly {
    public static final long TYPEID = -8399522464290840620L;

    private String _topic; // 主题

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
        var data = new Zeze.Builtin.Token.BTopic.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Token.BTopic.Data)other);
    }

    public void assign(BTopic.Data other) {
        setTopic(other._topic);
        _unknown_ = null;
    }

    public void assign(BTopic other) {
        setTopic(other.getTopic());
        _unknown_ = other._unknown_;
    }

    public BTopic copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTopic copy() {
        var copy = new BTopic();
        copy.assign(this);
        return copy;
    }

    public static void swap(BTopic a, BTopic b) {
        BTopic save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__topic extends Zeze.Transaction.Logs.LogString {
        public Log__topic(BTopic bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTopic)getBelong())._topic = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Token.BTopic: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("topic=").append(getTopic()).append(System.lineSeparator());
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
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setTopic(rs.getString(_parents_name_ + "topic"));
        if (getTopic() == null)
            setTopic("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "topic", getTopic());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "topic", "string", "", ""));
        return vars;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -8399522464290840620L;

    private String _topic; // 主题

    public String getTopic() {
        return _topic;
    }

    public void setTopic(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _topic = value;
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
        var bean = new Zeze.Builtin.Token.BTopic();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BTopic)other);
    }

    public void assign(BTopic other) {
        _topic = other.getTopic();
    }

    public void assign(BTopic.Data other) {
        _topic = other._topic;
    }

    @Override
    public BTopic.Data copy() {
        var copy = new BTopic.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BTopic.Data a, BTopic.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
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
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Token.BTopic: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("topic=").append(_topic).append(System.lineSeparator());
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
}
}
