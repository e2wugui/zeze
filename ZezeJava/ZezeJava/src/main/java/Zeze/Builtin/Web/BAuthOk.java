// auto-generated @formatter:off
package Zeze.Builtin.Web;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BAuthOk extends Zeze.Transaction.Bean {
    private final Zeze.Builtin.Web.BRequest _Request;
    private String _Account;

    public Zeze.Builtin.Web.BRequest getRequest(){
        return _Request;
    }

    public String getAccount() {
        if (!isManaged())
            return _Account;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _Account;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__Account)txn.GetLog(this.getObjectId() + 2);
        return log != null ? log.Value : _Account;
    }

    public void setAccount(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Account = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__Account(this, 2, value));
    }

    public BAuthOk() {
         this(0);
    }

    public BAuthOk(int _varId_) {
        super(_varId_);
        _Request = new Zeze.Builtin.Web.BRequest(1);
        _Account = "";
    }

    public void Assign(BAuthOk other) {
        getRequest().Assign(other.getRequest());
        setAccount(other.getAccount());
    }

    public BAuthOk CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BAuthOk Copy() {
        var copy = new BAuthOk();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BAuthOk a, BAuthOk b) {
        BAuthOk save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public static final long TYPEID = 8740526856922157470L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private static final class Log__Account extends Zeze.Transaction.Logs.LogString {
        public Log__Account(BAuthOk bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void Commit() { ((BAuthOk)getBelong())._Account = Value; }
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Web.BAuthOk: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Request").append('=').append(System.lineSeparator());
        getRequest().BuildString(sb, level + 4);
        sb.append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Account").append('=').append(getAccount()).append(System.lineSeparator());
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
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 1, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            getRequest().Encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        {
            String _x_ = getAccount();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void Decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _o_.ReadBean(getRequest(), _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setAccount(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Request.InitRootInfo(root, this);
    }

    @Override
    public boolean NegativeCheck() {
        if (getRequest().NegativeCheck())
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
                case 1: _Request.FollowerApply(vlog); break;
                case 2: _Account = ((Zeze.Transaction.Logs.LogString)vlog).Value; break;
            }
        }
    }
}
