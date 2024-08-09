// auto-generated @formatter:off
package Zeze.Builtin.GlobalCacheManagerWithRaft;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BReduceParam extends Zeze.Transaction.Bean implements BReduceParamReadOnly {
    public static final long TYPEID = -7052326232144455304L;

    private Zeze.Net.Binary _GlobalKey;
    private int _State;
    private Zeze.Util.Id128 _ReduceTid;

    private static final java.lang.invoke.VarHandle vh_GlobalKey;
    private static final java.lang.invoke.VarHandle vh_State;
    private static final java.lang.invoke.VarHandle vh_ReduceTid;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_GlobalKey = _l_.findVarHandle(BReduceParam.class, "_GlobalKey", Zeze.Net.Binary.class);
            vh_State = _l_.findVarHandle(BReduceParam.class, "_State", int.class);
            vh_ReduceTid = _l_.findVarHandle(BReduceParam.class, "_ReduceTid", Zeze.Util.Id128.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public Zeze.Net.Binary getGlobalKey() {
        if (!isManaged())
            return _GlobalKey;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _GlobalKey;
        var log = (Zeze.Transaction.Logs.LogBinary)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _GlobalKey;
    }

    public void setGlobalKey(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _GlobalKey = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBinary(this, 1, vh_GlobalKey, _v_));
    }

    @Override
    public int getState() {
        if (!isManaged())
            return _State;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _State;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _State;
    }

    public void setState(int _v_) {
        if (!isManaged()) {
            _State = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 2, vh_State, _v_));
    }

    @Override
    public Zeze.Util.Id128 getReduceTid() {
        if (!isManaged())
            return _ReduceTid;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ReduceTid;
        @SuppressWarnings("unchecked")
        var log = (Zeze.Transaction.Logs.LogBeanKey<Zeze.Util.Id128>)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _ReduceTid;
    }

    public void setReduceTid(Zeze.Util.Id128 _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ReduceTid = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBeanKey<>(this, 3, vh_ReduceTid, _v_));
    }

    @SuppressWarnings("deprecation")
    public BReduceParam() {
        _GlobalKey = Zeze.Net.Binary.Empty;
        _ReduceTid = new Zeze.Util.Id128();
    }

    @SuppressWarnings("deprecation")
    public BReduceParam(Zeze.Net.Binary _GlobalKey_, int _State_, Zeze.Util.Id128 _ReduceTid_) {
        if (_GlobalKey_ == null)
            _GlobalKey_ = Zeze.Net.Binary.Empty;
        _GlobalKey = _GlobalKey_;
        _State = _State_;
        if (_ReduceTid_ == null)
            _ReduceTid_ = new Zeze.Util.Id128();
        _ReduceTid = _ReduceTid_;
    }

    @Override
    public void reset() {
        setGlobalKey(Zeze.Net.Binary.Empty);
        setState(0);
        setReduceTid(new Zeze.Util.Id128());
        _unknown_ = null;
    }

    public void assign(BReduceParam _o_) {
        setGlobalKey(_o_.getGlobalKey());
        setState(_o_.getState());
        setReduceTid(_o_.getReduceTid());
        _unknown_ = _o_._unknown_;
    }

    public BReduceParam copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BReduceParam copy() {
        var _c_ = new BReduceParam();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BReduceParam _a_, BReduceParam _b_) {
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
        _s_.append("Zeze.Builtin.GlobalCacheManagerWithRaft.BReduceParam: {\n");
        _s_.append(_i1_).append("GlobalKey=").append(getGlobalKey()).append(",\n");
        _s_.append(_i1_).append("State=").append(getState()).append(",\n");
        _s_.append(_i1_).append("ReduceTid=");
        getReduceTid().buildString(_s_, _l_ + 8);
        _s_.append('\n');
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
            var _x_ = getGlobalKey();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            int _x_ = getState();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 3, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            getReduceTid().encode(_o_);
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
            setGlobalKey(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setState(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _o_.ReadBean(getReduceTid(), _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BReduceParam))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BReduceParam)_o_;
        if (!getGlobalKey().equals(_b_.getGlobalKey()))
            return false;
        if (getState() != _b_.getState())
            return false;
        if (!getReduceTid().equals(_b_.getReduceTid()))
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getState() < 0)
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
                case 1: _GlobalKey = _v_.binaryValue(); break;
                case 2: _State = _v_.intValue(); break;
                case 3: _ReduceTid = ((Zeze.Transaction.Logs.LogBeanKey<Zeze.Util.Id128>)_v_).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setGlobalKey(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "GlobalKey")));
        setState(_r_.getInt(_pn_ + "State"));
        _p_.add("ReduceTid");
        getReduceTid().decodeResultSet(_p_, _r_);
        _p_.remove(_p_.size() - 1);
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendBinary(_pn_ + "GlobalKey", getGlobalKey());
        _s_.appendInt(_pn_ + "State", getState());
        _p_.add("ReduceTid");
        getReduceTid().encodeSQLStatement(_p_, _s_);
        _p_.remove(_p_.size() - 1);
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "GlobalKey", "binary", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "State", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "ReduceTid", "Zeze.Util.Id128", "", ""));
        return _v_;
    }
}
