// auto-generated
package Game.Rank;

import Zeze.Serialize.*;

public final class BRankCounters extends Zeze.Transaction.Bean implements BRankCountersReadOnly {
    private Zeze.Transaction.Collections.PMap2<Game.Rank.BConcurrentKey, Game.Rank.BRankCounter> _Counters;

    public Zeze.Transaction.Collections.PMap2<Game.Rank.BConcurrentKey, Game.Rank.BRankCounter> getCounters() {
        return _Counters;
    }


    public BRankCounters() {
         this(0);
    }

    public BRankCounters(int _varId_) {
        super(_varId_);
        _Counters = new Zeze.Transaction.Collections.PMap2<Game.Rank.BConcurrentKey, Game.Rank.BRankCounter>(getObjectId() + 1, (_v) -> new Log__Counters(this, _v));
    }

    public void Assign(BRankCounters other) {
        getCounters().clear();
        for (var e : other.getCounters().entrySet()) {
            getCounters().put(e.getKey(), e.getValue().Copy());
        }
    }

    public BRankCounters CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BRankCounters Copy() {
        var copy = new BRankCounters();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BRankCounters a, BRankCounters b) {
        BRankCounters save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public final static long TYPEID = -7316366693928035206L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private final class Log__Counters extends Zeze.Transaction.Collections.PMap.LogV<Game.Rank.BConcurrentKey, Game.Rank.BRankCounter> {
        public Log__Counters(BRankCounters host, org.pcollections.PMap<Game.Rank.BConcurrentKey, Game.Rank.BRankCounter> value) { super(host, value); }
        @Override
        public long getLogKey() { return getBean().getObjectId() + 1; }
        public BRankCounters getBeanTyped() { return (BRankCounters)getBean(); }
        @Override
        public void Commit() { Commit(getBeanTyped()._Counters); }
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
        sb.append(" ".repeat(level * 4)).append("Game.Rank.BRankCounters: {").append(System.lineSeparator());
        level++;
        sb.append(" ".repeat(level * 4)).append("Counters").append("=[").append(System.lineSeparator());
        level++;
        for (var _kv_ : getCounters().entrySet()) {
            sb.append("(").append(System.lineSeparator());
            sb.append(" ".repeat(level * 4)).append("Key").append("=").append(System.lineSeparator());
            _kv_.getKey().BuildString(sb, level + 1);
            sb.append(",").append(System.lineSeparator());
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
            _os_.WriteInt(ByteBuffer.BEAN);
            _os_.WriteInt(ByteBuffer.BEAN);
            _os_.WriteInt(getCounters().size());
            for  (var _e_ : getCounters().entrySet())
            {
                _e_.getKey().Encode(_os_);
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
                        getCounters().clear();
                        for (int size = _os_.ReadInt(); size > 0; --size) {
                            Game.Rank.BConcurrentKey _k_ = new Game.Rank.BConcurrentKey();
                            _k_.Decode(_os_);
                            Game.Rank.BRankCounter _v_ = new Game.Rank.BRankCounter();
                            _v_.Decode(_os_);
                            getCounters().put(_k_, _v_);
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
        _Counters.InitRootInfo(root, this);
    }

    @Override
    public boolean NegativeCheck() {
        for (var _v_ : getCounters().values())
        {
            if (_v_.NegativeCheck()) return true;
        }
        return false;
    }

}
