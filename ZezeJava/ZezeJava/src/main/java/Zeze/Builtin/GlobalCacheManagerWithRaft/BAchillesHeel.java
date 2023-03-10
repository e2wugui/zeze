// auto-generated @formatter:off
package Zeze.Builtin.GlobalCacheManagerWithRaft;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BAchillesHeel extends Zeze.Transaction.Bean implements BAchillesHeelReadOnly {
    public static final long TYPEID = -1597142225818031748L;

    private int _ServerId;
    private String _SecureKey;
    private int _GlobalCacheManagerHashIndex;

    @Override
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

    @Override
    public String getSecureKey() {
        if (!isManaged())
            return _SecureKey;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _SecureKey;
        var log = (Log__SecureKey)txn.getLog(objectId() + 2);
        return log != null ? log.value : _SecureKey;
    }

    public void setSecureKey(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _SecureKey = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__SecureKey(this, 2, value));
    }

    @Override
    public int getGlobalCacheManagerHashIndex() {
        if (!isManaged())
            return _GlobalCacheManagerHashIndex;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _GlobalCacheManagerHashIndex;
        var log = (Log__GlobalCacheManagerHashIndex)txn.getLog(objectId() + 3);
        return log != null ? log.value : _GlobalCacheManagerHashIndex;
    }

    public void setGlobalCacheManagerHashIndex(int value) {
        if (!isManaged()) {
            _GlobalCacheManagerHashIndex = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__GlobalCacheManagerHashIndex(this, 3, value));
    }

    @SuppressWarnings("deprecation")
    public BAchillesHeel() {
        _SecureKey = "";
    }

    @SuppressWarnings("deprecation")
    public BAchillesHeel(int _ServerId_, String _SecureKey_, int _GlobalCacheManagerHashIndex_) {
        _ServerId = _ServerId_;
        if (_SecureKey_ == null)
            throw new IllegalArgumentException();
        _SecureKey = _SecureKey_;
        _GlobalCacheManagerHashIndex = _GlobalCacheManagerHashIndex_;
    }

    public void assign(BAchillesHeel other) {
        setServerId(other.getServerId());
        setSecureKey(other.getSecureKey());
        setGlobalCacheManagerHashIndex(other.getGlobalCacheManagerHashIndex());
    }

    public BAchillesHeel copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BAchillesHeel copy() {
        var copy = new BAchillesHeel();
        copy.assign(this);
        return copy;
    }

    public static void swap(BAchillesHeel a, BAchillesHeel b) {
        BAchillesHeel save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ServerId extends Zeze.Transaction.Logs.LogInt {
        public Log__ServerId(BAchillesHeel bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BAchillesHeel)getBelong())._ServerId = value; }
    }

    private static final class Log__SecureKey extends Zeze.Transaction.Logs.LogString {
        public Log__SecureKey(BAchillesHeel bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BAchillesHeel)getBelong())._SecureKey = value; }
    }

    private static final class Log__GlobalCacheManagerHashIndex extends Zeze.Transaction.Logs.LogInt {
        public Log__GlobalCacheManagerHashIndex(BAchillesHeel bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BAchillesHeel)getBelong())._GlobalCacheManagerHashIndex = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.GlobalCacheManagerWithRaft.BAchillesHeel: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ServerId=").append(getServerId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("SecureKey=").append(getSecureKey()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("GlobalCacheManagerHashIndex=").append(getGlobalCacheManagerHashIndex()).append(System.lineSeparator());
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
    public void decode(ByteBuffer _o_) {
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
    public boolean negativeCheck() {
        if (getServerId() < 0)
            return true;
        if (getGlobalCacheManagerHashIndex() < 0)
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
                case 1: _ServerId = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 2: _SecureKey = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 3: _GlobalCacheManagerHashIndex = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
            }
        }
    }
}
