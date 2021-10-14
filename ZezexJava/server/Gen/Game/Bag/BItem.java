// auto-generated
package Game.Bag;

import Zeze.Serialize.*;

public final class BItem extends Zeze.Transaction.Bean implements BItemReadOnly {
    private int _Id;
    private int _Number;
    private Zeze.Transaction.DynamicBean _Extra;

    public int getId(){
        if (false == this.isManaged())
            return _Id;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _Id;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__Id)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.getValue() : _Id;
    }

    public void setId(int value){
        if (false == this.isManaged()) {
            _Id = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__Id(this, value));
    }

    public int getNumber(){
        if (false == this.isManaged())
            return _Number;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null) return _Number;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__Number)txn.GetLog(this.getObjectId() + 2);
        return log != null ? log.getValue() : _Number;
    }

    public void setNumber(int value){
        if (false == this.isManaged()) {
            _Number = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__Number(this, value));
    }

    public Zeze.Transaction.DynamicBean getExtra() {
        return _Extra;
    }

    public Game.Item.BHorseExtra getExtra_Game_Item_BHorseExtra(){
        return (Game.Item.BHorseExtra)getExtra().getBean();
    }
    public void setExtra(Game.Item.BHorseExtra value) {
        getExtra().setBean(value);
    }

    public Game.Item.BFoodExtra getExtra_Game_Item_BFoodExtra(){
        return (Game.Item.BFoodExtra)getExtra().getBean();
    }
    public void setExtra(Game.Item.BFoodExtra value) {
        getExtra().setBean(value);
    }

    public Game.Equip.BEquipExtra getExtra_Game_Equip_BEquipExtra(){
        return (Game.Equip.BEquipExtra)getExtra().getBean();
    }
    public void setExtra(Game.Equip.BEquipExtra value) {
        getExtra().setBean(value);
    }


    public BItem() {
         this(0);
    }

    public BItem(int _varId_) {
        super(_varId_);
        _Extra = new Zeze.Transaction.DynamicBean(3, (_b_) -> GetSpecialTypeIdFromBean_Extra(_b_), (_i_) -> CreateBeanFromSpecialTypeId_Extra(_i_));
    }

    public void Assign(BItem other) {
        setId(other.getId());
        setNumber(other.getNumber());
        getExtra().Assign(other.getExtra());
    }

    public BItem CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BItem Copy() {
        var copy = new BItem();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BItem a, BItem b) {
        BItem save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public final static long TYPEID = -5504101817093603404L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private final static class Log__Id extends Zeze.Transaction.Log1<BItem, Integer> {
        public Log__Id(BItem self, Integer value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 1; }
        @Override
        public void Commit() { this.getBeanTyped()._Id = this.getValue(); }
    }

    private final static class Log__Number extends Zeze.Transaction.Log1<BItem, Integer> {
        public Log__Number(BItem self, Integer value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 2; }
        @Override
        public void Commit() { this.getBeanTyped()._Number = this.getValue(); }
    }

    public static long GetSpecialTypeIdFromBean_Extra(Zeze.Transaction.Bean bean) {
        var _typeId_ = bean.getTypeId();
        if (_typeId_ == Zeze.Transaction.EmptyBean.TYPEID)
            return Zeze.Transaction.EmptyBean.TYPEID;
        if (_typeId_ == -6414823809446200925L)
            return -6414823809446200925; // Game.Item.BHorseExtra
        if (_typeId_ == -5635260117858385112L)
            return -5635260117858385112; // Game.Item.BFoodExtra
        if (_typeId_ == 1076067654005167423L)
            return 1076067654005167423; // Game.Equip.BEquipExtra
        throw new RuntimeException("Unknown Bean! dynamic@Game.Bag.BItem:Extra");
    }

    public static Zeze.Transaction.Bean CreateBeanFromSpecialTypeId_Extra(long typeId) {
        if (typeId == -6414823809446200925L)
            return new Game.Item.BHorseExtra();
        if (typeId == -5635260117858385112L)
            return new Game.Item.BFoodExtra();
        if (typeId == 1076067654005167423L)
            return new Game.Equip.BEquipExtra();
        return null;
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
        sb.append(" ".repeat(level * 4)).append("Game.Bag.BItem: {").append(System.lineSeparator());
        level++;
        sb.append(" ".repeat(level * 4)).append("Id").append("=").append(getId()).append(",").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("Number").append("=").append(getNumber()).append(",").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("Extra").append("=").append(System.lineSeparator());
        getExtra().getBean().BuildString(sb, level + 1);
        sb.append("").append(System.lineSeparator());
        sb.append("}");
    }

    @Override
    public void Encode(ByteBuffer _os_) {
        _os_.WriteInt(3); // Variables.Count
        _os_.WriteInt(ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT);
        _os_.WriteInt(getId());
        _os_.WriteInt(ByteBuffer.INT | 2 << ByteBuffer.TAG_SHIFT);
        _os_.WriteInt(getNumber());
        _os_.WriteInt(ByteBuffer.DYNAMIC | 3 << ByteBuffer.TAG_SHIFT);
        getExtra().Encode(_os_);
    }

    @Override
    public void Decode(ByteBuffer _os_) {
        for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
            int _tagid_ = _os_.ReadInt();
            switch (_tagid_) {
                case (ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT): 
                    setId(_os_.ReadInt());
                    break;
                case (ByteBuffer.INT | 2 << ByteBuffer.TAG_SHIFT): 
                    setNumber(_os_.ReadInt());
                    break;
                case (ByteBuffer.DYNAMIC | 3 << ByteBuffer.TAG_SHIFT): 
                    getExtra().Decode(_os_);
                    break;
                default:
                    ByteBuffer.SkipUnknownField(_tagid_, _os_);
                    break;
            }
        }
    }

    @Override
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _Extra.InitRootInfo(root, this);
    }

    @Override
    public boolean NegativeCheck() {
        if (getId() < 0) return true;
        if (getNumber() < 0) return true;
        if (getExtra().NegativeCheck()) return true;
        return false;
    }

}
