// auto-generated @formatter:off
package Zeze.Builtin.Online;

import Zeze.Serialize.ByteBuffer;

// tables
@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BOnline extends Zeze.Transaction.Bean implements BOnlineReadOnly {
    public static final long TYPEID = -7786403987996508020L;

    private String _LinkName;
    private long _LinkSid;

    private transient Object __zeze_map_key__;

    @Override
    public Object mapKey() {
        return __zeze_map_key__;
    }

    @Override
    public void mapKey(Object value) {
        __zeze_map_key__ = value;
    }

    @Override
    public String getLinkName() {
        if (!isManaged())
            return _LinkName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _LinkName;
        var log = (Log__LinkName)txn.getLog(objectId() + 1);
        return log != null ? log.value : _LinkName;
    }

    public void setLinkName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _LinkName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__LinkName(this, 1, value));
    }

    @Override
    public long getLinkSid() {
        if (!isManaged())
            return _LinkSid;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _LinkSid;
        var log = (Log__LinkSid)txn.getLog(objectId() + 2);
        return log != null ? log.value : _LinkSid;
    }

    public void setLinkSid(long value) {
        if (!isManaged()) {
            _LinkSid = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__LinkSid(this, 2, value));
    }

    @SuppressWarnings("deprecation")
    public BOnline() {
        _LinkName = "";
    }

    @SuppressWarnings("deprecation")
    public BOnline(String _LinkName_, long _LinkSid_) {
        if (_LinkName_ == null)
            throw new IllegalArgumentException();
        _LinkName = _LinkName_;
        _LinkSid = _LinkSid_;
    }

    public void assign(BOnline other) {
        setLinkName(other.getLinkName());
        setLinkSid(other.getLinkSid());
    }

    @Deprecated
    public void Assign(BOnline other) {
        assign(other);
    }

    public BOnline copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BOnline copy() {
        var copy = new BOnline();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BOnline Copy() {
        return copy();
    }

    public static void swap(BOnline a, BOnline b) {
        BOnline save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__LinkName extends Zeze.Transaction.Logs.LogString {
        public Log__LinkName(BOnline bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BOnline)getBelong())._LinkName = value; }
    }

    private static final class Log__LinkSid extends Zeze.Transaction.Logs.LogLong {
        public Log__LinkSid(BOnline bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BOnline)getBelong())._LinkSid = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Online.BOnline: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("LinkName").append('=').append(getLinkName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("LinkSid").append('=').append(getLinkSid()).append(System.lineSeparator());
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
            String _x_ = getLinkName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            long _x_ = getLinkSid();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setLinkName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setLinkSid(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
    }

    @Override
    protected void resetChildrenRootInfo() {
    }

    @Override
    public boolean negativeCheck() {
        if (getLinkSid() < 0)
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
                case 1: _LinkName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 2: _LinkSid = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
            }
        }
    }
}
