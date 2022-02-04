package kr.uracle.ums.monit.vo;


import kr.uracle.ums.monit.common.Watcher.WatcherState;
import kr.uracle.ums.monit.common.Watcher.WatcherTarget;
import lombok.Data;

@Data
public class WatcherHistoryVo {
	private String watcherName;
	private WatcherTarget type;
	private WatcherState status;
	private long statusTime;
	private long leadTime;
	private long alarmTime;
	private String message;
	
	public long autoSetLeadTime(long now) {
		long leadTime = now -getStatusTime();
		setLeadTime(leadTime);
		return leadTime;
	}
	

}
