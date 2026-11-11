// auto-generated rocks @formatter:off
package rocksdynamic.Fixture;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"ConstantConditions", "ConstantValue", "UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "SuspiciousNameCombination"})
public final class Defaults extends Zeze.Raft.RocksRaft.Bean {
    public static final long TYPEID = -411829569423683963L;

    private int _Count;
    private boolean _Enabled;
    private String _Label;
    private Zeze.Serialize.Vector2 _Position;
    private final rocksdynamic.Fixture.Quantity _Override;

    public int getCount() {
        if (!isManaged())
            return _Count;
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        if (txn == null)
            return _Count;
        var log = txn.getLog(objectId() + 1);
        if (log == null)
            return _Count;
        return ((Zeze.Raft.RocksRaft.Log1.LogInt)log).value;
    }

    public void setCount(int value) {
        if (!isManaged()) {
            _Count = value;
            return;
        }
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        txn.putLog(new Zeze.Raft.RocksRaft.Log1.LogInt(this, 1, value));
    }

    public boolean isEnabled() {
        if (!isManaged())
            return _Enabled;
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        if (txn == null)
            return _Enabled;
        var log = txn.getLog(objectId() + 2);
        if (log == null)
            return _Enabled;
        return ((Zeze.Raft.RocksRaft.Log1.LogBool)log).value;
    }

    public void setEnabled(boolean value) {
        if (!isManaged()) {
            _Enabled = value;
            return;
        }
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        txn.putLog(new Zeze.Raft.RocksRaft.Log1.LogBool(this, 2, value));
    }

    public String getLabel() {
        if (!isManaged())
            return _Label;
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        if (txn == null)
            return _Label;
        var log = txn.getLog(objectId() + 3);
        if (log == null)
            return _Label;
        return ((Zeze.Raft.RocksRaft.Log1.LogString)log).value;
    }

    public void setLabel(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Label = value;
            return;
        }
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        txn.putLog(new Zeze.Raft.RocksRaft.Log1.LogString(this, 3, value));
    }

    public Zeze.Serialize.Vector2 getPosition() {
        if (!isManaged())
            return _Position;
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        if (txn == null)
            return _Position;
        var log = txn.getLog(objectId() + 4);
        if (log == null)
            return _Position;
        return ((Zeze.Raft.RocksRaft.Log1.LogVector2)log).value;
    }

    public void setPosition(Zeze.Serialize.Vector2 value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Position = value;
            return;
        }
        var txn = Zeze.Raft.RocksRaft.Transaction.getCurrent();
        txn.putLog(new Zeze.Raft.RocksRaft.Log1.LogVector2(this, 4, value));
    }

    public rocksdynamic.Fixture.Quantity getOverride() {
        return _Override;
    }

    public Defaults() {
        _Count = 7;
        _Enabled = true;
        _Label = "schema";
        _Position = new Zeze.Serialize.Vector2(1, 2);
        _Override = new rocksdynamic.Fixture.Quantity(9);
        _Override.variableId(5);
    }

    public Defaults(int _Count_, boolean _Enabled_, String _Label_, Zeze.Serialize.Vector2 _Position_) {
        _Count = _Count_;
        _Enabled = _Enabled_;
        if (_Label_ == null)
            throw new IllegalArgumentException();
        _Label = _Label_;
        if (_Position_ == null)
            throw new IllegalArgumentException();
        _Position = _Position_;
        _Override = new rocksdynamic.Fixture.Quantity(9);
        _Override.variableId(5);
    }

    public void assign(Defaults other) {
        if (other == this)
            return;
        setCount(other.getCount());
        setEnabled(other.isEnabled());
        setLabel(other.getLabel());
        setPosition(other.getPosition());
        _Override.assign(other._Override);
    }

    public Defaults copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public Defaults copy() {
        var copy = new Defaults();
        copy.assign(this);
        return copy;
    }

    public static void swap(Defaults a, Defaults b) {
        Defaults save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        var _i1_ = Zeze.Util.Str.indent(_l_ + 4);
        _s_.append("rocksdynamic.Fixture.Defaults: {\n");
        _s_.append(_i1_).append("Count=").append(getCount()).append(",\n");
        _s_.append(_i1_).append("Enabled=").append(isEnabled()).append(",\n");
        _s_.append(_i1_).append("Label=").append(getLabel()).append(",\n");
        _s_.append(_i1_).append("Position=").append(getPosition()).append(",\n");
        _s_.append(_i1_).append("Override=");
        getOverride().buildString(_s_, _l_ + 8);
        _s_.append('\n');
        _s_.append(Zeze.Util.Str.indent(_l_)).append('}');
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
            int _x_ = getCount();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            boolean _x_ = isEnabled();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        {
            String _x_ = getLabel();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = getPosition();
            if (_x_ != null && !_x_.isZero()) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.VECTOR2);
                _o_.WriteVector2(_x_);
            }
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 5, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            getOverride().encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _Count = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        } else {
            _Count = 0;
        }
        if (_i_ == 2) {
            _Enabled = _o_.ReadBool(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        } else {
            _Enabled = false;
        }
        if (_i_ == 3) {
            _Label = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        } else {
            _Label = "";
        }
        if (_i_ == 4) {
            _Position = _o_.ReadVector2(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        } else {
            _Position = Zeze.Serialize.Vector2.ZERO;
        }
        if (_i_ == 5) {
            _o_.ReadBean(_Override, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        } else {
            _Override.restoreOmittedFields(new rocksdynamic.Fixture.Quantity());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    public void restoreOmittedFields(Defaults _defaults_) {
        setCount(_defaults_.getCount());
        setEnabled(_defaults_.isEnabled());
        setLabel(_defaults_.getLabel());
        setPosition(_defaults_.getPosition());
        _Override.restoreOmittedFields(_defaults_._Override);
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Raft.RocksRaft.Record.@Nullable RootInfo root) {
        _Override.initRootInfo(root, this);
    }

    @Override
    public void leaderApplyNoRecursive(Zeze.Raft.RocksRaft.Log vlog) {
        switch (vlog.getVariableId()) {
            case 1: _Count = ((Zeze.Raft.RocksRaft.Log1.LogInt)vlog).value; break;
            case 2: _Enabled = ((Zeze.Raft.RocksRaft.Log1.LogBool)vlog).value; break;
            case 3: _Label = ((Zeze.Raft.RocksRaft.Log1.LogString)vlog).value; break;
            case 4: _Position = ((Zeze.Raft.RocksRaft.Log1.LogVector2)vlog).value; break;
        }
    }

    @Override
    public void followerApply(Zeze.Raft.RocksRaft.Log log) {
        var vars = ((Zeze.Raft.RocksRaft.LogBean)log).getVariables();
        if (vars == null)
            return;
        for (var it = vars.iterator(); it.moveToNext(); ) {
            var vlog = it.value();
            switch (vlog.getVariableId()) {
                case 1: _Count = ((Zeze.Raft.RocksRaft.Log1.LogInt)vlog).value; break;
                case 2: _Enabled = ((Zeze.Raft.RocksRaft.Log1.LogBool)vlog).value; break;
                case 3: _Label = ((Zeze.Raft.RocksRaft.Log1.LogString)vlog).value; break;
                case 4: _Position = ((Zeze.Raft.RocksRaft.Log1.LogVector2)vlog).value; break;
                case 5: _Override.followerApply(vlog); break;
            }
        }
    }
}
