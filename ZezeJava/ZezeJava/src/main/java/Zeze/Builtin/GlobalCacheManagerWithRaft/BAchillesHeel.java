// auto-generated @formatter:off
package Zeze.Builtin.GlobalCacheManagerWithRaft;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BAchillesHeel extends Zeze.Transaction.Bean implements BAchillesHeelReadOnly {
    public static final long TYPEID = -1597142225818031748L;

    private int _ServerId;
    private String _SecureKey;
    private int _GlobalCacheManagerHashIndex;

    private static final java.lang.invoke.VarHandle vh_ServerId;
    private static final java.lang.invoke.VarHandle vh_SecureKey;
    private static final java.lang.invoke.VarHandle vh_GlobalCacheManagerHashIndex;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_ServerId = _l_.findVarHandle(BAchillesHeel.class, "_ServerId", int.class);
            vh_SecureKey = _l_.findVarHandle(BAchillesHeel.class, "_SecureKey", String.class);
            vh_GlobalCacheManagerHashIndex = _l_.findVarHandle(BAchillesHeel.class, "_GlobalCacheManagerHashIndex", int.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public int getServerId() {
        if (!isManaged())
            return _ServerId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ServerId;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _ServerId;
    }

    public void setServerId(int _v_) {
        if (!isManaged()) {
            _ServerId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 1, vh_ServerId, _v_));
    }

    @Override
    public String getSecureKey() {
        if (!isManaged())
            return _SecureKey;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _SecureKey;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 2);
        return log != null ? log.stringValue() : _SecureKey;
    }

    public void setSecureKey(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _SecureKey = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 2, vh_SecureKey, _v_));
    }

    @Override
    public int getGlobalCacheManagerHashIndex() {
        if (!isManaged())
            return _GlobalCacheManagerHashIndex;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _GlobalCacheManagerHashIndex;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _GlobalCacheManagerHashIndex;
    }

    public void setGlobalCacheManagerHashIndex(int _v_) {
        if (!isManaged()) {
            _GlobalCacheManagerHashIndex = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 3, vh_GlobalCacheManagerHashIndex, _v_));
    }

    @SuppressWarnings("deprecation")
    public BAchillesHeel() {
        _SecureKey = "";
    }

    @SuppressWarnings("deprecation")
    public BAchillesHeel(int _ServerId_, String _SecureKey_, int _GlobalCacheManagerHashIndex_) {
        _ServerId = _ServerId_;
        if (_SecureKey_ == null)
            _SecureKey_ = "";
        _SecureKey = _SecureKey_;
        _GlobalCacheManagerHashIndex = _GlobalCacheManagerHashIndex_;
    }

    @Override
    public void reset() {
        setServerId(0);
        setSecureKey("");
        setGlobalCacheManagerHashIndex(0);
        _unknown_ = null;
    }

    public void assign(BAchillesHeel _o_) {
        setServerId(_o_.getServerId());
        setSecureKey(_o_.getSecureKey());
        setGlobalCacheManagerHashIndex(_o_.getGlobalCacheManagerHashIndex());
        _unknown_ = _o_._unknown_;
    }

    public BAchillesHeel copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BAchillesHeel copy() {
        var _c_ = new BAchillesHeel();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BAchillesHeel _a_, BAchillesHeel _b_) {
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
        _s_.append("Zeze.Builtin.GlobalCacheManagerWithRaft.BAchillesHeel: {\n");
        _s_.append(_i1_).append("ServerId=").append(getServerId()).append(",\n");
        _s_.append(_i1_).append("SecureKey=").append(getSecureKey()).append(",\n");
        _s_.append(_i1_).append("GlobalCacheManagerHashIndex=").append(getGlobalCacheManagerHashIndex()).append('\n');
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
            int _x_ = getServerId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            String _x_ = getSecureKey();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            int _x_ = getGlobalCacheManagerHashIndex();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
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
            setServerId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setSecureKey(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setGlobalCacheManagerHashIndex(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BAchillesHeel))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BAchillesHeel)_o_;
        if (getServerId() != _b_.getServerId())
            return false;
        if (!getSecureKey().equals(_b_.getSecureKey()))
            return false;
        if (getGlobalCacheManagerHashIndex() != _b_.getGlobalCacheManagerHashIndex())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getServerId() < 0)
            return true;
        if (getGlobalCacheManagerHashIndex() < 0)
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
                case 1: _ServerId = _v_.intValue(); break;
                case 2: _SecureKey = _v_.stringValue(); break;
                case 3: _GlobalCacheManagerHashIndex = _v_.intValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setServerId(_r_.getInt(_pn_ + "ServerId"));
        setSecureKey(_r_.getString(_pn_ + "SecureKey"));
        if (getSecureKey() == null)
            setSecureKey("");
        setGlobalCacheManagerHashIndex(_r_.getInt(_pn_ + "GlobalCacheManagerHashIndex"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendInt(_pn_ + "ServerId", getServerId());
        _s_.appendString(_pn_ + "SecureKey", getSecureKey());
        _s_.appendInt(_pn_ + "GlobalCacheManagerHashIndex", getGlobalCacheManagerHashIndex());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "ServerId", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "SecureKey", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "GlobalCacheManagerHashIndex", "int", "", ""));
        return _v_;
    }
}
