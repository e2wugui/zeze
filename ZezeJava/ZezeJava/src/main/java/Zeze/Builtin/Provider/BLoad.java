// auto-generated @formatter:off
package Zeze.Builtin.Provider;

import Zeze.Serialize.ByteBuffer;

public final class BLoad extends Zeze.Transaction.Bean {
    private int _Online; // 用户数量
    private int _ProposeMaxOnline; // 建议最大用户数量
    private int _OnlineNew; // 最近上线用户数量，一般是一秒内的。用来防止短时间内给同一个gs分配太多用户。

    public int getOnline() {
        if (!isManaged())
            return _Online;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _Online;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__Online)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.getValue() : _Online;
    }

    public void setOnline(int value) {
        if (!isManaged()) {
            _Online = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__Online(this, value));
    }

    public int getProposeMaxOnline() {
        if (!isManaged())
            return _ProposeMaxOnline;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _ProposeMaxOnline;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ProposeMaxOnline)txn.GetLog(this.getObjectId() + 2);
        return log != null ? log.getValue() : _ProposeMaxOnline;
    }

    public void setProposeMaxOnline(int value) {
        if (!isManaged()) {
            _ProposeMaxOnline = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__ProposeMaxOnline(this, value));
    }

    public int getOnlineNew() {
        if (!isManaged())
            return _OnlineNew;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _OnlineNew;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__OnlineNew)txn.GetLog(this.getObjectId() + 3);
        return log != null ? log.getValue() : _OnlineNew;
    }

    public void setOnlineNew(int value) {
        if (!isManaged()) {
            _OnlineNew = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__OnlineNew(this, value));
    }

    public BLoad() {
         this(0);
    }

    public BLoad(int _varId_) {
        super(_varId_);
    }

    public void Assign(BLoad other) {
        setOnline(other.getOnline());
        setProposeMaxOnline(other.getProposeMaxOnline());
        setOnlineNew(other.getOnlineNew());
    }

    public BLoad CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BLoad Copy() {
        var copy = new BLoad();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BLoad a, BLoad b) {
        BLoad save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public static final long TYPEID = 8972064501607813483L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private static final class Log__Online extends Zeze.Transaction.Log1<BLoad, Integer> {
        public Log__Online(BLoad self, Integer value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 1; }
        @Override
        public void Commit() { this.getBeanTyped()._Online = this.getValue(); }
    }

    private static final class Log__ProposeMaxOnline extends Zeze.Transaction.Log1<BLoad, Integer> {
        public Log__ProposeMaxOnline(BLoad self, Integer value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 2; }
        @Override
        public void Commit() { this.getBeanTyped()._ProposeMaxOnline = this.getValue(); }
    }

    private static final class Log__OnlineNew extends Zeze.Transaction.Log1<BLoad, Integer> {
        public Log__OnlineNew(BLoad self, Integer value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 3; }
        @Override
        public void Commit() { this.getBeanTyped()._OnlineNew = this.getValue(); }
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
    public int getPreAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void setPreAllocSize(int size) {
        _PRE_ALLOC_SIZE_ = size;
    }

    @SuppressWarnings("UnusedAssignment")
    @Override
    public void Encode(ByteBuffer _o_) {
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

    @SuppressWarnings("UnusedAssignment")
    @Override
    public void Decode(ByteBuffer _o_) {
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
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean NegativeCheck() {
        if (getOnline() < 0)
            return true;
        if (getProposeMaxOnline() < 0)
            return true;
        if (getOnlineNew() < 0)
            return true;
        return false;
    }
}
