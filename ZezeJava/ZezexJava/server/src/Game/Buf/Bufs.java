package Game.Buf;

import Game.*;
import Game.Fight.IModuleFight;

public class Bufs implements IBufs {
	private final long RoleId;
	public final long getRoleId() {
		return RoleId;
	}
	private final BBufs bean;

	public Bufs(long roleId, BBufs bean) {
		this.RoleId = roleId;
		this.bean = bean;
	}

	@Override
	public IBuf getBuf(int id) {
		var bBuf = bean.getBufs().get(id);
		if (null != bBuf) {
			var extraTypeId = bBuf.getExtra().getTypeId();
			if (extraTypeId == BBufExtra.TYPEID)
				return new BufExtra(bBuf, (BBufExtra)bBuf.getExtra().getBean());
			throw new RuntimeException("unknown extra");
		}
		return null;
	}

	public final void detach(int id) {
		if (bean.getBufs().remove(id) != null) {
			// 因为没有取消Scheduler，所以可能发生删除不存在的buf。

			// context 可以缓存，demo就不考虑了。
			var context = App.Instance.Zeze.getHotManager().getModuleContext("Game.Fight", IModuleFight.class);
			var fight = context.getService();
			fight.StartCalculateFighter(getRoleId());
		}
	}

	public final void attach(int id) {
		// config: create Buf by id.
		BBuf buf = new BBuf();
		buf.setId(id);
		buf.setAttachTime(System.currentTimeMillis());
		buf.setContinueTime(3600 * 1000); // 1 hour
		buf.setExtra(new BBufExtra());

		(new BufExtra(buf, buf.getExtra_Game_Buf_BBufExtra())).Attach(this);
	}

	public final void attach(Buf buf) {
		// config: conflict 等
		bean.getBufs().put(buf.getId(), buf.getBean());

		Zeze.Util.Task.schedule(buf.getContinueTime(), () -> detach(buf.getId()));
		// context 可以缓存，demo就不考虑了。
		var context = App.Instance.Zeze.getHotManager().getModuleContext("Game.Fight", IModuleFight.class);
		var fight = context.getService();
		fight.StartCalculateFighter(getRoleId());
	}

	@Override
	public void calculateFighter(Game.Fight.IFighter fighter) {
		for (var bufId : bean.getBufs().keySet()) {
			var buf = getBuf(bufId);
			if (null != buf)
				buf.calculateFighter(fighter);
		}
	}
}
