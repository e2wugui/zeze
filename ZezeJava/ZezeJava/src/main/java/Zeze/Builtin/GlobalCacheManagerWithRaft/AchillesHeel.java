// auto-generated @formatter:off
package Zeze.Builtin.GlobalCacheManagerWithRaft;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class AchillesHeel extends Zeze.Transaction.Bean {
    private int _ServerId;
    private String _SecureKey;
    private int _GlobalCacheManagerHashIndex;

    public int getServerId() {
        if (!isManaged())
            return _ServerId;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _ServerId;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ServerId)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.Value : _ServerId;
    }

    public void setServerId(int value) {
        if (!isManaged()) {
            _ServerId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__ServerId(this, 1, value));
    }

    public String getSecureKey() {
        if (!isManaged())
            return _SecureKey;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _SecureKey;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__SecureKey)txn.GetLog(this.getObjectId() + 2);
        return log != null ? log.Value : _SecureKey;
    }

    public void setSecureKey(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _SecureKey = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__SecureKey(this, 2, value));
    }

    public int getGlobalCacheManagerHashIndex() {
        if (!isManaged())
            return _GlobalCacheManagerHashIndex;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _GlobalCacheManagerHashIndex;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__GlobalCacheManagerHashIndex)txn.GetLog(this.getObjectId() + 3);
        return log != null ? log.Value : _GlobalCacheManagerHashIndex;
    }

    public void setGlobalCacheManagerHashIndex(int value) {
        if (!isManaged()) {
            _GlobalCacheManagerHashIndex = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__GlobalCacheManagerHashIndex(this, 3, value));
    }

    public AchillesHeel() {
        _SecureKey = "";
    }

    public AchillesHeel(int _ServerId_, String _SecureKey_, int _GlobalCacheManagerHashIndex_) {
        _ServerId = _ServerId_;
        _SecureKey = _SecureKey_;
        _GlobalCacheManagerHashIndex = _GlobalCacheManagerHashIndex_;
    }

    public void Assign(AchillesHeel other) {
        setServerId(other.getServerId());
        setSecureKey(other.getSecureKey());
        setGlobalCacheManagerHashIndex(other.getGlobalCacheManagerHashIndex());
    }

    public AchillesHeel CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public AchillesHeel Copy() {
        var copy = new AchillesHeel();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(AchillesHeel a, AchillesHeel b) {
        AchillesHeel save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public static final long TYPEID = -2721594206374974168L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private static final class Log__ServerId extends Zeze.Transaction.Logs.LogInt {
        public Log__ServerId(AchillesHeel bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((AchillesHeel)getBelong())._ServerId = Value; }
    }

    private static final class Log__SecureKey extends Zeze.Transaction.Logs.LogString {
        public Log__SecureKey(AchillesHeel bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((AchillesHeel)getBelong())._SecureKey = Value; }
    }

    private static final class Log__GlobalCacheManagerHashIndex extends Zeze.Transaction.Logs.LogInt {
        public Log__GlobalCacheManagerHashIndex(AchillesHeel bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((AchillesHeel)getBelong())._GlobalCacheManagerHashIndex = Value; }
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.GlobalCacheManagerWithRaft.AchillesHeel: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ServerId").append('=').append(getServerId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("SecureKey").append('=').append(getSecureKey()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("GlobalCacheManagerHashIndex").append('=').append(getGlobalCacheManagerHashIndex()).append(System.lineSeparator());
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
            int _x_ = getServerId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            String _x_ = getSecureKey();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            int _x_ = getGlobalCacheManagerHashIndex();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
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
            setServerId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setSecureKey(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setGlobalCacheManagerHashIndex(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
    }

    @Override
    protected void ResetChildrenRootInfo() {
    }

    @Override
    public boolean NegativeCheck() {
        if (getServerId() < 0)
            return true;
        if (getGlobalCacheManagerHashIndex() < 0)
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
                case 1: _ServerId = ((Zeze.Transaction.Logs.LogInt)vlog).Value; break;
                case 2: _SecureKey = ((Zeze.Transaction.Logs.LogString)vlog).Value; break;
                case 3: _GlobalCacheManagerHashIndex = ((Zeze.Transaction.Logs.LogInt)vlog).Value; break;
            }
        }
    }
}
