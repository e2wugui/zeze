// auto-generated @formatter:off
package Zeze.Builtin.HttpSession;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BSessionValue extends Zeze.Transaction.Bean implements BSessionValueReadOnly {
    public static final long TYPEID = -635791163229543571L;

    private long _CreateTime;
    private long _ExpireTime;
    private final Zeze.Transaction.Collections.PMap1<String, String> _Properties;

    @Override
    public long getCreateTime() {
        if (!isManaged())
            return _CreateTime;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _CreateTime;
        var log = (Log__CreateTime)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _CreateTime;
    }

    public void setCreateTime(long _v_) {
        if (!isManaged()) {
            _CreateTime = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__CreateTime(this, 1, _v_));
    }

    @Override
    public long getExpireTime() {
        if (!isManaged())
            return _ExpireTime;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ExpireTime;
        var log = (Log__ExpireTime)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _ExpireTime;
    }

    public void setExpireTime(long _v_) {
        if (!isManaged()) {
            _ExpireTime = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__ExpireTime(this, 2, _v_));
    }

    public Zeze.Transaction.Collections.PMap1<String, String> getProperties() {
        return _Properties;
    }

    @Override
    public Zeze.Transaction.Collections.PMap1ReadOnly<String, String> getPropertiesReadOnly() {
        return new Zeze.Transaction.Collections.PMap1ReadOnly<>(_Properties);
    }

    @SuppressWarnings("deprecation")
    public BSessionValue() {
        _Properties = new Zeze.Transaction.Collections.PMap1<>(String.class, String.class);
        _Properties.variableId(3);
    }

    @SuppressWarnings("deprecation")
    public BSessionValue(long _CreateTime_, long _ExpireTime_) {
        _CreateTime = _CreateTime_;
        _ExpireTime = _ExpireTime_;
        _Properties = new Zeze.Transaction.Collections.PMap1<>(String.class, String.class);
        _Properties.variableId(3);
    }

    @Override
    public void reset() {
        setCreateTime(0);
        setExpireTime(0);
        _Properties.clear();
        _unknown_ = null;
    }

    public void assign(BSessionValue _o_) {
        setCreateTime(_o_.getCreateTime());
        setExpireTime(_o_.getExpireTime());
        _Properties.assign(_o_._Properties);
        _unknown_ = _o_._unknown_;
    }

    public BSessionValue copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BSessionValue copy() {
        var _c_ = new BSessionValue();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BSessionValue _a_, BSessionValue _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__CreateTime extends Zeze.Transaction.Logs.LogLong {
        public Log__CreateTime(BSessionValue _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BSessionValue)getBelong())._CreateTime = value; }
    }

    private static final class Log__ExpireTime extends Zeze.Transaction.Logs.LogLong {
        public Log__ExpireTime(BSessionValue _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BSessionValue)getBelong())._ExpireTime = value; }
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
        _s_.append("Zeze.Builtin.HttpSession.BSessionValue: {\n");
        _s_.append(_i1_).append("CreateTime=").append(getCreateTime()).append(",\n");
        _s_.append(_i1_).append("ExpireTime=").append(getExpireTime()).append(",\n");
        _s_.append(_i1_).append("Properties={");
        if (!_Properties.isEmpty()) {
            _s_.append('\n');
            for (var _e_ : _Properties.entrySet()) {
                _s_.append(_i2_).append("Key=").append(_e_.getKey()).append(",\n");
                _s_.append(_i2_).append("Value=").append(_e_.getValue()).append(",\n");
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
            long _x_ = getCreateTime();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getExpireTime();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = _Properties;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BYTES, ByteBuffer.BYTES);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteString(_e_.getKey());
                    _o_.WriteString(_e_.getValue());
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
            setCreateTime(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setExpireTime(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            var _x_ = _Properties;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadString(_s_);
                    var _v_ = _o_.ReadString(_t_);
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
        if (!(_o_ instanceof BSessionValue))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BSessionValue)_o_;
        if (getCreateTime() != _b_.getCreateTime())
            return false;
        if (getExpireTime() != _b_.getExpireTime())
            return false;
        if (!_Properties.equals(_b_._Properties))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _Properties.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _Properties.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getCreateTime() < 0)
            return true;
        if (getExpireTime() < 0)
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
                case 1: _CreateTime = _v_.longValue(); break;
                case 2: _ExpireTime = _v_.longValue(); break;
                case 3: _Properties.followerApply(_v_); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setCreateTime(_r_.getLong(_pn_ + "CreateTime"));
        setExpireTime(_r_.getLong(_pn_ + "ExpireTime"));
        Zeze.Serialize.Helper.decodeJsonMap(this, "Properties", _Properties, _r_.getString(_pn_ + "Properties"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendLong(_pn_ + "CreateTime", getCreateTime());
        _s_.appendLong(_pn_ + "ExpireTime", getExpireTime());
        _s_.appendString(_pn_ + "Properties", Zeze.Serialize.Helper.encodeJson(_Properties));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "CreateTime", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "ExpireTime", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Properties", "map", "string", "string"));
        return _v_;
    }
}
