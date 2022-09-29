// auto-generated @formatter:off
package Zeze.Builtin.LinkdBase;

import Zeze.Serialize.ByteBuffer;

// linkd to client
@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BReportError extends Zeze.Transaction.Bean {
    public static final long TYPEID = -947669033141460287L;

    public static final int FromLink = 0;
    public static final int FromProvider = 1;
    public static final int CodeNotAuthed = 1;
    public static final int CodeNoProvider = 2;

    private int _from;
    private int _code;
    private String _desc;

    public int getFrom() {
        if (!isManaged())
            return _from;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _from;
        var log = (Log__from)txn.getLog(objectId() + 1);
        return log != null ? log.value : _from;
    }

    public void setFrom(int value) {
        if (!isManaged()) {
            _from = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__from(this, 1, value));
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
    public BReportError() {
        _desc = "";
    }

    @SuppressWarnings("deprecation")
    public BReportError(int _from_, int _code_, String _desc_) {
        _from = _from_;
        _code = _code_;
        if (_desc_ == null)
            throw new IllegalArgumentException();
        _desc = _desc_;
    }

    public void assign(BReportError other) {
        setFrom(other.getFrom());
        setCode(other.getCode());
        setDesc(other.getDesc());
    }

    @Deprecated
    public void Assign(BReportError other) {
        assign(other);
    }

    public BReportError copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BReportError copy() {
        var copy = new BReportError();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BReportError Copy() {
        return copy();
    }

    public static void swap(BReportError a, BReportError b) {
        BReportError save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__from extends Zeze.Transaction.Logs.LogInt {
        public Log__from(BReportError bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BReportError)getBelong())._from = value; }
    }

    private static final class Log__code extends Zeze.Transaction.Logs.LogInt {
        public Log__code(BReportError bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BReportError)getBelong())._code = value; }
    }

    private static final class Log__desc extends Zeze.Transaction.Logs.LogString {
        public Log__desc(BReportError bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BReportError)getBelong())._desc = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.LinkdBase.BReportError: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("from").append('=').append(getFrom()).append(',').append(System.lineSeparator());
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
            int _x_ = getFrom();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
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
            setFrom(_o_.ReadInt(_t_));
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
        if (getFrom() < 0)
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
                case 1: _from = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 2: _code = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 3: _desc = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
            }
        }
    }
}
