// auto-generated @formatter:off
package Zeze.Builtin.ServiceManagerWithRaft;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BAllocateIdResult extends Zeze.Transaction.Bean implements BAllocateIdResultReadOnly {
    public static final long TYPEID = 2977588884516208594L;

    private String _Name;
    private long _StartId;
    private int _Count;

    @Override
    public String getName() {
        if (!isManaged())
            return _Name;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Name;
        var log = (Log__Name)txn.getLog(objectId() + 1);
        return log != null ? log.value : _Name;
    }

    public void setName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Name = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Name(this, 1, value));
    }

    @Override
    public long getStartId() {
        if (!isManaged())
            return _StartId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _StartId;
        var log = (Log__StartId)txn.getLog(objectId() + 2);
        return log != null ? log.value : _StartId;
    }

    public void setStartId(long value) {
        if (!isManaged()) {
            _StartId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__StartId(this, 2, value));
    }

    @Override
    public int getCount() {
        if (!isManaged())
            return _Count;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Count;
        var log = (Log__Count)txn.getLog(objectId() + 3);
        return log != null ? log.value : _Count;
    }

    public void setCount(int value) {
        if (!isManaged()) {
            _Count = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Count(this, 3, value));
    }

    @SuppressWarnings("deprecation")
    public BAllocateIdResult() {
        _Name = "";
    }

    @SuppressWarnings("deprecation")
    public BAllocateIdResult(String _Name_, long _StartId_, int _Count_) {
        if (_Name_ == null)
            throw new IllegalArgumentException();
        _Name = _Name_;
        _StartId = _StartId_;
        _Count = _Count_;
    }

    public void assign(BAllocateIdResult other) {
        setName(other.getName());
        setStartId(other.getStartId());
        setCount(other.getCount());
    }

    @Deprecated
    public void Assign(BAllocateIdResult other) {
        assign(other);
    }

    public BAllocateIdResult copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BAllocateIdResult copy() {
        var copy = new BAllocateIdResult();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BAllocateIdResult Copy() {
        return copy();
    }

    public static void swap(BAllocateIdResult a, BAllocateIdResult b) {
        BAllocateIdResult save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Name extends Zeze.Transaction.Logs.LogString {
        public Log__Name(BAllocateIdResult bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BAllocateIdResult)getBelong())._Name = value; }
    }

    private static final class Log__StartId extends Zeze.Transaction.Logs.LogLong {
        public Log__StartId(BAllocateIdResult bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BAllocateIdResult)getBelong())._StartId = value; }
    }

    private static final class Log__Count extends Zeze.Transaction.Logs.LogInt {
        public Log__Count(BAllocateIdResult bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BAllocateIdResult)getBelong())._Count = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.ServiceManagerWithRaft.BAllocateIdResult: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Name=").append(getName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("StartId=").append(getStartId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Count=").append(getCount()).append(System.lineSeparator());
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
            String _x_ = getName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            long _x_ = getStartId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            int _x_ = getCount();
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
            setName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setStartId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setCount(_o_.ReadInt(_t_));
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
        if (getStartId() < 0)
            return true;
        if (getCount() < 0)
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
                case 1: _Name = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 2: _StartId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 3: _Count = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
            }
        }
    }
}
