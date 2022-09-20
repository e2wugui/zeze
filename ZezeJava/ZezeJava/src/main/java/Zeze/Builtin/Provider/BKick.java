// auto-generated @formatter:off
package Zeze.Builtin.Provider;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BKick extends Zeze.Transaction.Bean {
    public static final long TYPEID = -6855697390328479333L;

    public static final int ErrorProtocolUnknown = 1;
    public static final int ErrorDecode = 2;
    public static final int ErrorProtocolException = 3;
    public static final int ErrorDuplicateLogin = 4;
    public static final int ErrorSeeDescription = 5;

    private long _linksid;
    private int _code;
    private String _desc; // // for debug

    public long getLinksid() {
        if (!isManaged())
            return _linksid;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _linksid;
        var log = (Log__linksid)txn.getLog(objectId() + 1);
        return log != null ? log.value : _linksid;
    }

    public void setLinksid(long value) {
        if (!isManaged()) {
            _linksid = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__linksid(this, 1, value));
    }

    public int getCode() {
        if (!isManaged())
            return _code;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _code;
        var log = (Log__code)txn.getLog(objectId() + 2);
        return log != null ? log.value : _code;
    }

    public void setCode(int value) {
        if (!isManaged()) {
            _code = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__code(this, 2, value));
    }

    public String getDesc() {
        if (!isManaged())
            return _desc;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _desc;
        var log = (Log__desc)txn.getLog(objectId() + 3);
        return log != null ? log.value : _desc;
    }

    public void setDesc(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _desc = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__desc(this, 3, value));
    }

    @SuppressWarnings("deprecation")
    public BKick() {
        _desc = "";
    }

    @SuppressWarnings("deprecation")
    public BKick(long _linksid_, int _code_, String _desc_) {
        _linksid = _linksid_;
        _code = _code_;
        if (_desc_ == null)
            throw new IllegalArgumentException();
        _desc = _desc_;
    }

    public void assign(BKick other) {
        setLinksid(other.getLinksid());
        setCode(other.getCode());
        setDesc(other.getDesc());
    }

    @Deprecated
    public void Assign(BKick other) {
        assign(other);
    }

    public BKick copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BKick copy() {
        var copy = new BKick();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BKick Copy() {
        return copy();
    }

    public static void swap(BKick a, BKick b) {
        BKick save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__linksid extends Zeze.Transaction.Logs.LogLong {
        public Log__linksid(BKick bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BKick)getBelong())._linksid = value; }
    }

    private static final class Log__code extends Zeze.Transaction.Logs.LogInt {
        public Log__code(BKick bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BKick)getBelong())._code = value; }
    }

    private static final class Log__desc extends Zeze.Transaction.Logs.LogString {
        public Log__desc(BKick bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BKick)getBelong())._desc = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Provider.BKick: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("linksid").append('=').append(getLinksid()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("code").append('=').append(getCode()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("desc").append('=').append(getDesc()).append(System.lineSeparator());
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
            long _x_ = getLinksid();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            int _x_ = getCode();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            String _x_ = getDesc();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setLinksid(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setCode(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setDesc(_o_.ReadString(_t_));
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
        if (getLinksid() < 0)
            return true;
        if (getCode() < 0)
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
                case 1: _linksid = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 2: _code = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 3: _desc = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
            }
        }
    }
}
