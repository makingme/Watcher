package kr.uracle.ums.monit.vo;

import kr.uracle.ums.monit.executor.Watcher.WatcherState;
import kr.uracle.ums.monit.executor.Watcher.WatcherTarget;
import lombok.Data;

@Data
public class WatcherHistoryVo {
	private String watcherName;
	private WatcherTarget type;
	private WatcherState status;
	private long statusTime;
	private long createTime;
	private long startTime;
	private long endTime;
	private long leadTime;
	private String message;
	
	public long autoSetLeadTime() {
		long leadTime = computeLeadTime();
		setLeadTime(leadTime);
		return leadTime;
	}
	
	public long computeLeadTime() {
		return getEndTime() - getStartTime();
	}
}
