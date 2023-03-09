package Game.Buf;

import Game.*;

public class Bufs {
	private final long RoleId;
	public final long getRoleId() {
		return RoleId;
	}
	private final BBufs bean;

	public Bufs(long roleId, BBufs bean) {
		this.RoleId = roleId;
		this.bean = bean;
	}

	public final Buf GetBuf(int id) {
		var bBuf = bean.getBufs().get(id);
		if (null != bBuf) {
			var extraTypeId = bBuf.getExtra().getTypeId();
			if (extraTypeId == BBufExtra.TYPEID)
				return new BufExtra(bBuf, (BBufExtra)bBuf.getExtra().getBean());
			throw new RuntimeException("unknown extra");
		}
		return null;
	}

	public final void Detach(int id) {
		if (bean.getBufs().remove(id) != null) {
			// 因为没有取消Scheduler，所以可能发生删除不存在的buf。
			App.Instance.Game_Fight.StartCalculateFighter(getRoleId());
		}
	}

	public final void Attach(int id) {
		// config: create Buf by id.
		BBuf buf = new BBuf();
		buf.setId(id);
		buf.setAttachTime(System.currentTimeMillis());
		buf.setContinueTime(3600 * 1000); // 1 hour
		buf.setExtra(new BBufExtra());

		(new BufExtra(buf, buf.getExtra_Game_Buf_BBufExtra())).Attach(this);
	}

	public final void Attach(Buf buf) {
		// config: conflict 等
		bean.getBufs().put(buf.getId(), buf.getBean());

		Zeze.Util.Task.schedule(buf.getContinueTime(), () -> Detach(buf.getId()));
		App.Instance.Game_Fight.StartCalculateFighter(getRoleId());
	}

	public final void CalculateFighter(Game.Fight.Fighter fighter) {
		for (var bufid : bean.getBufs().keySet()) {
			GetBuf(bufid).CalculateFighter(fighter);
		}
	}
}
