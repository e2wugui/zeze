package Zeze.Util;

/** 
 定时延期执行任务。有 System.Threading.Timer，这个没必要了。
*/
@FunctionalInterface
public interface SchedulerHandle {
	public void handle(Task thisTask) throws Throwable;
}