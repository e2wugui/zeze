// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BEndMove extends Zeze.Transaction.Bean implements BEndMoveReadOnly {
    public static final long TYPEID = 1744858924397766646L;

    private final Zeze.Transaction.Collections.CollOne<Zeze.Builtin.Dbh2.BBucketMeta> _To;

    public Zeze.Builtin.Dbh2.BBucketMeta getTo() {
        return _To.getValue();
    }

    public void setTo(Zeze.Builtin.Dbh2.BBucketMeta value) {
        _To.setValue(value);
    }

    @Override
    public Zeze.Builtin.Dbh2.BBucketMetaReadOnly getToReadOnly() {
        return _To.getValue();
    }

    @SuppressWarnings("deprecation")
    public BEndMove() {
        _To = new Zeze.Transaction.Collections.CollOne<>(new Zeze.Builtin.Dbh2.BBucketMeta(), Zeze.Builtin.Dbh2.BBucketMeta.class);
        _To.variableId(1);
    }

    @Override
    public void reset() {
        _To.reset();
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Dbh2.Master.BEndMove.Data toData() {
        var data = new Zeze.Builtin.Dbh2.Master.BEndMove.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.Dbh2.Master.BEndMove.Data)other);
    }

    public void assign(BEndMove.Data other) {
        Zeze.Builtin.Dbh2.BBucketMeta data_To = new Zeze.Builtin.Dbh2.BBucketMeta();
        data_To.assign(other._To);
        _To.setValue(data_To);
        _unknown_ = null;
    }

    public void assign(BEndMove other) {
        _To.assign(other._To);
        _unknown_ = other._unknown_;
    }

    public BEndMove copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BEndMove copy() {
        var copy = new BEndMove();
        copy.assign(this);
        return copy;
    }

    public static void swap(BEndMove a, BEndMove b) {
        BEndMove save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.Master.BEndMove: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("To=").append(System.lineSeparator());
        _To.buildString(sb, level + 4);
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
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 1, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _To.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        _o_.writeAllUnknownFields(_i_, _ui_, _u_);
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        ByteBuffer _u_ = null;
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _o_.ReadBean(_To, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _To.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _To.initRootInfoWithRedo(root, this);
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
                case 1: _To.followerApply(vlog); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        parents.add("To");
        _To.decodeResultSet(parents, rs);
        parents.remove(parents.size() - 1);
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        parents.add("To");
        _To.encodeSQLStatement(parents, st);
        parents.remove(parents.size() - 1);
    }

    @Override
    public java.util.List<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "To", "Zeze.Builtin.Dbh2.BBucketMeta", "", ""));
        return vars;
    }

    @Override
    public Zeze.Transaction.Bean toPrevious() {
        return null; // todo BEndMove
    }

public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 1744858924397766646L;

    private Zeze.Builtin.Dbh2.BBucketMeta.Data _To;

    public Zeze.Builtin.Dbh2.BBucketMeta.Data getTo() {
        return _To;
    }

    public void setTo(Zeze.Builtin.Dbh2.BBucketMeta.Data value) {
        if (value == null)
            throw new IllegalArgumentException();
        _To = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _To = new Zeze.Builtin.Dbh2.BBucketMeta.Data();
    }

    @SuppressWarnings("deprecation")
    public Data(Zeze.Builtin.Dbh2.BBucketMeta.Data _To_) {
        if (_To_ == null)
            _To_ = new Zeze.Builtin.Dbh2.BBucketMeta.Data();
        _To = _To_;
    }

    @Override
    public void reset() {
        _To.reset();
    }

    @Override
    public Zeze.Builtin.Dbh2.Master.BEndMove toBean() {
        var bean = new Zeze.Builtin.Dbh2.Master.BEndMove();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BEndMove)other);
    }

    public void assign(BEndMove other) {
        _To.assign(other._To.getValue());
    }

    public void assign(BEndMove.Data other) {
        _To.assign(other._To);
    }

    @Override
    public BEndMove.Data copy() {
        var copy = new BEndMove.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BEndMove.Data a, BEndMove.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BEndMove.Data clone() {
        return (BEndMove.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Dbh2.Master.BEndMove: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("To=").append(System.lineSeparator());
        _To.buildString(sb, level + 4);
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
            int _j_ = _o_.WriteTag(_i_, 1, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _To.encode(_o_);
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
        if (_i_ == 1) {
            _o_.ReadBean(_To, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
