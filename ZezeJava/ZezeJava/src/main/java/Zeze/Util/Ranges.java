package Zeze.Util;

import java.util.ArrayList;
import org.jetbrains.annotations.NotNull;

public final class Ranges {
	public static final class Range {
		private final int first; // [first, last)
		private final int last;

		public Range(String @NotNull [] pair) {
			first = Integer.parseInt(pair[0]);
			last = pair.length > 1 ? Integer.parseInt(pair[1]) + 1 : first + 1;
			if (first < 0 || last < 0 || first >= last) {
				throw new IllegalArgumentException("error new range : " + this);
			}
		}

		public Range(int first, int last) {
			if (first < 0 || last < 0 || first >= last)
				throw new IllegalArgumentException("error new range : " + this);
			this.first = first;
			this.last = last;
		}

		public boolean hasIntersection(@NotNull Range r) {
			return !(r.last <= first || r.first >= last);
		}

		@Override
		public @NotNull String toString() {
			if (first + 1 == last) {
				return String.valueOf(first);
			}
			return first + "-" + last;
		}

		public boolean include(@NotNull Range r) {
			return r.first >= first && r.last <= last;
		}
	}

	private final ArrayList<Range> ranges = new ArrayList<>();

	public Ranges() {
	}

	public Ranges(@NotNull String str) {
		for (String s : str.split(",", -1)) {
			if (s.isEmpty())
				continue;
			String[] pair = s.split("-", -1);
			if (pair.length == 0)
				continue;
			if (pair.length > 2)
				throw new IllegalArgumentException("error format: '" + str + "'");
			ranges.add(new Range(pair));
		}
	}

	public void checkAdd(@NotNull Ranges rs) {
		for (Range r : rs.ranges)
			checkAdd(r);
	}

	public void checkAdd(@NotNull Range r) {
		for (Range _r : ranges) {
			if (_r.hasIntersection(r))
				throw new IllegalStateException(ranges + " checkAdd " + r);
		}
		ranges.add(r);
	}

	public void checkAdd(int type) {
		checkAdd(new Range(type, type + 1));
	}

	public boolean include(@NotNull Ranges rs) {
		for (Range r : rs.ranges) {
			if (include(r))
				return true;
		}
		return false;
	}

	public boolean include(@NotNull Range r) {
		for (Range _r : ranges) {
			if (_r.include(r))
				return true;
		}
		return false;
	}

	public void assertInclude(@NotNull Range r) {
		if (!include(r))
			throw new AssertionError(ranges + " NOT Include " + r);
	}

	public void assertInclude(@NotNull Ranges rs) {
		for (Range r : rs.ranges)
			assertInclude(r);
	}

	public void assertInclude(int type) {
		assertInclude(new Range(type, type + 1));
	}
}
