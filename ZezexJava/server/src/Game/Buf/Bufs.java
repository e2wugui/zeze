package Game.Buf;

import Game.Buf.*;
import MySqlX.XDevAPI.CRUD.*;
import Game.*;

public class Bufs {
	private long RoleId;
	public final long getRoleId() {
		return RoleId;
	}
	private BBufs bean;

	public Bufs(long roleId, BBufs bean) {
		this.RoleId = roleId;
		this.bean = bean;
	}

	public final Buf GetBuf(int id) {
		V bBuf;
		tangible.OutObject<V> tempOut_bBuf = new tangible.OutObject<V>();
		if (bean.getBufs().TryGetValue(id, tempOut_bBuf)) {
		bBuf = tempOut_bBuf.outArgValue;
			switch (bBuf.Extra.TypeId) {
				case BBufExtra.TYPEID:
					return new BufExtra(bBuf, (BBufExtra)bBuf.Extra.Bean);
				default:
					throw new RuntimeException("unknown extra");
			}
		}
	else {
		bBuf = tempOut_bBuf.outArgValue;
	}
		return null;
	}

	public final void Detach(int id) {
		if (bean.getBufs().Remove(id)) {
			// 因为没有取消Scheduler，所以可能发生删除不存在的buf。
			App.getInstance().getGameFight().StartCalculateFighter(getRoleId());
		}
	}

	public final void Attach(int id) {
		// TODO config: create Buf by id.
		BBuf buf = new BBuf();
		buf.Id = id;
		buf.AttachTime = Zeze.Util.Time.NowUnixMillis;
		buf.ContinueTime = 3600 * 1000; // 1 hour
		buf.Extra_Game_Buf_BBufExtra = new BBufExtra();

		(new BufExtra(buf, buf.getExtraGameBufBBufExtra())).Attach(this);
	}

	public final void Attach(Buf buf) {
		// TODO config: conflict 等
		bean.getBufs().set(buf.getId(), buf.getBean());

		Zeze.Util.Scheduler.Instance.Schedule((ThisTask) -> Detach(buf.getId()), buf.getContinueTime(), -1);
		App.getInstance().getGameFight().StartCalculateFighter(getRoleId());
	}

	public final void CalculateFighter(Game.Fight.Fighter fighter) {
		for (var bufid : bean.getBufs().keySet()) {
			GetBuf(bufid).CalculateFighter(fighter);
		}
	}
}