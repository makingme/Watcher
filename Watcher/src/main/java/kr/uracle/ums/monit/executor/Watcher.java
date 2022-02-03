package kr.uracle.ums.monit.executor;


import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import kr.uracle.ums.monit.common.Notice;
import kr.uracle.ums.monit.common.Notice.NotificationType;
import kr.uracle.ums.monit.common.Notice.TargetInfo;
import kr.uracle.ums.monit.vo.WatcherHistoryVo;



public abstract class Watcher implements Runnable{
	
	private static final Logger log = LoggerFactory.getLogger(Watcher.class);
	
	// 감시대상
	public enum WatcherTarget {
		FILE, TABLE, LOG
	}
	
	// 상태 - 생성, 초기화, 준비, 선수행, 수행, 후수행, 종료
	public enum WatcherState {
		CREATE, INIT, PRE, ING, POST, DONE, WAIT 
	}
	
	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
		
	//와처모듈 로그 VO
	protected final WatcherHistoryVo history = new WatcherHistoryVo();
	
	//알림메시지 조립 패턴
	protected final Pattern pattern = Pattern.compile("\\$\\{[^\\s,;\\(\\)\\*\\+\\-]+\\}");
	
	//알람 담당
	protected Notice notificater = new Notificater();
		
	// 설정
	protected final Map<String, Object> config;
	
	// 감시 타겟 타입
	protected final WatcherTarget target;
	
	// 현재 시간 변수(ms)
	private long mark =0;

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
	protected String watcherName = "NONAME";
	
	// 공통 초기화
	public boolean init() {
		if(config == null) return false;
		
		//와처 이름 설정
		if(ObjectUtils.isNotEmpty(config.get("NAME"))) this.watcherName = config.get("NAME").toString();
		//와처 히스토리 기록
		history.setWatcherName(watcherName);
		history.setType(target);
		history.setStatus(WatcherState.INIT);
		history.setStatusTime(now(true));
		//와처 알람채널 설정
		if(ObjectUtils.isEmpty(config.get("NOTICE"))) return false;
		if(ObjectUtils.isNotEmpty(config.get("NOTICLASS"))) {
			// 노티피케이션 커스텀 클래스 생성
			try {
				Class cls = Class.forName(config.get("NOTICLASS").toString());
				Constructor constructor = cls.getConstructor(null); 
				notificater = (Notice) constructor.newInstance(null);
			} catch (Exception e) {
				log.error("알림 커스터 클래스({})를 생성 중 에러 발생", config.get("NOTICLASS").toString());
				log.error("에러메시지:{}", e.getMessage());
				return false;
			}
		}
		try {
			notificater.setChannel( NotificationType.valueOf(config.get("NOTICE").toString()));
		}catch(IllegalArgumentException e) {
			log.error("{}는 지원하지 않는 NOTICE 타입입니다.", config.get("NOTICE").toString());
			return false;
		}
		//알림 발신 정보 설정
		if(ObjectUtils.isEmpty(config.get("SENDER"))) return false;
		notificater.setSenderInfo(config.get("SENDER").toString());
		
		//알림 메시지 원장 설정
		if(ObjectUtils.isEmpty(config.get("MESSAGE"))) return false;
		notificater.setLodgerMessage(config.get("MESSAGE").toString());
		
		//와처 감시 주기 지정 - DEFAULT 30s(30000ms)
		if(ObjectUtils.isNotEmpty(config.get("CYCLE"))) idleMils = Long.valueOf(config.get("CYCLE").toString().replaceAll("\\D", "0")); 
		
		//알림 수신 대상자 설정
		@SuppressWarnings("unchecked")
		List<String> targetNames = (List<String>)config.get("TARGETS");
		try {
			for(String key :targetNames) {
				if(config.get("key") == null || !(config.get("key") instanceof List) ) continue;
				notificater.addAllTarget(gson.fromJson(config.get(key).toString(), new TypeToken<List<TargetInfo>>(){}.getType()));
			}		
		}catch(JsonSyntaxException e) {
			log.error("{} 설정 중 타겟정보 포맷이 옳바르지 않습니다.", watcherName);
			return false;
		}
		
		//알림 수신 대상자 확인
		if(notificater.getTargetCount() == 0) return false;
		
		//기능와처 초기화 영역 - 와처 동작여부 활성화 필히포함시켜야함 isWorking=true;
		return extraInit();
	}
	
	// 상속 클래스 초기화 메소드
	abstract public boolean extraInit();
	abstract public int watch();
	abstract public void postDoing(int resultCode);
	
	@Override
	public void run() {

		isWorking = init();
		if(!isWorking) log.error("초기화 실패로 {} 종료 됨", watcherName);
		while(isWorking) {
			history.setStartTime(now(true));
			int resultCode=watch();
			if(doPost)postDoing(resultCode);
			history.setEndTime(now(true));
			history.autoSetLeadTime();
			history.setStatus(WatcherState.DONE);
			history.setStatusTime(now(false));

			synchronized(this){
				history.setStatus(WatcherState.WAIT);
				history.setStatusTime(now(true));
				while ( idleMils > 0 ){
					try {
						this.wait(idleMils);
					} catch (InterruptedException e) {
						log.error("감시 대기 중 InterruptedException 발생, 에러상세:{}",e.getMessage());
						break;
					}
				}
			}

		}
	}
	
	public String sendAlram() {
		notificater.sendNotification(watcherName);
		return notificater.getSendMessage();
	}
	
	public long now(boolean reload) {
		if(reload) mark = System.currentTimeMillis();
		return mark;
	}
	
	public WatcherHistoryVo getHistoryVo() {
		return history;
	}
	
	public void stop() {
		isWorking = false;
	}

}
