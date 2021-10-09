package Zeze.Util;

import Zeze.*;
import java.util.*;

public class CubeIndex implements java.lang.Comparable<CubeIndex> {
	private long X;
	public final long getX() {
		return X;
	}
	public final void setX(long value) {
		X = value;
	}
	private long Y;
	public final long getY() {
		return Y;
	}
	public final void setY(long value) {
		Y = value;
	}
	private long Z;
	public final long getZ() {
		return Z;
	}
	public final void setZ(long value) {
		Z = value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 17;
		result = prime * result + (new Long(getX())).hashCode();
		result = prime * result + (new Long(getY())).hashCode();
		result = prime * result + (new Long(getZ())).hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}

		boolean tempVar = obj instanceof CubeIndex;
		CubeIndex other = tempVar ? (CubeIndex)obj : null;
		if (tempVar) {
			return getX() == other.getX() && getY() == other.getY() && getZ() == other.getZ();
		}
		return false;
	}

	public final int compareTo(CubeIndex other) {
		int c = (new Long(getX())).compareTo(other.getX());
		if (c != 0) {
			return c;
		}
		c = (new Long(getY())).compareTo(other.getY());
		if (c != 0) {
			return c;
		}
		return (new Long(getZ())).compareTo(other.getZ());
	}
}