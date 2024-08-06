// auto-generated @formatter:off
package Zeze.Builtin.Collections.LinkedMap;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BClearJobState extends Zeze.Transaction.Bean implements BClearJobStateReadOnly {
    public static final long TYPEID = 8599835992466746563L;

    private long _HeadNodeId;
    private long _TailNodeId;
    private String _LinkedMapName;

    @Override
    public long getHeadNodeId() {
        if (!isManaged())
            return _HeadNodeId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _HeadNodeId;
        var log = (Log__HeadNodeId)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _HeadNodeId;
    }

    public void setHeadNodeId(long _v_) {
        if (!isManaged()) {
            _HeadNodeId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__HeadNodeId(this, 1, _v_));
    }

    @Override
    public long getTailNodeId() {
        if (!isManaged())
            return _TailNodeId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _TailNodeId;
        var log = (Log__TailNodeId)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _TailNodeId;
    }

    public void setTailNodeId(long _v_) {
        if (!isManaged()) {
            _TailNodeId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__TailNodeId(this, 2, _v_));
    }

    @Override
    public String getLinkedMapName() {
        if (!isManaged())
            return _LinkedMapName;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _LinkedMapName;
        var log = (Log__LinkedMapName)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _LinkedMapName;
    }

    public void setLinkedMapName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _LinkedMapName = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__LinkedMapName(this, 3, _v_));
    }

    @SuppressWarnings("deprecation")
    public BClearJobState() {
        _LinkedMapName = "";
    }

    @SuppressWarnings("deprecation")
    public BClearJobState(long _HeadNodeId_, long _TailNodeId_, String _LinkedMapName_) {
        _HeadNodeId = _HeadNodeId_;
        _TailNodeId = _TailNodeId_;
        if (_LinkedMapName_ == null)
            _LinkedMapName_ = "";
        _LinkedMapName = _LinkedMapName_;
    }

    @Override
    public void reset() {
        setHeadNodeId(0);
        setTailNodeId(0);
        setLinkedMapName("");
        _unknown_ = null;
    }

    public void assign(BClearJobState _o_) {
        setHeadNodeId(_o_.getHeadNodeId());
        setTailNodeId(_o_.getTailNodeId());
        setLinkedMapName(_o_.getLinkedMapName());
        _unknown_ = _o_._unknown_;
    }

    public BClearJobState copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BClearJobState copy() {
        var _c_ = new BClearJobState();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BClearJobState _a_, BClearJobState _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__HeadNodeId extends Zeze.Transaction.Logs.LogLong {
        public Log__HeadNodeId(BClearJobState _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BClearJobState)getBelong())._HeadNodeId = value; }
    }

    private static final class Log__TailNodeId extends Zeze.Transaction.Logs.LogLong {
        public Log__TailNodeId(BClearJobState _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BClearJobState)getBelong())._TailNodeId = value; }
    }

    private static final class Log__LinkedMapName extends Zeze.Transaction.Logs.LogString {
        public Log__LinkedMapName(BClearJobState _b_, int _i_, String _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BClearJobState)getBelong())._LinkedMapName = value; }
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
        _s_.append("Zeze.Builtin.Collections.LinkedMap.BClearJobState: {\n");
        _s_.append(_i1_).append("HeadNodeId=").append(getHeadNodeId()).append(",\n");
        _s_.append(_i1_).append("TailNodeId=").append(getTailNodeId()).append(",\n");
        _s_.append(_i1_).append("LinkedMapName=").append(getLinkedMapName()).append('\n');
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
            long _x_ = getHeadNodeId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getTailNodeId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            String _x_ = getLinkedMapName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
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
            setHeadNodeId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setTailNodeId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setLinkedMapName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BClearJobState))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BClearJobState)_o_;
        if (getHeadNodeId() != _b_.getHeadNodeId())
            return false;
        if (getTailNodeId() != _b_.getTailNodeId())
            return false;
        if (!getLinkedMapName().equals(_b_.getLinkedMapName()))
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getHeadNodeId() < 0)
            return true;
        if (getTailNodeId() < 0)
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
                case 1: _HeadNodeId = _v_.longValue(); break;
                case 2: _TailNodeId = _v_.longValue(); break;
                case 3: _LinkedMapName = _v_.stringValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setHeadNodeId(_r_.getLong(_pn_ + "HeadNodeId"));
        setTailNodeId(_r_.getLong(_pn_ + "TailNodeId"));
        setLinkedMapName(_r_.getString(_pn_ + "LinkedMapName"));
        if (getLinkedMapName() == null)
            setLinkedMapName("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendLong(_pn_ + "HeadNodeId", getHeadNodeId());
        _s_.appendLong(_pn_ + "TailNodeId", getTailNodeId());
        _s_.appendString(_pn_ + "LinkedMapName", getLinkedMapName());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "HeadNodeId", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "TailNodeId", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "LinkedMapName", "string", "", ""));
        return _v_;
    }
}
