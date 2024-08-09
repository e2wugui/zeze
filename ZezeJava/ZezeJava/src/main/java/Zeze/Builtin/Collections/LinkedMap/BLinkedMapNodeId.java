// auto-generated @formatter:off
package Zeze.Builtin.Collections.LinkedMap;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BLinkedMapNodeId extends Zeze.Transaction.Bean implements BLinkedMapNodeIdReadOnly {
    public static final long TYPEID = -6424218657633143196L;

    private long _NodeId; // KeyValue对所属的节点ID. 每个节点有多个KeyValue对共享

    private static final java.lang.invoke.VarHandle vh_NodeId;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_NodeId = _l_.findVarHandle(BLinkedMapNodeId.class, "_NodeId", long.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public long getNodeId() {
        if (!isManaged())
            return _NodeId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _NodeId;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _NodeId;
    }

    public void setNodeId(long _v_) {
        if (!isManaged()) {
            _NodeId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 1, vh_NodeId, _v_));
    }

    @SuppressWarnings("deprecation")
    public BLinkedMapNodeId() {
    }

    @SuppressWarnings("deprecation")
    public BLinkedMapNodeId(long _NodeId_) {
        _NodeId = _NodeId_;
    }

    @Override
    public void reset() {
        setNodeId(0);
        _unknown_ = null;
    }

    public void assign(BLinkedMapNodeId _o_) {
        setNodeId(_o_.getNodeId());
        _unknown_ = _o_._unknown_;
    }

    public BLinkedMapNodeId copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BLinkedMapNodeId copy() {
        var _c_ = new BLinkedMapNodeId();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BLinkedMapNodeId _a_, BLinkedMapNodeId _b_) {
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
        _s_.append("Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeId: {\n");
        _s_.append(_i1_).append("NodeId=").append(getNodeId()).append('\n');
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
            long _x_ = getNodeId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
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
            setNodeId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BLinkedMapNodeId))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BLinkedMapNodeId)_o_;
        if (getNodeId() != _b_.getNodeId())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getNodeId() < 0)
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
                case 1: _NodeId = _v_.longValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setNodeId(_r_.getLong(_pn_ + "NodeId"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendLong(_pn_ + "NodeId", getNodeId());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "NodeId", "long", "", ""));
        return _v_;
    }
}
