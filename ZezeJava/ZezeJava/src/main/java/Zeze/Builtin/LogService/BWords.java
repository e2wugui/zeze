// auto-generated @formatter:off
package Zeze.Builtin.LogService;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BWords extends Zeze.Transaction.Bean implements BWordsReadOnly {
    public static final long TYPEID = -5889615878938106586L;

    private long _BeginTime;
    private long _EndTime;
    private final Zeze.Transaction.Collections.PList1<String> _Words;
    private boolean _ContainsAll;

    @Override
    public long getBeginTime() {
        if (!isManaged())
            return _BeginTime;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _BeginTime;
        var log = (Log__BeginTime)txn.getLog(objectId() + 1);
        return log != null ? log.value : _BeginTime;
    }

    public void setBeginTime(long value) {
        if (!isManaged()) {
            _BeginTime = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__BeginTime(this, 1, value));
    }

    @Override
    public long getEndTime() {
        if (!isManaged())
            return _EndTime;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _EndTime;
        var log = (Log__EndTime)txn.getLog(objectId() + 2);
        return log != null ? log.value : _EndTime;
    }

    public void setEndTime(long value) {
        if (!isManaged()) {
            _EndTime = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__EndTime(this, 2, value));
    }

    public Zeze.Transaction.Collections.PList1<String> getWords() {
        return _Words;
    }

    @Override
    public Zeze.Transaction.Collections.PList1ReadOnly<String> getWordsReadOnly() {
        return new Zeze.Transaction.Collections.PList1ReadOnly<>(_Words);
    }

    @Override
    public boolean isContainsAll() {
        if (!isManaged())
            return _ContainsAll;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ContainsAll;
        var log = (Log__ContainsAll)txn.getLog(objectId() + 4);
        return log != null ? log.value : _ContainsAll;
    }

    public void setContainsAll(boolean value) {
        if (!isManaged()) {
            _ContainsAll = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ContainsAll(this, 4, value));
    }

    @SuppressWarnings("deprecation")
    public BWords() {
        _Words = new Zeze.Transaction.Collections.PList1<>(String.class);
        _Words.variableId(3);
    }

    @SuppressWarnings("deprecation")
    public BWords(long _BeginTime_, long _EndTime_, boolean _ContainsAll_) {
        _BeginTime = _BeginTime_;
        _EndTime = _EndTime_;
        _Words = new Zeze.Transaction.Collections.PList1<>(String.class);
        _Words.variableId(3);
        _ContainsAll = _ContainsAll_;
    }

    @Override
    public void reset() {
        setBeginTime(0);
        setEndTime(0);
        _Words.clear();
        setContainsAll(false);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.LogService.BWords.Data toData() {
        var data = new Zeze.Builtin.LogService.BWords.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.LogService.BWords.Data)other);
    }

    public void assign(BWords.Data other) {
        setBeginTime(other._BeginTime);
        setEndTime(other._EndTime);
        _Words.clear();
        _Words.addAll(other._Words);
        setContainsAll(other._ContainsAll);
        _unknown_ = null;
    }

    public void assign(BWords other) {
        setBeginTime(other.getBeginTime());
        setEndTime(other.getEndTime());
        _Words.clear();
        _Words.addAll(other._Words);
        setContainsAll(other.isContainsAll());
        _unknown_ = other._unknown_;
    }

    public BWords copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BWords copy() {
        var copy = new BWords();
        copy.assign(this);
        return copy;
    }

    public static void swap(BWords a, BWords b) {
        BWords save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__BeginTime extends Zeze.Transaction.Logs.LogLong {
        public Log__BeginTime(BWords bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BWords)getBelong())._BeginTime = value; }
    }

    private static final class Log__EndTime extends Zeze.Transaction.Logs.LogLong {
        public Log__EndTime(BWords bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BWords)getBelong())._EndTime = value; }
    }

    private static final class Log__ContainsAll extends Zeze.Transaction.Logs.LogBool {
        public Log__ContainsAll(BWords bean, int varId, boolean value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BWords)getBelong())._ContainsAll = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.LogService.BWords: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("BeginTime=").append(getBeginTime()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("EndTime=").append(getEndTime()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Words=[");
        if (!_Words.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _Words) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(_item_).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ContainsAll=").append(isContainsAll()).append(System.lineSeparator());
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
            long _x_ = getBeginTime();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getEndTime();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = _Words;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BYTES);
                for (var _v_ : _x_) {
                    _o_.WriteString(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            boolean _x_ = isContainsAll();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        _o_.writeAllUnknownFields(_i_, _ui_, _u_);
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        ByteBuffer _u_ = null;
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setBeginTime(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setEndTime(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            var _x_ = _Words;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadString(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setContainsAll(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Words.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _Words.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getBeginTime() < 0)
            return true;
        if (getEndTime() < 0)
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
                case 1: _BeginTime = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 2: _EndTime = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 3: _Words.followerApply(vlog); break;
                case 4: _ContainsAll = ((Zeze.Transaction.Logs.LogBool)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setBeginTime(rs.getLong(_parents_name_ + "BeginTime"));
        setEndTime(rs.getLong(_parents_name_ + "EndTime"));
        Zeze.Serialize.Helper.decodeJsonList(_Words, String.class, rs.getString(_parents_name_ + "Words"));
        setContainsAll(rs.getBoolean(_parents_name_ + "ContainsAll"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendLong(_parents_name_ + "BeginTime", getBeginTime());
        st.appendLong(_parents_name_ + "EndTime", getEndTime());
        st.appendString(_parents_name_ + "Words", Zeze.Serialize.Helper.encodeJson(_Words));
        st.appendBoolean(_parents_name_ + "ContainsAll", isContainsAll());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "BeginTime", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "EndTime", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Words", "list", "", "string"));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "ContainsAll", "bool", "", ""));
        return vars;
    }

public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -5889615878938106586L;

    private long _BeginTime;
    private long _EndTime;
    private java.util.ArrayList<String> _Words;
    private boolean _ContainsAll;

    public long getBeginTime() {
        return _BeginTime;
    }

    public void setBeginTime(long value) {
        _BeginTime = value;
    }

    public long getEndTime() {
        return _EndTime;
    }

    public void setEndTime(long value) {
        _EndTime = value;
    }

    public java.util.ArrayList<String> getWords() {
        return _Words;
    }

    public void setWords(java.util.ArrayList<String> value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Words = value;
    }

    public boolean isContainsAll() {
        return _ContainsAll;
    }

    public void setContainsAll(boolean value) {
        _ContainsAll = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Words = new java.util.ArrayList<>();
    }

    @SuppressWarnings("deprecation")
    public Data(long _BeginTime_, long _EndTime_, java.util.ArrayList<String> _Words_, boolean _ContainsAll_) {
        _BeginTime = _BeginTime_;
        _EndTime = _EndTime_;
        if (_Words_ == null)
            _Words_ = new java.util.ArrayList<>();
        _Words = _Words_;
        _ContainsAll = _ContainsAll_;
    }

    @Override
    public void reset() {
        _BeginTime = 0;
        _EndTime = 0;
        _Words.clear();
        _ContainsAll = false;
    }

    @Override
    public Zeze.Builtin.LogService.BWords toBean() {
        var bean = new Zeze.Builtin.LogService.BWords();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BWords)other);
    }

    public void assign(BWords other) {
        _BeginTime = other.getBeginTime();
        _EndTime = other.getEndTime();
        _Words.clear();
        _Words.addAll(other._Words);
        _ContainsAll = other.isContainsAll();
    }

    public void assign(BWords.Data other) {
        _BeginTime = other._BeginTime;
        _EndTime = other._EndTime;
        _Words.clear();
        _Words.addAll(other._Words);
        _ContainsAll = other._ContainsAll;
    }

    @Override
    public BWords.Data copy() {
        var copy = new BWords.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BWords.Data a, BWords.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BWords.Data clone() {
        return (BWords.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.LogService.BWords: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("BeginTime=").append(_BeginTime).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("EndTime=").append(_EndTime).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Words=[");
        if (!_Words.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _Words) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(_item_).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ContainsAll=").append(_ContainsAll).append(System.lineSeparator());
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
            long _x_ = _BeginTime;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = _EndTime;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = _Words;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BYTES);
                for (var _v_ : _x_) {
                    _o_.WriteString(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            boolean _x_ = _ContainsAll;
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _BeginTime = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _EndTime = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            var _x_ = _Words;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadString(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _ContainsAll = _o_.ReadBool(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
