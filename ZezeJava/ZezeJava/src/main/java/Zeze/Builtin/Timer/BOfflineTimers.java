// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BOfflineTimers extends Zeze.Transaction.Bean {
    public static final long TYPEID = -4429519688247847602L;

    private int _ServerId; // 下线之后第一个来注册Timer的服务器Id，一般是下线服务器Id。
    private long _LoginVersion;
    private final Zeze.Transaction.Collections.PMap2<Long, Zeze.Builtin.Timer.BOfflineTimer> _OfflineTimers;

    public int getServerId() {
        if (!isManaged())
            return _ServerId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ServerId;
        var log = (Log__ServerId)txn.getLog(objectId() + 1);
        return log != null ? log.value : _ServerId;
    }

    public void setServerId(int value) {
        if (!isManaged()) {
            _ServerId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ServerId(this, 1, value));
    }

    public long getLoginVersion() {
        if (!isManaged())
            return _LoginVersion;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _LoginVersion;
        var log = (Log__LoginVersion)txn.getLog(objectId() + 2);
        return log != null ? log.value : _LoginVersion;
    }

    public void setLoginVersion(long value) {
        if (!isManaged()) {
            _LoginVersion = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__LoginVersion(this, 2, value));
    }

    public Zeze.Transaction.Collections.PMap2<Long, Zeze.Builtin.Timer.BOfflineTimer> getOfflineTimers() {
        return _OfflineTimers;
    }

    @SuppressWarnings("deprecation")
    public BOfflineTimers() {
        _ServerId = -1;
        _LoginVersion = -1;
        _OfflineTimers = new Zeze.Transaction.Collections.PMap2<>(Long.class, Zeze.Builtin.Timer.BOfflineTimer.class);
        _OfflineTimers.variableId(3);
    }

    @SuppressWarnings("deprecation")
    public BOfflineTimers(int _ServerId_, long _LoginVersion_) {
        _ServerId = _ServerId_;
        _LoginVersion = _LoginVersion_;
        _OfflineTimers = new Zeze.Transaction.Collections.PMap2<>(Long.class, Zeze.Builtin.Timer.BOfflineTimer.class);
        _OfflineTimers.variableId(3);
    }

    public void assign(BOfflineTimers other) {
        setServerId(other.getServerId());
        setLoginVersion(other.getLoginVersion());
        _OfflineTimers.clear();
        for (var e : other._OfflineTimers.entrySet())
            _OfflineTimers.put(e.getKey(), e.getValue().copy());
    }

    @Deprecated
    public void Assign(BOfflineTimers other) {
        assign(other);
    }

    public BOfflineTimers copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BOfflineTimers copy() {
        var copy = new BOfflineTimers();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BOfflineTimers Copy() {
        return copy();
    }

    public static void swap(BOfflineTimers a, BOfflineTimers b) {
        BOfflineTimers save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ServerId extends Zeze.Transaction.Logs.LogInt {
        public Log__ServerId(BOfflineTimers bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BOfflineTimers)getBelong())._ServerId = value; }
    }

    private static final class Log__LoginVersion extends Zeze.Transaction.Logs.LogLong {
        public Log__LoginVersion(BOfflineTimers bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BOfflineTimers)getBelong())._LoginVersion = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Timer.BOfflineTimers: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ServerId").append('=').append(getServerId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("LoginVersion").append('=').append(getLoginVersion()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("OfflineTimers").append("=[").append(System.lineSeparator());
        level += 4;
        for (var _kv_ : _OfflineTimers.entrySet()) {
            sb.append(Zeze.Util.Str.indent(level)).append('(').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append("Key").append('=').append(_kv_.getKey()).append(',').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append("Value").append('=').append(System.lineSeparator());
            _kv_.getValue().buildString(sb, level + 4);
            sb.append(',').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append(')').append(System.lineSeparator());
        }
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append(']').append(System.lineSeparator());
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

    @Override
    public void encode(ByteBuffer _o_) {
        int _i_ = 0;
        {
            int _x_ = getServerId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            long _x_ = getLoginVersion();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = _OfflineTimers;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _e_.getValue().encode(_o_);
                }
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setServerId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setLoginVersion(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            var _x_ = _OfflineTimers;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadLong(_s_);
                    var _v_ = _o_.ReadBean(new Zeze.Builtin.Timer.BOfflineTimer(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _OfflineTimers.initRootInfo(root, this);
    }

    @Override
    protected void resetChildrenRootInfo() {
        _OfflineTimers.resetRootInfo();
    }

    @Override
    public boolean negativeCheck() {
        if (getServerId() < 0)
            return true;
        if (getLoginVersion() < 0)
            return true;
        for (var _v_ : _OfflineTimers.values()) {
            if (_v_.negativeCheck())
                return true;
        }
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
                case 1: _ServerId = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 2: _LoginVersion = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 3: _OfflineTimers.followerApply(vlog); break;
            }
        }
    }
}
