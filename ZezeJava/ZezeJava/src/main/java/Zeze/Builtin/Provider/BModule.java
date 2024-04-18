// auto-generated @formatter:off
package Zeze.Builtin.Provider;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

// gs to link
@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BModule extends Zeze.Transaction.Bean implements BModuleReadOnly {
    public static final long TYPEID = 5883923521926593765L;

    public static final int ChoiceTypeDefault = 0; // 默认是ChoiceTypeRequest
    public static final int ChoiceTypeHashAccount = 1; // 按账号名的一致性hash选取
    public static final int ChoiceTypeHashRoleId = 2; // 按角色ID的一致性hash选取
    public static final int ChoiceTypeFeedFullOneByOne = 3; // 使用全局迭代器选取符合条件的(有效,非过载,非超限,匹配版本)
    public static final int ChoiceTypeHashSourceAddress = 4; // 按来源IP地址端口的一致性hash选取
    public static final int ChoiceTypeLoad = 5; // 从符合条件的(有效,非过载,匹配版本)里面以剩余承载量为权重选取
    public static final int ChoiceTypeRequest = 6; // 从符合条件的(有效,非过载,匹配版本)里面以最近5秒请求量反比为权重选取
    public static final int ConfigTypeDefault = 0;
    public static final int ConfigTypeSpecial = 1;
    public static final int ConfigTypeDynamic = 2;

    private int _ChoiceType;
    private int _ConfigType;

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
    public int getChoiceType() {
        if (!isManaged())
            return _ChoiceType;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ChoiceType;
        var log = (Log__ChoiceType)txn.getLog(objectId() + 1);
        return log != null ? log.value : _ChoiceType;
    }

    public void setChoiceType(int value) {
        if (!isManaged()) {
            _ChoiceType = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ChoiceType(this, 1, value));
    }

    @Override
    public int getConfigType() {
        if (!isManaged())
            return _ConfigType;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ConfigType;
        var log = (Log__ConfigType)txn.getLog(objectId() + 2);
        return log != null ? log.value : _ConfigType;
    }

    public void setConfigType(int value) {
        if (!isManaged()) {
            _ConfigType = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ConfigType(this, 2, value));
    }

    @SuppressWarnings("deprecation")
    public BModule() {
    }

    @SuppressWarnings("deprecation")
    public BModule(int _ChoiceType_, int _ConfigType_) {
        _ChoiceType = _ChoiceType_;
        _ConfigType = _ConfigType_;
    }

    @Override
    public void reset() {
        setChoiceType(0);
        setConfigType(0);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Provider.BModule.Data toData() {
        var data = new Zeze.Builtin.Provider.BModule.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Provider.BModule.Data)other);
    }

    public void assign(BModule.Data other) {
        setChoiceType(other._ChoiceType);
        setConfigType(other._ConfigType);
        _unknown_ = null;
    }

    public void assign(BModule other) {
        setChoiceType(other.getChoiceType());
        setConfigType(other.getConfigType());
        _unknown_ = other._unknown_;
    }

    public BModule copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BModule copy() {
        var copy = new BModule();
        copy.assign(this);
        return copy;
    }

    public static void swap(BModule a, BModule b) {
        BModule save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ChoiceType extends Zeze.Transaction.Logs.LogInt {
        public Log__ChoiceType(BModule bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BModule)getBelong())._ChoiceType = value; }
    }

    private static final class Log__ConfigType extends Zeze.Transaction.Logs.LogInt {
        public Log__ConfigType(BModule bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BModule)getBelong())._ConfigType = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Provider.BModule: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ChoiceType=").append(getChoiceType()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ConfigType=").append(getConfigType()).append(System.lineSeparator());
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
            int _x_ = getChoiceType();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getConfigType();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
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
            setChoiceType(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setConfigType(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean negativeCheck() {
        if (getChoiceType() < 0)
            return true;
        if (getConfigType() < 0)
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
                case 1: _ChoiceType = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 2: _ConfigType = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setChoiceType(rs.getInt(_parents_name_ + "ChoiceType"));
        setConfigType(rs.getInt(_parents_name_ + "ConfigType"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendInt(_parents_name_ + "ChoiceType", getChoiceType());
        st.appendInt(_parents_name_ + "ConfigType", getConfigType());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "ChoiceType", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "ConfigType", "int", "", ""));
        return vars;
    }

// gs to link
@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 5883923521926593765L;

    public static final int ChoiceTypeDefault = 0; // 默认是ChoiceTypeRequest
    public static final int ChoiceTypeHashAccount = 1; // 按账号名的一致性hash选取
    public static final int ChoiceTypeHashRoleId = 2; // 按角色ID的一致性hash选取
    public static final int ChoiceTypeFeedFullOneByOne = 3; // 使用全局迭代器选取符合条件的(有效,非过载,非超限,匹配版本)
    public static final int ChoiceTypeHashSourceAddress = 4; // 按来源IP地址端口的一致性hash选取
    public static final int ChoiceTypeLoad = 5; // 从符合条件的(有效,非过载,匹配版本)里面以剩余承载量为权重选取
    public static final int ChoiceTypeRequest = 6; // 从符合条件的(有效,非过载,匹配版本)里面以最近5秒请求量反比为权重选取
    public static final int ConfigTypeDefault = 0;
    public static final int ConfigTypeSpecial = 1;
    public static final int ConfigTypeDynamic = 2;

    private int _ChoiceType;
    private int _ConfigType;

    public int getChoiceType() {
        return _ChoiceType;
    }

    public void setChoiceType(int value) {
        _ChoiceType = value;
    }

    public int getConfigType() {
        return _ConfigType;
    }

    public void setConfigType(int value) {
        _ConfigType = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
    }

    @SuppressWarnings("deprecation")
    public Data(int _ChoiceType_, int _ConfigType_) {
        _ChoiceType = _ChoiceType_;
        _ConfigType = _ConfigType_;
    }

    @Override
    public void reset() {
        _ChoiceType = 0;
        _ConfigType = 0;
    }

    @Override
    public Zeze.Builtin.Provider.BModule toBean() {
        var bean = new Zeze.Builtin.Provider.BModule();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BModule)other);
    }

    public void assign(BModule other) {
        _ChoiceType = other.getChoiceType();
        _ConfigType = other.getConfigType();
    }

    public void assign(BModule.Data other) {
        _ChoiceType = other._ChoiceType;
        _ConfigType = other._ConfigType;
    }

    @Override
    public BModule.Data copy() {
        var copy = new BModule.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BModule.Data a, BModule.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BModule.Data clone() {
        return (BModule.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Provider.BModule: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ChoiceType=").append(_ChoiceType).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ConfigType=").append(_ConfigType).append(System.lineSeparator());
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
            int _x_ = _ChoiceType;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = _ConfigType;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _ChoiceType = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _ConfigType = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
