// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BOfflineAccountCustom extends Zeze.Transaction.Bean {
    public static final long TYPEID = -8019295337231502138L;

    private String _Account;
    private String _ClientId;
    private final Zeze.Transaction.DynamicBean _CustomData;

    public static Zeze.Transaction.DynamicBean newDynamicBean_CustomData() {
        return new Zeze.Transaction.DynamicBean(3, Zeze.Component.Timer::getSpecialTypeIdFromBean, Zeze.Component.Timer::createBeanFromSpecialTypeId);
    }

    public static long getSpecialTypeIdFromBean_CustomData(Zeze.Transaction.Bean bean) {
        return Zeze.Component.Timer.getSpecialTypeIdFromBean(bean);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_CustomData(long typeId) {
        return Zeze.Component.Timer.createBeanFromSpecialTypeId(typeId);
    }

    public String getAccount() {
        if (!isManaged())
            return _Account;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Account;
        var log = (Log__Account)txn.getLog(objectId() + 1);
        return log != null ? log.value : _Account;
    }

    public void setAccount(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Account = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Account(this, 1, value));
    }

    public String getClientId() {
        if (!isManaged())
            return _ClientId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ClientId;
        var log = (Log__ClientId)txn.getLog(objectId() + 2);
        return log != null ? log.value : _ClientId;
    }

    public void setClientId(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ClientId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ClientId(this, 2, value));
    }

    public Zeze.Transaction.DynamicBean getCustomData() {
        return _CustomData;
    }


    @SuppressWarnings("deprecation")
    public BOfflineAccountCustom() {
        _Account = "";
        _ClientId = "";
        _CustomData = newDynamicBean_CustomData();
    }

    @SuppressWarnings("deprecation")
    public BOfflineAccountCustom(String _Account_, String _ClientId_) {
        if (_Account_ == null)
            throw new IllegalArgumentException();
        _Account = _Account_;
        if (_ClientId_ == null)
            throw new IllegalArgumentException();
        _ClientId = _ClientId_;
        _CustomData = newDynamicBean_CustomData();
    }

    public void assign(BOfflineAccountCustom other) {
        setAccount(other.getAccount());
        setClientId(other.getClientId());
        _CustomData.assign(other._CustomData);
    }

    @Deprecated
    public void Assign(BOfflineAccountCustom other) {
        assign(other);
    }

    public BOfflineAccountCustom copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BOfflineAccountCustom copy() {
        var copy = new BOfflineAccountCustom();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BOfflineAccountCustom Copy() {
        return copy();
    }

    public static void swap(BOfflineAccountCustom a, BOfflineAccountCustom b) {
        BOfflineAccountCustom save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Account extends Zeze.Transaction.Logs.LogString {
        public Log__Account(BOfflineAccountCustom bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BOfflineAccountCustom)getBelong())._Account = value; }
    }

    private static final class Log__ClientId extends Zeze.Transaction.Logs.LogString {
        public Log__ClientId(BOfflineAccountCustom bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BOfflineAccountCustom)getBelong())._ClientId = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Timer.BOfflineAccountCustom: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Account").append('=').append(getAccount()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ClientId").append('=').append(getClientId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("CustomData").append('=').append(System.lineSeparator());
        _CustomData.getBean().buildString(sb, level + 4);
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
            String _x_ = getAccount();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getClientId();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = _CustomData;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.DYNAMIC);
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
            setAccount(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setClientId(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _o_.ReadDynamic(_CustomData, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _CustomData.initRootInfo(root, this);
    }

    @Override
    protected void resetChildrenRootInfo() {
        _CustomData.resetRootInfo();
    }

    @Override
    public boolean negativeCheck() {
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
                case 1: _Account = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 2: _ClientId = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 3: _CustomData.followerApply(vlog); break;
            }
        }
    }
}
