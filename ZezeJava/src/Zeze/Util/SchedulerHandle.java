package Zeze.Util;

import Zeze.*;
import Zeze.Net.Protocol;

import java.util.*;
import java.util.concurrent.TimeUnit;

/** 
 定时延期执行任务。有 System.Threading.Timer，这个没必要了。
*/
@FunctionalInterface
public interface SchedulerHandle {
	public void handle(Task thisTask);
}