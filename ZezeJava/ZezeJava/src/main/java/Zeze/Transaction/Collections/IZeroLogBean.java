package Zeze.Transaction.Collections;

import Zeze.Transaction.Changes;

/**
 * 当Bean新插入表或者Bean内部的容器中时，
 * 它在增量更新日志传输的时候不再传输Bean本身，而是它的改变（LogBean）。
 * 对于事务外传入的Bean，需要根据当前Bean的值“构建出它的增量日志（LogBean）,
 * 接收日志方可以new Bean()并且apply日志得到当前值。
 * “Zero”表示新近Put的Bean在增量日志里面总是隐含从0开始。
 */
public interface IZeroLogBean {
	void zeroLogBean(Changes changes);
}
