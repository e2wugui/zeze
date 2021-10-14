package Game.Bag;

import Zeze.Serialize.*;
import Game.*;

public final class BItem extends Zeze.Transaction.Bean implements BItemReadOnly {
	private int _Id;
	private int _Number;
	private Zeze.Transaction.DynamicBean _Extra;

	public int getId() {
		if (false == this.isManaged()) {
			return _Id;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _Id;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__Id)txn.GetLog(this.getObjectId() + 1);
		return log != null ? log.getValue() : _Id;
	}
	public void setId(int value) {
		if (false == this.isManaged()) {
			_Id = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__Id(this, value));
	}

	public int getNumber() {
		if (false == this.isManaged()) {
			return _Number;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _Number;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__Number)txn.GetLog(this.getObjectId() + 2);
		return log != null ? log.getValue() : _Number;
	}
	public void setNumber(int value) {
		if (false == this.isManaged()) {
			_Number = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__Number(this, value));
	}

	public Zeze.Transaction.DynamicBean getExtra() {
		return _Extra;
	}
	private Zeze.Transaction.DynamicBeanReadOnly Game.Bag.BItemReadOnly.Extra -> getExtra();

	public Game.Item.BHorseExtra getExtraGameItemBHorseExtra() {
		return (Game.Item.BHorseExtra)getExtra().Bean;
	}
	public void setExtraGameItemBHorseExtra(Game.Item.BHorseExtra value) {
		getExtra().Bean = value;
	}

	private Game.Item.BHorseExtraReadOnly Game.Bag.BItemReadOnly.Extra_Game_Item_BHorseExtra -> getExtraGameItemBHorseExtra();

	public Game.Item.BFoodExtra getExtraGameItemBFoodExtra() {
		return (Game.Item.BFoodExtra)getExtra().Bean;
	}
	public void setExtraGameItemBFoodExtra(Game.Item.BFoodExtra value) {
		getExtra().Bean = value;
	}

	private Game.Item.BFoodExtraReadOnly Game.Bag.BItemReadOnly.Extra_Game_Item_BFoodExtra -> getExtraGameItemBFoodExtra();

	public Game.Equip.BEquipExtra getExtraGameEquipBEquipExtra() {
		return (Game.Equip.BEquipExtra)getExtra().Bean;
	}
	public void setExtraGameEquipBEquipExtra(Game.Equip.BEquipExtra value) {
		getExtra().Bean = value;
	}

	private Game.Equip.BEquipExtraReadOnly Game.Bag.BItemReadOnly.Extra_Game_Equip_BEquipExtra -> getExtraGameEquipBEquipExtra();


	public BItem() {
		this(0);
	}

	public BItem(int _varId_) {
		super(_varId_);
		_Extra = new Zeze.Transaction.DynamicBean(3, GetSpecialTypeIdFromBean_Extra, CreateBeanFromSpecialTypeId_Extra);
	}

	public void Assign(BItem other) {
		setId(other.getId());
		setNumber(other.getNumber());
		getExtra().Assign(other.getExtra());
	}

	public BItem CopyIfManaged() {
		return isManaged() ? Copy() :this;
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

	public static final long TYPEID = -5504101817093603404;
	@Override
	public long getTypeId() {
		return TYPEID;
	}

	private final static class Log__Id extends Zeze.Transaction.Log<BItem, Integer> {
		public Log__Id(BItem self, int value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 1;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._Id = this.getValue();
		}
	}

	private final static class Log__Number extends Zeze.Transaction.Log<BItem, Integer> {
		public Log__Number(BItem self, int value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 2;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._Number = this.getValue();
		}
	}

	public static long GetSpecialTypeIdFromBean_Extra(Zeze.Transaction.Bean bean) {
		switch (bean.TypeId) {
			case Zeze.Transaction.EmptyBean.TYPEID:
				return Zeze.Transaction.EmptyBean.TYPEID;
			case -6414823809446200925:
				return -6414823809446200925; // Game.Item.BHorseExtra
			case -5635260117858385112:
				return -5635260117858385112; // Game.Item.BFoodExtra
			case 1076067654005167423:
				return 1076067654005167423; // Game.Equip.BEquipExtra
		}
		throw new RuntimeException("Unknown Bean! dynamic@Game.Bag.BItem:Extra");
	}

	public static Zeze.Transaction.Bean CreateBeanFromSpecialTypeId_Extra(long typeId) {
		switch (typeId) {
			case -6414823809446200925:
				return new Game.Item.BHorseExtra();
			case -5635260117858385112:
				return new Game.Item.BFoodExtra();
			case 1076067654005167423:
				return new Game.Equip.BEquipExtra();
		}
		return null;
	}


	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		BuildString(sb, 0);
		sb.append(System.lineSeparator());
		return sb.toString();
	}

	@Override
	public void BuildString(StringBuilder sb, int level) {
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Game.Bag.BItem: {").Append(System.lineSeparator());
		level++;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Id").Append("=").Append(getId()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Number").Append("=").Append(getNumber()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Extra").Append("=").Append(System.lineSeparator());
		getExtra().Bean.BuildString(sb, level + 1);
		sb.append("").Append(System.lineSeparator());
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
				case ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT:
					setId(_os_.ReadInt());
					break;
				case ByteBuffer.INT | 2 << ByteBuffer.TAG_SHIFT:
					setNumber(_os_.ReadInt());
					break;
				case ByteBuffer.DYNAMIC | 3 << ByteBuffer.TAG_SHIFT:
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
		if (getId() < 0) {
			return true;
		}
		if (getNumber() < 0) {
			return true;
		}
		if (getExtra().NegativeCheck()) {
			return true;
		}
		return false;
	}

}