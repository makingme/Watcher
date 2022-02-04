package kr.uracle.ums.monit.executor;


import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.annotation.PreDestroy;

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
		history.setStatusTime(now(false));
		history.setMessage("Just Created");
	}
		
	// 동작 플래그
	protected boolean isWorking = false;
	
	// 후처리 활성화여부
	protected boolean doPost = false;
	
	protected Long idleMils = 3000L;
	// 쓰레드명
	protected String watcherName = "NONAME";
	
	// 공통 초기화
	public boolean init() {
		//와처 히스토리 기록
		history.setWatcherName(watcherName);
		history.setType(target);
		history.setStatus(WatcherState.INIT);
		history.setStatusTime(now(true));
		
		if(config == null) {
			log.error("WATCHER 설정을 불러오지 못함");
			return false;
		}
		
		//와처 이름 설정
		if(ObjectUtils.isEmpty(config.get("NAME"))) {
			log.error("WATCHER NAME 설정 누락");
			return false;
		}
		this.watcherName = config.get("NAME").toString();
		
		//와처 알람채널 설정
		if(ObjectUtils.isEmpty(config.get("NOTICE"))) {
			log.error("NOTICE 설정이 없습니다.");
			return false;
		}
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
			log.error("{}는 지원하지 않는 NOTICE 타입", config.get("NOTICE").toString());
			return false;
		}
		
		//알람자 이름설정
		notificater.setName(target.toString()+notificater.getChannel().toString());
		
		//알림 발신 정보 설정
		if(ObjectUtils.isEmpty(config.get("SENDER"))) {
			log.error("SENDER(발신정보) 설정 누락");
			return false;
		}
		
		notificater.setSenderInfo(config.get("SENDER").toString());
		
		//알림 메시지 원장 설정
		if(ObjectUtils.isEmpty(config.get("MESSAGE"))) {
			log.error("알림메시지(MESSAGE) 설정 누락");
			return false;
		}
		notificater.setLodgerMessage(config.get("MESSAGE").toString());
		
		//와처 감시 주기 지정 - DEFAULT 30s(30000ms)
		if(ObjectUtils.isNotEmpty(config.get("CYCLE"))) idleMils = Long.valueOf(config.get("CYCLE").toString().replaceAll("\\D", "0"))*1000; 
		
		//알림 수신 대상자 설정
		@SuppressWarnings("unchecked")
		List<String> targetNames = (List<String>)config.get("TARGETS");
		try {
			for(String key :targetNames) {
				if(config.get(key) == null || !(config.get(key) instanceof List) ) continue;
				notificater.addAllTarget(gson.fromJson(config.get(key).toString(), new TypeToken<List<TargetInfo>>(){}.getType()));
			}		
		}catch(JsonSyntaxException e) {
			log.error("{} 설정 중 타겟정보 포맷이 옳바르지 않음", watcherName);
			return false;
		}
		
		//알림 수신 대상자 확인
		if(notificater.getTargetCount() == 0) {
			log.error("알림 수신대상자 없음");
			return false;
		}
		
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
		if(!isWorking) {
			log.error("초기화 실패로 {} 종료 됨", watcherName);
			System.exit(0);
		}
		while(isWorking) {
			history.autoSetLeadTime(now(true));
			history.setStatus(WatcherState.ING);
			history.setStatusTime(now(false));
			int resultCode=watch();
			if(doPost)postDoing(resultCode);
			history.autoSetLeadTime(now(true));
			history.setStatus(WatcherState.DONE);
			history.setStatusTime(now(false));
			

			if(resultCode ==1) { // 알람
				resultCode = sendAlram();
			}else if(resultCode == -1) { // 재기동 필요 에러
				
			}
			synchronized(this){
				history.setStatus(WatcherState.WAIT);
				history.setStatusTime(now(true));
				long t_wait = 0 ;
				log.debug("[{}] 감시 중....", watcherName);
				while (t_wait < idleMils){
					try {
						this.wait(1000);
					} catch (InterruptedException e) {
						log.error("감시 대기 중 InterruptedException 발생, 에러상세:{}",e.getMessage());
						break;
					}
					t_wait += 1000;
				}
			}

		}
		
	}
	
	public int sendAlram() {
		int rslt=notificater.sendNotification(watcherName);
		if(rslt >0) {
			log.info("{} 채널 알람발송, 내용: {}", notificater.getChannel() ,notificater.getSendMessage());
		}else if(rslt == 0 ) {
			log.info("{} 채널 알람발송 실패, 무시건수{}", notificater.getChannel() ,notificater.getIgnoreCount());
		}else {
			log.error("{} 채널 알람발송 중 에러 발생:{}", notificater.getSendMessage());
		}
		return rslt;
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
	

    public void destroy() {
    	stop();
    }
	
}
