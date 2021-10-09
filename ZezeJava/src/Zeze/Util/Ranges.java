package Zeze.Util;

import Zeze.*;
import java.util.*;

public final class Ranges {
	public final static class Range {
		private int first; // [first, last)
		private int last;

		public Range(String[] pair) {
			first = Integer.parseInt(pair[0]);
			last = pair.length > 1 ? Integer.parseInt(pair[1]) + 1 : first + 1;
			if (first < 0 || last < 0 || first >= last) {
				throw new RuntimeException("error new range : " + this);
			}
		}
		public Range(int first, int last) {
			this.first = first;
			this.last = last;
			if (first < 0 || last < 0 || first >= last) {
				throw new RuntimeException("error new range : " + this);
			}
		}

		public boolean HasIntersection(Range r) {
			return !(r.last <= first || r.first >= last);
		}

		@Override
		public String toString() {
			if (first + 1 == last) {
				return String.valueOf(first);
			}
			return String.valueOf(first) + "-" + String.valueOf(last);
		}

		public boolean Include(Range r) {
			return r.first >= first && r.last <= last;
		}
	}

	private ArrayList<Range> ranges = new ArrayList<Range>();

	public Ranges() {

	}

	public Ranges(String str) {
		for (String s : str.split("[,]", -1)) {
			if (s.length() == 0) {
				continue;
			}
			String[] pair = s.split("[-]", -1);
			if (pair.length == 0) {
				continue;
			}
			if (pair.length > 2) {
				throw new RuntimeException("error format: '" + str + "'");
			}
			ranges.add(new Range(pair));
		}
	}

	public void CheckAdd(Ranges rs) {
		for (Range r : rs.ranges) {
			CheckAdd(r);
		}
	}

	public void CheckAdd(Range r) {
		for (Range _r : ranges) {
			if (_r.HasIntersection(r)) {
				throw new RuntimeException(ranges + " checkAdd " + r);
			}
		}
		ranges.add(r);
	}

	public void CheckAdd(int type) {
		CheckAdd(new Range(type, type + 1));
	}

	public boolean Include(Ranges rs) {
		for (Range r : rs.ranges) {
			if (Include(r)) {
				return true;
			}
		}
		return false;
	}

	public boolean Include(Range r) {
		for (Range _r : ranges) {
			if (_r.Include(r)) {
				return true;
			}
		}
		return false;
	}

	public void AssertInclude(Range r) {
		if (Include(r)) {
			return;
		}
		throw new RuntimeException(ranges + " NOT Include " + r);
	}

	public void AssertInclude(Ranges rs) {
		for (Range r : rs.ranges) {
			AssertInclude(r);
		}
	}

	public void AssertInclude(int type) {
		AssertInclude(new Range(type, type + 1));
	}
}