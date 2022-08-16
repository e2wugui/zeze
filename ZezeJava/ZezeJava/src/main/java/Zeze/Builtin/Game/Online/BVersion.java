// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BVersion extends Zeze.Transaction.Bean {
    private long _LoginVersion;
    private final Zeze.Transaction.Collections.PSet1<String> _ReliableNotifyMark;
    private long _ReliableNotifyConfirmIndex;
    private long _ReliableNotifyIndex;
    private int _ServerId;

    public long getLoginVersion() {
        if (!isManaged())
            return _LoginVersion;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _LoginVersion;
        var log = (Log__LoginVersion)txn.GetLog(objectId() + 1);
        return log != null ? log.Value : _LoginVersion;
    }

    public void setLoginVersion(long value) {
        if (!isManaged()) {
            _LoginVersion = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.PutLog(new Log__LoginVersion(this, 1, value));
    }

    public Zeze.Transaction.Collections.PSet1<String> getReliableNotifyMark() {
        return _ReliableNotifyMark;
    }

    public long getReliableNotifyConfirmIndex() {
        if (!isManaged())
            return _ReliableNotifyConfirmIndex;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ReliableNotifyConfirmIndex;
        var log = (Log__ReliableNotifyConfirmIndex)txn.GetLog(objectId() + 3);
        return log != null ? log.Value : _ReliableNotifyConfirmIndex;
    }

    public void setReliableNotifyConfirmIndex(long value) {
        if (!isManaged()) {
            _ReliableNotifyConfirmIndex = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.PutLog(new Log__ReliableNotifyConfirmIndex(this, 3, value));
    }

    public long getReliableNotifyIndex() {
        if (!isManaged())
            return _ReliableNotifyIndex;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ReliableNotifyIndex;
        var log = (Log__ReliableNotifyIndex)txn.GetLog(objectId() + 4);
        return log != null ? log.Value : _ReliableNotifyIndex;
    }

    public void setReliableNotifyIndex(long value) {
        if (!isManaged()) {
            _ReliableNotifyIndex = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.PutLog(new Log__ReliableNotifyIndex(this, 4, value));
    }

    public int getServerId() {
        if (!isManaged())
            return _ServerId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ServerId;
        var log = (Log__ServerId)txn.GetLog(objectId() + 5);
        return log != null ? log.Value : _ServerId;
    }

    public void setServerId(int value) {
        if (!isManaged()) {
            _ServerId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.PutLog(new Log__ServerId(this, 5, value));
    }

    public BVersion() {
        _ReliableNotifyMark = new Zeze.Transaction.Collections.PSet1<>(String.class);
        _ReliableNotifyMark.VariableId = 2;
    }

    public BVersion(long _LoginVersion_, long _ReliableNotifyConfirmIndex_, long _ReliableNotifyIndex_, int _ServerId_) {
        _LoginVersion = _LoginVersion_;
        _ReliableNotifyMark = new Zeze.Transaction.Collections.PSet1<>(String.class);
        _ReliableNotifyMark.VariableId = 2;
        _ReliableNotifyConfirmIndex = _ReliableNotifyConfirmIndex_;
        _ReliableNotifyIndex = _ReliableNotifyIndex_;
        _ServerId = _ServerId_;
    }

    public void Assign(BVersion other) {
        setLoginVersion(other.getLoginVersion());
        getReliableNotifyMark().clear();
        for (var e : other.getReliableNotifyMark())
            getReliableNotifyMark().add(e);
        setReliableNotifyConfirmIndex(other.getReliableNotifyConfirmIndex());
        setReliableNotifyIndex(other.getReliableNotifyIndex());
        setServerId(other.getServerId());
    }

    public BVersion CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BVersion Copy() {
        var copy = new BVersion();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BVersion a, BVersion b) {
        BVersion save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public BVersion CopyBean() {
        return Copy();
    }

    public static final long TYPEID = -4544955921052723023L;

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__LoginVersion extends Zeze.Transaction.Logs.LogLong {
        public Log__LoginVersion(BVersion bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BVersion)getBelong())._LoginVersion = Value; }
    }

    private static final class Log__ReliableNotifyConfirmIndex extends Zeze.Transaction.Logs.LogLong {
        public Log__ReliableNotifyConfirmIndex(BVersion bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BVersion)getBelong())._ReliableNotifyConfirmIndex = Value; }
    }

    private static final class Log__ReliableNotifyIndex extends Zeze.Transaction.Logs.LogLong {
        public Log__ReliableNotifyIndex(BVersion bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BVersion)getBelong())._ReliableNotifyIndex = Value; }
    }

    private static final class Log__ServerId extends Zeze.Transaction.Logs.LogInt {
        public Log__ServerId(BVersion bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BVersion)getBelong())._ServerId = Value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        BuildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void BuildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.Online.BVersion: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("LoginVersion").append('=').append(getLoginVersion()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ReliableNotifyMark").append("=[").append(System.lineSeparator());
        level += 4;
        for (var _item_ : getReliableNotifyMark()) {
            sb.append(Zeze.Util.Str.indent(level)).append("Item").append('=').append(_item_).append(',').append(System.lineSeparator());
        }
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ReliableNotifyConfirmIndex").append('=').append(getReliableNotifyConfirmIndex()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ReliableNotifyIndex").append('=').append(getReliableNotifyIndex()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ServerId").append('=').append(getServerId()).append(System.lineSeparator());
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
            var _x_ = getReliableNotifyMark();
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BYTES);
                for (var _v_ : _x_)
                    _o_.WriteString(_v_);
            }
        }
        {
            long _x_ = getReliableNotifyConfirmIndex();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
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
            int _x_ = getServerId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
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
            var _x_ = getReliableNotifyMark();
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadString(_t_));
            } else
                _o_.SkipUnknownField(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setReliableNotifyConfirmIndex(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setReliableNotifyIndex(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setServerId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _ReliableNotifyMark.InitRootInfo(root, this);
    }

    @Override
    protected void ResetChildrenRootInfo() {
        _ReliableNotifyMark.ResetRootInfo();
    }

    @Override
    public boolean NegativeCheck() {
        if (getLoginVersion() < 0)
            return true;
        if (getReliableNotifyConfirmIndex() < 0)
            return true;
        if (getReliableNotifyIndex() < 0)
            return true;
        if (getServerId() < 0)
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
                case 2: _ReliableNotifyMark.FollowerApply(vlog); break;
                case 3: _ReliableNotifyConfirmIndex = ((Zeze.Transaction.Logs.LogLong)vlog).Value; break;
                case 4: _ReliableNotifyIndex = ((Zeze.Transaction.Logs.LogLong)vlog).Value; break;
                case 5: _ServerId = ((Zeze.Transaction.Logs.LogInt)vlog).Value; break;
            }
        }
    }
}
