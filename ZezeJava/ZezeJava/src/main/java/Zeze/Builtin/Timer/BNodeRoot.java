// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BNodeRoot extends Zeze.Transaction.Bean implements BNodeRootReadOnly {
    public static final long TYPEID = 4685790459206796029L;

    private long _HeadNodeId; // 节点双链表的头结点ID, tNodes表的key, 0表示空链表, 总是在头结点插入
    private long _TailNodeId; // 节点双链表的尾结点ID, tNodes表的key, 0表示空链表
    private long _LoadSerialNo; // 每次启动时都递增的序列号, 用来处理跟接管的并发
    private long _Version; // 最高的定时器版本(tIndexs.Version), 用于被接管时判断

    @Override
    public long getHeadNodeId() {
        if (!isManaged())
            return _HeadNodeId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _HeadNodeId;
        var log = (Log__HeadNodeId)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _HeadNodeId;
    }

    public void setHeadNodeId(long _v_) {
        if (!isManaged()) {
            _HeadNodeId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__HeadNodeId(this, 1, _v_));
    }

    @Override
    public long getTailNodeId() {
        if (!isManaged())
            return _TailNodeId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _TailNodeId;
        var log = (Log__TailNodeId)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _TailNodeId;
    }

    public void setTailNodeId(long _v_) {
        if (!isManaged()) {
            _TailNodeId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__TailNodeId(this, 2, _v_));
    }

    @Override
    public long getLoadSerialNo() {
        if (!isManaged())
            return _LoadSerialNo;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _LoadSerialNo;
        var log = (Log__LoadSerialNo)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _LoadSerialNo;
    }

    public void setLoadSerialNo(long _v_) {
        if (!isManaged()) {
            _LoadSerialNo = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__LoadSerialNo(this, 3, _v_));
    }

    @Override
    public long getVersion() {
        if (!isManaged())
            return _Version;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Version;
        var log = (Log__Version)_t_.getLog(objectId() + 4);
        return log != null ? log.value : _Version;
    }

    public void setVersion(long _v_) {
        if (!isManaged()) {
            _Version = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__Version(this, 4, _v_));
    }

    @SuppressWarnings("deprecation")
    public BNodeRoot() {
    }

    @SuppressWarnings("deprecation")
    public BNodeRoot(long _HeadNodeId_, long _TailNodeId_, long _LoadSerialNo_, long _Version_) {
        _HeadNodeId = _HeadNodeId_;
        _TailNodeId = _TailNodeId_;
        _LoadSerialNo = _LoadSerialNo_;
        _Version = _Version_;
    }

    @Override
    public void reset() {
        setHeadNodeId(0);
        setTailNodeId(0);
        setLoadSerialNo(0);
        setVersion(0);
        _unknown_ = null;
    }

    public void assign(BNodeRoot _o_) {
        setHeadNodeId(_o_.getHeadNodeId());
        setTailNodeId(_o_.getTailNodeId());
        setLoadSerialNo(_o_.getLoadSerialNo());
        setVersion(_o_.getVersion());
        _unknown_ = _o_._unknown_;
    }

    public BNodeRoot copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BNodeRoot copy() {
        var _c_ = new BNodeRoot();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BNodeRoot _a_, BNodeRoot _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__HeadNodeId extends Zeze.Transaction.Logs.LogLong {
        public Log__HeadNodeId(BNodeRoot _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BNodeRoot)getBelong())._HeadNodeId = value; }
    }

    private static final class Log__TailNodeId extends Zeze.Transaction.Logs.LogLong {
        public Log__TailNodeId(BNodeRoot _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BNodeRoot)getBelong())._TailNodeId = value; }
    }

    private static final class Log__LoadSerialNo extends Zeze.Transaction.Logs.LogLong {
        public Log__LoadSerialNo(BNodeRoot _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BNodeRoot)getBelong())._LoadSerialNo = value; }
    }

    private static final class Log__Version extends Zeze.Transaction.Logs.LogLong {
        public Log__Version(BNodeRoot _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BNodeRoot)getBelong())._Version = value; }
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        var _i1_ = Zeze.Util.Str.indent(_l_ + 4);
        _s_.append("Zeze.Builtin.Timer.BNodeRoot: {\n");
        _s_.append(_i1_).append("HeadNodeId=").append(getHeadNodeId()).append(",\n");
        _s_.append(_i1_).append("TailNodeId=").append(getTailNodeId()).append(",\n");
        _s_.append(_i1_).append("LoadSerialNo=").append(getLoadSerialNo()).append(",\n");
        _s_.append(_i1_).append("Version=").append(getVersion()).append('\n');
        _s_.append(Zeze.Util.Str.indent(_l_)).append('}');
    }

    private static int _PRE_ALLOC_SIZE_ = 16;

    @Override
    public int preAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void preAllocSize(int _s_) {
        _PRE_ALLOC_SIZE_ = _s_;
    }

    private byte[] _unknown_;

    public byte[] unknown() {
        return _unknown_;
    }

    public void clearUnknown() {
        _unknown_ = null;
    }

    @Override
    public void encode(ByteBuffer _o_) {
        ByteBuffer _u_ = null;
        var _ua_ = _unknown_;
        var _ui_ = _ua_ != null ? (_u_ = ByteBuffer.Wrap(_ua_)).readUnknownIndex() : Long.MAX_VALUE;
        int _i_ = 0;
        {
            long _x_ = getHeadNodeId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getTailNodeId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getLoadSerialNo();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getVersion();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        _o_.writeAllUnknownFields(_i_, _ui_, _u_);
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        ByteBuffer _u_ = null;
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setHeadNodeId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setTailNodeId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setLoadSerialNo(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setVersion(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BNodeRoot))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BNodeRoot)_o_;
        if (getHeadNodeId() != _b_.getHeadNodeId())
            return false;
        if (getTailNodeId() != _b_.getTailNodeId())
            return false;
        if (getLoadSerialNo() != _b_.getLoadSerialNo())
            return false;
        if (getVersion() != _b_.getVersion())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getHeadNodeId() < 0)
            return true;
        if (getTailNodeId() < 0)
            return true;
        if (getLoadSerialNo() < 0)
            return true;
        if (getVersion() < 0)
            return true;
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void followerApply(Zeze.Transaction.Log _l_) {
        var _vs_ = ((Zeze.Transaction.Collections.LogBean)_l_).getVariables();
        if (_vs_ == null)
            return;
        for (var _i_ = _vs_.iterator(); _i_.moveToNext(); ) {
            var _v_ = _i_.value();
            switch (_v_.getVariableId()) {
                case 1: _HeadNodeId = _v_.longValue(); break;
                case 2: _TailNodeId = _v_.longValue(); break;
                case 3: _LoadSerialNo = _v_.longValue(); break;
                case 4: _Version = _v_.longValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setHeadNodeId(_r_.getLong(_pn_ + "HeadNodeId"));
        setTailNodeId(_r_.getLong(_pn_ + "TailNodeId"));
        setLoadSerialNo(_r_.getLong(_pn_ + "LoadSerialNo"));
        setVersion(_r_.getLong(_pn_ + "Version"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendLong(_pn_ + "HeadNodeId", getHeadNodeId());
        _s_.appendLong(_pn_ + "TailNodeId", getTailNodeId());
        _s_.appendLong(_pn_ + "LoadSerialNo", getLoadSerialNo());
        _s_.appendLong(_pn_ + "Version", getVersion());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "HeadNodeId", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "TailNodeId", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "LoadSerialNo", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "Version", "long", "", ""));
        return _v_;
    }
}
