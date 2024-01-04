package Zeze.Game.Task;

import Zeze.Builtin.Game.TaskModule.BTask;
import Zeze.Net.Binary;

public interface Reward {
	int getRewardId();

	// 下面两个方法的结果通常打包到给客户端的任务结构里面，由客户端完成任务奖励提示的显示。
	// 每种任务奖励的显示方式都可能不一样。
	int getRewardType();
	Binary getRewardParam(long roleId);

	// 发放奖励，一般是任务完成的时候由任务系统调用。广义的，这个方法也可以实现任意时机的奖励发放。
	void reward(long roleId);
}
