// auto-generated @formatter:off
package Zeze.Builtin.Online;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BReliableNotifyConfirm extends Zeze.Transaction.Bean implements BReliableNotifyConfirmReadOnly {
    public static final long TYPEID = 7657736965823286884L;

    private String _ClientId;
    private long _ReliableNotifyConfirmIndex;
    private boolean _Sync;

    private static final java.lang.invoke.VarHandle vh_ClientId;
    private static final java.lang.invoke.VarHandle vh_ReliableNotifyConfirmIndex;
    private static final java.lang.invoke.VarHandle vh_Sync;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_ClientId = _l_.findVarHandle(BReliableNotifyConfirm.class, "_ClientId", String.class);
            vh_ReliableNotifyConfirmIndex = _l_.findVarHandle(BReliableNotifyConfirm.class, "_ReliableNotifyConfirmIndex", long.class);
            vh_Sync = _l_.findVarHandle(BReliableNotifyConfirm.class, "_Sync", boolean.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public String getClientId() {
        if (!isManaged())
            return _ClientId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ClientId;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 1);
        return log != null ? log.stringValue() : _ClientId;
    }

    public void setClientId(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ClientId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 1, vh_ClientId, _v_));
    }

    @Override
    public long getReliableNotifyConfirmIndex() {
        if (!isManaged())
            return _ReliableNotifyConfirmIndex;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ReliableNotifyConfirmIndex;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _ReliableNotifyConfirmIndex;
    }

    public void setReliableNotifyConfirmIndex(long _v_) {
        if (!isManaged()) {
            _ReliableNotifyConfirmIndex = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 2, vh_ReliableNotifyConfirmIndex, _v_));
    }

    @Override
    public boolean isSync() {
        if (!isManaged())
            return _Sync;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Sync;
        var log = (Zeze.Transaction.Logs.LogBool)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _Sync;
    }

    public void setSync(boolean _v_) {
        if (!isManaged()) {
            _Sync = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBool(this, 3, vh_Sync, _v_));
    }

    @SuppressWarnings("deprecation")
    public BReliableNotifyConfirm() {
        _ClientId = "";
    }

    @SuppressWarnings("deprecation")
    public BReliableNotifyConfirm(String _ClientId_, long _ReliableNotifyConfirmIndex_, boolean _Sync_) {
        if (_ClientId_ == null)
            _ClientId_ = "";
        _ClientId = _ClientId_;
        _ReliableNotifyConfirmIndex = _ReliableNotifyConfirmIndex_;
        _Sync = _Sync_;
    }

    @Override
    public void reset() {
        setClientId("");
        setReliableNotifyConfirmIndex(0);
        setSync(false);
        _unknown_ = null;
    }

    public void assign(BReliableNotifyConfirm _o_) {
        setClientId(_o_.getClientId());
        setReliableNotifyConfirmIndex(_o_.getReliableNotifyConfirmIndex());
        setSync(_o_.isSync());
        _unknown_ = _o_._unknown_;
    }

    public BReliableNotifyConfirm copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BReliableNotifyConfirm copy() {
        var _c_ = new BReliableNotifyConfirm();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BReliableNotifyConfirm _a_, BReliableNotifyConfirm _b_) {
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
        _s_.append("Zeze.Builtin.Online.BReliableNotifyConfirm: {\n");
        _s_.append(_i1_).append("ClientId=").append(getClientId()).append(",\n");
        _s_.append(_i1_).append("ReliableNotifyConfirmIndex=").append(getReliableNotifyConfirmIndex()).append(",\n");
        _s_.append(_i1_).append("Sync=").append(isSync()).append('\n');
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
            String _x_ = getClientId();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            long _x_ = getReliableNotifyConfirmIndex();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            boolean _x_ = isSync();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
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
            setClientId(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setReliableNotifyConfirmIndex(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setSync(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BReliableNotifyConfirm))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BReliableNotifyConfirm)_o_;
        if (!getClientId().equals(_b_.getClientId()))
            return false;
        if (getReliableNotifyConfirmIndex() != _b_.getReliableNotifyConfirmIndex())
            return false;
        if (isSync() != _b_.isSync())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getReliableNotifyConfirmIndex() < 0)
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
                case 1: _ClientId = _v_.stringValue(); break;
                case 2: _ReliableNotifyConfirmIndex = _v_.longValue(); break;
                case 3: _Sync = _v_.booleanValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setClientId(_r_.getString(_pn_ + "ClientId"));
        if (getClientId() == null)
            setClientId("");
        setReliableNotifyConfirmIndex(_r_.getLong(_pn_ + "ReliableNotifyConfirmIndex"));
        setSync(_r_.getBoolean(_pn_ + "Sync"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "ClientId", getClientId());
        _s_.appendLong(_pn_ + "ReliableNotifyConfirmIndex", getReliableNotifyConfirmIndex());
        _s_.appendBoolean(_pn_ + "Sync", isSync());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "ClientId", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "ReliableNotifyConfirmIndex", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Sync", "bool", "", ""));
        return _v_;
    }
}
