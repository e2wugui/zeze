// auto-generated @formatter:off
package Zeze.Builtin.Collections.BoolList;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BValue extends Zeze.Transaction.Bean implements BValueReadOnly {
    public static final long TYPEID = -6830223910089940882L;

    private long _Item0;
    private long _Item1;
    private long _Item2;
    private long _Item3;
    private long _Item4;
    private long _Item5;
    private long _Item6;
    private long _Item7;

    @Override
    public long getItem0() {
        if (!isManaged())
            return _Item0;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Item0;
        var log = (Log__Item0)txn.getLog(objectId() + 1);
        return log != null ? log.value : _Item0;
    }

    public void setItem0(long value) {
        if (!isManaged()) {
            _Item0 = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Item0(this, 1, value));
    }

    @Override
    public long getItem1() {
        if (!isManaged())
            return _Item1;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Item1;
        var log = (Log__Item1)txn.getLog(objectId() + 2);
        return log != null ? log.value : _Item1;
    }

    public void setItem1(long value) {
        if (!isManaged()) {
            _Item1 = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Item1(this, 2, value));
    }

    @Override
    public long getItem2() {
        if (!isManaged())
            return _Item2;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Item2;
        var log = (Log__Item2)txn.getLog(objectId() + 3);
        return log != null ? log.value : _Item2;
    }

    public void setItem2(long value) {
        if (!isManaged()) {
            _Item2 = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Item2(this, 3, value));
    }

    @Override
    public long getItem3() {
        if (!isManaged())
            return _Item3;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Item3;
        var log = (Log__Item3)txn.getLog(objectId() + 4);
        return log != null ? log.value : _Item3;
    }

    public void setItem3(long value) {
        if (!isManaged()) {
            _Item3 = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Item3(this, 4, value));
    }

    @Override
    public long getItem4() {
        if (!isManaged())
            return _Item4;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Item4;
        var log = (Log__Item4)txn.getLog(objectId() + 5);
        return log != null ? log.value : _Item4;
    }

    public void setItem4(long value) {
        if (!isManaged()) {
            _Item4 = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Item4(this, 5, value));
    }

    @Override
    public long getItem5() {
        if (!isManaged())
            return _Item5;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Item5;
        var log = (Log__Item5)txn.getLog(objectId() + 6);
        return log != null ? log.value : _Item5;
    }

    public void setItem5(long value) {
        if (!isManaged()) {
            _Item5 = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Item5(this, 6, value));
    }

    @Override
    public long getItem6() {
        if (!isManaged())
            return _Item6;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Item6;
        var log = (Log__Item6)txn.getLog(objectId() + 7);
        return log != null ? log.value : _Item6;
    }

    public void setItem6(long value) {
        if (!isManaged()) {
            _Item6 = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Item6(this, 7, value));
    }

    @Override
    public long getItem7() {
        if (!isManaged())
            return _Item7;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Item7;
        var log = (Log__Item7)txn.getLog(objectId() + 8);
        return log != null ? log.value : _Item7;
    }

    public void setItem7(long value) {
        if (!isManaged()) {
            _Item7 = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Item7(this, 8, value));
    }

    @SuppressWarnings("deprecation")
    public BValue() {
    }

    @SuppressWarnings("deprecation")
    public BValue(long _Item0_, long _Item1_, long _Item2_, long _Item3_, long _Item4_, long _Item5_, long _Item6_, long _Item7_) {
        _Item0 = _Item0_;
        _Item1 = _Item1_;
        _Item2 = _Item2_;
        _Item3 = _Item3_;
        _Item4 = _Item4_;
        _Item5 = _Item5_;
        _Item6 = _Item6_;
        _Item7 = _Item7_;
    }

    @Override
    public void reset() {
        setItem0(0);
        setItem1(0);
        setItem2(0);
        setItem3(0);
        setItem4(0);
        setItem5(0);
        setItem6(0);
        setItem7(0);
        _unknown_ = null;
    }

    public void assign(BValue other) {
        setItem0(other.getItem0());
        setItem1(other.getItem1());
        setItem2(other.getItem2());
        setItem3(other.getItem3());
        setItem4(other.getItem4());
        setItem5(other.getItem5());
        setItem6(other.getItem6());
        setItem7(other.getItem7());
        _unknown_ = other._unknown_;
    }

    public BValue copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BValue copy() {
        var copy = new BValue();
        copy.assign(this);
        return copy;
    }

    public static void swap(BValue a, BValue b) {
        BValue save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Item0 extends Zeze.Transaction.Logs.LogLong {
        public Log__Item0(BValue bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BValue)getBelong())._Item0 = value; }
    }

    private static final class Log__Item1 extends Zeze.Transaction.Logs.LogLong {
        public Log__Item1(BValue bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BValue)getBelong())._Item1 = value; }
    }

    private static final class Log__Item2 extends Zeze.Transaction.Logs.LogLong {
        public Log__Item2(BValue bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BValue)getBelong())._Item2 = value; }
    }

    private static final class Log__Item3 extends Zeze.Transaction.Logs.LogLong {
        public Log__Item3(BValue bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BValue)getBelong())._Item3 = value; }
    }

    private static final class Log__Item4 extends Zeze.Transaction.Logs.LogLong {
        public Log__Item4(BValue bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BValue)getBelong())._Item4 = value; }
    }

    private static final class Log__Item5 extends Zeze.Transaction.Logs.LogLong {
        public Log__Item5(BValue bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BValue)getBelong())._Item5 = value; }
    }

    private static final class Log__Item6 extends Zeze.Transaction.Logs.LogLong {
        public Log__Item6(BValue bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BValue)getBelong())._Item6 = value; }
    }

    private static final class Log__Item7 extends Zeze.Transaction.Logs.LogLong {
        public Log__Item7(BValue bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BValue)getBelong())._Item7 = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Collections.BoolList.BValue: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Item0=").append(getItem0()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Item1=").append(getItem1()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Item2=").append(getItem2()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Item3=").append(getItem3()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Item4=").append(getItem4()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Item5=").append(getItem5()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Item6=").append(getItem6()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Item7=").append(getItem7()).append(System.lineSeparator());
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
            long _x_ = getItem0();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getItem1();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getItem2();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getItem3();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getItem4();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getItem5();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getItem6();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 7, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getItem7();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 8, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
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
            setItem0(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setItem1(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setItem2(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setItem3(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setItem4(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            setItem5(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 7) {
            setItem6(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 8) {
            setItem7(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean negativeCheck() {
        if (getItem0() < 0)
            return true;
        if (getItem1() < 0)
            return true;
        if (getItem2() < 0)
            return true;
        if (getItem3() < 0)
            return true;
        if (getItem4() < 0)
            return true;
        if (getItem5() < 0)
            return true;
        if (getItem6() < 0)
            return true;
        if (getItem7() < 0)
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
                case 1: _Item0 = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 2: _Item1 = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 3: _Item2 = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 4: _Item3 = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 5: _Item4 = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 6: _Item5 = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 7: _Item6 = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 8: _Item7 = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setItem0(rs.getLong(_parents_name_ + "Item0"));
        setItem1(rs.getLong(_parents_name_ + "Item1"));
        setItem2(rs.getLong(_parents_name_ + "Item2"));
        setItem3(rs.getLong(_parents_name_ + "Item3"));
        setItem4(rs.getLong(_parents_name_ + "Item4"));
        setItem5(rs.getLong(_parents_name_ + "Item5"));
        setItem6(rs.getLong(_parents_name_ + "Item6"));
        setItem7(rs.getLong(_parents_name_ + "Item7"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendLong(_parents_name_ + "Item0", getItem0());
        st.appendLong(_parents_name_ + "Item1", getItem1());
        st.appendLong(_parents_name_ + "Item2", getItem2());
        st.appendLong(_parents_name_ + "Item3", getItem3());
        st.appendLong(_parents_name_ + "Item4", getItem4());
        st.appendLong(_parents_name_ + "Item5", getItem5());
        st.appendLong(_parents_name_ + "Item6", getItem6());
        st.appendLong(_parents_name_ + "Item7", getItem7());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Item0", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Item1", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Item2", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "Item3", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "Item4", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(6, "Item5", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(7, "Item6", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(8, "Item7", "long", "", ""));
        return vars;
    }
}
