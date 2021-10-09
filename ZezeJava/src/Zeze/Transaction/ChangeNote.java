package Zeze.Transaction;

import Zeze.*;
import java.util.*;

public abstract class ChangeNote {
	public abstract Bean getBean();

	public abstract void Merge(ChangeNote other);

	public void SetChangedValue(Util.IdentityHashMap<Bean, Bean> values) { // only ChangeNoteMap2 need
	}
}