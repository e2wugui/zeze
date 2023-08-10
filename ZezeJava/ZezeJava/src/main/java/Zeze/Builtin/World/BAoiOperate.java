// auto-generated @formatter:off
package Zeze.Builtin.World;

import Zeze.Serialize.ByteBuffer;

// 一个具体的操作。
@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BAoiOperate extends Zeze.Transaction.Bean implements BAoiOperateReadOnly {
    public static final long TYPEID = 7467019147847621003L;

    private int _OperateId;
    private Zeze.Net.Binary _Param;
    private final Zeze.Transaction.Collections.PMap2<Long, Zeze.Builtin.World.BAoiOperate> _Children;

    private transient Object __zeze_map_key__;

    @Override
    public Object mapKey() {
        return __zeze_map_key__;
    }

    @Override
    public void mapKey(Object value) {
        __zeze_map_key__ = value;
    }

    @Override
    public int getOperateId() {
        if (!isManaged())
            return _OperateId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _OperateId;
        var log = (Log__OperateId)txn.getLog(objectId() + 1);
        return log != null ? log.value : _OperateId;
    }

    public void setOperateId(int value) {
        if (!isManaged()) {
            _OperateId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__OperateId(this, 1, value));
    }

    @Override
    public Zeze.Net.Binary getParam() {
        if (!isManaged())
            return _Param;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Param;
        var log = (Log__Param)txn.getLog(objectId() + 2);
        return log != null ? log.value : _Param;
    }

    public void setParam(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Param = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Param(this, 2, value));
    }

    public Zeze.Transaction.Collections.PMap2<Long, Zeze.Builtin.World.BAoiOperate> getChildren() {
        return _Children;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<Long, Zeze.Builtin.World.BAoiOperate, Zeze.Builtin.World.BAoiOperateReadOnly> getChildrenReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_Children);
    }

    @SuppressWarnings("deprecation")
    public BAoiOperate() {
        _Param = Zeze.Net.Binary.Empty;
        _Children = new Zeze.Transaction.Collections.PMap2<>(Long.class, Zeze.Builtin.World.BAoiOperate.class);
        _Children.variableId(3);
    }

    @SuppressWarnings("deprecation")
    public BAoiOperate(int _OperateId_, Zeze.Net.Binary _Param_) {
        _OperateId = _OperateId_;
        if (_Param_ == null)
            _Param_ = Zeze.Net.Binary.Empty;
        _Param = _Param_;
        _Children = new Zeze.Transaction.Collections.PMap2<>(Long.class, Zeze.Builtin.World.BAoiOperate.class);
        _Children.variableId(3);
    }

    @Override
    public void reset() {
        setOperateId(0);
        setParam(Zeze.Net.Binary.Empty);
        _Children.clear();
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.World.BAoiOperate.Data toData() {
        var data = new Zeze.Builtin.World.BAoiOperate.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.World.BAoiOperate.Data)other);
    }

    public void assign(BAoiOperate.Data other) {
        setOperateId(other._OperateId);
        setParam(other._Param);
        _Children.clear();
        for (var e : other._Children.entrySet()) {
            Zeze.Builtin.World.BAoiOperate data = new Zeze.Builtin.World.BAoiOperate();
            data.assign(e.getValue());
            _Children.put(e.getKey(), data);
        }
        _unknown_ = null;
    }

    public void assign(BAoiOperate other) {
        setOperateId(other.getOperateId());
        setParam(other.getParam());
        _Children.clear();
        for (var e : other._Children.entrySet())
            _Children.put(e.getKey(), e.getValue().copy());
        _unknown_ = other._unknown_;
    }

    public BAoiOperate copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BAoiOperate copy() {
        var copy = new BAoiOperate();
        copy.assign(this);
        return copy;
    }

    public static void swap(BAoiOperate a, BAoiOperate b) {
        BAoiOperate save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__OperateId extends Zeze.Transaction.Logs.LogInt {
        public Log__OperateId(BAoiOperate bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BAoiOperate)getBelong())._OperateId = value; }
    }

    private static final class Log__Param extends Zeze.Transaction.Logs.LogBinary {
        public Log__Param(BAoiOperate bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BAoiOperate)getBelong())._Param = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.World.BAoiOperate: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("OperateId=").append(getOperateId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Param=").append(getParam()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Children={");
        if (!_Children.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _kv_ : _Children.entrySet()) {
                sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(_kv_.getKey()).append(',').append(System.lineSeparator());
                sb.append(Zeze.Util.Str.indent(level)).append("Value=").append(System.lineSeparator());
                _kv_.getValue().buildString(sb, level + 4);
                sb.append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append('}').append(System.lineSeparator());
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
            int _x_ = getOperateId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = getParam();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            var _x_ = _Children;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _e_.getValue().encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
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
            setOperateId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setParam(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            var _x_ = _Children;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadLong(_s_);
                    var _v_ = _o_.ReadBean(new Zeze.Builtin.World.BAoiOperate(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Children.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _Children.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getOperateId() < 0)
            return true;
        for (var _v_ : _Children.values()) {
            if (_v_.negativeCheck())
                return true;
        }
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
                case 1: _OperateId = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 2: _Param = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
                case 3: _Children.followerApply(vlog); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setOperateId(rs.getInt(_parents_name_ + "OperateId"));
        setParam(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "Param")));
        if (getParam() == null)
            setParam(Zeze.Net.Binary.Empty);
        Zeze.Serialize.Helper.decodeJsonMap(this, "Children", _Children, rs.getString(_parents_name_ + "Children"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendInt(_parents_name_ + "OperateId", getOperateId());
        st.appendBinary(_parents_name_ + "Param", getParam());
        st.appendString(_parents_name_ + "Children", Zeze.Serialize.Helper.encodeJson(_Children));
    }

    @Override
    public java.util.List<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "OperateId", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Param", "binary", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Children", "map", "long", "BAoiOperate"));
        return vars;
    }

    @Override
    public Zeze.Transaction.Bean toPrevious() {
        return null; // todo BAoiOperate
    }

// 一个具体的操作。
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 7467019147847621003L;

    private int _OperateId;
    private Zeze.Net.Binary _Param;
    private java.util.HashMap<Long, Zeze.Builtin.World.BAoiOperate.Data> _Children;

    public int getOperateId() {
        return _OperateId;
    }

    public void setOperateId(int value) {
        _OperateId = value;
    }

    public Zeze.Net.Binary getParam() {
        return _Param;
    }

    public void setParam(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Param = value;
    }

    public java.util.HashMap<Long, Zeze.Builtin.World.BAoiOperate.Data> getChildren() {
        return _Children;
    }

    public void setChildren(java.util.HashMap<Long, Zeze.Builtin.World.BAoiOperate.Data> value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Children = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Param = Zeze.Net.Binary.Empty;
        _Children = new java.util.HashMap<>();
    }

    @SuppressWarnings("deprecation")
    public Data(int _OperateId_, Zeze.Net.Binary _Param_, java.util.HashMap<Long, Zeze.Builtin.World.BAoiOperate.Data> _Children_) {
        _OperateId = _OperateId_;
        if (_Param_ == null)
            _Param_ = Zeze.Net.Binary.Empty;
        _Param = _Param_;
        if (_Children_ == null)
            _Children_ = new java.util.HashMap<>();
        _Children = _Children_;
    }

    @Override
    public void reset() {
        _OperateId = 0;
        _Param = Zeze.Net.Binary.Empty;
        _Children.clear();
    }

    @Override
    public Zeze.Builtin.World.BAoiOperate toBean() {
        var bean = new Zeze.Builtin.World.BAoiOperate();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BAoiOperate)other);
    }

    public void assign(BAoiOperate other) {
        _OperateId = other.getOperateId();
        _Param = other.getParam();
        _Children.clear();
        for (var e : other._Children.entrySet()) {
            Zeze.Builtin.World.BAoiOperate.Data data = new Zeze.Builtin.World.BAoiOperate.Data();
            data.assign(e.getValue());
            _Children.put(e.getKey(), data);
        }
    }

    public void assign(BAoiOperate.Data other) {
        _OperateId = other._OperateId;
        _Param = other._Param;
        _Children.clear();
        for (var e : other._Children.entrySet())
            _Children.put(e.getKey(), e.getValue().copy());
    }

    @Override
    public BAoiOperate.Data copy() {
        var copy = new BAoiOperate.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BAoiOperate.Data a, BAoiOperate.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BAoiOperate.Data clone() {
        return (BAoiOperate.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.World.BAoiOperate: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("OperateId=").append(_OperateId).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Param=").append(_Param).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Children={");
        if (!_Children.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _kv_ : _Children.entrySet()) {
                sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(_kv_.getKey()).append(',').append(System.lineSeparator());
                sb.append(Zeze.Util.Str.indent(level)).append("Value=").append(System.lineSeparator());
                _kv_.getValue().buildString(sb, level + 4);
                sb.append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append('}').append(System.lineSeparator());
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
            int _x_ = _OperateId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = _Param;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            var _x_ = _Children;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _e_.getValue().encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _OperateId = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _Param = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            var _x_ = _Children;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadLong(_s_);
                    var _v_ = _o_.ReadBean(new Zeze.Builtin.World.BAoiOperate.Data(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
