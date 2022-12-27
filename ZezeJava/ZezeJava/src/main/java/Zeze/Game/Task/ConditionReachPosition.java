package Zeze.Game.Task;

import javax.json.JsonObject;
import Zeze.Builtin.Game.TaskBase.BTConditionReachPosition;
import Zeze.Builtin.Game.TaskBase.BTConditionReachPositionEvent;
import Zeze.Builtin.Game.TaskBase.BTaskCondition;
import Zeze.Game.TaskConditionBase;
import Zeze.Game.TaskPhase;
import Zeze.Transaction.Bean;

public class ConditionReachPosition extends TaskConditionBase<BTConditionReachPosition, BTConditionReachPositionEvent> {
	// @formatter:off
	public static final int ReachPosition2D = 1;
	public static final int ReachPosition3D = 2;
	public ConditionReachPosition(TaskPhase phase) {
		super(phase);
	}

	private static double distance2D(double x1, double y1, double x2, double y2) {
		return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
	}

	private static double distance3D(double x1, double y1, double z1, double x2, double y2, double z2) {
		return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2) + Math.pow(z1 - z2, 2));
	}

	@Override
	public boolean accept(Bean eventBean) throws Throwable {
		if (!(eventBean instanceof BTConditionReachPositionEvent))
			return false;

		var bean = getExtendedBean();
		if (bean.isReached())
			return false;

		if (bean.getDimension() == ReachPosition2D) {
			var reachPositionEventBean = (BTConditionReachPositionEvent)eventBean;
			if (distance2D(bean.getX(), bean.getY(), reachPositionEventBean.getX(), reachPositionEventBean.getY()) <= bean.getRadius()) {
				bean.setReached(true);
//				onComplete();
				return true;
			}
		}
		if (bean.getDimension() == ReachPosition3D) {
			var reachPositionEventBean = (BTConditionReachPositionEvent)eventBean;
			if (distance3D(bean.getX(), bean.getY(), bean.getZ(), reachPositionEventBean.getX(), reachPositionEventBean.getY(), reachPositionEventBean.getZ()) <= bean.getRadius()) {
				bean.setReached(true);
				return true;
			}
		}
		return false; // supposed to be not reachable
	}

	@Override
	public boolean isCompleted() { return getExtendedBean().isReached(); }
	@Override
	protected void loadBeanExtended(BTaskCondition bean) {

	}
	@Override
	public String getType() {
		return "ReachPosition";
	}
	@Override
	protected void loadJsonExtended(JsonObject json) {

	}
	// @formatter:on
}
