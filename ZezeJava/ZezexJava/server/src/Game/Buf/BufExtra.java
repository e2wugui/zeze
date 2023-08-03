package Game.Buf;

public class BufExtra extends Buf {
	public final BBufExtra extra;

	public BufExtra(BBuf bean, BBufExtra extra) {
		super(bean);
		this.extra = extra;
	}

	@Override
	public void calculateFighter(Game.Fight.IFighter fighter) {
		fighter.setAttack(fighter.getAttack() + 10.0f);
		fighter.setDefence(fighter.getDefence() + 10.0f);
	}
}
