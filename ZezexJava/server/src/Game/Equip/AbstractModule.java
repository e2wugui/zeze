package Game.Equip;

import Game.*;

// auto-generated


public abstract class AbstractModule implements Zeze.IModule {
	@Override
	public String getFullName() {
		return "Game.Equip";
	}
	@Override
	public String getName() {
		return "Equip";
	}
	@Override
	public int getId() {
		return 7;
	}

	public static final int ResultCodeCannotEquip = 1;
	public static final int ResultCodeItemNotFound = 2;
	public static final int ResultCodeBagIsFull = 3;
	public static final int ResultCodeEquipNotFound = 4;

	public abstract int ProcessEquipementRequest(Equipement rpc);

	public abstract int ProcessUnequipementRequest(Unequipement rpc);

}