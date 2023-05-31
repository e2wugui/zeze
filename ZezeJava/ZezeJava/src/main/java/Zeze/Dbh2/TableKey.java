package Zeze.Dbh2;

import org.jetbrains.annotations.NotNull;
import Zeze.Net.Binary;

public class TableKey implements Comparable<TableKey> {
	private final String name;
	private final Binary key;

	public TableKey(String name, Binary key) {
		this.name = name;
		this.key = key;
	}

	public String getName() {
		return name;
	}

	public Binary getKey() {
		return key;
	}

	@Override
	public int compareTo(@NotNull TableKey o) {
		int c = name.compareTo(o.name);
		if (c != 0)
			return c;
		return key.compareTo(o.key);
	}
}
