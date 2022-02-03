package kr.uracle.ums.monit.mng;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import kr.uracle.ums.monit.executor.FileWatcher;
import kr.uracle.ums.monit.executor.Watcher;
import kr.uracle.ums.monit.executor.Watcher.WatcherTarget;

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
					Watcher watcher = new FileWatcher(WatcherTarget.FILE, config);
					Thread t = new Thread(watcher);
					t.start();
					aliveWatcher.put(watcher, t);
				}
			}
			
		}
		
	}
}
