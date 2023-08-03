package Game.Equip;

public class Equip implements IEquip {
	private final BItem item;
	private final BEquipExtra equip;

	public Equip(Game.Equip.BItem bItem, BEquipExtra extra) {
		this.item = bItem;
		this.equip = extra;
	}

	@Override
	public void calculateFighter(Game.Fight.IFighter fighter) {
		fighter.setAttack(fighter.getAttack() + 20.0f);
		fighter.setDefence(fighter.getDefence() + 20.0f);
	}

	@Override
	public int getId() {
		return item.getId();
	}

	@Override
	public boolean use() {
		return false;
	}

	@Override
	public String formatTip() {
		return "equip" + getId();
	}
}
