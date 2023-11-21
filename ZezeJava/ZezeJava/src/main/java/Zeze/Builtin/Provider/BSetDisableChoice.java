// auto-generated @formatter:off
package Zeze.Builtin.Provider;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BSetDisableChoice extends Zeze.Transaction.Bean implements BSetDisableChoiceReadOnly {
    public static final long TYPEID = -1885417062233949302L;

    private boolean _DisableChoice;

    @Override
    public boolean isDisableChoice() {
        if (!isManaged())
            return _DisableChoice;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _DisableChoice;
        var log = (Log__DisableChoice)txn.getLog(objectId() + 1);
        return log != null ? log.value : _DisableChoice;
    }

    public void setDisableChoice(boolean value) {
        if (!isManaged()) {
            _DisableChoice = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__DisableChoice(this, 1, value));
    }

    @SuppressWarnings("deprecation")
    public BSetDisableChoice() {
    }

    @SuppressWarnings("deprecation")
    public BSetDisableChoice(boolean _DisableChoice_) {
        _DisableChoice = _DisableChoice_;
    }

    @Override
    public void reset() {
        setDisableChoice(false);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Provider.BSetDisableChoice.Data toData() {
        var data = new Zeze.Builtin.Provider.BSetDisableChoice.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Provider.BSetDisableChoice.Data)other);
    }

    public void assign(BSetDisableChoice.Data other) {
        setDisableChoice(other._DisableChoice);
        _unknown_ = null;
    }

    public void assign(BSetDisableChoice other) {
        setDisableChoice(other.isDisableChoice());
        _unknown_ = other._unknown_;
    }

    public BSetDisableChoice copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BSetDisableChoice copy() {
        var copy = new BSetDisableChoice();
        copy.assign(this);
        return copy;
    }

    public static void swap(BSetDisableChoice a, BSetDisableChoice b) {
        BSetDisableChoice save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__DisableChoice extends Zeze.Transaction.Logs.LogBool {
        public Log__DisableChoice(BSetDisableChoice bean, int varId, boolean value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BSetDisableChoice)getBelong())._DisableChoice = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Provider.BSetDisableChoice: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("DisableChoice=").append(isDisableChoice()).append(System.lineSeparator());
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
            boolean _x_ = isDisableChoice();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
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
            setDisableChoice(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
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
                case 1: _DisableChoice = ((Zeze.Transaction.Logs.LogBool)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setDisableChoice(rs.getBoolean(_parents_name_ + "DisableChoice"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendBoolean(_parents_name_ + "DisableChoice", isDisableChoice());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "DisableChoice", "bool", "", ""));
        return vars;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -1885417062233949302L;

    private boolean _DisableChoice;

    public boolean isDisableChoice() {
        return _DisableChoice;
    }

    public void setDisableChoice(boolean value) {
        _DisableChoice = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
    }

    @SuppressWarnings("deprecation")
    public Data(boolean _DisableChoice_) {
        _DisableChoice = _DisableChoice_;
    }

    @Override
    public void reset() {
        _DisableChoice = false;
    }

    @Override
    public Zeze.Builtin.Provider.BSetDisableChoice toBean() {
        var bean = new Zeze.Builtin.Provider.BSetDisableChoice();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BSetDisableChoice)other);
    }

    public void assign(BSetDisableChoice other) {
        _DisableChoice = other.isDisableChoice();
    }

    public void assign(BSetDisableChoice.Data other) {
        _DisableChoice = other._DisableChoice;
    }

    @Override
    public BSetDisableChoice.Data copy() {
        var copy = new BSetDisableChoice.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BSetDisableChoice.Data a, BSetDisableChoice.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BSetDisableChoice.Data clone() {
        return (BSetDisableChoice.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Provider.BSetDisableChoice: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("DisableChoice=").append(_DisableChoice).append(System.lineSeparator());
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
            boolean _x_ = _DisableChoice;
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _DisableChoice = _o_.ReadBool(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
