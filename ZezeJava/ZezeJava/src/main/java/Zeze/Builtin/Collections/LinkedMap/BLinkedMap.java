// auto-generated @formatter:off
package Zeze.Builtin.Collections.LinkedMap;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BLinkedMap extends Zeze.Transaction.Bean implements BLinkedMapReadOnly {
    public static final long TYPEID = -8443895985300072767L;

    private long _HeadNodeId;
    private long _TailNodeId;
    private long _Count;
    private long _LastNodeId; // 最近分配过的NodeId, 用于下次分配

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
    public long getCount() {
        if (!isManaged())
            return _Count;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Count;
        var log = (Log__Count)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _Count;
    }

    public void setCount(long _v_) {
        if (!isManaged()) {
            _Count = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__Count(this, 3, _v_));
    }

    @Override
    public long getLastNodeId() {
        if (!isManaged())
            return _LastNodeId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _LastNodeId;
        var log = (Log__LastNodeId)_t_.getLog(objectId() + 4);
        return log != null ? log.value : _LastNodeId;
    }

    public void setLastNodeId(long _v_) {
        if (!isManaged()) {
            _LastNodeId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__LastNodeId(this, 4, _v_));
    }

    @SuppressWarnings("deprecation")
    public BLinkedMap() {
    }

    @SuppressWarnings("deprecation")
    public BLinkedMap(long _HeadNodeId_, long _TailNodeId_, long _Count_, long _LastNodeId_) {
        _HeadNodeId = _HeadNodeId_;
        _TailNodeId = _TailNodeId_;
        _Count = _Count_;
        _LastNodeId = _LastNodeId_;
    }

    @Override
    public void reset() {
        setHeadNodeId(0);
        setTailNodeId(0);
        setCount(0);
        setLastNodeId(0);
        _unknown_ = null;
    }

    public void assign(BLinkedMap _o_) {
        setHeadNodeId(_o_.getHeadNodeId());
        setTailNodeId(_o_.getTailNodeId());
        setCount(_o_.getCount());
        setLastNodeId(_o_.getLastNodeId());
        _unknown_ = _o_._unknown_;
    }

    public BLinkedMap copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BLinkedMap copy() {
        var _c_ = new BLinkedMap();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BLinkedMap _a_, BLinkedMap _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__HeadNodeId extends Zeze.Transaction.Logs.LogLong {
        public Log__HeadNodeId(BLinkedMap _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BLinkedMap)getBelong())._HeadNodeId = value; }
    }

    private static final class Log__TailNodeId extends Zeze.Transaction.Logs.LogLong {
        public Log__TailNodeId(BLinkedMap _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BLinkedMap)getBelong())._TailNodeId = value; }
    }

    private static final class Log__Count extends Zeze.Transaction.Logs.LogLong {
        public Log__Count(BLinkedMap _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BLinkedMap)getBelong())._Count = value; }
    }

    private static final class Log__LastNodeId extends Zeze.Transaction.Logs.LogLong {
        public Log__LastNodeId(BLinkedMap _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BLinkedMap)getBelong())._LastNodeId = value; }
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Zeze.Builtin.Collections.LinkedMap.BLinkedMap: {").append(System.lineSeparator());
        _l_ += 4;
        _s_.append(Zeze.Util.Str.indent(_l_)).append("HeadNodeId=").append(getHeadNodeId()).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("TailNodeId=").append(getTailNodeId()).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Count=").append(getCount()).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("LastNodeId=").append(getLastNodeId()).append(System.lineSeparator());
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
            long _x_ = getCount();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getLastNodeId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
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
            setHeadNodeId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setTailNodeId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setCount(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setLastNodeId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BLinkedMap))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BLinkedMap)_o_;
        if (getHeadNodeId() != _b_.getHeadNodeId())
            return false;
        if (getTailNodeId() != _b_.getTailNodeId())
            return false;
        if (getCount() != _b_.getCount())
            return false;
        if (getLastNodeId() != _b_.getLastNodeId())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getHeadNodeId() < 0)
            return true;
        if (getTailNodeId() < 0)
            return true;
        if (getCount() < 0)
            return true;
        if (getLastNodeId() < 0)
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
                case 3: _Count = _v_.longValue(); break;
                case 4: _LastNodeId = _v_.longValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setHeadNodeId(_r_.getLong(_pn_ + "HeadNodeId"));
        setTailNodeId(_r_.getLong(_pn_ + "TailNodeId"));
        setCount(_r_.getLong(_pn_ + "Count"));
        setLastNodeId(_r_.getLong(_pn_ + "LastNodeId"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendLong(_pn_ + "HeadNodeId", getHeadNodeId());
        _s_.appendLong(_pn_ + "TailNodeId", getTailNodeId());
        _s_.appendLong(_pn_ + "Count", getCount());
        _s_.appendLong(_pn_ + "LastNodeId", getLastNodeId());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "HeadNodeId", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "TailNodeId", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Count", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "LastNodeId", "long", "", ""));
        return _v_;
    }
}
