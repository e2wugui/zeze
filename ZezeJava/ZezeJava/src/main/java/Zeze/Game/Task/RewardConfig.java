package Zeze.Game.Task;

import java.util.HashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RewardConfig {
	private final HashMap<Integer, Reward> rewards = new HashMap<>();

	public Reward getReward(int rewardId) {
		return rewards.get(rewardId);
	}

	/**
	 * 应用需要从自己的reward配置中装载奖励，并加入到这里。
	 * @param reward reward
	 */
	public void putReward(@NotNull Reward reward) {
		if (null != rewards.putIfAbsent(reward.getRewardId(), reward))
			throw new RuntimeException("duplicate rewardId=" + reward.getRewardId());
	}
}
