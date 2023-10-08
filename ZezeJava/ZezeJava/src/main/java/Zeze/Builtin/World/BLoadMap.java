// auto-generated @formatter:off
package Zeze.Builtin.World;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BLoadMap extends Zeze.Transaction.Bean implements BLoadMapReadOnly {
    public static final long TYPEID = 4047532170397087334L;

    private int _MapId; // 地图配置编号
    private final Zeze.Transaction.Collections.CollOne<Zeze.Builtin.World.BLoad> _LoadSum; // 所有地图实例的累计
    private final Zeze.Transaction.Collections.PMap2<Long, Zeze.Builtin.World.BLoad> _Instances; // 地图实例（线）的负载

    @Override
    public int getMapId() {
        if (!isManaged())
            return _MapId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _MapId;
        var log = (Log__MapId)txn.getLog(objectId() + 1);
        return log != null ? log.value : _MapId;
    }

    public void setMapId(int value) {
        if (!isManaged()) {
            _MapId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__MapId(this, 1, value));
    }

    public Zeze.Builtin.World.BLoad getLoadSum() {
        return _LoadSum.getValue();
    }

    public void setLoadSum(Zeze.Builtin.World.BLoad value) {
        _LoadSum.setValue(value);
    }

    @Override
    public Zeze.Builtin.World.BLoadReadOnly getLoadSumReadOnly() {
        return _LoadSum.getValue();
    }

    public Zeze.Transaction.Collections.PMap2<Long, Zeze.Builtin.World.BLoad> getInstances() {
        return _Instances;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<Long, Zeze.Builtin.World.BLoad, Zeze.Builtin.World.BLoadReadOnly> getInstancesReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_Instances);
    }

    @SuppressWarnings("deprecation")
    public BLoadMap() {
        _LoadSum = new Zeze.Transaction.Collections.CollOne<>(new Zeze.Builtin.World.BLoad(), Zeze.Builtin.World.BLoad.class);
        _LoadSum.variableId(2);
        _Instances = new Zeze.Transaction.Collections.PMap2<>(Long.class, Zeze.Builtin.World.BLoad.class);
        _Instances.variableId(3);
    }

    @SuppressWarnings("deprecation")
    public BLoadMap(int _MapId_) {
        _MapId = _MapId_;
        _LoadSum = new Zeze.Transaction.Collections.CollOne<>(new Zeze.Builtin.World.BLoad(), Zeze.Builtin.World.BLoad.class);
        _LoadSum.variableId(2);
        _Instances = new Zeze.Transaction.Collections.PMap2<>(Long.class, Zeze.Builtin.World.BLoad.class);
        _Instances.variableId(3);
    }

    @Override
    public void reset() {
        setMapId(0);
        _LoadSum.reset();
        _Instances.clear();
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.World.BLoadMap.Data toData() {
        var data = new Zeze.Builtin.World.BLoadMap.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.World.BLoadMap.Data)other);
    }

    public void assign(BLoadMap.Data other) {
        setMapId(other._MapId);
        Zeze.Builtin.World.BLoad data_LoadSum = new Zeze.Builtin.World.BLoad();
        data_LoadSum.assign(other._LoadSum);
        _LoadSum.setValue(data_LoadSum);
        _Instances.clear();
        for (var e : other._Instances.entrySet()) {
            Zeze.Builtin.World.BLoad data = new Zeze.Builtin.World.BLoad();
            data.assign(e.getValue());
            _Instances.put(e.getKey(), data);
        }
        _unknown_ = null;
    }

    public void assign(BLoadMap other) {
        setMapId(other.getMapId());
        _LoadSum.assign(other._LoadSum);
        _Instances.clear();
        for (var e : other._Instances.entrySet())
            _Instances.put(e.getKey(), e.getValue().copy());
        _unknown_ = other._unknown_;
    }

    public BLoadMap copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BLoadMap copy() {
        var copy = new BLoadMap();
        copy.assign(this);
        return copy;
    }

    public static void swap(BLoadMap a, BLoadMap b) {
        BLoadMap save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__MapId extends Zeze.Transaction.Logs.LogInt {
        public Log__MapId(BLoadMap bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BLoadMap)getBelong())._MapId = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.World.BLoadMap: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("MapId=").append(getMapId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("LoadSum=").append(System.lineSeparator());
        _LoadSum.buildString(sb, level + 4);
        sb.append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Instances={");
        if (!_Instances.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _kv_ : _Instances.entrySet()) {
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
            int _x_ = getMapId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 2, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _LoadSum.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        {
            var _x_ = _Instances;
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
            setMapId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _o_.ReadBean(_LoadSum, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            var _x_ = _Instances;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadLong(_s_);
                    var _v_ = _o_.ReadBean(new Zeze.Builtin.World.BLoad(), _t_);
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
        _LoadSum.initRootInfo(root, this);
        _Instances.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _LoadSum.initRootInfoWithRedo(root, this);
        _Instances.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getMapId() < 0)
            return true;
        if (_LoadSum.negativeCheck())
            return true;
        for (var _v_ : _Instances.values()) {
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
                case 1: _MapId = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 2: _LoadSum.followerApply(vlog); break;
                case 3: _Instances.followerApply(vlog); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setMapId(rs.getInt(_parents_name_ + "MapId"));
        parents.add("LoadSum");
        _LoadSum.decodeResultSet(parents, rs);
        parents.remove(parents.size() - 1);
        Zeze.Serialize.Helper.decodeJsonMap(this, "Instances", _Instances, rs.getString(_parents_name_ + "Instances"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendInt(_parents_name_ + "MapId", getMapId());
        parents.add("LoadSum");
        _LoadSum.encodeSQLStatement(parents, st);
        parents.remove(parents.size() - 1);
        st.appendString(_parents_name_ + "Instances", Zeze.Serialize.Helper.encodeJson(_Instances));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "MapId", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "LoadSum", "BLoad", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Instances", "map", "long", "BLoad"));
        return vars;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 4047532170397087334L;

    private int _MapId; // 地图配置编号
    private Zeze.Builtin.World.BLoad.Data _LoadSum; // 所有地图实例的累计
    private java.util.HashMap<Long, Zeze.Builtin.World.BLoad.Data> _Instances; // 地图实例（线）的负载

    public int getMapId() {
        return _MapId;
    }

    public void setMapId(int value) {
        _MapId = value;
    }

    public Zeze.Builtin.World.BLoad.Data getLoadSum() {
        return _LoadSum;
    }

    public void setLoadSum(Zeze.Builtin.World.BLoad.Data value) {
        if (value == null)
            throw new IllegalArgumentException();
        _LoadSum = value;
    }

    public java.util.HashMap<Long, Zeze.Builtin.World.BLoad.Data> getInstances() {
        return _Instances;
    }

    public void setInstances(java.util.HashMap<Long, Zeze.Builtin.World.BLoad.Data> value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Instances = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _LoadSum = new Zeze.Builtin.World.BLoad.Data();
        _Instances = new java.util.HashMap<>();
    }

    @SuppressWarnings("deprecation")
    public Data(int _MapId_, Zeze.Builtin.World.BLoad.Data _LoadSum_, java.util.HashMap<Long, Zeze.Builtin.World.BLoad.Data> _Instances_) {
        _MapId = _MapId_;
        if (_LoadSum_ == null)
            _LoadSum_ = new Zeze.Builtin.World.BLoad.Data();
        _LoadSum = _LoadSum_;
        if (_Instances_ == null)
            _Instances_ = new java.util.HashMap<>();
        _Instances = _Instances_;
    }

    @Override
    public void reset() {
        _MapId = 0;
        _LoadSum.reset();
        _Instances.clear();
    }

    @Override
    public Zeze.Builtin.World.BLoadMap toBean() {
        var bean = new Zeze.Builtin.World.BLoadMap();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BLoadMap)other);
    }

    public void assign(BLoadMap other) {
        _MapId = other.getMapId();
        _LoadSum.assign(other._LoadSum.getValue());
        _Instances.clear();
        for (var e : other._Instances.entrySet()) {
            Zeze.Builtin.World.BLoad.Data data = new Zeze.Builtin.World.BLoad.Data();
            data.assign(e.getValue());
            _Instances.put(e.getKey(), data);
        }
    }

    public void assign(BLoadMap.Data other) {
        _MapId = other._MapId;
        _LoadSum.assign(other._LoadSum);
        _Instances.clear();
        for (var e : other._Instances.entrySet())
            _Instances.put(e.getKey(), e.getValue().copy());
    }

    @Override
    public BLoadMap.Data copy() {
        var copy = new BLoadMap.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BLoadMap.Data a, BLoadMap.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BLoadMap.Data clone() {
        return (BLoadMap.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.World.BLoadMap: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("MapId=").append(_MapId).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("LoadSum=").append(System.lineSeparator());
        _LoadSum.buildString(sb, level + 4);
        sb.append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Instances={");
        if (!_Instances.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _kv_ : _Instances.entrySet()) {
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
            int _x_ = _MapId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 2, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _LoadSum.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        {
            var _x_ = _Instances;
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
            _MapId = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _o_.ReadBean(_LoadSum, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            var _x_ = _Instances;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadLong(_s_);
                    var _v_ = _o_.ReadBean(new Zeze.Builtin.World.BLoad.Data(), _t_);
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
