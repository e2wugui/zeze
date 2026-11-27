package Game.Item;

import Game.Equip.BItem;
import Game.Fight.IFighter;

public class Food implements IFood {
	private final BItem item;
	private final BFoodExtra extra;

	public Food(BItem item, BFoodExtra extra) {
		this.item = item;
		this.extra = extra;
	}

	@Override
	public final int getAmount() {
		return extra.getAmmount();
	}

	@Override
	public int getId() {
		return item.getId();
	}

	@Override
	public boolean use() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String formatTip() {
		return "food" + getId();
	}

	@Override
	public void calculateFighter(IFighter fighter) {

	}
}
