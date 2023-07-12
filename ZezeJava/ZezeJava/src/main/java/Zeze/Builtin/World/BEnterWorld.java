// auto-generated @formatter:off
package Zeze.Builtin.World;

import Zeze.Serialize.ByteBuffer;

/*
为了不污染根空间，改成Command了。
			<protocol name="SwitchWorld" argument="BSwitchWorld" handle="server"/> mapId==-1，进入地图由服务器控制，此时仅仅表示客户端准备好进入地图了。
			<protocol name="EnterWorld" argument="BEnterWorld" handle="client"/>
			<protocol name="EnterConfirm" argument="BEnterConfirm" handle="server"/>

			Aoi-Notify
			<protocol name="AoiEnter" argument="BAoiEnter" handle="client"/>
			<protocol name="AoiOperate" argument="BAoiOperate" handle="client"/>
			<protocol name="AoiLeave" argument="BAoiLeave" handle="client"/>
*/
@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BEnterWorld extends Zeze.Transaction.Bean implements BEnterWorldReadOnly {
    public static final long TYPEID = -4883142059980084950L;

    private int _MapId;
    private Zeze.Serialize.Vector3 _Position;
    private final Zeze.Transaction.Collections.PList2<Zeze.Builtin.World.BAoiOperates> _PriorityData; // 优先数据，服务器第一次进入的时候就跟随EnterWorld发送给客户端。

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

    @Override
    public Zeze.Serialize.Vector3 getPosition() {
        if (!isManaged())
            return _Position;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Position;
        var log = (Log__Position)txn.getLog(objectId() + 2);
        return log != null ? log.value : _Position;
    }

    public void setPosition(Zeze.Serialize.Vector3 value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Position = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Position(this, 2, value));
    }

    public Zeze.Transaction.Collections.PList2<Zeze.Builtin.World.BAoiOperates> getPriorityData() {
        return _PriorityData;
    }

    @Override
    public Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.World.BAoiOperates, Zeze.Builtin.World.BAoiOperatesReadOnly> getPriorityDataReadOnly() {
        return new Zeze.Transaction.Collections.PList2ReadOnly<>(_PriorityData);
    }

    @SuppressWarnings("deprecation")
    public BEnterWorld() {
        _Position = Zeze.Serialize.Vector3.ZERO;
        _PriorityData = new Zeze.Transaction.Collections.PList2<>(Zeze.Builtin.World.BAoiOperates.class);
        _PriorityData.variableId(3);
    }

    @SuppressWarnings("deprecation")
    public BEnterWorld(int _MapId_, Zeze.Serialize.Vector3 _Position_) {
        _MapId = _MapId_;
        if (_Position_ == null)
            _Position_ = Zeze.Serialize.Vector3.ZERO;
        _Position = _Position_;
        _PriorityData = new Zeze.Transaction.Collections.PList2<>(Zeze.Builtin.World.BAoiOperates.class);
        _PriorityData.variableId(3);
    }

    @Override
    public void reset() {
        setMapId(0);
        setPosition(Zeze.Serialize.Vector3.ZERO);
        _PriorityData.clear();
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.World.BEnterWorld.Data toData() {
        var data = new Zeze.Builtin.World.BEnterWorld.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.World.BEnterWorld.Data)other);
    }

    public void assign(BEnterWorld.Data other) {
        setMapId(other._MapId);
        setPosition(other._Position);
        _PriorityData.clear();
        for (var e : other._PriorityData) {
            Zeze.Builtin.World.BAoiOperates data = new Zeze.Builtin.World.BAoiOperates();
            data.assign(e);
            _PriorityData.add(data);
        }
        _unknown_ = null;
    }

    public void assign(BEnterWorld other) {
        setMapId(other.getMapId());
        setPosition(other.getPosition());
        _PriorityData.clear();
        for (var e : other._PriorityData)
            _PriorityData.add(e.copy());
        _unknown_ = other._unknown_;
    }

    public BEnterWorld copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BEnterWorld copy() {
        var copy = new BEnterWorld();
        copy.assign(this);
        return copy;
    }

    public static void swap(BEnterWorld a, BEnterWorld b) {
        BEnterWorld save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__MapId extends Zeze.Transaction.Logs.LogInt {
        public Log__MapId(BEnterWorld bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BEnterWorld)getBelong())._MapId = value; }
    }

    private static final class Log__Position extends Zeze.Transaction.Logs.LogVector3 {
        public Log__Position(BEnterWorld bean, int varId, Zeze.Serialize.Vector3 value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BEnterWorld)getBelong())._Position = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.World.BEnterWorld: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("MapId=").append(getMapId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Position=").append(getPosition()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("PriorityData=[");
        if (!_PriorityData.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _PriorityData) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(System.lineSeparator());
                _item_.buildString(sb, level + 4);
                sb.append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append(']').append(System.lineSeparator());
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

    private ByteBuffer _unknown_;

    @Override
    public void encode(ByteBuffer _o_) {
        var _u_ = _unknown_;
        var _ui_ = _u_ != null ? (_u_ = ByteBuffer.Wrap(_u_)).readUnknownIndex() : Long.MAX_VALUE;
        int _i_ = 0;
        {
            int _x_ = getMapId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = getPosition();
            if (_x_ != null && !_x_.isZero()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.VECTOR3);
                _o_.WriteVector3(_x_);
            }
        }
        {
            var _x_ = _PriorityData;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BEAN);
                for (var _v_ : _x_) {
                    _v_.encode(_o_);
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
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setMapId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setPosition(_o_.ReadVector3(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            var _x_ = _PriorityData;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new Zeze.Builtin.World.BAoiOperates(), _t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        _o_.skipAllUnknownFields(_t_);
    }

    @Override
    public void decodeWithUnknown(ByteBuffer _o_) {
        ByteBuffer _u_ = null;
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setMapId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setPosition(_o_.ReadVector3(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            var _x_ = _PriorityData;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new Zeze.Builtin.World.BAoiOperates(), _t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _PriorityData.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _PriorityData.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getMapId() < 0)
            return true;
        for (var _v_ : _PriorityData) {
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
                case 2: _Position = ((Zeze.Transaction.Logs.LogVector3)vlog).value; break;
                case 3: _PriorityData.followerApply(vlog); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setMapId(rs.getInt(_parents_name_ + "MapId"));
        parents.add("Position");
        setPosition(Zeze.Serialize.Helper.decodeVector3(parents, rs));
        parents.remove(parents.size() - 1);
        Zeze.Serialize.Helper.decodeJsonList(_PriorityData, Zeze.Builtin.World.BAoiOperates.class, rs.getString(_parents_name_ + "PriorityData"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendInt(_parents_name_ + "MapId", getMapId());
        parents.add("Position");
        Zeze.Serialize.Helper.encodeVector3(getPosition(), parents, st);
        parents.remove(parents.size() - 1);
        st.appendString(_parents_name_ + "PriorityData", Zeze.Serialize.Helper.encodeJson(_PriorityData));
    }

/*
为了不污染根空间，改成Command了。
			<protocol name="SwitchWorld" argument="BSwitchWorld" handle="server"/> mapId==-1，进入地图由服务器控制，此时仅仅表示客户端准备好进入地图了。
			<protocol name="EnterWorld" argument="BEnterWorld" handle="client"/>
			<protocol name="EnterConfirm" argument="BEnterConfirm" handle="server"/>

			Aoi-Notify
			<protocol name="AoiEnter" argument="BAoiEnter" handle="client"/>
			<protocol name="AoiOperate" argument="BAoiOperate" handle="client"/>
			<protocol name="AoiLeave" argument="BAoiLeave" handle="client"/>
*/
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -4883142059980084950L;

    private int _MapId;
    private Zeze.Serialize.Vector3 _Position;
    private java.util.ArrayList<Zeze.Builtin.World.BAoiOperates.Data> _PriorityData; // 优先数据，服务器第一次进入的时候就跟随EnterWorld发送给客户端。

    public int getMapId() {
        return _MapId;
    }

    public void setMapId(int value) {
        _MapId = value;
    }

    public Zeze.Serialize.Vector3 getPosition() {
        return _Position;
    }

    public void setPosition(Zeze.Serialize.Vector3 value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Position = value;
    }

    public java.util.ArrayList<Zeze.Builtin.World.BAoiOperates.Data> getPriorityData() {
        return _PriorityData;
    }

    public void setPriorityData(java.util.ArrayList<Zeze.Builtin.World.BAoiOperates.Data> value) {
        if (value == null)
            throw new IllegalArgumentException();
        _PriorityData = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Position = Zeze.Serialize.Vector3.ZERO;
        _PriorityData = new java.util.ArrayList<>();
    }

    @SuppressWarnings("deprecation")
    public Data(int _MapId_, Zeze.Serialize.Vector3 _Position_, java.util.ArrayList<Zeze.Builtin.World.BAoiOperates.Data> _PriorityData_) {
        _MapId = _MapId_;
        if (_Position_ == null)
            _Position_ = Zeze.Serialize.Vector3.ZERO;
        _Position = _Position_;
        if (_PriorityData_ == null)
            _PriorityData_ = new java.util.ArrayList<>();
        _PriorityData = _PriorityData_;
    }

    @Override
    public void reset() {
        _MapId = 0;
        _Position = Zeze.Serialize.Vector3.ZERO;
        _PriorityData.clear();
    }

    @Override
    public Zeze.Builtin.World.BEnterWorld toBean() {
        var bean = new Zeze.Builtin.World.BEnterWorld();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BEnterWorld)other);
    }

    public void assign(BEnterWorld other) {
        _MapId = other.getMapId();
        _Position = other.getPosition();
        _PriorityData.clear();
        for (var e : other._PriorityData) {
            Zeze.Builtin.World.BAoiOperates.Data data = new Zeze.Builtin.World.BAoiOperates.Data();
            data.assign(e);
            _PriorityData.add(data);
        }
    }

    public void assign(BEnterWorld.Data other) {
        _MapId = other._MapId;
        _Position = other._Position;
        _PriorityData.clear();
        for (var e : other._PriorityData)
            _PriorityData.add(e.copy());
    }

    @Override
    public BEnterWorld.Data copy() {
        var copy = new BEnterWorld.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BEnterWorld.Data a, BEnterWorld.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BEnterWorld.Data clone() {
        return (BEnterWorld.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.World.BEnterWorld: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("MapId=").append(_MapId).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Position=").append(_Position).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("PriorityData=[");
        if (!_PriorityData.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _PriorityData) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(System.lineSeparator());
                _item_.buildString(sb, level + 4);
                sb.append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append(']').append(System.lineSeparator());
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
            var _x_ = _Position;
            if (_x_ != null && !_x_.isZero()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.VECTOR3);
                _o_.WriteVector3(_x_);
            }
        }
        {
            var _x_ = _PriorityData;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BEAN);
                for (var _v_ : _x_) {
                    _v_.encode(_o_);
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
            _Position = _o_.ReadVector3(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            var _x_ = _PriorityData;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new Zeze.Builtin.World.BAoiOperates.Data(), _t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
