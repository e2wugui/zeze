// auto-generated @formatter:off
package Zeze.Builtin.ServiceManagerWithRaft;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BAllocateIdArgument extends Zeze.Transaction.Bean implements BAllocateIdArgumentReadOnly {
    public static final long TYPEID = -6520929625965816878L;

    private String _Name;
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
    public int getCount() {
        if (!isManaged())
            return _Count;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Count;
        var log = (Log__Count)txn.getLog(objectId() + 2);
        return log != null ? log.value : _Count;
    }

    public void setCount(int value) {
        if (!isManaged()) {
            _Count = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Count(this, 2, value));
    }

    @SuppressWarnings("deprecation")
    public BAllocateIdArgument() {
        _Name = "";
    }

    @SuppressWarnings("deprecation")
    public BAllocateIdArgument(String _Name_, int _Count_) {
        if (_Name_ == null)
            throw new IllegalArgumentException();
        _Name = _Name_;
        _Count = _Count_;
    }

    public void assign(BAllocateIdArgument other) {
        setName(other.getName());
        setCount(other.getCount());
    }

    @Deprecated
    public void Assign(BAllocateIdArgument other) {
        assign(other);
    }

    public BAllocateIdArgument copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BAllocateIdArgument copy() {
        var copy = new BAllocateIdArgument();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BAllocateIdArgument Copy() {
        return copy();
    }

    public static void swap(BAllocateIdArgument a, BAllocateIdArgument b) {
        BAllocateIdArgument save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Name extends Zeze.Transaction.Logs.LogString {
        public Log__Name(BAllocateIdArgument bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BAllocateIdArgument)getBelong())._Name = value; }
    }

    private static final class Log__Count extends Zeze.Transaction.Logs.LogInt {
        public Log__Count(BAllocateIdArgument bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BAllocateIdArgument)getBelong())._Count = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.ServiceManagerWithRaft.BAllocateIdArgument: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Name=").append(getName()).append(',').append(System.lineSeparator());
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
            int _x_ = getCount();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
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
                case 2: _Count = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
            }
        }
    }
}
