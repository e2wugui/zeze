package Game.Fight;

import Game.*;

public class Fighter {
	private BFighterId Id;
	public final BFighterId getId() {
		return Id;
	}
	private BFighter Bean;
	public final BFighter getBean() {
		return Bean;
	}

	public Fighter(BFighterId id, BFighter bean) {
		this.Id = id;
		this.Bean = bean;
	}

}