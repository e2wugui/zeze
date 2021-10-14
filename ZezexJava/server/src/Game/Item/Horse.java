package Game.Item;

import Game.*;

public class Horse extends Item {
	private BHorseExtra extra;

	public Horse(Game.Bag.BItem bItem, BHorseExtra extra) {
		super(bItem);
		this.extra = extra;
	}

	public final int getSpeed() {
		return extra.getSpeed();
	}

	@Override
	public boolean Use() {
		return false;
	}
}