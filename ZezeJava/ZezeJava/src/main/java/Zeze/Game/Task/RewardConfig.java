package Zeze.Game.Task;

import java.util.HashMap;
import org.jetbrains.annotations.NotNull;

public class RewardConfig {
	private final HashMap<Integer, Reward> rewards = new HashMap<>();

	public Reward getReward(int rewardId) {
		return rewards.get(rewardId);
	}

	/**
	 * 应用需要从自己的reward配置中装载奖励，并加入到这里。
	 * 【RewardId需要所有奖励能互斥】
	 * 如果应用自己的奖励配置支持多态，即对应每一个奖励类型，拥有一个特别的配置结构，
	 * 那么RewardId就自然所有奖励互斥。
	 * 如果应用自己的奖励配置不支持多态，那么对应每个奖励类型，一张自己的配置表，
	 * 此时要注意不同表之间的RewardId需要互斥。
	 * @param reward reward
	 */
	public void putReward(@NotNull Reward reward) {
		if (null != rewards.putIfAbsent(reward.getRewardId(), reward))
			throw new RuntimeException("duplicate rewardId=" + reward.getRewardId());
	}
}
