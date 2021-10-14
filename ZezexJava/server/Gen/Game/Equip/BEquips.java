// auto-generated
package Game.Equip;

import Zeze.Serialize.*;

public final class BEquips extends Zeze.Transaction.Bean implements BEquipsReadOnly {
    private Zeze.Transaction.Collections.PMap2<Integer, Game.Bag.BItem> _Items; // key is equip position

    public Zeze.Transaction.Collections.PMap2<Integer, Game.Bag.BItem> getItems() {
        return _Items;
    }


    public BEquips() {
         this(0);
    }

    public BEquips(int _varId_) {
        super(_varId_);
        _Items = new Zeze.Transaction.Collections.PMap2<Integer, Game.Bag.BItem>(getObjectId() + 1, (_v) -> new Log__Items(this, _v));
    }

    public void Assign(BEquips other) {
        getItems().clear();
        for (var e : other.getItems().entrySet()) {
            getItems().put(e.getKey(), e.getValue().Copy());
        }
    }

    public BEquips CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BEquips Copy() {
        var copy = new BEquips();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BEquips a, BEquips b) {
        BEquips save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public final static long TYPEID = 2444609496742764204L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private final class Log__Items extends Zeze.Transaction.Collections.PMap.LogV<Integer, Game.Bag.BItem> {
        public Log__Items(BEquips host, org.pcollections.PMap<Integer, Game.Bag.BItem> value) { super(host, value); }
        @Override
        public long getLogKey() { return getBean().getObjectId() + 1; }
        public BEquips getBeanTyped() { return (BEquips)getBean(); }
        @Override
        public void Commit() { Commit(getBeanTyped()._Items); }
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
        sb.append(" ".repeat(level * 4)).append("Game.Equip.BEquips: {").append(System.lineSeparator());
        level++;
        sb.append(" ".repeat(level * 4)).append("Items").append("=[").append(System.lineSeparator());
        level++;
        for (var _kv_ : getItems().entrySet()) {
            sb.append("(").append(System.lineSeparator());
            sb.append(" ".repeat(level * 4)).append("Key").append("=").append(_kv_.getKey()).append(",").append(System.lineSeparator());
            sb.append(" ".repeat(level * 4)).append("Value").append("=").append(System.lineSeparator());
            _kv_.getValue().BuildString(sb, level + 1);
            sb.append(",").append(System.lineSeparator());
            sb.append(")").append(System.lineSeparator());
        }
        level--;
        sb.append(" ".repeat(level * 4)).append("]").append(System.lineSeparator());
        sb.append("}");
    }

    @Override
    public void Encode(ByteBuffer _os_) {
        _os_.WriteInt(1); // Variables.Count
        _os_.WriteInt(ByteBuffer.MAP | 1 << ByteBuffer.TAG_SHIFT);
        {
            var _state_ = _os_.BeginWriteSegment();
            _os_.WriteInt(ByteBuffer.INT);
            _os_.WriteInt(ByteBuffer.BEAN);
            _os_.WriteInt(getItems().size());
            for  (var _e_ : getItems().entrySet())
            {
                _os_.WriteInt(_e_.getKey());
                _e_.getValue().Encode(_os_);
            }
            _os_.EndWriteSegment(_state_); 
        }
    }

    @Override
    public void Decode(ByteBuffer _os_) {
        for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
            int _tagid_ = _os_.ReadInt();
            switch (_tagid_) {
                case (ByteBuffer.MAP | 1 << ByteBuffer.TAG_SHIFT):
                    {
                        var _state_ = _os_.BeginReadSegment();
                        _os_.ReadInt(); // skip key typetag
                        _os_.ReadInt(); // skip value typetag
                        getItems().clear();
                        for (int size = _os_.ReadInt(); size > 0; --size) {
                            int _k_;
                            _k_ = _os_.ReadInt();
                            Game.Bag.BItem _v_ = new Game.Bag.BItem();
                            _v_.Decode(_os_);
                            getItems().put(_k_, _v_);
                        }
                        _os_.EndReadSegment(_state_);
                    }
                    break;
                default:
                    ByteBuffer.SkipUnknownField(_tagid_, _os_);
                    break;
            }
        }
    }

    @Override
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Items.InitRootInfo(root, this);
    }

    @Override
    public boolean NegativeCheck() {
        for (var _v_ : getItems().values())
        {
            if (_v_.NegativeCheck()) return true;
        }
        return false;
    }

}
