package Game.Item;

public class Food extends Item {
	private final BFoodExtra extra;

	public Food(Game.Bag.BItem bItem, BFoodExtra extra) {
		super(bItem);
		this.extra = extra;
	}

	public final int getAccount() {
		return extra.getAmmount();
	}

	@Override
	public boolean Use() {
		throw new UnsupportedOperationException();
	}
}
