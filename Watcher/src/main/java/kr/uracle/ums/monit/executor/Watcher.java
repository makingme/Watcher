package kr.uracle.ums.monit.executor;


import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import kr.uracle.ums.monit.executor.Notificater.NotificationType;
import kr.uracle.ums.monit.executor.Notificater.TargetInfo;
import kr.uracle.ums.monit.vo.WatcherHistoryVo;



public abstract class Watcher implements Runnable{
	
	private static final Logger log = LoggerFactory.getLogger(Watcher.class);
		
	public enum WatcherTarget {
		FILE, TABLE, LOG
	}
	public enum WatcherState {
		CREATE, INIT, READY, PRE, ING, POST, DONE 
	}
	
	public final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	
	//알람 담당
	private Notificater notificater = new Notificater();
	//와처모듈 로그 VO
	private WatcherHistoryVo history = new WatcherHistoryVo(); 
	
	private long mark =0;
	// 설정
	protected final Map<String, Object> config;
	
	// 감시 타겟 타입
	protected final WatcherTarget target;
	

	public Watcher(WatcherTarget target, Map<String, Object> config) {
		this.target = target;
		this.config = config;
		history.setStatus(WatcherState.CREATE);
		history.setCreateTime(now(true));
		history.setStatusTime(now(false));
		history.setMessage("Just Created");
	}
		
	// 동작 플래그
	protected boolean isWorking = false;
	
	// 후처리 활성화여부
	protected boolean doPost = false;
	
	protected Long idleMils = 30000L;
	// 쓰레드명
	protected String workerName = "NONAME";
	
	// 공통 초기화
	public void init() {
		if(config == null) return;
		
		//와처 이름 설정
		if(config.get("NAME") != null) this.workerName = config.get("NAME").toString();
		//와처 히스토리 기록
		history.setWatcherName(workerName);
		history.setType(target);
		history.setStatus(WatcherState.INIT);
		
		//와처 알람채널 설정
		if(config.get("NOTICE") == null) return;
		try {
			notificater.setChannel( NotificationType.valueOf(config.get("NOTICE").toString()));
		}catch(IllegalArgumentException e) {
			log.error("{}는 지원하지 않는 NOTICE 타입입니다.", config.get("NOTICE").toString());
			return;
		}
		//알림 발신 정보 설정
		if(config.get("SENDER") == null) return;
		notificater.setSenderInfo(config.get("SENDER").toString());
		
		//와처 감시 주기 지정 - DEFAULT 30s(30000ms)
		if(config.get("CYCLE") != null) idleMils = Long.valueOf(config.get("CYCLE").toString().replaceAll("\\D", "0")); 
		
		//알림 수신 대상자 설정
		@SuppressWarnings("unchecked")
		List<String> targetNames = (List<String>)config.get("TARGETS");
		try {
			for(String key :targetNames) {
				if(config.get("key") == null || !(config.get("key") instanceof List) ) continue;
				notificater.addAllTarget(gson.fromJson(config.get(key).toString(), new TypeToken<List<TargetInfo>>(){}.getType()));
			}			
		}catch(JsonSyntaxException e) {
			log.error("{} 설정 중 타겟정보 포맷이 옳바르지 않습니다.", workerName);
			return ;
		}
		
		//알림 수신 대상자 확인
		if(notificater.getTargetCount() == 0) return;
		

		
		//기능와처 초기화 영역 - 와처 동작여부 활성화 필히포함시켜야함 isWorking=true;

		extraInit();
	}
	
	// 상속 클래스 초기화 메소드
	abstract public void extraInit();
	abstract public int watch();
	abstract public void postDoing(int resultCode);
	
	@Override
	public void run() {

		init();
	
		while(isWorking) {
			history.setStartTime(now(true));
			int resultCode=watch();
			if(doPost)postDoing(resultCode);
			history.setEndTime(now(true));
			history.autoSetLeadTime();
			long wait = 0 ;
			synchronized(this){
				while ( wait < idleMils ){
					try {
						this.wait(idleMils < 1000 ? idleMils : 1000);
					} catch (InterruptedException e) {
						log.error("감시 대기 중 InterruptedException 발생, 에러상세:{}",e.getMessage());
						break;
					}
					wait += 1000 ;
				}
			}		
		
		}
	}
	
	public long now(boolean reload) {
		if(reload) mark = System.currentTimeMillis();
		return mark;
	}
	
	public WatcherHistoryVo getHistoryVo() {
		return history;
	}

}
