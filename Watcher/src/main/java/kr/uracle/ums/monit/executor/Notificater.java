package kr.uracle.ums.monit.executor;

import java.util.ArrayList;
import java.util.List;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Data;

@Data
public class Notificater {
	private static final Logger log = LoggerFactory.getLogger(Notificater.class);
	
	enum NotificationType {
		SMS, KKO, PUSH, NTOK, MAIL, LOG
	}
	
	private String senderInfo;
	private NotificationType channel = NotificationType.SMS;
	private List<TargetInfo> targetList = new ArrayList<TargetInfo>(10);
	@Data
	public class TargetInfo{
		private String alias;
		private String contact;
	}
	
	public int addTarget(TargetInfo targetInfo) {
		targetList.add(targetInfo);
		return targetList.size();
	}
	
	public int addAllTarget(List<TargetInfo> targetInfoList)  {
		targetList.addAll(targetInfoList);
		return targetList.size();
	}
	
	public int getTargetCount() {
		return targetList.size(); 
	}
	
	public int sendNotification(String module, String message) {
		return 0;
	}
}
