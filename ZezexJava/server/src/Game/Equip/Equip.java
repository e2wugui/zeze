package Game.Equip;

import Game.*;

public class Equip extends Game.Item.Item {

	public Equip(Game.Bag.BItem bItem, BEquipExtra extra) {
		super(bItem);

	}

	@Override
	public void CalculateFighter(Game.Fight.Fighter fighter) {
		fighter.getBean().Attack += 20.0f;
		fighter.getBean().Defence += 20.0f;
	}

	@Override
	public boolean Use() {
		return false;
	}
}