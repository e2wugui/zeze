// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BTConditionNPCTalkEvent extends Zeze.Transaction.Bean implements BTConditionNPCTalkEventReadOnly {
    public static final long TYPEID = -4899454112203602000L;

    private boolean _finished; // 如果完成了对话，也可以用这个Event发一条，下面的就留空。
    private String _dialogId;
    private int _dialogOption; // 选择了第几个选项

    @Override
    public boolean isFinished() {
        if (!isManaged())
            return _finished;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _finished;
        var log = (Log__finished)txn.getLog(objectId() + 3);
        return log != null ? log.value : _finished;
    }

    public void setFinished(boolean value) {
        if (!isManaged()) {
            _finished = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__finished(this, 3, value));
    }

    @Override
    public String getDialogId() {
        if (!isManaged())
            return _dialogId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _dialogId;
        var log = (Log__dialogId)txn.getLog(objectId() + 4);
        return log != null ? log.value : _dialogId;
    }

    public void setDialogId(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _dialogId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__dialogId(this, 4, value));
    }

    @Override
    public int getDialogOption() {
        if (!isManaged())
            return _dialogOption;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _dialogOption;
        var log = (Log__dialogOption)txn.getLog(objectId() + 5);
        return log != null ? log.value : _dialogOption;
    }

    public void setDialogOption(int value) {
        if (!isManaged()) {
            _dialogOption = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__dialogOption(this, 5, value));
    }

    @SuppressWarnings("deprecation")
    public BTConditionNPCTalkEvent() {
        _dialogId = "";
    }

    @SuppressWarnings("deprecation")
    public BTConditionNPCTalkEvent(boolean _finished_, String _dialogId_, int _dialogOption_) {
        _finished = _finished_;
        if (_dialogId_ == null)
            _dialogId_ = "";
        _dialogId = _dialogId_;
        _dialogOption = _dialogOption_;
    }

    @Override
    public void reset() {
        setFinished(false);
        setDialogId("");
        setDialogOption(0);
        _unknown_ = null;
    }

    public void assign(BTConditionNPCTalkEvent other) {
        setFinished(other.isFinished());
        setDialogId(other.getDialogId());
        setDialogOption(other.getDialogOption());
        _unknown_ = other._unknown_;
    }

    public BTConditionNPCTalkEvent copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTConditionNPCTalkEvent copy() {
        var copy = new BTConditionNPCTalkEvent();
        copy.assign(this);
        return copy;
    }

    public static void swap(BTConditionNPCTalkEvent a, BTConditionNPCTalkEvent b) {
        BTConditionNPCTalkEvent save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__finished extends Zeze.Transaction.Logs.LogBool {
        public Log__finished(BTConditionNPCTalkEvent bean, int varId, boolean value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTConditionNPCTalkEvent)getBelong())._finished = value; }
    }

    private static final class Log__dialogId extends Zeze.Transaction.Logs.LogString {
        public Log__dialogId(BTConditionNPCTalkEvent bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTConditionNPCTalkEvent)getBelong())._dialogId = value; }
    }

    private static final class Log__dialogOption extends Zeze.Transaction.Logs.LogInt {
        public Log__dialogOption(BTConditionNPCTalkEvent bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTConditionNPCTalkEvent)getBelong())._dialogOption = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.TaskBase.BTConditionNPCTalkEvent: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("finished=").append(isFinished()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("dialogId=").append(getDialogId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("dialogOption=").append(getDialogOption()).append(System.lineSeparator());
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
        while (_ui_ < 3) {
            _i_ = _o_.writeUnknownField(_i_, _ui_, _u_);
            _ui_ = _u_.readUnknownIndex();
        }
        {
            boolean _x_ = isFinished();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        {
            String _x_ = getDialogId();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            int _x_ = getDialogOption();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
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
        while ((_t_ & 0xff) > 1 && _i_ < 3) {
            _u_ = _o_.readUnknownField(_i_, _t_, _u_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setFinished(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setDialogId(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setDialogOption(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean negativeCheck() {
        if (getDialogOption() < 0)
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
                case 3: _finished = ((Zeze.Transaction.Logs.LogBool)vlog).value; break;
                case 4: _dialogId = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 5: _dialogOption = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setFinished(rs.getBoolean(_parents_name_ + "finished"));
        setDialogId(rs.getString(_parents_name_ + "dialogId"));
        if (getDialogId() == null)
            setDialogId("");
        setDialogOption(rs.getInt(_parents_name_ + "dialogOption"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendBoolean(_parents_name_ + "finished", isFinished());
        st.appendString(_parents_name_ + "dialogId", getDialogId());
        st.appendInt(_parents_name_ + "dialogOption", getDialogOption());
    }

    @Override
    public java.util.List<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "finished", "bool", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "dialogId", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "dialogOption", "int", "", ""));
        return vars;
    }

    @Override
    public Zeze.Transaction.Bean toPrevious() {
        return null; // todo BTConditionNPCTalkEvent
    }
}
