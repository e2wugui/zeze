// auto-generated @formatter:off
package Zeze.Builtin.Collections.Queue;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

/*
				1. 单向链表。2. Value没有索引。3. 每个Value记录加入的时间。4. 只能从Head提取，从Tail添加。5. 用作Stack时也可以从Head添加。
				链表结构: (NewStackNode -＞) Head -＞ ... -＞ Tail (-＞ NewQueueNode)。
				第一个用户是Table.GC，延迟删除记录。
				【兼容】
				单向链表原来只发生在自己的Queue内，使用long NodeId指向下一个节点，查询节点时候总是使用自己的Queue.Name和NodeId构造BQueueNodeKey。
				现在为了支持在Queue之间splice，需要使用BQueueNodeKey来指示下一个节点。
				为了兼容旧数据，原来的long类型的变量不能删除，新版需要发现是旧版数据，然后读取并构造出新的BQueueNodeKey。
				1. Root(BQueue)兼容旧数据规则：
				if (Root.HeadNodeKey.Name.isEmpty()) {
					Root.HeadNodeKey = new BQueueNodeKey(ThisQueue.Name, Root.HeadNodeId);
					Root.TailNodeKey = new BQueueNodeKey(ThisQueue.Name, Root.TailNodeId);
				}
				2. Node(BQueueNode) 兼容旧数据规则：
				if (Node.NextNodeKey.Name.isEmpty()) {
					Node.NextNodeKey = new BQueueNodeKey(ThisNode.NodeKey.Name, Node.NextNodeId);
				}
				3. Splice两个Queue时，指向另一个Queue的NodeKey需要先处理好，即已经是新版的结构。现在的代码刚好符合。
*/
@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BQueue extends Zeze.Transaction.Bean implements BQueueReadOnly {
    public static final long TYPEID = -4684745065046332255L;

    private long _HeadNodeId; // 废弃，新的遍历寻找使用HeadNodeKey，【但是不能删，兼容需要读取】
    private long _TailNodeId; // 废弃，新的遍历寻找使用TailNodeKey，【但是不能删，兼容需要读取】
    private long _Count;
    private long _LastNodeId; // 最近分配过的NodeId, 用于下次分配
    private long _LoadSerialNo; // walk 开始的时候递增
    private Zeze.Builtin.Collections.Queue.BQueueNodeKey _HeadNodeKey;
    private Zeze.Builtin.Collections.Queue.BQueueNodeKey _TailNodeKey;

    private static final java.lang.invoke.VarHandle vh_HeadNodeId;
    private static final java.lang.invoke.VarHandle vh_TailNodeId;
    private static final java.lang.invoke.VarHandle vh_Count;
    private static final java.lang.invoke.VarHandle vh_LastNodeId;
    private static final java.lang.invoke.VarHandle vh_LoadSerialNo;
    private static final java.lang.invoke.VarHandle vh_HeadNodeKey;
    private static final java.lang.invoke.VarHandle vh_TailNodeKey;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_HeadNodeId = _l_.findVarHandle(BQueue.class, "_HeadNodeId", long.class);
            vh_TailNodeId = _l_.findVarHandle(BQueue.class, "_TailNodeId", long.class);
            vh_Count = _l_.findVarHandle(BQueue.class, "_Count", long.class);
            vh_LastNodeId = _l_.findVarHandle(BQueue.class, "_LastNodeId", long.class);
            vh_LoadSerialNo = _l_.findVarHandle(BQueue.class, "_LoadSerialNo", long.class);
            vh_HeadNodeKey = _l_.findVarHandle(BQueue.class, "_HeadNodeKey", Zeze.Builtin.Collections.Queue.BQueueNodeKey.class);
            vh_TailNodeKey = _l_.findVarHandle(BQueue.class, "_TailNodeKey", Zeze.Builtin.Collections.Queue.BQueueNodeKey.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public long getHeadNodeId() {
        if (!isManaged())
            return _HeadNodeId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _HeadNodeId;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _HeadNodeId;
    }

    public void setHeadNodeId(long _v_) {
        if (!isManaged()) {
            _HeadNodeId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 1, vh_HeadNodeId, _v_));
    }

    @Override
    public long getTailNodeId() {
        if (!isManaged())
            return _TailNodeId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _TailNodeId;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _TailNodeId;
    }

    public void setTailNodeId(long _v_) {
        if (!isManaged()) {
            _TailNodeId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 2, vh_TailNodeId, _v_));
    }

    @Override
    public long getCount() {
        if (!isManaged())
            return _Count;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Count;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _Count;
    }

    public void setCount(long _v_) {
        if (!isManaged()) {
            _Count = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 3, vh_Count, _v_));
    }

    @Override
    public long getLastNodeId() {
        if (!isManaged())
            return _LastNodeId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _LastNodeId;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 4);
        return log != null ? log.value : _LastNodeId;
    }

    public void setLastNodeId(long _v_) {
        if (!isManaged()) {
            _LastNodeId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 4, vh_LastNodeId, _v_));
    }

    @Override
    public long getLoadSerialNo() {
        if (!isManaged())
            return _LoadSerialNo;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _LoadSerialNo;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 5);
        return log != null ? log.value : _LoadSerialNo;
    }

    public void setLoadSerialNo(long _v_) {
        if (!isManaged()) {
            _LoadSerialNo = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 5, vh_LoadSerialNo, _v_));
    }

    @Override
    public Zeze.Builtin.Collections.Queue.BQueueNodeKey getHeadNodeKey() {
        if (!isManaged())
            return _HeadNodeKey;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _HeadNodeKey;
        @SuppressWarnings("unchecked")
        var log = (Zeze.Transaction.Logs.LogBeanKey<Zeze.Builtin.Collections.Queue.BQueueNodeKey>)_t_.getLog(objectId() + 6);
        return log != null ? log.value : _HeadNodeKey;
    }

    public void setHeadNodeKey(Zeze.Builtin.Collections.Queue.BQueueNodeKey _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _HeadNodeKey = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBeanKey<>(this, 6, vh_HeadNodeKey, _v_));
    }

    @Override
    public Zeze.Builtin.Collections.Queue.BQueueNodeKey getTailNodeKey() {
        if (!isManaged())
            return _TailNodeKey;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _TailNodeKey;
        @SuppressWarnings("unchecked")
        var log = (Zeze.Transaction.Logs.LogBeanKey<Zeze.Builtin.Collections.Queue.BQueueNodeKey>)_t_.getLog(objectId() + 7);
        return log != null ? log.value : _TailNodeKey;
    }

    public void setTailNodeKey(Zeze.Builtin.Collections.Queue.BQueueNodeKey _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _TailNodeKey = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBeanKey<>(this, 7, vh_TailNodeKey, _v_));
    }

    @SuppressWarnings("deprecation")
    public BQueue() {
        _HeadNodeKey = new Zeze.Builtin.Collections.Queue.BQueueNodeKey();
        _TailNodeKey = new Zeze.Builtin.Collections.Queue.BQueueNodeKey();
    }

    @SuppressWarnings("deprecation")
    public BQueue(long _HeadNodeId_, long _TailNodeId_, long _Count_, long _LastNodeId_, long _LoadSerialNo_, Zeze.Builtin.Collections.Queue.BQueueNodeKey _HeadNodeKey_, Zeze.Builtin.Collections.Queue.BQueueNodeKey _TailNodeKey_) {
        _HeadNodeId = _HeadNodeId_;
        _TailNodeId = _TailNodeId_;
        _Count = _Count_;
        _LastNodeId = _LastNodeId_;
        _LoadSerialNo = _LoadSerialNo_;
        if (_HeadNodeKey_ == null)
            _HeadNodeKey_ = new Zeze.Builtin.Collections.Queue.BQueueNodeKey();
        _HeadNodeKey = _HeadNodeKey_;
        if (_TailNodeKey_ == null)
            _TailNodeKey_ = new Zeze.Builtin.Collections.Queue.BQueueNodeKey();
        _TailNodeKey = _TailNodeKey_;
    }

    @Override
    public void reset() {
        setHeadNodeId(0);
        setTailNodeId(0);
        setCount(0);
        setLastNodeId(0);
        setLoadSerialNo(0);
        setHeadNodeKey(new Zeze.Builtin.Collections.Queue.BQueueNodeKey());
        setTailNodeKey(new Zeze.Builtin.Collections.Queue.BQueueNodeKey());
        _unknown_ = null;
    }

    public void assign(BQueue _o_) {
        setHeadNodeId(_o_.getHeadNodeId());
        setTailNodeId(_o_.getTailNodeId());
        setCount(_o_.getCount());
        setLastNodeId(_o_.getLastNodeId());
        setLoadSerialNo(_o_.getLoadSerialNo());
        setHeadNodeKey(_o_.getHeadNodeKey());
        setTailNodeKey(_o_.getTailNodeKey());
        _unknown_ = _o_._unknown_;
    }

    public BQueue copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BQueue copy() {
        var _c_ = new BQueue();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BQueue _a_, BQueue _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
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
        _s_.append("Zeze.Builtin.Collections.Queue.BQueue: {\n");
        _s_.append(_i1_).append("HeadNodeId=").append(getHeadNodeId()).append(",\n");
        _s_.append(_i1_).append("TailNodeId=").append(getTailNodeId()).append(",\n");
        _s_.append(_i1_).append("Count=").append(getCount()).append(",\n");
        _s_.append(_i1_).append("LastNodeId=").append(getLastNodeId()).append(",\n");
        _s_.append(_i1_).append("LoadSerialNo=").append(getLoadSerialNo()).append(",\n");
        _s_.append(_i1_).append("HeadNodeKey=");
        getHeadNodeKey().buildString(_s_, _l_ + 8);
        _s_.append(",\n");
        _s_.append(_i1_).append("TailNodeKey=");
        getTailNodeKey().buildString(_s_, _l_ + 8);
        _s_.append('\n');
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
            long _x_ = getCount();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getLastNodeId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getLoadSerialNo();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 6, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            getHeadNodeKey().encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 7, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            getTailNodeKey().encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
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
            setCount(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setLastNodeId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setLoadSerialNo(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            _o_.ReadBean(getHeadNodeKey(), _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 7) {
            _o_.ReadBean(getTailNodeKey(), _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BQueue))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BQueue)_o_;
        if (getHeadNodeId() != _b_.getHeadNodeId())
            return false;
        if (getTailNodeId() != _b_.getTailNodeId())
            return false;
        if (getCount() != _b_.getCount())
            return false;
        if (getLastNodeId() != _b_.getLastNodeId())
            return false;
        if (getLoadSerialNo() != _b_.getLoadSerialNo())
            return false;
        if (!getHeadNodeKey().equals(_b_.getHeadNodeKey()))
            return false;
        if (!getTailNodeKey().equals(_b_.getTailNodeKey()))
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getHeadNodeId() < 0)
            return true;
        if (getTailNodeId() < 0)
            return true;
        if (getCount() < 0)
            return true;
        if (getLastNodeId() < 0)
            return true;
        if (getLoadSerialNo() < 0)
            return true;
        if (getHeadNodeKey().negativeCheck())
            return true;
        if (getTailNodeKey().negativeCheck())
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
                case 3: _Count = _v_.longValue(); break;
                case 4: _LastNodeId = _v_.longValue(); break;
                case 5: _LoadSerialNo = _v_.longValue(); break;
                case 6: _HeadNodeKey = ((Zeze.Transaction.Logs.LogBeanKey<Zeze.Builtin.Collections.Queue.BQueueNodeKey>)_v_).value; break;
                case 7: _TailNodeKey = ((Zeze.Transaction.Logs.LogBeanKey<Zeze.Builtin.Collections.Queue.BQueueNodeKey>)_v_).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setHeadNodeId(_r_.getLong(_pn_ + "HeadNodeId"));
        setTailNodeId(_r_.getLong(_pn_ + "TailNodeId"));
        setCount(_r_.getLong(_pn_ + "Count"));
        setLastNodeId(_r_.getLong(_pn_ + "LastNodeId"));
        setLoadSerialNo(_r_.getLong(_pn_ + "LoadSerialNo"));
        _p_.add("HeadNodeKey");
        getHeadNodeKey().decodeResultSet(_p_, _r_);
        _p_.remove(_p_.size() - 1);
        _p_.add("TailNodeKey");
        getTailNodeKey().decodeResultSet(_p_, _r_);
        _p_.remove(_p_.size() - 1);
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendLong(_pn_ + "HeadNodeId", getHeadNodeId());
        _s_.appendLong(_pn_ + "TailNodeId", getTailNodeId());
        _s_.appendLong(_pn_ + "Count", getCount());
        _s_.appendLong(_pn_ + "LastNodeId", getLastNodeId());
        _s_.appendLong(_pn_ + "LoadSerialNo", getLoadSerialNo());
        _p_.add("HeadNodeKey");
        getHeadNodeKey().encodeSQLStatement(_p_, _s_);
        _p_.remove(_p_.size() - 1);
        _p_.add("TailNodeKey");
        getTailNodeKey().encodeSQLStatement(_p_, _s_);
        _p_.remove(_p_.size() - 1);
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "HeadNodeId", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "TailNodeId", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Count", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "LastNodeId", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "LoadSerialNo", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(6, "HeadNodeKey", "Zeze.Builtin.Collections.Queue.BQueueNodeKey", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(7, "TailNodeKey", "Zeze.Builtin.Collections.Queue.BQueueNodeKey", "", ""));
        return _v_;
    }
}
