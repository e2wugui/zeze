// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

import Zeze.Serialize.ByteBuffer;

// 内置条件类型：NPC对话
@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BTConditionNPCTalk extends Zeze.Transaction.Bean implements BTConditionNPCTalkReadOnly {
    public static final long TYPEID = -6781843567973700426L;

    private long _npcId;
    private final Zeze.Transaction.Collections.PMap1<String, Integer> _dialogOptions; // key为对话的Id（String类型），value为对话选项有几个。如果没有就不用加。
    private final Zeze.Transaction.Collections.PMap1<String, Integer> _dialogSelected; // key为对话的Id（String类型），value为选了第几个选项。如果还没选初始化为-1。

    @Override
    public long getNpcId() {
        if (!isManaged())
            return _npcId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _npcId;
        var log = (Log__npcId)txn.getLog(objectId() + 3);
        return log != null ? log.value : _npcId;
    }

    public void setNpcId(long value) {
        if (!isManaged()) {
            _npcId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__npcId(this, 3, value));
    }

    public Zeze.Transaction.Collections.PMap1<String, Integer> getDialogOptions() {
        return _dialogOptions;
    }

    @Override
    public Zeze.Transaction.Collections.PMap1ReadOnly<String, Integer> getDialogOptionsReadOnly() {
        return new Zeze.Transaction.Collections.PMap1ReadOnly<>(_dialogOptions);
    }

    public Zeze.Transaction.Collections.PMap1<String, Integer> getDialogSelected() {
        return _dialogSelected;
    }

    @Override
    public Zeze.Transaction.Collections.PMap1ReadOnly<String, Integer> getDialogSelectedReadOnly() {
        return new Zeze.Transaction.Collections.PMap1ReadOnly<>(_dialogSelected);
    }

    @SuppressWarnings("deprecation")
    public BTConditionNPCTalk() {
        _dialogOptions = new Zeze.Transaction.Collections.PMap1<>(String.class, Integer.class);
        _dialogOptions.variableId(4);
        _dialogSelected = new Zeze.Transaction.Collections.PMap1<>(String.class, Integer.class);
        _dialogSelected.variableId(5);
    }

    @SuppressWarnings("deprecation")
    public BTConditionNPCTalk(long _npcId_) {
        _npcId = _npcId_;
        _dialogOptions = new Zeze.Transaction.Collections.PMap1<>(String.class, Integer.class);
        _dialogOptions.variableId(4);
        _dialogSelected = new Zeze.Transaction.Collections.PMap1<>(String.class, Integer.class);
        _dialogSelected.variableId(5);
    }

    public void assign(BTConditionNPCTalk other) {
        setNpcId(other.getNpcId());
        _dialogOptions.clear();
        _dialogOptions.putAll(other.getDialogOptions());
        _dialogSelected.clear();
        _dialogSelected.putAll(other.getDialogSelected());
    }

    public BTConditionNPCTalk copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTConditionNPCTalk copy() {
        var copy = new BTConditionNPCTalk();
        copy.assign(this);
        return copy;
    }

    public static void swap(BTConditionNPCTalk a, BTConditionNPCTalk b) {
        BTConditionNPCTalk save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__npcId extends Zeze.Transaction.Logs.LogLong {
        public Log__npcId(BTConditionNPCTalk bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTConditionNPCTalk)getBelong())._npcId = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.TaskBase.BTConditionNPCTalk: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("npcId=").append(getNpcId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("dialogOptions={");
        if (!_dialogOptions.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _kv_ : _dialogOptions.entrySet()) {
                sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(_kv_.getKey()).append(',').append(System.lineSeparator());
                sb.append(Zeze.Util.Str.indent(level)).append("Value=").append(_kv_.getValue()).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append('}').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("dialogSelected={");
        if (!_dialogSelected.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _kv_ : _dialogSelected.entrySet()) {
                sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(_kv_.getKey()).append(',').append(System.lineSeparator());
                sb.append(Zeze.Util.Str.indent(level)).append("Value=").append(_kv_.getValue()).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append('}').append(System.lineSeparator());
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
            long _x_ = getNpcId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = _dialogOptions;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BYTES, ByteBuffer.INTEGER);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteString(_e_.getKey());
                    _o_.WriteLong(_e_.getValue());
                }
            }
        }
        {
            var _x_ = _dialogSelected;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BYTES, ByteBuffer.INTEGER);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteString(_e_.getKey());
                    _o_.WriteLong(_e_.getValue());
                }
            }
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
            setNpcId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            var _x_ = _dialogOptions;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadString(_s_);
                    var _v_ = _o_.ReadInt(_t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            var _x_ = _dialogSelected;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadString(_s_);
                    var _v_ = _o_.ReadInt(_t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _dialogOptions.initRootInfo(root, this);
        _dialogSelected.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _dialogOptions.initRootInfoWithRedo(root, this);
        _dialogSelected.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getNpcId() < 0)
            return true;
        for (var _v_ : _dialogOptions.values()) {
            if (_v_ < 0)
                return true;
        }
        for (var _v_ : _dialogSelected.values()) {
            if (_v_ < 0)
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
                case 3: _npcId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 4: _dialogOptions.followerApply(vlog); break;
                case 5: _dialogSelected.followerApply(vlog); break;
            }
        }
    }
}
