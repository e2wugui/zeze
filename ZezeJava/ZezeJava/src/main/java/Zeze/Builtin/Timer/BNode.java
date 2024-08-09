// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

// 若干个timer打包的节点
@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BNode extends Zeze.Transaction.Bean implements BNodeReadOnly {
    public static final long TYPEID = -44647384323818353L;

    private long _PrevNodeId; // 节点双链表的前一个节点ID, tNodes表的key, 循环指向,不应该无效
    private long _NextNodeId; // 节点双链表的后一个节点ID, tNodes表的key, 循环指向,不应该无效
    private final Zeze.Transaction.Collections.PMap2<String, Zeze.Builtin.Timer.BTimer> _Timers; // 该节点的所有timer, key是timerId(用户指定的,或"@"+Base64编码的自动分配ID), 数量上限是Zeze.Component.Timer.CountPerNode(50)

    private static final java.lang.invoke.VarHandle vh_PrevNodeId;
    private static final java.lang.invoke.VarHandle vh_NextNodeId;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_PrevNodeId = _l_.findVarHandle(BNode.class, "_PrevNodeId", long.class);
            vh_NextNodeId = _l_.findVarHandle(BNode.class, "_NextNodeId", long.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public long getPrevNodeId() {
        if (!isManaged())
            return _PrevNodeId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _PrevNodeId;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _PrevNodeId;
    }

    public void setPrevNodeId(long _v_) {
        if (!isManaged()) {
            _PrevNodeId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 1, vh_PrevNodeId, _v_));
    }

    @Override
    public long getNextNodeId() {
        if (!isManaged())
            return _NextNodeId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _NextNodeId;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _NextNodeId;
    }

    public void setNextNodeId(long _v_) {
        if (!isManaged()) {
            _NextNodeId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 2, vh_NextNodeId, _v_));
    }

    public Zeze.Transaction.Collections.PMap2<String, Zeze.Builtin.Timer.BTimer> getTimers() {
        return _Timers;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<String, Zeze.Builtin.Timer.BTimer, Zeze.Builtin.Timer.BTimerReadOnly> getTimersReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_Timers);
    }

    @SuppressWarnings("deprecation")
    public BNode() {
        _Timers = new Zeze.Transaction.Collections.PMap2<>(String.class, Zeze.Builtin.Timer.BTimer.class);
        _Timers.variableId(3);
    }

    @SuppressWarnings("deprecation")
    public BNode(long _PrevNodeId_, long _NextNodeId_) {
        _PrevNodeId = _PrevNodeId_;
        _NextNodeId = _NextNodeId_;
        _Timers = new Zeze.Transaction.Collections.PMap2<>(String.class, Zeze.Builtin.Timer.BTimer.class);
        _Timers.variableId(3);
    }

    @Override
    public void reset() {
        setPrevNodeId(0);
        setNextNodeId(0);
        _Timers.clear();
        _unknown_ = null;
    }

    public void assign(BNode _o_) {
        setPrevNodeId(_o_.getPrevNodeId());
        setNextNodeId(_o_.getNextNodeId());
        _Timers.clear();
        for (var _e_ : _o_._Timers.entrySet())
            _Timers.put(_e_.getKey(), _e_.getValue().copy());
        _unknown_ = _o_._unknown_;
    }

    public BNode copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BNode copy() {
        var _c_ = new BNode();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BNode _a_, BNode _b_) {
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
        var _i2_ = Zeze.Util.Str.indent(_l_ + 8);
        _s_.append("Zeze.Builtin.Timer.BNode: {\n");
        _s_.append(_i1_).append("PrevNodeId=").append(getPrevNodeId()).append(",\n");
        _s_.append(_i1_).append("NextNodeId=").append(getNextNodeId()).append(",\n");
        _s_.append(_i1_).append("Timers={");
        if (!_Timers.isEmpty()) {
            _s_.append('\n');
            for (var _e_ : _Timers.entrySet()) {
                _s_.append(_i2_).append("Key=").append(_e_.getKey()).append(",\n");
                _s_.append(_i2_).append("Value=");
                _e_.getValue().buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("}\n");
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
            long _x_ = getPrevNodeId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getNextNodeId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = _Timers;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BYTES, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteString(_e_.getKey());
                    _e_.getValue().encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
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
            setPrevNodeId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setNextNodeId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            var _x_ = _Timers;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadString(_s_);
                    var _v_ = _o_.ReadBean(new Zeze.Builtin.Timer.BTimer(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BNode))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BNode)_o_;
        if (getPrevNodeId() != _b_.getPrevNodeId())
            return false;
        if (getNextNodeId() != _b_.getNextNodeId())
            return false;
        if (!_Timers.equals(_b_._Timers))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _Timers.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _Timers.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getPrevNodeId() < 0)
            return true;
        if (getNextNodeId() < 0)
            return true;
        for (var _v_ : _Timers.values()) {
            if (_v_.negativeCheck())
                return true;
        }
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
                case 1: _PrevNodeId = _v_.longValue(); break;
                case 2: _NextNodeId = _v_.longValue(); break;
                case 3: _Timers.followerApply(_v_); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setPrevNodeId(_r_.getLong(_pn_ + "PrevNodeId"));
        setNextNodeId(_r_.getLong(_pn_ + "NextNodeId"));
        Zeze.Serialize.Helper.decodeJsonMap(this, "Timers", _Timers, _r_.getString(_pn_ + "Timers"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendLong(_pn_ + "PrevNodeId", getPrevNodeId());
        _s_.appendLong(_pn_ + "NextNodeId", getNextNodeId());
        _s_.appendString(_pn_ + "Timers", Zeze.Serialize.Helper.encodeJson(_Timers));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "PrevNodeId", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "NextNodeId", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Timers", "map", "string", "Zeze.Builtin.Timer.BTimer"));
        return _v_;
    }
}
