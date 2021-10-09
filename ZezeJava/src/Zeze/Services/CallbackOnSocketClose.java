package Zeze.Services;

import Zeze.*;
import java.util.*;

@FunctionalInterface
public interface CallbackOnSocketClose {
	void invoke(long sessionId);
}