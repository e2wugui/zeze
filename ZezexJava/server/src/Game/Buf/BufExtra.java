package Game.Buf;

import Game.Buf.*;
import Game.*;

public class BufExtra extends Buf {
	private BBufExtra extra;

	public BufExtra(BBuf bean, BBufExtra extra) {
		super(bean);
		this.extra = extra;
	}

	@Override
	public void CalculateFighter(Game.Fight.Fighter fighter) {
		fighter.getBean().Attack += 10.0f;
		fighter.getBean().Defence += 10.0f;
	}
}