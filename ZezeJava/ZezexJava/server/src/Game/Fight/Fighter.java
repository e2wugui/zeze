package Game.Fight;

public class Fighter implements IFighter {
	private final BFighterId id;
	private final BFighter bean;

	//@Override
	public BFighterId getId() {
		return id;
	}

	public BFighter getBean() {
		return bean;
	}

	public Fighter(BFighterId id, BFighter bean) {
		this.id = id;
		this.bean = bean;
	}

	@Override
	public float getAttack() {
		return bean.getAttack();
	}

	@Override
	public float getDefence() {
		return bean.getDefence();
	}

	@Override
	public void setAttack(float value) {
		bean.setAttack(value);
	}

	@Override
	public void setDefence(float value) {
		bean.setDefence(value);
	}
}
