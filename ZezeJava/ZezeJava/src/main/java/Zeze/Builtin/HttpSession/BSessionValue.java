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
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _CreateTime;
        var log = (Log__CreateTime)txn.getLog(objectId() + 1);
        return log != null ? log.value : _CreateTime;
    }

    public void setCreateTime(long value) {
        if (!isManaged()) {
            _CreateTime = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__CreateTime(this, 1, value));
    }

    @Override
    public long getExpireTime() {
        if (!isManaged())
            return _ExpireTime;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ExpireTime;
        var log = (Log__ExpireTime)txn.getLog(objectId() + 2);
        return log != null ? log.value : _ExpireTime;
    }

    public void setExpireTime(long value) {
        if (!isManaged()) {
            _ExpireTime = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ExpireTime(this, 2, value));
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

    public void assign(BSessionValue other) {
        setCreateTime(other.getCreateTime());
        setExpireTime(other.getExpireTime());
        _Properties.assign(other._Properties);
        _unknown_ = other._unknown_;
    }

    public BSessionValue copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BSessionValue copy() {
        var copy = new BSessionValue();
        copy.assign(this);
        return copy;
    }

    public static void swap(BSessionValue a, BSessionValue b) {
        BSessionValue save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__CreateTime extends Zeze.Transaction.Logs.LogLong {
        public Log__CreateTime(BSessionValue bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BSessionValue)getBelong())._CreateTime = value; }
    }

    private static final class Log__ExpireTime extends Zeze.Transaction.Logs.LogLong {
        public Log__ExpireTime(BSessionValue bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BSessionValue)getBelong())._ExpireTime = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.HttpSession.BSessionValue: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("CreateTime=").append(getCreateTime()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ExpireTime=").append(getExpireTime()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Properties={");
        if (!_Properties.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _kv_ : _Properties.entrySet()) {
                sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(_kv_.getKey()).append(',').append(System.lineSeparator());
                sb.append(Zeze.Util.Str.indent(level)).append("Value=").append(_kv_.getValue()).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append('}').append(System.lineSeparator());
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
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Properties.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _Properties.initRootInfoWithRedo(root, this);
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
    public void followerApply(Zeze.Transaction.Log log) {
        var vars = ((Zeze.Transaction.Collections.LogBean)log).getVariables();
        if (vars == null)
            return;
        for (var it = vars.iterator(); it.moveToNext(); ) {
            var vlog = it.value();
            switch (vlog.getVariableId()) {
                case 1: _CreateTime = vlog.longValue(); break;
                case 2: _ExpireTime = vlog.longValue(); break;
                case 3: _Properties.followerApply(vlog); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setCreateTime(rs.getLong(_parents_name_ + "CreateTime"));
        setExpireTime(rs.getLong(_parents_name_ + "ExpireTime"));
        Zeze.Serialize.Helper.decodeJsonMap(this, "Properties", _Properties, rs.getString(_parents_name_ + "Properties"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendLong(_parents_name_ + "CreateTime", getCreateTime());
        st.appendLong(_parents_name_ + "ExpireTime", getExpireTime());
        st.appendString(_parents_name_ + "Properties", Zeze.Serialize.Helper.encodeJson(_Properties));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "CreateTime", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "ExpireTime", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Properties", "map", "string", "string"));
        return vars;
    }
}
