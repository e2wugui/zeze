// auto-generated @formatter:off
package Zeze.Builtin.Provider;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BSend extends Zeze.Transaction.Bean {
    private final Zeze.Transaction.Collections.PSet1<Long> _linkSids;
    private long _protocolType;
    private Zeze.Net.Binary _protocolWholeData; // 完整的协议打包，包括了 type, size

    public Zeze.Transaction.Collections.PSet1<Long> getLinkSids() {
        return _linkSids;
    }

    public long getProtocolType() {
        if (!isManaged())
            return _protocolType;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _protocolType;
        var log = (Log__protocolType)txn.GetLog(objectId() + 2);
        return log != null ? log.Value : _protocolType;
    }

    public void setProtocolType(long value) {
        if (!isManaged()) {
            _protocolType = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.PutLog(new Log__protocolType(this, 2, value));
    }

    public Zeze.Net.Binary getProtocolWholeData() {
        if (!isManaged())
            return _protocolWholeData;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _protocolWholeData;
        var log = (Log__protocolWholeData)txn.GetLog(objectId() + 3);
        return log != null ? log.Value : _protocolWholeData;
    }

    public void setProtocolWholeData(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _protocolWholeData = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.PutLog(new Log__protocolWholeData(this, 3, value));
    }

    @SuppressWarnings("deprecation")
    public BSend() {
        _linkSids = new Zeze.Transaction.Collections.PSet1<>(Long.class);
        _linkSids.variableId(1);
        _protocolWholeData = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BSend(long _protocolType_, Zeze.Net.Binary _protocolWholeData_) {
        _linkSids = new Zeze.Transaction.Collections.PSet1<>(Long.class);
        _linkSids.variableId(1);
        _protocolType = _protocolType_;
        if (_protocolWholeData_ == null)
            throw new IllegalArgumentException();
        _protocolWholeData = _protocolWholeData_;
    }

    public void assign(BSend other) {
        getLinkSids().clear();
        for (var e : other.getLinkSids())
            getLinkSids().add(e);
        setProtocolType(other.getProtocolType());
        setProtocolWholeData(other.getProtocolWholeData());
    }

    @Deprecated
    public void Assign(BSend other) {
        assign(other);
    }

    public BSend copyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BSend copy() {
        var copy = new BSend();
        copy.Assign(this);
        return copy;
    }

    @Deprecated
    public BSend Copy() {
        return copy();
    }

    public static void swap(BSend a, BSend b) {
        BSend save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public BSend copyBean() {
        return Copy();
    }

    public static final long TYPEID = 545774009128015305L;

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__protocolType extends Zeze.Transaction.Logs.LogLong {
        public Log__protocolType(BSend bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BSend)getBelong())._protocolType = Value; }
    }

    private static final class Log__protocolWholeData extends Zeze.Transaction.Logs.LogBinary {
        public Log__protocolWholeData(BSend bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BSend)getBelong())._protocolWholeData = Value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Provider.BSend: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("linkSids").append("=[").append(System.lineSeparator());
        level += 4;
        for (var _item_ : getLinkSids()) {
            sb.append(Zeze.Util.Str.indent(level)).append("Item").append('=').append(_item_).append(',').append(System.lineSeparator());
        }
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("protocolType").append('=').append(getProtocolType()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("protocolWholeData").append('=').append(getProtocolWholeData()).append(System.lineSeparator());
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
            var _x_ = getLinkSids();
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                for (var _v_ : _x_)
                    _o_.WriteLong(_v_);
            }
        }
        {
            long _x_ = getProtocolType();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = getProtocolWholeData();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            var _x_ = getLinkSids();
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadLong(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setProtocolType(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setProtocolWholeData(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _linkSids.initRootInfo(root, this);
    }

    @Override
    protected void resetChildrenRootInfo() {
        _linkSids.resetRootInfo();
    }

    @Override
    public boolean negativeCheck() {
        for (var _v_ : getLinkSids()) {
            if (_v_ < 0)
                return true;
        }
        if (getProtocolType() < 0)
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
                case 1: _linkSids.followerApply(vlog); break;
                case 2: _protocolType = ((Zeze.Transaction.Logs.LogLong)vlog).Value; break;
                case 3: _protocolWholeData = ((Zeze.Transaction.Logs.LogBinary)vlog).Value; break;
            }
        }
    }
}
