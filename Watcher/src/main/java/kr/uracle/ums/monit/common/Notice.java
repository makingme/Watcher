package kr.uracle.ums.monit.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public abstract class Notice {
	public enum NotificationType {
		SMS, KKO, PUSH, NTOK, MAIL, LOG
	}
		
	public String senderInfo;
	public NotificationType channel = NotificationType.SMS;
	public List<TargetInfo> targetList = new ArrayList<TargetInfo>(10);
	public String sendMessage = null;
	public String lodgerMessage = null;
	public long sendTimeCyle = 5*60*1000;
	private long sendTime = 0;
	
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
	
	//지정 시간동안 한번만 알람 발송할수 있도록 boolean return 하는 메소드 필
	protected boolean isOK() {
		if(sendTime != 0 && System.currentTimeMillis() -sendTime < sendTimeCyle) return false;
		return true;
	}
	
	public int sendNotification(String module) {

		if(isOK()) return sendNotification();
		
		return 0;
	}
	abstract public int sendNotification();
}
