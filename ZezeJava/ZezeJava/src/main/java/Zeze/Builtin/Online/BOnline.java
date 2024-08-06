// auto-generated @formatter:off
package Zeze.Builtin.Online;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BOnline extends Zeze.Transaction.Bean implements BOnlineReadOnly {
    public static final long TYPEID = -7786403987996508020L;

    private Zeze.Builtin.Online.BLink _Link;
    private long _LoginVersion;
    private final Zeze.Transaction.Collections.PSet1<String> _ReliableNotifyMark;
    private long _ReliableNotifyIndex;
    private long _ReliableNotifyConfirmIndex;
    private int _ServerId;
    private long _LogoutVersion;

    private transient Object __zeze_map_key__;

    @Override
    public Object mapKey() {
        return __zeze_map_key__;
    }

    @Override
    public void mapKey(Object _v_) {
        __zeze_map_key__ = _v_;
    }

    @Override
    public Zeze.Builtin.Online.BLink getLink() {
        if (!isManaged())
            return _Link;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Link;
        var log = (Log__Link)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _Link;
    }

    public void setLink(Zeze.Builtin.Online.BLink _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Link = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__Link(this, 1, _v_));
    }

    @Override
    public long getLoginVersion() {
        if (!isManaged())
            return _LoginVersion;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _LoginVersion;
        var log = (Log__LoginVersion)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _LoginVersion;
    }

    public void setLoginVersion(long _v_) {
        if (!isManaged()) {
            _LoginVersion = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__LoginVersion(this, 2, _v_));
    }

    public Zeze.Transaction.Collections.PSet1<String> getReliableNotifyMark() {
        return _ReliableNotifyMark;
    }

    @Override
    public Zeze.Transaction.Collections.PSet1ReadOnly<String> getReliableNotifyMarkReadOnly() {
        return new Zeze.Transaction.Collections.PSet1ReadOnly<>(_ReliableNotifyMark);
    }

    @Override
    public long getReliableNotifyIndex() {
        if (!isManaged())
            return _ReliableNotifyIndex;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ReliableNotifyIndex;
        var log = (Log__ReliableNotifyIndex)_t_.getLog(objectId() + 4);
        return log != null ? log.value : _ReliableNotifyIndex;
    }

    public void setReliableNotifyIndex(long _v_) {
        if (!isManaged()) {
            _ReliableNotifyIndex = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__ReliableNotifyIndex(this, 4, _v_));
    }

    @Override
    public long getReliableNotifyConfirmIndex() {
        if (!isManaged())
            return _ReliableNotifyConfirmIndex;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ReliableNotifyConfirmIndex;
        var log = (Log__ReliableNotifyConfirmIndex)_t_.getLog(objectId() + 5);
        return log != null ? log.value : _ReliableNotifyConfirmIndex;
    }

    public void setReliableNotifyConfirmIndex(long _v_) {
        if (!isManaged()) {
            _ReliableNotifyConfirmIndex = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__ReliableNotifyConfirmIndex(this, 5, _v_));
    }

    @Override
    public int getServerId() {
        if (!isManaged())
            return _ServerId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ServerId;
        var log = (Log__ServerId)_t_.getLog(objectId() + 6);
        return log != null ? log.value : _ServerId;
    }

    public void setServerId(int _v_) {
        if (!isManaged()) {
            _ServerId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__ServerId(this, 6, _v_));
    }

    @Override
    public long getLogoutVersion() {
        if (!isManaged())
            return _LogoutVersion;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _LogoutVersion;
        var log = (Log__LogoutVersion)_t_.getLog(objectId() + 7);
        return log != null ? log.value : _LogoutVersion;
    }

    public void setLogoutVersion(long _v_) {
        if (!isManaged()) {
            _LogoutVersion = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__LogoutVersion(this, 7, _v_));
    }

    @SuppressWarnings("deprecation")
    public BOnline() {
        _Link = new Zeze.Builtin.Online.BLink();
        _ReliableNotifyMark = new Zeze.Transaction.Collections.PSet1<>(String.class);
        _ReliableNotifyMark.variableId(3);
    }

    @SuppressWarnings("deprecation")
    public BOnline(Zeze.Builtin.Online.BLink _Link_, long _LoginVersion_, long _ReliableNotifyIndex_, long _ReliableNotifyConfirmIndex_, int _ServerId_, long _LogoutVersion_) {
        if (_Link_ == null)
            _Link_ = new Zeze.Builtin.Online.BLink();
        _Link = _Link_;
        _LoginVersion = _LoginVersion_;
        _ReliableNotifyMark = new Zeze.Transaction.Collections.PSet1<>(String.class);
        _ReliableNotifyMark.variableId(3);
        _ReliableNotifyIndex = _ReliableNotifyIndex_;
        _ReliableNotifyConfirmIndex = _ReliableNotifyConfirmIndex_;
        _ServerId = _ServerId_;
        _LogoutVersion = _LogoutVersion_;
    }

    @Override
    public void reset() {
        setLink(new Zeze.Builtin.Online.BLink());
        setLoginVersion(0);
        _ReliableNotifyMark.clear();
        setReliableNotifyIndex(0);
        setReliableNotifyConfirmIndex(0);
        setServerId(0);
        setLogoutVersion(0);
        _unknown_ = null;
    }

    public void assign(BOnline _o_) {
        setLink(_o_.getLink());
        setLoginVersion(_o_.getLoginVersion());
        _ReliableNotifyMark.assign(_o_._ReliableNotifyMark);
        setReliableNotifyIndex(_o_.getReliableNotifyIndex());
        setReliableNotifyConfirmIndex(_o_.getReliableNotifyConfirmIndex());
        setServerId(_o_.getServerId());
        setLogoutVersion(_o_.getLogoutVersion());
        _unknown_ = _o_._unknown_;
    }

    public BOnline copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BOnline copy() {
        var _c_ = new BOnline();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BOnline _a_, BOnline _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Link extends Zeze.Transaction.Logs.LogBeanKey<Zeze.Builtin.Online.BLink> {
        public Log__Link(BOnline _b_, int _i_, Zeze.Builtin.Online.BLink _v_) { super(Zeze.Builtin.Online.BLink.class, _b_, _i_, _v_); }

        @Override
        public void commit() { ((BOnline)getBelong())._Link = value; }
    }

    private static final class Log__LoginVersion extends Zeze.Transaction.Logs.LogLong {
        public Log__LoginVersion(BOnline _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BOnline)getBelong())._LoginVersion = value; }
    }

    private static final class Log__ReliableNotifyIndex extends Zeze.Transaction.Logs.LogLong {
        public Log__ReliableNotifyIndex(BOnline _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BOnline)getBelong())._ReliableNotifyIndex = value; }
    }

    private static final class Log__ReliableNotifyConfirmIndex extends Zeze.Transaction.Logs.LogLong {
        public Log__ReliableNotifyConfirmIndex(BOnline _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BOnline)getBelong())._ReliableNotifyConfirmIndex = value; }
    }

    private static final class Log__ServerId extends Zeze.Transaction.Logs.LogInt {
        public Log__ServerId(BOnline _b_, int _i_, int _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BOnline)getBelong())._ServerId = value; }
    }

    private static final class Log__LogoutVersion extends Zeze.Transaction.Logs.LogLong {
        public Log__LogoutVersion(BOnline _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BOnline)getBelong())._LogoutVersion = value; }
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
        _s_.append("Zeze.Builtin.Online.BOnline: {\n");
        _s_.append(_i1_).append("Link=");
        getLink().buildString(_s_, _l_ + 8);
        _s_.append(",\n");
        _s_.append(_i1_).append("LoginVersion=").append(getLoginVersion()).append(",\n");
        _s_.append(_i1_).append("ReliableNotifyMark={");
        if (!_ReliableNotifyMark.isEmpty()) {
            _s_.append('\n');
            for (var _v_ : _ReliableNotifyMark) {
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("ReliableNotifyIndex=").append(getReliableNotifyIndex()).append(",\n");
        _s_.append(_i1_).append("ReliableNotifyConfirmIndex=").append(getReliableNotifyConfirmIndex()).append(",\n");
        _s_.append(_i1_).append("ServerId=").append(getServerId()).append(",\n");
        _s_.append(_i1_).append("LogoutVersion=").append(getLogoutVersion()).append('\n');
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
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 1, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            getLink().encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        {
            long _x_ = getLoginVersion();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = _ReliableNotifyMark;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BYTES);
                for (var _v_ : _x_) {
                    _o_.WriteString(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            long _x_ = getReliableNotifyIndex();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getReliableNotifyConfirmIndex();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            int _x_ = getServerId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            long _x_ = getLogoutVersion();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 7, ByteBuffer.INTEGER);
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
            _o_.ReadBean(getLink(), _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setLoginVersion(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            var _x_ = _ReliableNotifyMark;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadString(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setReliableNotifyIndex(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setReliableNotifyConfirmIndex(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            setServerId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 7) {
            setLogoutVersion(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BOnline))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BOnline)_o_;
        if (!getLink().equals(_b_.getLink()))
            return false;
        if (getLoginVersion() != _b_.getLoginVersion())
            return false;
        if (!_ReliableNotifyMark.equals(_b_._ReliableNotifyMark))
            return false;
        if (getReliableNotifyIndex() != _b_.getReliableNotifyIndex())
            return false;
        if (getReliableNotifyConfirmIndex() != _b_.getReliableNotifyConfirmIndex())
            return false;
        if (getServerId() != _b_.getServerId())
            return false;
        if (getLogoutVersion() != _b_.getLogoutVersion())
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _ReliableNotifyMark.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _ReliableNotifyMark.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getLink().negativeCheck())
            return true;
        if (getLoginVersion() < 0)
            return true;
        if (getReliableNotifyIndex() < 0)
            return true;
        if (getReliableNotifyConfirmIndex() < 0)
            return true;
        if (getServerId() < 0)
            return true;
        if (getLogoutVersion() < 0)
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
                case 1: _Link = ((Zeze.Transaction.Logs.LogBeanKey<Zeze.Builtin.Online.BLink>)_v_).value; break;
                case 2: _LoginVersion = _v_.longValue(); break;
                case 3: _ReliableNotifyMark.followerApply(_v_); break;
                case 4: _ReliableNotifyIndex = _v_.longValue(); break;
                case 5: _ReliableNotifyConfirmIndex = _v_.longValue(); break;
                case 6: _ServerId = _v_.intValue(); break;
                case 7: _LogoutVersion = _v_.longValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        _p_.add("Link");
        getLink().decodeResultSet(_p_, _r_);
        _p_.remove(_p_.size() - 1);
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setLoginVersion(_r_.getLong(_pn_ + "LoginVersion"));
        Zeze.Serialize.Helper.decodeJsonSet(_ReliableNotifyMark, String.class, _r_.getString(_pn_ + "ReliableNotifyMark"));
        setReliableNotifyIndex(_r_.getLong(_pn_ + "ReliableNotifyIndex"));
        setReliableNotifyConfirmIndex(_r_.getLong(_pn_ + "ReliableNotifyConfirmIndex"));
        setServerId(_r_.getInt(_pn_ + "ServerId"));
        setLogoutVersion(_r_.getLong(_pn_ + "LogoutVersion"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        _p_.add("Link");
        getLink().encodeSQLStatement(_p_, _s_);
        _p_.remove(_p_.size() - 1);
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendLong(_pn_ + "LoginVersion", getLoginVersion());
        _s_.appendString(_pn_ + "ReliableNotifyMark", Zeze.Serialize.Helper.encodeJson(_ReliableNotifyMark));
        _s_.appendLong(_pn_ + "ReliableNotifyIndex", getReliableNotifyIndex());
        _s_.appendLong(_pn_ + "ReliableNotifyConfirmIndex", getReliableNotifyConfirmIndex());
        _s_.appendInt(_pn_ + "ServerId", getServerId());
        _s_.appendLong(_pn_ + "LogoutVersion", getLogoutVersion());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Link", "Zeze.Builtin.Online.BLink", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "LoginVersion", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "ReliableNotifyMark", "set", "", "string"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "ReliableNotifyIndex", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "ReliableNotifyConfirmIndex", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(6, "ServerId", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(7, "LogoutVersion", "long", "", ""));
        return _v_;
    }
}
