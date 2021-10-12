package Zeze.Transaction;

import java.util.*;

public abstract class ChangeNote {
	public abstract Bean getBean();

	public abstract void Merge(ChangeNote other);

	public void SetChangedValue(IdentityHashMap<Bean, Bean> values) { // only ChangeNoteMap2 need
	}
}