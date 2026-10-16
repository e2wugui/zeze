// auto-generated @formatter:off
package Game.Login;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"EqualsAndHashcode", "ConstantConditions", "ConstantValue", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BRoles extends Zeze.Transaction.Bean implements BRolesReadOnly {
    public static final long TYPEID = -7562249081436134307L;

    private final Zeze.Transaction.Collections.PList2<Game.Login.BRole> _RoleList;
    private static final Zeze.Transaction.Collections.List2Meta<Game.Login.BRole> meta1_RoleList
            = Zeze.Transaction.Collections.List2Meta.get(Game.Login.BRole.class);

    private long _LastLoginRoleId;

    private static final java.lang.invoke.VarHandle vh_LastLoginRoleId;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_LastLoginRoleId = _l_.findVarHandle(BRoles.class, "_LastLoginRoleId", long.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    public Zeze.Transaction.Collections.PList2<Game.Login.BRole> getRoleList() {
        return _RoleList;
    }

    @Override
    public Zeze.Transaction.Collections.PList2ReadOnly<Game.Login.BRole, Game.Login.BRoleReadOnly> getRoleListReadOnly() {
        return new Zeze.Transaction.Collections.PList2ReadOnly<>(_RoleList);
    }

    @Override
    public long getLastLoginRoleId() {
        if (!isManaged())
            return _LastLoginRoleId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _LastLoginRoleId;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _LastLoginRoleId;
    }

    public void setLastLoginRoleId(long _v_) {
        if (!isManaged()) {
            _LastLoginRoleId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 2, vh_LastLoginRoleId, _v_));
    }

    @SuppressWarnings("deprecation")
    public BRoles() {
        _RoleList = new Zeze.Transaction.Collections.PList2<>(meta1_RoleList);
        _RoleList.variableId(1);
    }

    @SuppressWarnings("deprecation")
    public BRoles(long _LastLoginRoleId_) {
        _RoleList = new Zeze.Transaction.Collections.PList2<>(meta1_RoleList);
        _RoleList.variableId(1);
        _LastLoginRoleId = _LastLoginRoleId_;
    }

    @Override
    public void reset() {
        _RoleList.clear();
        setLastLoginRoleId(0);
        _unknown_ = null;
    }

    public void assign(BRoles _o_) {
        _RoleList.clear();
        for (var _e_ : _o_._RoleList)
            _RoleList.add(_e_.copy());
        setLastLoginRoleId(_o_.getLastLoginRoleId());
        _unknown_ = _o_._unknown_;
    }

    public BRoles copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BRoles copy() {
        var _c_ = new BRoles();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BRoles _a_, BRoles _b_) {
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
        _s_.append("Game.Login.BRoles: {\n");
        _s_.append(_i1_).append("RoleList=[");
        if (!_RoleList.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _RoleList) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_RoleList.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=");
                _v_.buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("],\n");
        _s_.append(_i1_).append("LastLoginRoleId=").append(getLastLoginRoleId()).append('\n');
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

    private byte @Nullable [] _unknown_;

    public byte @Nullable [] unknown() {
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
            var _x_ = _RoleList;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.LIST);
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
            long _x_ = getLastLoginRoleId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
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
            var _x_ = _RoleList;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new Game.Login.BRole(), _t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setLastLoginRoleId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(@Nullable Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BRoles))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BRoles)_o_;
        if (!_RoleList.equals(_b_._RoleList))
            return false;
        if (getLastLoginRoleId() != _b_.getLastLoginRoleId())
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _RoleList.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _RoleList.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        for (var _v_ : _RoleList) {
            if (_v_.negativeCheck())
                return true;
        }
        if (getLastLoginRoleId() < 0)
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
                case 1: _RoleList.followerApply(_v_); break;
                case 2: _LastLoginRoleId = _v_.longValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        Zeze.Serialize.Helper.decodeJsonList(_RoleList, Game.Login.BRole.class, _r_.getString(_pn_ + "RoleList"));
        setLastLoginRoleId(_r_.getLong(_pn_ + "LastLoginRoleId"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "RoleList", Zeze.Serialize.Helper.encodeJson(_RoleList));
        _s_.appendLong(_pn_ + "LastLoginRoleId", getLastLoginRoleId());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "RoleList", "list", "", "Game.Login.BRole"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "LastLoginRoleId", "long", "", ""));
        return _v_;
    }
}
