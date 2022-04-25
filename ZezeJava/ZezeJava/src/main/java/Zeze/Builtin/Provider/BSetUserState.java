// auto-generated @formatter:off
package Zeze.Builtin.Provider;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BSetUserState extends Zeze.Transaction.Bean {
    private long _linkSid;
    private final Zeze.Transaction.Collections.PList1<Long> _states;
    private Zeze.Net.Binary _statex;

    public long getLinkSid() {
        if (!isManaged())
            return _linkSid;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _linkSid;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__linkSid)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.getValue() : _linkSid;
    }

    public void setLinkSid(long value) {
        if (!isManaged()) {
            _linkSid = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__linkSid(this, value));
    }

    public Zeze.Transaction.Collections.PList1<Long> getStates() {
        return _states;
    }

    public Zeze.Net.Binary getStatex() {
        if (!isManaged())
            return _statex;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _statex;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__statex)txn.GetLog(this.getObjectId() + 3);
        return log != null ? log.getValue() : _statex;
    }

    public void setStatex(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _statex = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__statex(this, value));
    }

    public BSetUserState() {
         this(0);
    }

    public BSetUserState(int _varId_) {
        super(_varId_);
        _states = new Zeze.Transaction.Collections.PList1<>(getObjectId() + 2, (_v) -> new Log__states(this, _v));
        _statex = Zeze.Net.Binary.Empty;
    }

    public void Assign(BSetUserState other) {
        setLinkSid(other.getLinkSid());
        getStates().clear();
        for (var e : other.getStates())
            getStates().add(e);
        setStatex(other.getStatex());
    }

    public BSetUserState CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BSetUserState Copy() {
        var copy = new BSetUserState();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BSetUserState a, BSetUserState b) {
        BSetUserState save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public static final long TYPEID = -4860388989628287875L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private static final class Log__linkSid extends Zeze.Transaction.Log1<BSetUserState, Long> {
        public Log__linkSid(BSetUserState self, Long value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 1; }
        @Override
        public void Commit() { this.getBeanTyped()._linkSid = this.getValue(); }
    }

    private static final class Log__states extends Zeze.Transaction.Collections.PList.LogV<Long> {
        public Log__states(BSetUserState host, org.pcollections.PVector<Long> value) { super(host, value); }
        @Override
        public long getLogKey() { return getBean().getObjectId() + 2; }
        public BSetUserState getBeanTyped() { return (BSetUserState)getBean(); }
        @Override
        public void Commit() { Commit(getBeanTyped()._states); }
    }

    private static final class Log__statex extends Zeze.Transaction.Log1<BSetUserState, Zeze.Net.Binary> {
        public Log__statex(BSetUserState self, Zeze.Net.Binary value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 3; }
        @Override
        public void Commit() { this.getBeanTyped()._statex = this.getValue(); }
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Provider.BSetUserState: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("linkSid").append('=').append(getLinkSid()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("states").append("=[").append(System.lineSeparator());
        level += 4;
        for (var _item_ : getStates()) {
            sb.append(Zeze.Util.Str.indent(level)).append("Item").append('=').append(_item_).append(',').append(System.lineSeparator());
        }
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("statex").append('=').append(getStatex()).append(System.lineSeparator());
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
            long _x_ = getLinkSid();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = getStates();
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                for (var _v_ : _x_)
                    _o_.WriteLong(_v_);
            }
        }
        {
            var _x_ = getStatex();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void Decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setLinkSid(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = getStates();
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadLong(_t_));
            } else
                _o_.SkipUnknownField(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setStatex(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _states.InitRootInfo(root, this);
    }

    @Override
    public boolean NegativeCheck() {
        if (getLinkSid() < 0)
            return true;
        for (var _v_ : getStates()) {
            if (_v_ < 0)
                return true;
        }
        return false;
    }
}
