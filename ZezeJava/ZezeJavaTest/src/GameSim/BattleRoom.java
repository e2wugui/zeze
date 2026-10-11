package GameSim;

import java.util.Arrays;
import java.util.Random;

/**
 * 内存态战斗状态机（架构铁律：不经任何表、不经 Zeze）。
 *
 * <p>tick 循环纯内存计算：伤害公式 atk·rand(0.8~1.2)−def、掷骰定死 loot 参数、反击与死亡惩罚；
 * 结果只以事件投递给玩家线程，由玩家线程在自己的虚拟线程上执行结果事务
 * （单写者亲和：角色记录几乎全部由本人线程写，交易除外）。
 *
 * <p>房间 RNG 以房间序号为种子：队伍组成依赖匹配时机（涌现性），守恒不依赖战斗确定性。
 */
final class BattleRoom implements Runnable {
	private final GameWorld world;
	private final GameConfig cfg;
	private final PlayerSim[] party;
	private final Random rng;
	private final int[] monsterHp = new int[GameConfig.MonsterCount];
	private final long[] dmgSinceKill = new long[GameConfig.PartySize];
	private long tickSeq;

	BattleRoom(GameWorld world, PlayerSim[] party, int seq) {
		this.world = world;
		this.cfg = world.cfg;
		this.party = party;
		this.rng = new Random(seq * 1_000_003L);
	}

	@Override
	public void run() {
		try {
			Arrays.fill(monsterHp, GameConfig.MonsterHp);
			while (!world.stopped && aliveMonsters() > 0) {
				tick();
				Thread.sleep(cfg.wall(GameConfig.RoomTickMs));
			}
		} catch (InterruptedException ignored) {
		} finally {
			for (var p : party)
				p.roomDone();
		}
	}

	private int aliveMonsters() {
		int n = 0;
		for (int hp : monsterHp)
			if (hp > 0)
				++n;
		return n;
	}

	/** 随机选一只存活怪；全灭返回 -1 */
	private int randomAliveMonster() {
		int n = aliveMonsters();
		if (n == 0)
			return -1;
		int k = rng.nextInt(n);
		for (int i = 0; i < monsterHp.length; i++) {
			if (monsterHp[i] > 0 && k-- == 0)
				return i;
		}
		return -1;
	}

	private void tick() {
		tickSeq++;
		// 玩家攻击：每人打一只存活怪
		for (int i = 0; i < party.length; i++) {
			var p = party[i];
			if (!p.loggedIn)
				continue;
			int m = randomAliveMonster();
			if (m < 0)
				break;
			int atk = GameConfig.PlayerBaseAtk + p.equipAtk; // 装备加成读内存镜像
			int dmg = Math.max(1, (int)(atk * (0.8 + rng.nextDouble() * 0.4)) - GameConfig.MonsterDef);
			monsterHp[m] -= dmg;
			dmgSinceKill[i] += dmg;
			if (monsterHp[m] <= 0) {
				// 击杀：全队 loot 事件——房间掷骰在此刻定死全部参数（伤害累计自上一次击杀）
				for (int j = 0; j < party.length; j++) {
					long damage = dmgSinceKill[j];
					dmgSinceKill[j] = 0;
					party[j].inbox.add(PlayerSim.rollLoot(rng, 8 + rng.nextInt(5), damage));
				}
			}
		}
		// 怪物反击（每两 tick 一次）：命中随机存活队员；死亡→半血复活+惩罚事件
		if (tickSeq % 2 == 0) {
			var victim = party[rng.nextInt(party.length)];
			if (victim.loggedIn) {
				int hp = victim.hpMirror - GameConfig.MonsterAtk;
				if (hp <= 0) {
					hp = GameConfig.PlayerMaxHp / 2;
					victim.inbox.add(new PlayerSim.DeathEvent(10));
				}
				victim.hpMirror = hp; // 房间线程写镜像；药水治疗由玩家线程写，交错可接受
			}
		}
	}
}
