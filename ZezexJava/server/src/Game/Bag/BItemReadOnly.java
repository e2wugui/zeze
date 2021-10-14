package Game.Bag;

import Zeze.Serialize.*;
import Game.*;

// auto-generated



public interface BItemReadOnly {
	public long getTypeId();
	public void Encode(ByteBuffer _os_);
	public boolean NegativeCheck();
	public Zeze.Transaction.Bean CopyBean();

	public int getId();
	public int getNumber();
	public Zeze.Transaction.DynamicBeanReadOnly getExtra();

	public Game.Item.BHorseExtraReadOnly getExtraGameItemBHorseExtra();
	public Game.Item.BFoodExtraReadOnly getExtraGameItemBFoodExtra();
	public Game.Equip.BEquipExtraReadOnly getExtraGameEquipBEquipExtra();
}