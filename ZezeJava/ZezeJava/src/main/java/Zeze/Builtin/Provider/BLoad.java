// auto-generated @formatter:off
package Zeze.Builtin.Provider;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BLoad extends Zeze.Transaction.Bean {
    private int _Online; // 用户数量
    private int _ProposeMaxOnline; // 建议最大用户数量
    private int _OnlineNew; // 最近上线用户数量，一般是一秒内的。用来防止短时间内给同一个gs分配太多用户。

    public int getOnline() {
        if (!isManaged())
            return _Online;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Online;
        var log = (Log__Online)txn.getLog(objectId() + 1);
        return log != null ? log.value : _Online;
    }

    public void setOnline(int value) {
        if (!isManaged()) {
            _Online = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Online(this, 1, value));
    }

    public int getProposeMaxOnline() {
        if (!isManaged())
            return _ProposeMaxOnline;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ProposeMaxOnline;
        var log = (Log__ProposeMaxOnline)txn.getLog(objectId() + 2);
        return log != null ? log.value : _ProposeMaxOnline;
    }

    public void setProposeMaxOnline(int value) {
        if (!isManaged()) {
            _ProposeMaxOnline = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ProposeMaxOnline(this, 2, value));
    }

    public int getOnlineNew() {
        if (!isManaged())
            return _OnlineNew;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _OnlineNew;
        var log = (Log__OnlineNew)txn.getLog(objectId() + 3);
        return log != null ? log.value : _OnlineNew;
    }

    public void setOnlineNew(int value) {
        if (!isManaged()) {
            _OnlineNew = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__OnlineNew(this, 3, value));
    }

    @SuppressWarnings("deprecation")
    public BLoad() {
    }

    @SuppressWarnings("deprecation")
    public BLoad(int _Online_, int _ProposeMaxOnline_, int _OnlineNew_) {
        _Online = _Online_;
        _ProposeMaxOnline = _ProposeMaxOnline_;
        _OnlineNew = _OnlineNew_;
    }

    public void assign(BLoad other) {
        setOnline(other.getOnline());
        setProposeMaxOnline(other.getProposeMaxOnline());
        setOnlineNew(other.getOnlineNew());
    }

    @Deprecated
    public void Assign(BLoad other) {
        assign(other);
    }

    public BLoad copyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BLoad copy() {
        var copy = new BLoad();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BLoad Copy() {
        return copy();
    }

    public static void swap(BLoad a, BLoad b) {
        BLoad save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public BLoad copyBean() {
        return Copy();
    }

    public static final long TYPEID = 8972064501607813483L;

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Online extends Zeze.Transaction.Logs.LogInt {
        public Log__Online(BLoad bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BLoad)getBelong())._Online = value; }
    }

    private static final class Log__ProposeMaxOnline extends Zeze.Transaction.Logs.LogInt {
        public Log__ProposeMaxOnline(BLoad bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BLoad)getBelong())._ProposeMaxOnline = value; }
    }

    private static final class Log__OnlineNew extends Zeze.Transaction.Logs.LogInt {
        public Log__OnlineNew(BLoad bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BLoad)getBelong())._OnlineNew = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Provider.BLoad: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Online").append('=').append(getOnline()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ProposeMaxOnline").append('=').append(getProposeMaxOnline()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("OnlineNew").append('=').append(getOnlineNew()).append(System.lineSeparator());
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
            int _x_ = getOnline();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getProposeMaxOnline();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getOnlineNew();
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
            setOnline(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setProposeMaxOnline(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setOnlineNew(_o_.ReadInt(_t_));
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
        if (getOnline() < 0)
            return true;
        if (getProposeMaxOnline() < 0)
            return true;
        if (getOnlineNew() < 0)
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
                case 1: _Online = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 2: _ProposeMaxOnline = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 3: _OnlineNew = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
            }
        }
    }
}
