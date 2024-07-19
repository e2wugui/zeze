// auto-generated @formatter:off
package Zeze.Builtin.Collections.Queue;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

// 一个节点可以存多个KeyValue对，
@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BQueueNode extends Zeze.Transaction.Bean implements BQueueNodeReadOnly {
    public static final long TYPEID = 400956918018571167L;

    private long _NextNodeId; // 废弃，新的遍历寻找使用NextNodeKey，【但是不能删，兼容需要读取】
    private final Zeze.Transaction.Collections.PList2<Zeze.Builtin.Collections.Queue.BQueueNodeValue> _Values;
    private Zeze.Builtin.Collections.Queue.BQueueNodeKey _NextNodeKey; // NodeId为0表示已到达结尾。

    @Override
    public long getNextNodeId() {
        if (!isManaged())
            return _NextNodeId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _NextNodeId;
        var log = (Log__NextNodeId)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _NextNodeId;
    }

    public void setNextNodeId(long _v_) {
        if (!isManaged()) {
            _NextNodeId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__NextNodeId(this, 1, _v_));
    }

    public Zeze.Transaction.Collections.PList2<Zeze.Builtin.Collections.Queue.BQueueNodeValue> getValues() {
        return _Values;
    }

    @Override
    public Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.Collections.Queue.BQueueNodeValue, Zeze.Builtin.Collections.Queue.BQueueNodeValueReadOnly> getValuesReadOnly() {
        return new Zeze.Transaction.Collections.PList2ReadOnly<>(_Values);
    }

    @Override
    public Zeze.Builtin.Collections.Queue.BQueueNodeKey getNextNodeKey() {
        if (!isManaged())
            return _NextNodeKey;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _NextNodeKey;
        var log = (Log__NextNodeKey)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _NextNodeKey;
    }

    public void setNextNodeKey(Zeze.Builtin.Collections.Queue.BQueueNodeKey _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _NextNodeKey = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__NextNodeKey(this, 3, _v_));
    }

    @SuppressWarnings("deprecation")
    public BQueueNode() {
        _Values = new Zeze.Transaction.Collections.PList2<>(Zeze.Builtin.Collections.Queue.BQueueNodeValue.class);
        _Values.variableId(2);
        _NextNodeKey = new Zeze.Builtin.Collections.Queue.BQueueNodeKey();
    }

    @SuppressWarnings("deprecation")
    public BQueueNode(long _NextNodeId_, Zeze.Builtin.Collections.Queue.BQueueNodeKey _NextNodeKey_) {
        _NextNodeId = _NextNodeId_;
        _Values = new Zeze.Transaction.Collections.PList2<>(Zeze.Builtin.Collections.Queue.BQueueNodeValue.class);
        _Values.variableId(2);
        if (_NextNodeKey_ == null)
            _NextNodeKey_ = new Zeze.Builtin.Collections.Queue.BQueueNodeKey();
        _NextNodeKey = _NextNodeKey_;
    }

    @Override
    public void reset() {
        setNextNodeId(0);
        _Values.clear();
        setNextNodeKey(new Zeze.Builtin.Collections.Queue.BQueueNodeKey());
        _unknown_ = null;
    }

    public void assign(BQueueNode _o_) {
        setNextNodeId(_o_.getNextNodeId());
        _Values.clear();
        for (var _e_ : _o_._Values)
            _Values.add(_e_.copy());
        setNextNodeKey(_o_.getNextNodeKey());
        _unknown_ = _o_._unknown_;
    }

    public BQueueNode copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BQueueNode copy() {
        var _c_ = new BQueueNode();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BQueueNode _a_, BQueueNode _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__NextNodeId extends Zeze.Transaction.Logs.LogLong {
        public Log__NextNodeId(BQueueNode _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BQueueNode)getBelong())._NextNodeId = value; }
    }

    private static final class Log__NextNodeKey extends Zeze.Transaction.Logs.LogBeanKey<Zeze.Builtin.Collections.Queue.BQueueNodeKey> {
        public Log__NextNodeKey(BQueueNode _b_, int _i_, Zeze.Builtin.Collections.Queue.BQueueNodeKey _v_) { super(Zeze.Builtin.Collections.Queue.BQueueNodeKey.class, _b_, _i_, _v_); }

        @Override
        public void commit() { ((BQueueNode)getBelong())._NextNodeKey = value; }
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Zeze.Builtin.Collections.Queue.BQueueNode: {").append(System.lineSeparator());
        _l_ += 4;
        _s_.append(Zeze.Util.Str.indent(_l_)).append("NextNodeId=").append(getNextNodeId()).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Values=[");
        if (!_Values.isEmpty()) {
            _s_.append(System.lineSeparator());
            _l_ += 4;
            for (var _v_ : _Values) {
                _s_.append(Zeze.Util.Str.indent(_l_)).append("Item=").append(System.lineSeparator());
                _v_.buildString(_s_, _l_ + 4);
                _s_.append(',').append(System.lineSeparator());
            }
            _l_ -= 4;
            _s_.append(Zeze.Util.Str.indent(_l_));
        }
        _s_.append(']').append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("NextNodeKey=").append(System.lineSeparator());
        getNextNodeKey().buildString(_s_, _l_ + 4);
        _s_.append(System.lineSeparator());
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
            long _x_ = getNextNodeId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = _Values;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BEAN);
                for (var _v_ : _x_) {
                    _v_.encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 3, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            getNextNodeKey().encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
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
            setNextNodeId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = _Values;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new Zeze.Builtin.Collections.Queue.BQueueNodeValue(), _t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _o_.ReadBean(getNextNodeKey(), _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BQueueNode))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BQueueNode)_o_;
        if (getNextNodeId() != _b_.getNextNodeId())
            return false;
        if (!_Values.equals(_b_._Values))
            return false;
        if (!getNextNodeKey().equals(_b_.getNextNodeKey()))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _Values.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _Values.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getNextNodeId() < 0)
            return true;
        for (var _v_ : _Values) {
            if (_v_.negativeCheck())
                return true;
        }
        if (getNextNodeKey().negativeCheck())
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
                case 1: _NextNodeId = _v_.longValue(); break;
                case 2: _Values.followerApply(_v_); break;
                case 3: _NextNodeKey = ((Zeze.Transaction.Logs.LogBeanKey<Zeze.Builtin.Collections.Queue.BQueueNodeKey>)_v_).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setNextNodeId(_r_.getLong(_pn_ + "NextNodeId"));
        Zeze.Serialize.Helper.decodeJsonList(_Values, Zeze.Builtin.Collections.Queue.BQueueNodeValue.class, _r_.getString(_pn_ + "Values"));
        _p_.add("NextNodeKey");
        getNextNodeKey().decodeResultSet(_p_, _r_);
        _p_.remove(_p_.size() - 1);
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendLong(_pn_ + "NextNodeId", getNextNodeId());
        _s_.appendString(_pn_ + "Values", Zeze.Serialize.Helper.encodeJson(_Values));
        _p_.add("NextNodeKey");
        getNextNodeKey().encodeSQLStatement(_p_, _s_);
        _p_.remove(_p_.size() - 1);
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "NextNodeId", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Values", "list", "", "Zeze.Builtin.Collections.Queue.BQueueNodeValue"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "NextNodeKey", "Zeze.Builtin.Collections.Queue.BQueueNodeKey", "", ""));
        return _v_;
    }
}
