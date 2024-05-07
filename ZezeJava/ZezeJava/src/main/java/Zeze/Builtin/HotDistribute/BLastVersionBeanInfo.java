// auto-generated @formatter:off
package Zeze.Builtin.HotDistribute;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BLastVersionBeanInfo extends Zeze.Transaction.Bean implements BLastVersionBeanInfoReadOnly {
    public static final long TYPEID = -6575391224958548024L;

    private String _Name;
    private final Zeze.Transaction.Collections.PList2<Zeze.Builtin.HotDistribute.BVariable> _Variables;

    @Override
    public String getName() {
        if (!isManaged())
            return _Name;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Name;
        var log = (Log__Name)txn.getLog(objectId() + 1);
        return log != null ? log.value : _Name;
    }

    public void setName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Name = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Name(this, 1, value));
    }

    public Zeze.Transaction.Collections.PList2<Zeze.Builtin.HotDistribute.BVariable> getVariables() {
        return _Variables;
    }

    @Override
    public Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.HotDistribute.BVariable, Zeze.Builtin.HotDistribute.BVariableReadOnly> getVariablesReadOnly() {
        return new Zeze.Transaction.Collections.PList2ReadOnly<>(_Variables);
    }

    @SuppressWarnings("deprecation")
    public BLastVersionBeanInfo() {
        _Name = "";
        _Variables = new Zeze.Transaction.Collections.PList2<>(Zeze.Builtin.HotDistribute.BVariable.class);
        _Variables.variableId(2);
    }

    @SuppressWarnings("deprecation")
    public BLastVersionBeanInfo(String _Name_) {
        if (_Name_ == null)
            _Name_ = "";
        _Name = _Name_;
        _Variables = new Zeze.Transaction.Collections.PList2<>(Zeze.Builtin.HotDistribute.BVariable.class);
        _Variables.variableId(2);
    }

    @Override
    public void reset() {
        setName("");
        _Variables.clear();
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.HotDistribute.BLastVersionBeanInfo.Data toData() {
        var data = new Zeze.Builtin.HotDistribute.BLastVersionBeanInfo.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.HotDistribute.BLastVersionBeanInfo.Data)other);
    }

    public void assign(BLastVersionBeanInfo.Data other) {
        setName(other._Name);
        _Variables.clear();
        for (var e : other._Variables) {
            Zeze.Builtin.HotDistribute.BVariable data = new Zeze.Builtin.HotDistribute.BVariable();
            data.assign(e);
            _Variables.add(data);
        }
        _unknown_ = null;
    }

    public void assign(BLastVersionBeanInfo other) {
        setName(other.getName());
        _Variables.clear();
        for (var e : other._Variables)
            _Variables.add(e.copy());
        _unknown_ = other._unknown_;
    }

    public BLastVersionBeanInfo copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BLastVersionBeanInfo copy() {
        var copy = new BLastVersionBeanInfo();
        copy.assign(this);
        return copy;
    }

    public static void swap(BLastVersionBeanInfo a, BLastVersionBeanInfo b) {
        BLastVersionBeanInfo save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Name extends Zeze.Transaction.Logs.LogString {
        public Log__Name(BLastVersionBeanInfo bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BLastVersionBeanInfo)getBelong())._Name = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.HotDistribute.BLastVersionBeanInfo: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Name=").append(getName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Variables=[");
        if (!_Variables.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _Variables) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(System.lineSeparator());
                _item_.buildString(sb, level + 4);
                sb.append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append(']').append(System.lineSeparator());
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
            String _x_ = getName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = _Variables;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BEAN);
                for (var _v_ : _x_) {
                    _v_.encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
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
            setName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = _Variables;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new Zeze.Builtin.HotDistribute.BVariable(), _t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Variables.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _Variables.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
        for (var _v_ : _Variables) {
            if (_v_.negativeCheck())
                return true;
        }
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
                case 1: _Name = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 2: _Variables.followerApply(vlog); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setName(rs.getString(_parents_name_ + "Name"));
        if (getName() == null)
            setName("");
        Zeze.Serialize.Helper.decodeJsonList(_Variables, Zeze.Builtin.HotDistribute.BVariable.class, rs.getString(_parents_name_ + "Variables"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "Name", getName());
        st.appendString(_parents_name_ + "Variables", Zeze.Serialize.Helper.encodeJson(_Variables));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Name", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Variables", "list", "", "Zeze.Builtin.HotDistribute.BVariable"));
        return vars;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -6575391224958548024L;

    private String _Name;
    private java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> _Variables;

    public String getName() {
        return _Name;
    }

    public void setName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Name = value;
    }

    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> getVariables() {
        return _Variables;
    }

    public void setVariables(java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Variables = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Name = "";
        _Variables = new java.util.ArrayList<>();
    }

    @SuppressWarnings("deprecation")
    public Data(String _Name_, java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> _Variables_) {
        if (_Name_ == null)
            _Name_ = "";
        _Name = _Name_;
        if (_Variables_ == null)
            _Variables_ = new java.util.ArrayList<>();
        _Variables = _Variables_;
    }

    @Override
    public void reset() {
        _Name = "";
        _Variables.clear();
    }

    @Override
    public Zeze.Builtin.HotDistribute.BLastVersionBeanInfo toBean() {
        var bean = new Zeze.Builtin.HotDistribute.BLastVersionBeanInfo();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BLastVersionBeanInfo)other);
    }

    public void assign(BLastVersionBeanInfo other) {
        _Name = other.getName();
        _Variables.clear();
        for (var e : other._Variables) {
            Zeze.Builtin.HotDistribute.BVariable.Data data = new Zeze.Builtin.HotDistribute.BVariable.Data();
            data.assign(e);
            _Variables.add(data);
        }
    }

    public void assign(BLastVersionBeanInfo.Data other) {
        _Name = other._Name;
        _Variables.clear();
        for (var e : other._Variables)
            _Variables.add(e.copy());
    }

    @Override
    public BLastVersionBeanInfo.Data copy() {
        var copy = new BLastVersionBeanInfo.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BLastVersionBeanInfo.Data a, BLastVersionBeanInfo.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BLastVersionBeanInfo.Data clone() {
        return (BLastVersionBeanInfo.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.HotDistribute.BLastVersionBeanInfo: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Name=").append(_Name).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Variables=[");
        if (!_Variables.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _Variables) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(System.lineSeparator());
                _item_.buildString(sb, level + 4);
                sb.append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append(']').append(System.lineSeparator());
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
            String _x_ = _Name;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = _Variables;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BEAN);
                for (int _j_ = 0, _c_ = _x_.size(); _j_ < _c_; _j_++) {
                    var _v_ = _x_.get(_j_);
                    _v_.encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _Name = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = _Variables;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new Zeze.Builtin.HotDistribute.BVariable.Data(), _t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
