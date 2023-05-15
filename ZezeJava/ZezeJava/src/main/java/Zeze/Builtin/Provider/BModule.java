// auto-generated @formatter:off
package Zeze.Builtin.Provider;

import Zeze.Serialize.ByteBuffer;

// gs to link
@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BModule extends Zeze.Transaction.Bean implements BModuleReadOnly {
    public static final long TYPEID = 5883923521926593765L;

    public static final int ChoiceTypeDefault = 0; // choice by load
    public static final int ChoiceTypeHashAccount = 1;
    public static final int ChoiceTypeHashRoleId = 2;
    public static final int ChoiceTypeFeedFullOneByOne = 3;
    public static final int ChoiceTypeHashSourceAddress = 4;
    public static final int ConfigTypeDefault = 0;
    public static final int ConfigTypeSpecial = 1;
    public static final int ConfigTypeDynamic = 2;

    private int _ChoiceType;
    private int _ConfigType;
    private int _SubscribeType;

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

    @Override
    public int getSubscribeType() {
        if (!isManaged())
            return _SubscribeType;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _SubscribeType;
        var log = (Log__SubscribeType)txn.getLog(objectId() + 3);
        return log != null ? log.value : _SubscribeType;
    }

    public void setSubscribeType(int value) {
        if (!isManaged()) {
            _SubscribeType = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__SubscribeType(this, 3, value));
    }

    @SuppressWarnings("deprecation")
    public BModule() {
    }

    @SuppressWarnings("deprecation")
    public BModule(int _ChoiceType_, int _ConfigType_, int _SubscribeType_) {
        _ChoiceType = _ChoiceType_;
        _ConfigType = _ConfigType_;
        _SubscribeType = _SubscribeType_;
    }

    public void assign(BModule other) {
        setChoiceType(other.getChoiceType());
        setConfigType(other.getConfigType());
        setSubscribeType(other.getSubscribeType());
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

    private static final class Log__SubscribeType extends Zeze.Transaction.Logs.LogInt {
        public Log__SubscribeType(BModule bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BModule)getBelong())._SubscribeType = value; }
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
        sb.append(Zeze.Util.Str.indent(level)).append("ConfigType=").append(getConfigType()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("SubscribeType=").append(getSubscribeType()).append(System.lineSeparator());
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
        {
            int _x_ = getSubscribeType();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
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
            setChoiceType(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setConfigType(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setSubscribeType(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    public boolean negativeCheck() {
        if (getChoiceType() < 0)
            return true;
        if (getConfigType() < 0)
            return true;
        if (getSubscribeType() < 0)
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
                case 3: _SubscribeType = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setChoiceType(rs.getInt(_parents_name_ + "ChoiceType"));
        setConfigType(rs.getInt(_parents_name_ + "ConfigType"));
        setSubscribeType(rs.getInt(_parents_name_ + "SubscribeType"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendInt(_parents_name_ + "ChoiceType", getChoiceType());
        st.appendInt(_parents_name_ + "ConfigType", getConfigType());
        st.appendInt(_parents_name_ + "SubscribeType", getSubscribeType());
    }
}
