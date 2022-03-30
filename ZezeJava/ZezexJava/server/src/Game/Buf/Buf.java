package Game.Buf;

import Game.Buf.*;
import Game.*;

public abstract class Buf {
	private BBuf bean;

	public final int getId() {
		return bean.getId();
	}
	public final long getAttachTime() {
		return bean.getAttachTime();
	}
	public final long getContinueTime() {
		return bean.getContinueTime();
	}

	public final BBuf getBean() {
		return bean;
	}

	public Buf(BBuf bean) {
		this.bean = bean;
	}

	public abstract void CalculateFighter(Game.Fight.Fighter fighter);

	// 可以重载用来实现加入时的特殊操作。
	public void Attach(Bufs bufs) {
		bufs.Attach(this);
	}
}