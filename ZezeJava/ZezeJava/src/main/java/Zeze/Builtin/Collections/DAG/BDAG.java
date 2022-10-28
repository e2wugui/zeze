// auto-generated @formatter:off
package Zeze.Builtin.Collections.DAG;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
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
            throw new IllegalArgumentException();
        _StartNode = _StartNode_;
        if (_EndNode_ == null)
            throw new IllegalArgumentException();
        _EndNode = _EndNode_;
    }

    public void assign(BDAG other) {
        setNodeSum(other.getNodeSum());
        setEdgeSum(other.getEdgeSum());
        setStartNode(other.getStartNode());
        setEndNode(other.getEndNode());
    }

    @Deprecated
    public void Assign(BDAG other) {
        assign(other);
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

    @Deprecated
    public BDAG Copy() {
        return copy();
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
        sb.append(Zeze.Util.Str.indent(level)).append("NodeSum").append('=').append(getNodeSum()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("EdgeSum").append('=').append(getEdgeSum()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("StartNode").append('=').append(getStartNode()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("EndNode").append('=').append(getEndNode()).append(System.lineSeparator());
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

    @Override
    public void encode(ByteBuffer _o_) {
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
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
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
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
    }

    @Override
    protected void resetChildrenRootInfo() {
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
}
