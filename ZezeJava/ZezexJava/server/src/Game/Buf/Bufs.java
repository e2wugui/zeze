package Game.Buf;

import org.jspecify.annotations.Nullable;
import java.util.Objects;
import Game.*;
import Zeze.Util.TaskSpec;

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
	public @Nullable IBuf getBuf(int id) {
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
			Objects.requireNonNull(App.Instance.Game_Fight).StartCalculateFighter(getRoleId());
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

		TaskSpec.ofAction(() -> detach(buf.getId())).schedule(buf.getContinueTime());
		Objects.requireNonNull(App.Instance.Game_Fight).StartCalculateFighter(getRoleId());
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
