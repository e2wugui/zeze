// auto-generated @formatter:off
package Zeze.Builtin.World;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BEditData extends Zeze.Transaction.Bean implements BEditDataReadOnly {
    public static final long TYPEID = 590891812398888016L;

    private final Zeze.Transaction.Collections.CollOne<Zeze.Builtin.World.BCubeIndex> _CubeIndex;
    private Zeze.Builtin.World.BObjectId _ObjectId;
    private int _EditId;
    private Zeze.Net.Binary _Data;

    public Zeze.Builtin.World.BCubeIndex getCubeIndex() {
        return _CubeIndex.getValue();
    }

    public void setCubeIndex(Zeze.Builtin.World.BCubeIndex value) {
        _CubeIndex.setValue(value);
    }

    @Override
    public Zeze.Builtin.World.BCubeIndexReadOnly getCubeIndexReadOnly() {
        return _CubeIndex.getValue();
    }

    @Override
    public Zeze.Builtin.World.BObjectId getObjectId() {
        if (!isManaged())
            return _ObjectId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ObjectId;
        var log = (Log__ObjectId)txn.getLog(objectId() + 2);
        return log != null ? log.value : _ObjectId;
    }

    public void setObjectId(Zeze.Builtin.World.BObjectId value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _ObjectId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ObjectId(this, 2, value));
    }

    @Override
    public int getEditId() {
        if (!isManaged())
            return _EditId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _EditId;
        var log = (Log__EditId)txn.getLog(objectId() + 3);
        return log != null ? log.value : _EditId;
    }

    public void setEditId(int value) {
        if (!isManaged()) {
            _EditId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__EditId(this, 3, value));
    }

    @Override
    public Zeze.Net.Binary getData() {
        if (!isManaged())
            return _Data;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Data;
        var log = (Log__Data)txn.getLog(objectId() + 4);
        return log != null ? log.value : _Data;
    }

    public void setData(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Data = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Data(this, 4, value));
    }

    @SuppressWarnings("deprecation")
    public BEditData() {
        _CubeIndex = new Zeze.Transaction.Collections.CollOne<>(new Zeze.Builtin.World.BCubeIndex(), Zeze.Builtin.World.BCubeIndex.class);
        _CubeIndex.variableId(1);
        _ObjectId = new Zeze.Builtin.World.BObjectId();
        _Data = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BEditData(Zeze.Builtin.World.BObjectId _ObjectId_, int _EditId_, Zeze.Net.Binary _Data_) {
        _CubeIndex = new Zeze.Transaction.Collections.CollOne<>(new Zeze.Builtin.World.BCubeIndex(), Zeze.Builtin.World.BCubeIndex.class);
        _CubeIndex.variableId(1);
        if (_ObjectId_ == null)
            _ObjectId_ = new Zeze.Builtin.World.BObjectId();
        _ObjectId = _ObjectId_;
        _EditId = _EditId_;
        if (_Data_ == null)
            _Data_ = Zeze.Net.Binary.Empty;
        _Data = _Data_;
    }

    @Override
    public void reset() {
        _CubeIndex.reset();
        setObjectId(new Zeze.Builtin.World.BObjectId());
        setEditId(0);
        setData(Zeze.Net.Binary.Empty);
    }

    @Override
    public Zeze.Builtin.World.BEditData.Data toData() {
        var data = new Zeze.Builtin.World.BEditData.Data();
        data.assign(this);
        return data;
    }

    @Override
    public void assign(Zeze.Transaction.Data other) {
        assign((Zeze.Builtin.World.BEditData.Data)other);
    }

    public void assign(BEditData.Data other) {
        Zeze.Builtin.World.BCubeIndex data_CubeIndex = new Zeze.Builtin.World.BCubeIndex();
        data_CubeIndex.assign(other._CubeIndex);
        _CubeIndex.setValue(data_CubeIndex);
        setObjectId(other._ObjectId);
        setEditId(other._EditId);
        setData(other._Data);
    }

    public void assign(BEditData other) {
        _CubeIndex.assign(other._CubeIndex);
        setObjectId(other.getObjectId());
        setEditId(other.getEditId());
        setData(other.getData());
    }

    public BEditData copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BEditData copy() {
        var copy = new BEditData();
        copy.assign(this);
        return copy;
    }

    public static void swap(BEditData a, BEditData b) {
        BEditData save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ObjectId extends Zeze.Transaction.Logs.LogBeanKey<Zeze.Builtin.World.BObjectId> {
        public Log__ObjectId(BEditData bean, int varId, Zeze.Builtin.World.BObjectId value) { super(Zeze.Builtin.World.BObjectId.class, bean, varId, value); }

        @Override
        public void commit() { ((BEditData)getBelong())._ObjectId = value; }
    }

    private static final class Log__EditId extends Zeze.Transaction.Logs.LogInt {
        public Log__EditId(BEditData bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BEditData)getBelong())._EditId = value; }
    }

    private static final class Log__Data extends Zeze.Transaction.Logs.LogBinary {
        public Log__Data(BEditData bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BEditData)getBelong())._Data = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.World.BEditData: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("CubeIndex=").append(System.lineSeparator());
        _CubeIndex.buildString(sb, level + 4);
        sb.append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ObjectId=").append(System.lineSeparator());
        getObjectId().buildString(sb, level + 4);
        sb.append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("EditId=").append(getEditId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Data=").append(getData()).append(System.lineSeparator());
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
            _CubeIndex.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 2, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            getObjectId().encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        {
            int _x_ = getEditId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = getData();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _o_.ReadBean(_CubeIndex, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _o_.ReadBean(getObjectId(), _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setEditId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setData(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _CubeIndex.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _CubeIndex.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
        if (_CubeIndex.negativeCheck())
            return true;
        if (getObjectId().negativeCheck())
            return true;
        if (getEditId() < 0)
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
                case 1: _CubeIndex.followerApply(vlog); break;
                case 2: _ObjectId = ((Zeze.Transaction.Logs.LogBeanKey<Zeze.Builtin.World.BObjectId>)vlog).value; break;
                case 3: _EditId = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 4: _Data = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        parents.add("CubeIndex");
        _CubeIndex.decodeResultSet(parents, rs);
        parents.remove(parents.size() - 1);
        parents.add("ObjectId");
        getObjectId().decodeResultSet(parents, rs);
        parents.remove(parents.size() - 1);
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setEditId(rs.getInt(_parents_name_ + "EditId"));
        setData(new Zeze.Net.Binary(rs.getBytes(_parents_name_ + "Data")));
        if (getData() == null)
            setData(Zeze.Net.Binary.Empty);
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        parents.add("CubeIndex");
        _CubeIndex.encodeSQLStatement(parents, st);
        parents.remove(parents.size() - 1);
        parents.add("ObjectId");
        getObjectId().encodeSQLStatement(parents, st);
        parents.remove(parents.size() - 1);
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendInt(_parents_name_ + "EditId", getEditId());
        st.appendBinary(_parents_name_ + "Data", getData());
    }

public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 590891812398888016L;

    private Zeze.Builtin.World.BCubeIndex.Data _CubeIndex;
    private Zeze.Builtin.World.BObjectId _ObjectId;
    private int _EditId;
    private Zeze.Net.Binary _Data;

    public Zeze.Builtin.World.BCubeIndex.Data getCubeIndex() {
        return _CubeIndex;
    }

    public void setCubeIndex(Zeze.Builtin.World.BCubeIndex.Data value) {
        if (value == null)
            throw new IllegalArgumentException();
        _CubeIndex = value;
    }

    public Zeze.Builtin.World.BObjectId getObjectId() {
        return _ObjectId;
    }

    public void setObjectId(Zeze.Builtin.World.BObjectId value) {
        if (value == null)
            throw new IllegalArgumentException();
        _ObjectId = value;
    }

    public int getEditId() {
        return _EditId;
    }

    public void setEditId(int value) {
        _EditId = value;
    }

    public Zeze.Net.Binary getData() {
        return _Data;
    }

    public void setData(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        _Data = value;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _CubeIndex = new Zeze.Builtin.World.BCubeIndex.Data();
        _ObjectId = new Zeze.Builtin.World.BObjectId();
        _Data = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public Data(Zeze.Builtin.World.BCubeIndex.Data _CubeIndex_, Zeze.Builtin.World.BObjectId _ObjectId_, int _EditId_, Zeze.Net.Binary _Data_) {
        if (_CubeIndex_ == null)
            _CubeIndex_ = new Zeze.Builtin.World.BCubeIndex.Data();
        _CubeIndex = _CubeIndex_;
        if (_ObjectId_ == null)
            _ObjectId_ = new Zeze.Builtin.World.BObjectId();
        _ObjectId = _ObjectId_;
        _EditId = _EditId_;
        if (_Data_ == null)
            _Data_ = Zeze.Net.Binary.Empty;
        _Data = _Data_;
    }

    @Override
    public void reset() {
        _CubeIndex.reset();
        _ObjectId = new Zeze.Builtin.World.BObjectId();
        _EditId = 0;
        _Data = Zeze.Net.Binary.Empty;
    }

    @Override
    public Zeze.Builtin.World.BEditData toBean() {
        var bean = new Zeze.Builtin.World.BEditData();
        bean.assign(this);
        return bean;
    }

    @Override
    public void assign(Zeze.Transaction.Bean other) {
        assign((BEditData)other);
    }

    public void assign(BEditData other) {
        _CubeIndex.assign(other._CubeIndex.getValue());
        _ObjectId = other.getObjectId();
        _EditId = other.getEditId();
        _Data = other.getData();
    }

    public void assign(BEditData.Data other) {
        _CubeIndex.assign(other._CubeIndex);
        _ObjectId = other._ObjectId;
        _EditId = other._EditId;
        _Data = other._Data;
    }

    @Override
    public BEditData.Data copy() {
        var copy = new BEditData.Data();
        copy.assign(this);
        return copy;
    }

    public static void swap(BEditData.Data a, BEditData.Data b) {
        var save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BEditData.Data clone() {
        return (BEditData.Data)super.clone();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.World.BEditData: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("CubeIndex=").append(System.lineSeparator());
        _CubeIndex.buildString(sb, level + 4);
        sb.append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ObjectId=").append(System.lineSeparator());
        _ObjectId.buildString(sb, level + 4);
        sb.append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("EditId=").append(_EditId).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Data=").append(_Data).append(System.lineSeparator());
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
            _CubeIndex.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 2, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _ObjectId.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        {
            int _x_ = _EditId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = _Data;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _o_.ReadBean(_CubeIndex, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _o_.ReadBean(_ObjectId, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _EditId = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _Data = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
