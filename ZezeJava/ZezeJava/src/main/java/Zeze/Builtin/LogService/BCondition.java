// auto-generated @formatter:off
package Zeze.Builtin.LogService;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BCondition extends Zeze.Transaction.Bean implements BConditionReadOnly {
    public static final long TYPEID = -4711929821698256188L;

    public static final int ContainsAll = 0;
    public static final int ContainsAny = 1;
    public static final int ContainsNone = 2;

    private long _BeginTime;
    private long _EndTime;
    private final Zeze.Transaction.Collections.PList1<String> _Words;
    private int _ContainsType;
    private String _Pattern;

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
    public int getContainsType() {
        if (!isManaged())
            return _ContainsType;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ContainsType;
        var log = (Log__ContainsType)txn.getLog(objectId() + 4);
        return log != null ? log.value : _ContainsType;
    }

    public void setContainsType(int value) {
        if (!isManaged()) {
            _ContainsType = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ContainsType(this, 4, value));
    }

    @Override
    public String getPattern() {
        if (!isManaged())
            return _Pattern;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Pattern;
        var log = (Log__Pattern)txn.getLog(objectId() + 5);
        return log != null ? log.value : _Pattern;
    }

    public void setPattern(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Pattern = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Pattern(this, 5, value));
    }

    @SuppressWarnings("deprecation")
    public BCondition() {
        _Words = new Zeze.Transaction.Collections.PList1<>(String.class);
        _Words.variableId(3);
        _Pattern = "";
    }

    @SuppressWarnings("deprecation")
    public BCondition(long _BeginTime_, long _EndTime_, int _ContainsType_, String _Pattern_) {
        _BeginTime = _BeginTime_;
        _EndTime = _EndTime_;
        _Words = new Zeze.Transaction.Collections.PList1<>(String.class);
        _Words.variableId(3);
        _ContainsType = _ContainsType_;
        if (_Pattern_ == null)
            _Pattern_ = "";
        _Pattern = _Pattern_;
    }

    @Override
    public void reset() {
        setBeginTime(0);
        setEndTime(0);
        _Words.clear();
        setContainsType(0);
        setPattern("");
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.LogService.BCondition.Data toData() {
        var data = new Zeze.Builtin.LogService.BCondition.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.LogService.BCondition.Data)other);
    }

    public void assign(BCondition.Data other) {
        setBeginTime(other._BeginTime);
        setEndTime(other._EndTime);
        _Words.clear();
        _Words.addAll(other._Words);
        setContainsType(other._ContainsType);
        setPattern(other._Pattern);
        _unknown_ = null;
    }

    public void assign(BCondition other) {
        setBeginTime(other.getBeginTime());
        setEndTime(other.getEndTime());
        _Words.assign(other._Words);
        setContainsType(other.getContainsType());
        setPattern(other.getPattern());
        _unknown_ = other._unknown_;
    }

    public BCondition copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BCondition copy() {
        var copy = new BCondition();
        copy.assign(this);
        return copy;
    }

    public static void swap(BCondition a, BCondition b) {
        BCondition save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__BeginTime extends Zeze.Transaction.Logs.LogLong {
        public Log__BeginTime(BCondition bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BCondition)getBelong())._BeginTime = value; }
    }

    private static final class Log__EndTime extends Zeze.Transaction.Logs.LogLong {
        public Log__EndTime(BCondition bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BCondition)getBelong())._EndTime = value; }
    }

    private static final class Log__ContainsType extends Zeze.Transaction.Logs.LogInt {
        public Log__ContainsType(BCondition bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BCondition)getBelong())._ContainsType = value; }
    }

    private static final class Log__Pattern extends Zeze.Transaction.Logs.LogString {
        public Log__Pattern(BCondition bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BCondition)getBelong())._Pattern = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.LogService.BCondition: {").append(System.lineSeparator());
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
        sb.append(Zeze.Util.Str.indent(level)).append("ContainsType=").append(getContainsType()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Pattern=").append(getPattern()).append(System.lineSeparator());
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
            int _x_ = getContainsType();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            String _x_ = getPattern();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
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
            setContainsType(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setPattern(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BCondition))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BCondition)_o_;
        if (getBeginTime() != _b_.getBeginTime())
            return false;
        if (getEndTime() != _b_.getEndTime())
            return false;
        if (!_Words.equals(_b_._Words))
            return false;
        if (getContainsType() != _b_.getContainsType())
            return false;
        if (!getPattern().equals(_b_.getPattern()))
            return false;
        return true;
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
        if (getContainsType() < 0)
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
                case 1: _BeginTime = vlog.longValue(); break;
                case 2: _EndTime = vlog.longValue(); break;
                case 3: _Words.followerApply(vlog); break;
                case 4: _ContainsType = vlog.intValue(); break;
                case 5: _Pattern = vlog.stringValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setBeginTime(rs.getLong(_parents_name_ + "BeginTime"));
        setEndTime(rs.getLong(_parents_name_ + "EndTime"));
        Zeze.Serialize.Helper.decodeJsonList(_Words, String.class, rs.getString(_parents_name_ + "Words"));
        setContainsType(rs.getInt(_parents_name_ + "ContainsType"));
        setPattern(rs.getString(_parents_name_ + "Pattern"));
        if (getPattern() == null)
            setPattern("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendLong(_parents_name_ + "BeginTime", getBeginTime());
        st.appendLong(_parents_name_ + "EndTime", getEndTime());
        st.appendString(_parents_name_ + "Words", Zeze.Serialize.Helper.encodeJson(_Words));
        st.appendInt(_parents_name_ + "ContainsType", getContainsType());
        st.appendString(_parents_name_ + "Pattern", getPattern());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "BeginTime", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "EndTime", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Words", "list", "", "string"));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "ContainsType", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "Pattern", "string", "", ""));
        return vars;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -4711929821698256188L;

    public static final int ContainsAll = 0;
    public static final int ContainsAny = 1;
    public static final int ContainsNone = 2;

    private long _BeginTime;
    private long _EndTime;
    private java.util.ArrayList<String> _Words;
    private int _ContainsType;
    private String _Pattern;

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

    public int getContainsType() {
        return _ContainsType;
    }

    public void setContainsType(int value) {
        _ContainsType = value;
    }

    public String getPattern() {
        return _Pattern;
    }

    public void setPattern(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Pattern = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Words = new java.util.ArrayList<>();
        _Pattern = "";
    }

    @SuppressWarnings("deprecation")
    public Data(long _BeginTime_, long _EndTime_, java.util.ArrayList<String> _Words_, int _ContainsType_, String _Pattern_) {
        _BeginTime = _BeginTime_;
        _EndTime = _EndTime_;
        if (_Words_ == null)
            _Words_ = new java.util.ArrayList<>();
        _Words = _Words_;
        _ContainsType = _ContainsType_;
        if (_Pattern_ == null)
            _Pattern_ = "";
        _Pattern = _Pattern_;
    }

    @Override
    public void reset() {
        _BeginTime = 0;
        _EndTime = 0;
        _Words.clear();
        _ContainsType = 0;
        _Pattern = "";
    }

    @Override
    public Zeze.Builtin.LogService.BCondition toBean() {
        var bean = new Zeze.Builtin.LogService.BCondition();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BCondition)other);
    }

    public void assign(BCondition other) {
        _BeginTime = other.getBeginTime();
        _EndTime = other.getEndTime();
        _Words.clear();
        _Words.addAll(other._Words);
        _ContainsType = other.getContainsType();
        _Pattern = other.getPattern();
    }

    public void assign(BCondition.Data other) {
        _BeginTime = other._BeginTime;
        _EndTime = other._EndTime;
        _Words.clear();
        _Words.addAll(other._Words);
        _ContainsType = other._ContainsType;
        _Pattern = other._Pattern;
    }

    @Override
    public BCondition.Data copy() {
        var copy = new BCondition.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BCondition.Data a, BCondition.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BCondition.Data clone() {
        return (BCondition.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.LogService.BCondition: {").append(System.lineSeparator());
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
        sb.append(Zeze.Util.Str.indent(level)).append("ContainsType=").append(_ContainsType).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Pattern=").append(_Pattern).append(System.lineSeparator());
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append('}');
    }

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
                for (int _j_ = 0, _c_ = _x_.size(); _j_ < _c_; _j_++) {
                    var _v_ = _x_.get(_j_);
                    _o_.WriteString(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            int _x_ = _ContainsType;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            String _x_ = _Pattern;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
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
            _ContainsType = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            _Pattern = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
