package Zeze.Services;

import Zeze.*;
import java.util.*;

@FunctionalInterface
public interface CallbackOnSocketProcessInputBuffer {
	void invoke(long sessionId, Puerts.ArrayBuffer buffer, int offset, int len);
}