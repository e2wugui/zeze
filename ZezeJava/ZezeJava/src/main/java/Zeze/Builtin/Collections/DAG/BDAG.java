// auto-generated @formatter:off
package Zeze.Builtin.Collections.DAG;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
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
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _NodeSum;
        var log = (Log__NodeSum)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _NodeSum;
    }

    public void setNodeSum(long _v_) {
        if (!isManaged()) {
            _NodeSum = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__NodeSum(this, 1, _v_));
    }

    @Override
    public long getEdgeSum() {
        if (!isManaged())
            return _EdgeSum;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _EdgeSum;
        var log = (Log__EdgeSum)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _EdgeSum;
    }

    public void setEdgeSum(long _v_) {
        if (!isManaged()) {
            _EdgeSum = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__EdgeSum(this, 2, _v_));
    }

    @Override
    public String getStartNode() {
        if (!isManaged())
            return _StartNode;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _StartNode;
        var log = (Log__StartNode)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _StartNode;
    }

    public void setStartNode(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _StartNode = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__StartNode(this, 3, _v_));
    }

    @Override
    public String getEndNode() {
        if (!isManaged())
            return _EndNode;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _EndNode;
        var log = (Log__EndNode)_t_.getLog(objectId() + 4);
        return log != null ? log.value : _EndNode;
    }

    public void setEndNode(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _EndNode = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__EndNode(this, 4, _v_));
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

    public void assign(BDAG _o_) {
        setNodeSum(_o_.getNodeSum());
        setEdgeSum(_o_.getEdgeSum());
        setStartNode(_o_.getStartNode());
        setEndNode(_o_.getEndNode());
        _unknown_ = _o_._unknown_;
    }

    public BDAG copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BDAG copy() {
        var _c_ = new BDAG();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BDAG _a_, BDAG _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__NodeSum extends Zeze.Transaction.Logs.LogLong {
        public Log__NodeSum(BDAG _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BDAG)getBelong())._NodeSum = value; }
    }

    private static final class Log__EdgeSum extends Zeze.Transaction.Logs.LogLong {
        public Log__EdgeSum(BDAG _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BDAG)getBelong())._EdgeSum = value; }
    }

    private static final class Log__StartNode extends Zeze.Transaction.Logs.LogString {
        public Log__StartNode(BDAG _b_, int _i_, String _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BDAG)getBelong())._StartNode = value; }
    }

    private static final class Log__EndNode extends Zeze.Transaction.Logs.LogString {
        public Log__EndNode(BDAG _b_, int _i_, String _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BDAG)getBelong())._EndNode = value; }
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Zeze.Builtin.Collections.DAG.BDAG: {").append(System.lineSeparator());
        _l_ += 4;
        _s_.append(Zeze.Util.Str.indent(_l_)).append("NodeSum=").append(getNodeSum()).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("EdgeSum=").append(getEdgeSum()).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("StartNode=").append(getStartNode()).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("EndNode=").append(getEndNode()).append(System.lineSeparator());
        _l_ -= 4;
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
    public void decode(IByteBuffer _o_) {
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
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BDAG))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BDAG)_o_;
        if (getNodeSum() != _b_.getNodeSum())
            return false;
        if (getEdgeSum() != _b_.getEdgeSum())
            return false;
        if (!getStartNode().equals(_b_.getStartNode()))
            return false;
        if (!getEndNode().equals(_b_.getEndNode()))
            return false;
        return true;
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
    public void followerApply(Zeze.Transaction.Log _l_) {
        var _vs_ = ((Zeze.Transaction.Collections.LogBean)_l_).getVariables();
        if (_vs_ == null)
            return;
        for (var _i_ = _vs_.iterator(); _i_.moveToNext(); ) {
            var _v_ = _i_.value();
            switch (_v_.getVariableId()) {
                case 1: _NodeSum = _v_.longValue(); break;
                case 2: _EdgeSum = _v_.longValue(); break;
                case 3: _StartNode = _v_.stringValue(); break;
                case 4: _EndNode = _v_.stringValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setNodeSum(_r_.getLong(_pn_ + "NodeSum"));
        setEdgeSum(_r_.getLong(_pn_ + "EdgeSum"));
        setStartNode(_r_.getString(_pn_ + "StartNode"));
        if (getStartNode() == null)
            setStartNode("");
        setEndNode(_r_.getString(_pn_ + "EndNode"));
        if (getEndNode() == null)
            setEndNode("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendLong(_pn_ + "NodeSum", getNodeSum());
        _s_.appendLong(_pn_ + "EdgeSum", getEdgeSum());
        _s_.appendString(_pn_ + "StartNode", getStartNode());
        _s_.appendString(_pn_ + "EndNode", getEndNode());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "NodeSum", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "EdgeSum", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "StartNode", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "EndNode", "string", "", ""));
        return _v_;
    }
}
