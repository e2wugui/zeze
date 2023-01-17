package Zeze.Util;

/**
 * 定时延期执行任务
 */
@FunctionalInterface
public interface SchedulerHandle {
	void handle(Task thisTask) throws Exception;
}
