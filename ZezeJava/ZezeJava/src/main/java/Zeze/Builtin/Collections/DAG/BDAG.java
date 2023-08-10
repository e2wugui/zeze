// auto-generated @formatter:off
package Zeze.Builtin.Collections.DAG;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BDAG extends Zeze.Transaction.Bean implements BDAGReadOnly {
    public static final long TYPEID = -6511351497538000623L;

    private long _NodeSum; // 有向图的结点数
    private long _EdgeSum; // 有向图的边数
    private String _StartNode; // 有向图的起点ValueId（如果有的话），没有则置空（一般来说，对于任务Task都有起点与终点）
    private String _EndNode; // 有向图的终点（如果有的话），没有则置空（一般来说，对于任务Task都有起点与终点）

    @Override
    public long getNodeSum() {
        if (!isManaged())
            return _NodeSum;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _NodeSum;
        var log = (Log__NodeSum)txn.getLog(objectId() + 1);
        return log != null ? log.value : _NodeSum;
    }

    public void setNodeSum(long value) {
        if (!isManaged()) {
            _NodeSum = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__NodeSum(this, 1, value));
    }

    @Override
    public long getEdgeSum() {
        if (!isManaged())
            return _EdgeSum;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _EdgeSum;
        var log = (Log__EdgeSum)txn.getLog(objectId() + 2);
        return log != null ? log.value : _EdgeSum;
    }

    public void setEdgeSum(long value) {
        if (!isManaged()) {
            _EdgeSum = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__EdgeSum(this, 2, value));
    }

    @Override
    public String getStartNode() {
        if (!isManaged())
            return _StartNode;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _StartNode;
        var log = (Log__StartNode)txn.getLog(objectId() + 3);
        return log != null ? log.value : _StartNode;
    }

    public void setStartNode(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _StartNode = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__StartNode(this, 3, value));
    }

    @Override
    public String getEndNode() {
        if (!isManaged())
            return _EndNode;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _EndNode;
        var log = (Log__EndNode)txn.getLog(objectId() + 4);
        return log != null ? log.value : _EndNode;
    }

    public void setEndNode(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _EndNode = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__EndNode(this, 4, value));
    }

    @SuppressWarnings("deprecation")
    public BDAG() {
        _StartNode = "";
        _EndNode = "";
    }

    @SuppressWarnings("deprecation")
    public BDAG(long _NodeSum_, long _EdgeSum_, String _StartNode_, String _EndNode_) {
        _NodeSum = _NodeSum_;
        _EdgeSum = _EdgeSum_;
        if (_StartNode_ == null)
            _StartNode_ = "";
        _StartNode = _StartNode_;
        if (_EndNode_ == null)
            _EndNode_ = "";
        _EndNode = _EndNode_;
    }

    @Override
    public void reset() {
        setNodeSum(0);
        setEdgeSum(0);
        setStartNode("");
        setEndNode("");
        _unknown_ = null;
    }

    public void assign(BDAG other) {
        setNodeSum(other.getNodeSum());
        setEdgeSum(other.getEdgeSum());
        setStartNode(other.getStartNode());
        setEndNode(other.getEndNode());
        _unknown_ = other._unknown_;
    }

    public BDAG copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BDAG copy() {
        var copy = new BDAG();
        copy.assign(this);
        return copy;
    }

    public static void swap(BDAG a, BDAG b) {
        BDAG save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__NodeSum extends Zeze.Transaction.Logs.LogLong {
        public Log__NodeSum(BDAG bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BDAG)getBelong())._NodeSum = value; }
    }

    private static final class Log__EdgeSum extends Zeze.Transaction.Logs.LogLong {
        public Log__EdgeSum(BDAG bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BDAG)getBelong())._EdgeSum = value; }
    }

    private static final class Log__StartNode extends Zeze.Transaction.Logs.LogString {
        public Log__StartNode(BDAG bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BDAG)getBelong())._StartNode = value; }
    }

    private static final class Log__EndNode extends Zeze.Transaction.Logs.LogString {
        public Log__EndNode(BDAG bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BDAG)getBelong())._EndNode = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Collections.DAG.BDAG: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("NodeSum=").append(getNodeSum()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("EdgeSum=").append(getEdgeSum()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("StartNode=").append(getStartNode()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("EndNode=").append(getEndNode()).append(System.lineSeparator());
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
            long _x_ = getNodeSum();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getEdgeSum();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            String _x_ = getStartNode();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getEndNode();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        _o_.writeAllUnknownFields(_i_, _ui_, _u_);
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        ByteBuffer _u_ = null;
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setNodeSum(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setEdgeSum(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setStartNode(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setEndNode(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean negativeCheck() {
        if (getNodeSum() < 0)
            return true;
        if (getEdgeSum() < 0)
            return true;
        return false;
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
                case 1: _NodeSum = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 2: _EdgeSum = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 3: _StartNode = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 4: _EndNode = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setNodeSum(rs.getLong(_parents_name_ + "NodeSum"));
        setEdgeSum(rs.getLong(_parents_name_ + "EdgeSum"));
        setStartNode(rs.getString(_parents_name_ + "StartNode"));
        if (getStartNode() == null)
            setStartNode("");
        setEndNode(rs.getString(_parents_name_ + "EndNode"));
        if (getEndNode() == null)
            setEndNode("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendLong(_parents_name_ + "NodeSum", getNodeSum());
        st.appendLong(_parents_name_ + "EdgeSum", getEdgeSum());
        st.appendString(_parents_name_ + "StartNode", getStartNode());
        st.appendString(_parents_name_ + "EndNode", getEndNode());
    }

    @Override
    public java.util.List<Zeze.Transaction.Bean.Variable> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Transaction.Bean.Variable(1, "NodeSum", "long", "", ""));
        vars.add(new Zeze.Transaction.Bean.Variable(2, "EdgeSum", "long", "", ""));
        vars.add(new Zeze.Transaction.Bean.Variable(3, "StartNode", "string", "", ""));
        vars.add(new Zeze.Transaction.Bean.Variable(4, "EndNode", "string", "", ""));
        return vars;
    }

    @Override
    public Zeze.Transaction.Bean toPrevious() {
        return null; // todo BDAG
    }
}
