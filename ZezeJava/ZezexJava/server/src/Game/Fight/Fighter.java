package Game.Fight;

public class Fighter {
	private final BFighterId Id;
	public final BFighterId getId() {
		return Id;
	}
	private final BFighter Bean;
	public final BFighter getBean() {
		return Bean;
	}

	public Fighter(BFighterId id, BFighter bean) {
		this.Id = id;
		this.Bean = bean;
	}

}
