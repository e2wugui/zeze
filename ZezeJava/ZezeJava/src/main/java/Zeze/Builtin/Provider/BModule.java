// auto-generated @formatter:off
package Zeze.Builtin.Provider;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BModule extends Zeze.Transaction.Bean {
    public static final int ChoiceTypeDefault = 0; // choice by load
    public static final int ChoiceTypeHashAccount = 1;
    public static final int ChoiceTypeHashRoleId = 2;
    public static final int ChoiceTypeFeedFullOneByOne = 3;
    public static final int ConfigTypeDefault = 0;
    public static final int ConfigTypeSpecial = 1;
    public static final int ConfigTypeDynamic = 2;

    private int _ChoiceType;
    private int _ConfigType;
    private int _SubscribeType;

    private transient Object __zeze_map_key__;

    @Override
    public Object getMapKey() {
        return __zeze_map_key__;
    }

    @Override
    public void setMapKey(Object value) {
        __zeze_map_key__ = value;
    }

    public int getChoiceType() {
        if (!isManaged())
            return _ChoiceType;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _ChoiceType;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ChoiceType)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.Value : _ChoiceType;
    }

    public void setChoiceType(int value) {
        if (!isManaged()) {
            _ChoiceType = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__ChoiceType(this, 1, value));
    }

    public int getConfigType() {
        if (!isManaged())
            return _ConfigType;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _ConfigType;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__ConfigType)txn.GetLog(this.getObjectId() + 2);
        return log != null ? log.Value : _ConfigType;
    }

    public void setConfigType(int value) {
        if (!isManaged()) {
            _ConfigType = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__ConfigType(this, 2, value));
    }

    public int getSubscribeType() {
        if (!isManaged())
            return _SubscribeType;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _SubscribeType;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__SubscribeType)txn.GetLog(this.getObjectId() + 3);
        return log != null ? log.Value : _SubscribeType;
    }

    public void setSubscribeType(int value) {
        if (!isManaged()) {
            _SubscribeType = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__SubscribeType(this, 3, value));
    }

    public BModule() {
         this(0);
    }

    public BModule(int _varId_) {
        super(_varId_);
    }

    public void Assign(BModule other) {
        setChoiceType(other.getChoiceType());
        setConfigType(other.getConfigType());
        setSubscribeType(other.getSubscribeType());
    }

    public BModule CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BModule Copy() {
        var copy = new BModule();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BModule a, BModule b) {
        BModule save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public static final long TYPEID = 5883923521926593765L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private static final class Log__ChoiceType extends Zeze.Transaction.Logs.LogInt {
        public Log__ChoiceType(BModule bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BModule)getBelong())._ChoiceType = Value; }
    }

    private static final class Log__ConfigType extends Zeze.Transaction.Logs.LogInt {
        public Log__ConfigType(BModule bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BModule)getBelong())._ConfigType = Value; }
    }

    private static final class Log__SubscribeType extends Zeze.Transaction.Logs.LogInt {
        public Log__SubscribeType(BModule bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BModule)getBelong())._SubscribeType = Value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        BuildString(sb, 0);
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    @Override
    public void BuildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Provider.BModule: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ChoiceType").append('=').append(getChoiceType()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ConfigType").append('=').append(getConfigType()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("SubscribeType").append('=').append(getSubscribeType()).append(System.lineSeparator());
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append('}');
    }

    private static int _PRE_ALLOC_SIZE_ = 16;

    @Override
    public int getPreAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void setPreAllocSize(int size) {
        _PRE_ALLOC_SIZE_ = size;
    }

    @Override
    public void Encode(ByteBuffer _o_) {
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
    public void Decode(ByteBuffer _o_) {
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
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
    }

    @Override
    protected void ResetChildrenRootInfo() {
    }

    @Override
    public boolean NegativeCheck() {
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
    public void FollowerApply(Zeze.Transaction.Log log) {
        var vars = ((Zeze.Transaction.Collections.LogBean)log).getVariables();
        if (vars == null)
            return;
        for (var it = vars.iterator(); it.moveToNext(); ) {
            var vlog = it.value();
            switch (vlog.getVariableId()) {
                case 1: _ChoiceType = ((Zeze.Transaction.Logs.LogInt)vlog).Value; break;
                case 2: _ConfigType = ((Zeze.Transaction.Logs.LogInt)vlog).Value; break;
                case 3: _SubscribeType = ((Zeze.Transaction.Logs.LogInt)vlog).Value; break;
            }
        }
    }
}
