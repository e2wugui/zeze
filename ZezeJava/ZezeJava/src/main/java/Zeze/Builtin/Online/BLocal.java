// auto-generated @formatter:off
package Zeze.Builtin.Online;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BLocal extends Zeze.Transaction.Bean {
    private long _LoginVersion;
    private final Zeze.Transaction.Collections.PMap2<String, Zeze.Builtin.Online.BAny> _Datas;

    private Object __zeze_map_key__;

    @Override
    public Object getMapKey() {
        return __zeze_map_key__;
    }

    @Override
    public void setMapKey(Object value) {
        __zeze_map_key__ = value;
    }

    public long getLoginVersion() {
        if (!isManaged())
            return _LoginVersion;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _LoginVersion;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__LoginVersion)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.getValue() : _LoginVersion;
    }

    public void setLoginVersion(long value) {
        if (!isManaged()) {
            _LoginVersion = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__LoginVersion(this, 1, value));
    }

    public Zeze.Transaction.Collections.PMap2<String, Zeze.Builtin.Online.BAny> getDatas() {
        return _Datas;
    }

    public BLocal() {
         this(0);
    }

    public BLocal(int _varId_) {
        super(_varId_);
        _Datas = new Zeze.Transaction.Collections.PMap2<>(String.class, Zeze.Builtin.Online.BAny.class);
        _Datas.VariableId = 2;
    }

    public void Assign(BLocal other) {
        setLoginVersion(other.getLoginVersion());
        getDatas().clear();
        for (var e : other.getDatas().entrySet())
            getDatas().put(e.getKey(), e.getValue().Copy());
    }

    public BLocal CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BLocal Copy() {
        var copy = new BLocal();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BLocal a, BLocal b) {
        BLocal save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public static final long TYPEID = -6330089022826554666L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private static final class Log__LoginVersion extends Zeze.Transaction.Log1<BLocal, Long> {
        public Log__LoginVersion(BLocal bean, int varId, Long value) { super(bean, varId, value); }

        @Override
        public void Commit() { getBeanTyped()._LoginVersion = this.getValue(); }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        BuildString(sb, 0);
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    @Override
    public void BuildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Online.BLocal: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("LoginVersion").append('=').append(getLoginVersion()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Datas").append("=[").append(System.lineSeparator());
        level += 4;
        for (var _kv_ : getDatas().entrySet()) {
            sb.append(Zeze.Util.Str.indent(level)).append('(').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append("Key").append('=').append(_kv_.getKey()).append(',').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append("Value").append('=').append(System.lineSeparator());
            _kv_.getValue().BuildString(sb, level + 4);
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
    public int getPreAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void setPreAllocSize(int size) {
        _PRE_ALLOC_SIZE_ = size;
    }

    @Override
    public void Encode(ByteBuffer _o_) {
        int _i_ = 0;
        {
            long _x_ = getLoginVersion();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = getDatas();
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BYTES, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteString(_e_.getKey());
                    _e_.getValue().Encode(_o_);
                }
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void Decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setLoginVersion(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = getDatas();
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadString(_s_);
                    var _v_ = _o_.ReadBean(new Zeze.Builtin.Online.BAny(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownField(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Datas.InitRootInfo(root, this);
    }

    @Override
    public boolean NegativeCheck() {
        if (getLoginVersion() < 0)
            return true;
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void FollowerApply(Zeze.Transaction.Log log) {
        var vars = ((Zeze.Transaction.Collections.LogBean)log).getVariables();
        if (vars == null)
            return;
        for (var it = vars.iterator(); it.moveToNext(); ) {
            var vlog = it.value();
            switch (vlog.getVariableId()) {
                case 1: _LoginVersion = ((Zeze.Transaction.Logs.LogLong)vlog).Value; break;
                case 2: _Datas.FollowerApply(vlog); break;
            }
        }
    }
}
