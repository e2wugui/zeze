// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

import Zeze.Serialize.ByteBuffer;

// tables
@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BOnline extends Zeze.Transaction.Bean implements BOnlineReadOnly {
    public static final long TYPEID = -6079880688513613020L;

    private Zeze.Builtin.Game.Online.BLink _Link;

    @Override
    public Zeze.Builtin.Game.Online.BLink getLink() {
        if (!isManaged())
            return _Link;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Link;
        var log = (Log__Link)txn.getLog(objectId() + 3);
        return log != null ? log.value : _Link;
    }

    public void setLink(Zeze.Builtin.Game.Online.BLink value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Link = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Link(this, 3, value));
    }

    @SuppressWarnings("deprecation")
    public BOnline() {
        _Link = new Zeze.Builtin.Game.Online.BLink();
    }

    @SuppressWarnings("deprecation")
    public BOnline(Zeze.Builtin.Game.Online.BLink _Link_) {
        if (_Link_ == null)
            throw new IllegalArgumentException();
        _Link = _Link_;
    }

    public void assign(BOnline other) {
        setLink(other.getLink());
    }

    public BOnline copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BOnline copy() {
        var copy = new BOnline();
        copy.assign(this);
        return copy;
    }

    public static void swap(BOnline a, BOnline b) {
        BOnline save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Link extends Zeze.Transaction.Logs.LogBeanKey<Zeze.Builtin.Game.Online.BLink> {
        public Log__Link(BOnline bean, int varId, Zeze.Builtin.Game.Online.BLink value) { super(Zeze.Builtin.Game.Online.BLink.class, bean, varId, value); }

        @Override
        public void commit() { ((BOnline)getBelong())._Link = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.Online.BOnline: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Link=").append(System.lineSeparator());
        getLink().buildString(sb, level + 4);
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
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 3, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            getLink().encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        while ((_t_ & 0xff) > 1 && _i_ < 3) {
            _o_.SkipUnknownField(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _o_.ReadBean(getLink(), _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    public boolean negativeCheck() {
        if (getLink().negativeCheck())
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
                case 3: _Link = ((Zeze.Transaction.Logs.LogBeanKey<Zeze.Builtin.Game.Online.BLink>)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        parents.add("Link");
        getLink().decodeResultSet(parents, rs);
        parents.remove(parents.size() - 1);
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        parents.add("Link");
        getLink().encodeSQLStatement(parents, st);
        parents.remove(parents.size() - 1);
    }
}
