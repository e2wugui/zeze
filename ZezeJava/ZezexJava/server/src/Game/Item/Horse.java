package Game.Item;

public class Horse extends Item {
	private final BHorseExtra extra;

	public Horse(Game.Equip.BItem bItem, BHorseExtra extra) {
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
