package Game.Buf;

public abstract class Buf implements IBuf {
	private final BBuf bean;

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

	// 可以重载用来实现加入时的特殊操作。
	public void Attach(Bufs bufs) {
		bufs.attach(this);
	}
}
