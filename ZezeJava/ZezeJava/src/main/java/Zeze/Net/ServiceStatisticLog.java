package Zeze.Net;

import java.util.concurrent.ScheduledFuture;
import Zeze.Util.Action0;
import Zeze.Util.OutLong;
import Zeze.Util.Random;
import Zeze.Util.TaskSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** 每Service的网络收发统计周期日志（select/recv/send速率与发送缓冲积压）。 */
public final class ServiceStatisticLog implements Action0 {
	private static final @NotNull Logger logger = LogManager.getLogger("StatLog"); // 沿用统计日志通道
	private final @NotNull Service service;
	private @Nullable ScheduledFuture<?> statisticLogFuture;
	private int periodSec;
	private final long[] lastSizes = new long[]{-1, 0, 0, 0, 0, 0};

	public ServiceStatisticLog(@NotNull Service service) {
		this.service = service;
	}

	public @NotNull ScheduledFuture<?> startStatisticLog(int periodSec) {
		if (periodSec <= 0)
			throw new IllegalArgumentException("periodSec(" + periodSec + ") < 0");
		var f = statisticLogFuture;
		if (f != null && !f.isCancelled())
			cancelStartStatisticLog();
		this.periodSec = periodSec;
		f = TaskSpec.ofAction(this).schedulePeriodNow(Random.getInstance().nextLong(periodSec * 1000L), periodSec * 1000L);
		statisticLogFuture = f;
		return f;
	}

	public boolean cancelStartStatisticLog() {
		var f = statisticLogFuture;
		statisticLogFuture = null;
		return f != null && f.cancel(false);
	}

	@Override
	public void run() throws Exception {
		service.updateRecvSendSize();
		var selectors = service.getSelectors();
		long selectCount = selectors.getSelectCount();
		long recvCount = service.getRecvCount();
		long recvSize = service.getRecvSize();
		long sendCount = service.getSendCount();
		long sendSize = service.getSendSize();
		long sendRawSize = service.getSendRawSize();
		if (lastSizes[0] != -1) {
			long sn = (selectCount - lastSizes[0]) / periodSec;
			long rc = (recvCount - lastSizes[1]) / periodSec;
			long rs = (recvSize - lastSizes[2]) / periodSec;
			long sc = (sendCount - lastSizes[3]) / periodSec;
			long ss = (sendSize - lastSizes[4]) / periodSec;
			long sr = (sendRawSize - lastSizes[5]) / periodSec;
			var operates = new OutLong();
			var outBufSize = new OutLong();
			service.foreach(socket -> {
				if (socket instanceof TcpSocket tcp) {
					operates.value += tcp.getOperateSize();
					outBufSize.value += tcp.getOutputBufferSize();
				}
			});
			operates.value /= periodSec;
			outBufSize.value /= periodSec;
			logger.info("{}.{}.stat: select={}/{}, recv={}/{}, send={}/{}, sendRaw={}, sockets={}, ops={}, outBuf={}",
					service.getName(), service.getInstanceName(), sn, selectors.getCount(), rs, rc, ss, sc, sr,
					service.getSocketCount(), operates.value, outBufSize.value);
		}
		lastSizes[0] = selectCount;
		lastSizes[1] = recvCount;
		lastSizes[2] = recvSize;
		lastSizes[3] = sendCount;
		lastSizes[4] = sendSize;
		lastSizes[5] = sendRawSize;
	}
}
