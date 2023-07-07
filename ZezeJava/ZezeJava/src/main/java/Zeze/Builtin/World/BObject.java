// auto-generated @formatter:off
package Zeze.Builtin.World;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BObject extends Zeze.Transaction.Bean implements BObjectReadOnly {
    public static final long TYPEID = -2457457472033861643L;

    private Zeze.Serialize.Vector3 _Position;
    private String _PlayerId;
    private long _Linksid;
    private final Zeze.Transaction.DynamicBean _Data;

    public static Zeze.Transaction.DynamicBean newDynamicBean_Data() {
        return new Zeze.Transaction.DynamicBean(4, Zeze.World.World::getSpecialTypeIdFromBean, Zeze.World.World::createBeanFromSpecialTypeId);
    }

    public static long getSpecialTypeIdFromBean_4(Zeze.Transaction.Bean bean) {
        return Zeze.World.World.getSpecialTypeIdFromBean(bean);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_4(long typeId) {
        return Zeze.World.World.createBeanFromSpecialTypeId(typeId);
    }

    @Override
    public Zeze.Serialize.Vector3 getPosition() {
        if (!isManaged())
            return _Position;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Position;
        var log = (Log__Position)txn.getLog(objectId() + 1);
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
        txn.putLog(new Log__Position(this, 1, value));
    }

    @Override
    public String getPlayerId() {
        if (!isManaged())
            return _PlayerId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _PlayerId;
        var log = (Log__PlayerId)txn.getLog(objectId() + 2);
        return log != null ? log.value : _PlayerId;
    }

    public void setPlayerId(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _PlayerId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__PlayerId(this, 2, value));
    }

    @Override
    public long getLinksid() {
        if (!isManaged())
            return _Linksid;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Linksid;
        var log = (Log__Linksid)txn.getLog(objectId() + 3);
        return log != null ? log.value : _Linksid;
    }

    public void setLinksid(long value) {
        if (!isManaged()) {
            _Linksid = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Linksid(this, 3, value));
    }

    public Zeze.Transaction.DynamicBean getData() {
        return _Data;
    }

    @Override
    public Zeze.Transaction.DynamicBeanReadOnly getDataReadOnly() {
        return _Data;
    }

    @SuppressWarnings("deprecation")
    public BObject() {
        _Position = Zeze.Serialize.Vector3.ZERO;
        _PlayerId = "";
        _Data = newDynamicBean_Data();
    }

    @SuppressWarnings("deprecation")
    public BObject(Zeze.Serialize.Vector3 _Position_, String _PlayerId_, long _Linksid_) {
        if (_Position_ == null)
            _Position_ = Zeze.Serialize.Vector3.ZERO;
        _Position = _Position_;
        if (_PlayerId_ == null)
            _PlayerId_ = "";
        _PlayerId = _PlayerId_;
        _Linksid = _Linksid_;
        _Data = newDynamicBean_Data();
    }

    @Override
    public void reset() {
        setPosition(Zeze.Serialize.Vector3.ZERO);
        setPlayerId("");
        setLinksid(0);
        _Data.reset();
    }

    @Override
    public Zeze.Builtin.World.BObject.Data toData() {
        var data = new Zeze.Builtin.World.BObject.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.World.BObject.Data)other);
    }

    public void assign(BObject.Data other) {
        setPosition(other._Position);
        setPlayerId(other._PlayerId);
        setLinksid(other._Linksid);
        _Data.assign(other._Data);
    }

    public void assign(BObject other) {
        setPosition(other.getPosition());
        setPlayerId(other.getPlayerId());
        setLinksid(other.getLinksid());
        _Data.assign(other._Data);
    }

    public BObject copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BObject copy() {
        var copy = new BObject();
        copy.assign(this);
        return copy;
    }

    public static void swap(BObject a, BObject b) {
        BObject save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Position extends Zeze.Transaction.Logs.LogVector3 {
        public Log__Position(BObject bean, int varId, Zeze.Serialize.Vector3 value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BObject)getBelong())._Position = value; }
    }

    private static final class Log__PlayerId extends Zeze.Transaction.Logs.LogString {
        public Log__PlayerId(BObject bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BObject)getBelong())._PlayerId = value; }
    }

    private static final class Log__Linksid extends Zeze.Transaction.Logs.LogLong {
        public Log__Linksid(BObject bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BObject)getBelong())._Linksid = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.World.BObject: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Position=").append(getPosition()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("PlayerId=").append(getPlayerId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Linksid=").append(getLinksid()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Data=").append(System.lineSeparator());
        _Data.getBean().buildString(sb, level + 4);
        sb.append(System.lineSeparator());
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
            var _x_ = getPosition();
            if (_x_ != null && !_x_.isZero()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.VECTOR3);
                _o_.WriteVector3(_x_);
            }
        }
        {
            String _x_ = getPlayerId();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            long _x_ = getLinksid();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = _Data;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.DYNAMIC);
                _x_.encode(_o_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setPosition(_o_.ReadVector3(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setPlayerId(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setLinksid(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _o_.ReadDynamic(_Data, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Data.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _Data.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getLinksid() < 0)
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
                case 1: _Position = ((Zeze.Transaction.Logs.LogVector3)vlog).value; break;
                case 2: _PlayerId = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 3: _Linksid = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 4: _Data.followerApply(vlog); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        parents.add("Position");
        setPosition(Zeze.Serialize.Helper.decodeVector3(parents, rs));
        parents.remove(parents.size() - 1);
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setPlayerId(rs.getString(_parents_name_ + "PlayerId"));
        if (getPlayerId() == null)
            setPlayerId("");
        setLinksid(rs.getLong(_parents_name_ + "Linksid"));
        Zeze.Serialize.Helper.decodeJsonDynamic(_Data, rs.getString(_parents_name_ + "Data"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        parents.add("Position");
        Zeze.Serialize.Helper.encodeVector3(getPosition(), parents, st);
        parents.remove(parents.size() - 1);
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "PlayerId", getPlayerId());
        st.appendLong(_parents_name_ + "Linksid", getLinksid());
        st.appendString(_parents_name_ + "Data", Zeze.Serialize.Helper.encodeJson(_Data));
    }

public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -2457457472033861643L;

    private Zeze.Serialize.Vector3 _Position;
    private String _PlayerId;
    private long _Linksid;
    private final DynamicData_Data _Data;

    public static final class DynamicData_Data extends Zeze.Transaction.DynamicData {
        static {
            registerJsonParser(DynamicData_Data.class);
        }

        @Override
        public long toTypeId(Zeze.Transaction.Data data) {
            return Zeze.World.World.getSpecialTypeIdFromBean(data);
        }

        @Override
        public Zeze.Transaction.Data toData(long typeId) {
            return Zeze.World.World.createDataFromSpecialTypeId(typeId);
        }

        @Override
        public DynamicData_Data copy() {
            return (DynamicData_Data)super.copy();
        }
    }

    public Zeze.Serialize.Vector3 getPosition() {
        return _Position;
    }

    public void setPosition(Zeze.Serialize.Vector3 value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Position = value;
    }

    public String getPlayerId() {
        return _PlayerId;
    }

    public void setPlayerId(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _PlayerId = value;
    }

    public long getLinksid() {
        return _Linksid;
    }

    public void setLinksid(long value) {
        _Linksid = value;
    }

    public DynamicData_Data getData() {
        return _Data;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Position = Zeze.Serialize.Vector3.ZERO;
        _PlayerId = "";
        _Data = new DynamicData_Data();
    }

    @SuppressWarnings("deprecation")
    public Data(Zeze.Serialize.Vector3 _Position_, String _PlayerId_, long _Linksid_, DynamicData_Data _Data_) {
        if (_Position_ == null)
            _Position_ = Zeze.Serialize.Vector3.ZERO;
        _Position = _Position_;
        if (_PlayerId_ == null)
            _PlayerId_ = "";
        _PlayerId = _PlayerId_;
        _Linksid = _Linksid_;
        if (_Data_ == null)
            _Data_ = new DynamicData_Data();
        _Data = _Data_;
    }

    @Override
    public void reset() {
        _Position = Zeze.Serialize.Vector3.ZERO;
        _PlayerId = "";
        _Linksid = 0;
        _Data.reset();
    }

    @Override
    public Zeze.Builtin.World.BObject toBean() {
        var bean = new Zeze.Builtin.World.BObject();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BObject)other);
    }

    public void assign(BObject other) {
        _Position = other.getPosition();
        _PlayerId = other.getPlayerId();
        _Linksid = other.getLinksid();
        _Data.assign(other._Data);
    }

    public void assign(BObject.Data other) {
        _Position = other._Position;
        _PlayerId = other._PlayerId;
        _Linksid = other._Linksid;
        _Data.assign(other._Data);
    }

    @Override
    public BObject.Data copy() {
        var copy = new BObject.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BObject.Data a, BObject.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BObject.Data clone() {
        return (BObject.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.World.BObject: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Position=").append(_Position).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("PlayerId=").append(_PlayerId).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Linksid=").append(_Linksid).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Data=").append(System.lineSeparator());
        _Data.getData().buildString(sb, level + 4);
        sb.append(System.lineSeparator());
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
            var _x_ = _Position;
            if (_x_ != null && !_x_.isZero()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.VECTOR3);
                _o_.WriteVector3(_x_);
            }
        }
        {
            String _x_ = _PlayerId;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            long _x_ = _Linksid;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = _Data;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.DYNAMIC);
                _x_.encode(_o_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _Position = _o_.ReadVector3(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _PlayerId = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _Linksid = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _o_.ReadDynamic(_Data, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
