package Zeze.Util;

/**
 * Game Helper
 */
public class GameObjectId implements Comparable<GameObjectId> {
	private int Type;
	private int ConfigId;
	private long InstanceId;

	public final int getType() {
		return Type;
	}

	public final void setType(int value) {
		Type = value;
	}

	public final int getConfigId() {
		return ConfigId;
	}

	public final void setConfigId(int value) {
		ConfigId = value;
	}

	public final long getInstanceId() {
		return InstanceId;
	}

	public final void setInstanceId(long value) {
		InstanceId = value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 17;
		result = prime * result + Integer.hashCode(getType());
		result = prime * result + Integer.hashCode(getConfigId());
		result = prime * result + Long.hashCode(getInstanceId());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}

		if (obj instanceof GameObjectId) {
			GameObjectId o = (GameObjectId)obj;
			return getType() == o.getType() && getConfigId() == o.getConfigId() && getInstanceId() == o.getInstanceId();
		}
		return false;
	}

	@Override
	public final int compareTo(GameObjectId x) {
		int c = Integer.compare(getType(), x.getType());
		if (c != 0) {
			return c;
		}
		c = Integer.compare(getConfigId(), x.getConfigId());
		if (c != 0) {
			return c;
		}
		return Long.compare(getInstanceId(), x.getInstanceId());
	}
}
