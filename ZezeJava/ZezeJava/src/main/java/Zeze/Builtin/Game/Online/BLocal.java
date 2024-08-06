// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BLocal extends Zeze.Transaction.Bean implements BLocalReadOnly {
    public static final long TYPEID = 1038509325594826174L;

    private long _LoginVersion; // 角色登录(包括重登录)时复制为tonline.LoginVersion
    private final Zeze.Transaction.Collections.PMap2<String, Zeze.Builtin.Game.Online.BAny> _Datas; // Online模块LocalBean相关方法读写自定义数据, 用来保存角色的Online定时器等
    private Zeze.Builtin.Game.Online.BLink _Link; // 角色登录(包括重登录)时复制为tonline.Link

    @Override
    public long getLoginVersion() {
        if (!isManaged())
            return _LoginVersion;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _LoginVersion;
        var log = (Log__LoginVersion)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _LoginVersion;
    }

    public void setLoginVersion(long _v_) {
        if (!isManaged()) {
            _LoginVersion = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__LoginVersion(this, 1, _v_));
    }

    public Zeze.Transaction.Collections.PMap2<String, Zeze.Builtin.Game.Online.BAny> getDatas() {
        return _Datas;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<String, Zeze.Builtin.Game.Online.BAny, Zeze.Builtin.Game.Online.BAnyReadOnly> getDatasReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_Datas);
    }

    @Override
    public Zeze.Builtin.Game.Online.BLink getLink() {
        if (!isManaged())
            return _Link;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Link;
        var log = (Log__Link)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _Link;
    }

    public void setLink(Zeze.Builtin.Game.Online.BLink _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Link = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__Link(this, 3, _v_));
    }

    @SuppressWarnings("deprecation")
    public BLocal() {
        _Datas = new Zeze.Transaction.Collections.PMap2<>(String.class, Zeze.Builtin.Game.Online.BAny.class);
        _Datas.variableId(2);
        _Link = new Zeze.Builtin.Game.Online.BLink();
    }

    @SuppressWarnings("deprecation")
    public BLocal(long _LoginVersion_, Zeze.Builtin.Game.Online.BLink _Link_) {
        _LoginVersion = _LoginVersion_;
        _Datas = new Zeze.Transaction.Collections.PMap2<>(String.class, Zeze.Builtin.Game.Online.BAny.class);
        _Datas.variableId(2);
        if (_Link_ == null)
            _Link_ = new Zeze.Builtin.Game.Online.BLink();
        _Link = _Link_;
    }

    @Override
    public void reset() {
        setLoginVersion(0);
        _Datas.clear();
        setLink(new Zeze.Builtin.Game.Online.BLink());
        _unknown_ = null;
    }

    public void assign(BLocal _o_) {
        setLoginVersion(_o_.getLoginVersion());
        _Datas.clear();
        for (var _e_ : _o_._Datas.entrySet())
            _Datas.put(_e_.getKey(), _e_.getValue().copy());
        setLink(_o_.getLink());
        _unknown_ = _o_._unknown_;
    }

    public BLocal copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BLocal copy() {
        var _c_ = new BLocal();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BLocal _a_, BLocal _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__LoginVersion extends Zeze.Transaction.Logs.LogLong {
        public Log__LoginVersion(BLocal _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BLocal)getBelong())._LoginVersion = value; }
    }

    private static final class Log__Link extends Zeze.Transaction.Logs.LogBeanKey<Zeze.Builtin.Game.Online.BLink> {
        public Log__Link(BLocal _b_, int _i_, Zeze.Builtin.Game.Online.BLink _v_) { super(Zeze.Builtin.Game.Online.BLink.class, _b_, _i_, _v_); }

        @Override
        public void commit() { ((BLocal)getBelong())._Link = value; }
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
        _s_.append("Zeze.Builtin.Game.Online.BLocal: {\n");
        _s_.append(_i1_).append("LoginVersion=").append(getLoginVersion()).append(",\n");
        _s_.append(_i1_).append("Datas={");
        if (!_Datas.isEmpty()) {
            _s_.append('\n');
            for (var _e_ : _Datas.entrySet()) {
                _s_.append(_i2_).append("Key=").append(_e_.getKey()).append(",\n");
                _s_.append(_i2_).append("Value=");
                _e_.getValue().buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("Link=");
        getLink().buildString(_s_, _l_ + 8);
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
            long _x_ = getLoginVersion();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = _Datas;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.MAP);
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
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 3, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            getLink().encode(_o_);
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
            setLoginVersion(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = _Datas;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadString(_s_);
                    var _v_ = _o_.ReadBean(new Zeze.Builtin.Game.Online.BAny(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _o_.ReadBean(getLink(), _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BLocal))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BLocal)_o_;
        if (getLoginVersion() != _b_.getLoginVersion())
            return false;
        if (!_Datas.equals(_b_._Datas))
            return false;
        if (!getLink().equals(_b_.getLink()))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _Datas.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _Datas.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getLoginVersion() < 0)
            return true;
        if (getLink().negativeCheck())
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
                case 1: _LoginVersion = _v_.longValue(); break;
                case 2: _Datas.followerApply(_v_); break;
                case 3: _Link = ((Zeze.Transaction.Logs.LogBeanKey<Zeze.Builtin.Game.Online.BLink>)_v_).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setLoginVersion(_r_.getLong(_pn_ + "LoginVersion"));
        Zeze.Serialize.Helper.decodeJsonMap(this, "Datas", _Datas, _r_.getString(_pn_ + "Datas"));
        _p_.add("Link");
        getLink().decodeResultSet(_p_, _r_);
        _p_.remove(_p_.size() - 1);
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendLong(_pn_ + "LoginVersion", getLoginVersion());
        _s_.appendString(_pn_ + "Datas", Zeze.Serialize.Helper.encodeJson(_Datas));
        _p_.add("Link");
        getLink().encodeSQLStatement(_p_, _s_);
        _p_.remove(_p_.size() - 1);
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "LoginVersion", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Datas", "map", "string", "Zeze.Builtin.Game.Online.BAny"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Link", "Zeze.Builtin.Game.Online.BLink", "", ""));
        return _v_;
    }
}
