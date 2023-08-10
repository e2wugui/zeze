// auto-generated @formatter:off
package Zeze.Builtin.World;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BObject extends Zeze.Transaction.Bean implements BObjectReadOnly {
    public static final long TYPEID = -2457457472033861643L;

    private final Zeze.Transaction.DynamicBean _Data;

    public static Zeze.Transaction.DynamicBean newDynamicBean_Data() {
        return new Zeze.Transaction.DynamicBean(1, Zeze.World.World::getSpecialTypeIdFromBean, Zeze.World.World::createBeanFromSpecialTypeId);
    }

    public static long getSpecialTypeIdFromBean_1(Zeze.Transaction.Bean bean) {
        return Zeze.World.World.getSpecialTypeIdFromBean(bean);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_1(long typeId) {
        return Zeze.World.World.createBeanFromSpecialTypeId(typeId);
    }

    private final Zeze.Transaction.Collections.CollOne<Zeze.Builtin.World.BMove> _Moving;
    private String _PlayerId;
    private String _LinkName;
    private long _LinkSid;
    private int _Type;
    private int _ConfigId;

    public Zeze.Transaction.DynamicBean getData() {
        return _Data;
    }

    @Override
    public Zeze.Transaction.DynamicBeanReadOnly getDataReadOnly() {
        return _Data;
    }

    public Zeze.Builtin.World.BMove getMoving() {
        return _Moving.getValue();
    }

    public void setMoving(Zeze.Builtin.World.BMove value) {
        _Moving.setValue(value);
    }

    @Override
    public Zeze.Builtin.World.BMoveReadOnly getMovingReadOnly() {
        return _Moving.getValue();
    }

    @Override
    public String getPlayerId() {
        if (!isManaged())
            return _PlayerId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _PlayerId;
        var log = (Log__PlayerId)txn.getLog(objectId() + 3);
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
        txn.putLog(new Log__PlayerId(this, 3, value));
    }

    @Override
    public String getLinkName() {
        if (!isManaged())
            return _LinkName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _LinkName;
        var log = (Log__LinkName)txn.getLog(objectId() + 4);
        return log != null ? log.value : _LinkName;
    }

    public void setLinkName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _LinkName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__LinkName(this, 4, value));
    }

    @Override
    public long getLinkSid() {
        if (!isManaged())
            return _LinkSid;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _LinkSid;
        var log = (Log__LinkSid)txn.getLog(objectId() + 5);
        return log != null ? log.value : _LinkSid;
    }

    public void setLinkSid(long value) {
        if (!isManaged()) {
            _LinkSid = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__LinkSid(this, 5, value));
    }

    @Override
    public int getType() {
        if (!isManaged())
            return _Type;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Type;
        var log = (Log__Type)txn.getLog(objectId() + 6);
        return log != null ? log.value : _Type;
    }

    public void setType(int value) {
        if (!isManaged()) {
            _Type = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Type(this, 6, value));
    }

    @Override
    public int getConfigId() {
        if (!isManaged())
            return _ConfigId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ConfigId;
        var log = (Log__ConfigId)txn.getLog(objectId() + 7);
        return log != null ? log.value : _ConfigId;
    }

    public void setConfigId(int value) {
        if (!isManaged()) {
            _ConfigId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ConfigId(this, 7, value));
    }

    @SuppressWarnings("deprecation")
    public BObject() {
        _Data = newDynamicBean_Data();
        _Moving = new Zeze.Transaction.Collections.CollOne<>(new Zeze.Builtin.World.BMove(), Zeze.Builtin.World.BMove.class);
        _Moving.variableId(2);
        _PlayerId = "";
        _LinkName = "";
    }

    @SuppressWarnings("deprecation")
    public BObject(String _PlayerId_, String _LinkName_, long _LinkSid_, int _Type_, int _ConfigId_) {
        _Data = newDynamicBean_Data();
        _Moving = new Zeze.Transaction.Collections.CollOne<>(new Zeze.Builtin.World.BMove(), Zeze.Builtin.World.BMove.class);
        _Moving.variableId(2);
        if (_PlayerId_ == null)
            _PlayerId_ = "";
        _PlayerId = _PlayerId_;
        if (_LinkName_ == null)
            _LinkName_ = "";
        _LinkName = _LinkName_;
        _LinkSid = _LinkSid_;
        _Type = _Type_;
        _ConfigId = _ConfigId_;
    }

    @Override
    public void reset() {
        _Data.reset();
        _Moving.reset();
        setPlayerId("");
        setLinkName("");
        setLinkSid(0);
        setType(0);
        setConfigId(0);
        _unknown_ = null;
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
        _Data.assign(other._Data);
        Zeze.Builtin.World.BMove data_Moving = new Zeze.Builtin.World.BMove();
        data_Moving.assign(other._Moving);
        _Moving.setValue(data_Moving);
        setPlayerId(other._PlayerId);
        setLinkName(other._LinkName);
        setLinkSid(other._LinkSid);
        setType(other._Type);
        setConfigId(other._ConfigId);
        _unknown_ = null;
    }

    public void assign(BObject other) {
        _Data.assign(other._Data);
        _Moving.assign(other._Moving);
        setPlayerId(other.getPlayerId());
        setLinkName(other.getLinkName());
        setLinkSid(other.getLinkSid());
        setType(other.getType());
        setConfigId(other.getConfigId());
        _unknown_ = other._unknown_;
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

    private static final class Log__PlayerId extends Zeze.Transaction.Logs.LogString {
        public Log__PlayerId(BObject bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BObject)getBelong())._PlayerId = value; }
    }

    private static final class Log__LinkName extends Zeze.Transaction.Logs.LogString {
        public Log__LinkName(BObject bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BObject)getBelong())._LinkName = value; }
    }

    private static final class Log__LinkSid extends Zeze.Transaction.Logs.LogLong {
        public Log__LinkSid(BObject bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BObject)getBelong())._LinkSid = value; }
    }

    private static final class Log__Type extends Zeze.Transaction.Logs.LogInt {
        public Log__Type(BObject bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BObject)getBelong())._Type = value; }
    }

    private static final class Log__ConfigId extends Zeze.Transaction.Logs.LogInt {
        public Log__ConfigId(BObject bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BObject)getBelong())._ConfigId = value; }
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
        sb.append(Zeze.Util.Str.indent(level)).append("Data=").append(System.lineSeparator());
        _Data.getBean().buildString(sb, level + 4);
        sb.append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Moving=").append(System.lineSeparator());
        _Moving.buildString(sb, level + 4);
        sb.append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("PlayerId=").append(getPlayerId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("LinkName=").append(getLinkName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("LinkSid=").append(getLinkSid()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Type=").append(getType()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ConfigId=").append(getConfigId()).append(System.lineSeparator());
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
            var _x_ = _Data;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.DYNAMIC);
                _x_.encode(_o_);
            }
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 2, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _Moving.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        {
            String _x_ = getPlayerId();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getLinkName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            long _x_ = getLinkSid();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            int _x_ = getType();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getConfigId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 7, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
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
            _o_.ReadDynamic(_Data, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _o_.ReadBean(_Moving, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setPlayerId(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setLinkName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setLinkSid(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            setType(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 7) {
            setConfigId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Data.initRootInfo(root, this);
        _Moving.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _Data.initRootInfoWithRedo(root, this);
        _Moving.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
        if (_Moving.negativeCheck())
            return true;
        if (getLinkSid() < 0)
            return true;
        if (getType() < 0)
            return true;
        if (getConfigId() < 0)
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
                case 1: _Data.followerApply(vlog); break;
                case 2: _Moving.followerApply(vlog); break;
                case 3: _PlayerId = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 4: _LinkName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 5: _LinkSid = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 6: _Type = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 7: _ConfigId = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        Zeze.Serialize.Helper.decodeJsonDynamic(_Data, rs.getString(_parents_name_ + "Data"));
        parents.add("Moving");
        _Moving.decodeResultSet(parents, rs);
        parents.remove(parents.size() - 1);
        setPlayerId(rs.getString(_parents_name_ + "PlayerId"));
        if (getPlayerId() == null)
            setPlayerId("");
        setLinkName(rs.getString(_parents_name_ + "LinkName"));
        if (getLinkName() == null)
            setLinkName("");
        setLinkSid(rs.getLong(_parents_name_ + "LinkSid"));
        setType(rs.getInt(_parents_name_ + "Type"));
        setConfigId(rs.getInt(_parents_name_ + "ConfigId"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "Data", Zeze.Serialize.Helper.encodeJson(_Data));
        parents.add("Moving");
        _Moving.encodeSQLStatement(parents, st);
        parents.remove(parents.size() - 1);
        st.appendString(_parents_name_ + "PlayerId", getPlayerId());
        st.appendString(_parents_name_ + "LinkName", getLinkName());
        st.appendLong(_parents_name_ + "LinkSid", getLinkSid());
        st.appendInt(_parents_name_ + "Type", getType());
        st.appendInt(_parents_name_ + "ConfigId", getConfigId());
    }

    @Override
    public java.util.List<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Data", "dynamic", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Moving", "BMove", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "PlayerId", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "LinkName", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "LinkSid", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(6, "Type", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(7, "ConfigId", "int", "", ""));
        return vars;
    }

    @Override
    public Zeze.Transaction.Bean toPrevious() {
        return null; // todo BObject
    }

public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -2457457472033861643L;

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

    private Zeze.Builtin.World.BMove.Data _Moving;
    private String _PlayerId;
    private String _LinkName;
    private long _LinkSid;
    private int _Type;
    private int _ConfigId;

    public DynamicData_Data getData() {
        return _Data;
    }

    public Zeze.Builtin.World.BMove.Data getMoving() {
        return _Moving;
    }

    public void setMoving(Zeze.Builtin.World.BMove.Data value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Moving = value;
    }

    public String getPlayerId() {
        return _PlayerId;
    }

    public void setPlayerId(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _PlayerId = value;
    }

    public String getLinkName() {
        return _LinkName;
    }

    public void setLinkName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _LinkName = value;
    }

    public long getLinkSid() {
        return _LinkSid;
    }

    public void setLinkSid(long value) {
        _LinkSid = value;
    }

    public int getType() {
        return _Type;
    }

    public void setType(int value) {
        _Type = value;
    }

    public int getConfigId() {
        return _ConfigId;
    }

    public void setConfigId(int value) {
        _ConfigId = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Data = new DynamicData_Data();
        _Moving = new Zeze.Builtin.World.BMove.Data();
        _PlayerId = "";
        _LinkName = "";
    }

    @SuppressWarnings("deprecation")
    public Data(DynamicData_Data _Data_, Zeze.Builtin.World.BMove.Data _Moving_, String _PlayerId_, String _LinkName_, long _LinkSid_, int _Type_, int _ConfigId_) {
        if (_Data_ == null)
            _Data_ = new DynamicData_Data();
        _Data = _Data_;
        if (_Moving_ == null)
            _Moving_ = new Zeze.Builtin.World.BMove.Data();
        _Moving = _Moving_;
        if (_PlayerId_ == null)
            _PlayerId_ = "";
        _PlayerId = _PlayerId_;
        if (_LinkName_ == null)
            _LinkName_ = "";
        _LinkName = _LinkName_;
        _LinkSid = _LinkSid_;
        _Type = _Type_;
        _ConfigId = _ConfigId_;
    }

    @Override
    public void reset() {
        _Data.reset();
        _Moving.reset();
        _PlayerId = "";
        _LinkName = "";
        _LinkSid = 0;
        _Type = 0;
        _ConfigId = 0;
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
        _Data.assign(other._Data);
        _Moving.assign(other._Moving.getValue());
        _PlayerId = other.getPlayerId();
        _LinkName = other.getLinkName();
        _LinkSid = other.getLinkSid();
        _Type = other.getType();
        _ConfigId = other.getConfigId();
    }

    public void assign(BObject.Data other) {
        _Data.assign(other._Data);
        _Moving.assign(other._Moving);
        _PlayerId = other._PlayerId;
        _LinkName = other._LinkName;
        _LinkSid = other._LinkSid;
        _Type = other._Type;
        _ConfigId = other._ConfigId;
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
        sb.append(Zeze.Util.Str.indent(level)).append("Data=").append(System.lineSeparator());
        _Data.getData().buildString(sb, level + 4);
        sb.append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Moving=").append(System.lineSeparator());
        _Moving.buildString(sb, level + 4);
        sb.append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("PlayerId=").append(_PlayerId).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("LinkName=").append(_LinkName).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("LinkSid=").append(_LinkSid).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Type=").append(_Type).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ConfigId=").append(_ConfigId).append(System.lineSeparator());
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
            var _x_ = _Data;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.DYNAMIC);
                _x_.encode(_o_);
            }
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 2, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _Moving.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        {
            String _x_ = _PlayerId;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = _LinkName;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            long _x_ = _LinkSid;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            int _x_ = _Type;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = _ConfigId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 7, ByteBuffer.INTEGER);
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
            _o_.ReadDynamic(_Data, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _o_.ReadBean(_Moving, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _PlayerId = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _LinkName = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            _LinkSid = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            _Type = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 7) {
            _ConfigId = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
