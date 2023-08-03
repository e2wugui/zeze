package Game.Item;

import Game.Equip.BItem;
import Game.Fight.IFighter;

public class Horse implements IHorse {
	private final BItem item;
	private final BHorseExtra extra;

	public Horse(BItem item, BHorseExtra extra) {
		this.item = item;
		this.extra = extra;
	}

	@Override
	public int getSpeed() {
		return extra.getSpeed();
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
		return "horse" + getId();
	}

	@Override
	public void calculateFighter(IFighter fighter) {

	}
}
