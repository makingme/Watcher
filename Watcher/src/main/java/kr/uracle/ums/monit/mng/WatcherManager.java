package kr.uracle.ums.monit.mng;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PreDestroy;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import kr.uracle.ums.monit.common.Watcher;
import kr.uracle.ums.monit.common.Watcher.WatcherTarget;
import kr.uracle.ums.monit.executor.FileWatcher;


@Order(10)
@Component
public class WatcherManager implements ApplicationRunner{
	
	@Autowired
	SpecManager specManager;
	
	public static final Logger log = LoggerFactory.getLogger(WatcherManager.class);
	
	static ConcurrentHashMap<Watcher, Thread> aliveWatcher = new ConcurrentHashMap<Watcher, Thread>() ;
	
	public final static Map<String, String> typeMap = Stream.of(new String[][]{
		{"FILE", ""} ,{"LOG", "" }, {"TABLE", ""}
	}).collect(Collectors.toMap(p -> p[0], p -> p[1]));
	

	@Override
	public void run(ApplicationArguments args) throws Exception {
		if(ObjectUtils.isEmpty(specManager.specMap("TARGETS")) ||  !(specManager.specMap("TARGETS") instanceof Map)) {
			throw new Exception("수신자(TAGETS) 정보 누락");
		}
		Map<String, Object> targets = (Map<String, Object>)specManager.specMap("TARGETS");
		for(Map.Entry<String, Object> element :specManager.getSpecMap().entrySet()) {
			String key = element.getKey();
			if(!typeMap.containsKey(key)) continue; 
			if(key.equals("FILE")) {
				Object value = element.getValue();
				Map<String, Object> fileWatcherMap = (Map<String, Object>) value;
				if(ObjectUtils.isEmpty(fileWatcherMap.get("WATCHERS"))) continue;
				List<Map<String, Object>> watcherList = (List<Map<String, Object>>)fileWatcherMap.get("WATCHERS");
				for(Map<String, Object> config : watcherList) {
					if(config.containsKey("_PATH"))continue;
					for(Map.Entry<String, Object> t : targets.entrySet()) {
						if(config.containsKey(t.getKey())) continue;
						Object v = t.getValue();
						if(ObjectUtils.isNotEmpty(v))config.put(t.getKey(), v);
					}
					Watcher watcher = new FileWatcher(WatcherTarget.FILE, config);
					
					Thread t = new Thread(watcher);
					if(ObjectUtils.isNotEmpty(config.get("NAME")))t.setName(config.get("NAME").toString());
					t.start();
					aliveWatcher.put(watcher, t);
				}
			}
			
		}
		
	}
	
	//TODO when shutdown it will be called
	@PreDestroy
    public void destroy() {
		//ConcurrentHashMap<Watcher, Thread> aliveWatcher
		for( Map.Entry<Watcher, Thread> m: aliveWatcher.entrySet()) {
			Thread t = m.getValue();
			if(!t.isAlive()) continue;
			
			Watcher w =m.getKey();
			w.stop();
			t.interrupt();
			try {
				t.join(2000);
				if(!t.isAlive()) {
					log.info("{} 중지 완료!!", t.getName());
				}else {
					log.info("{} 중지 실패!!", t.getName());
					log.info("Thread 상태:{}", t.getState());
				}
			} catch (InterruptedException e) {
				log.error("{} 중지 중 에러 발생, 에러 상세:{}", t.getName(), e.getMessage());
			}
			
		}
    }	
}
