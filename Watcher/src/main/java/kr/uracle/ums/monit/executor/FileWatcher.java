package kr.uracle.ums.monit.executor;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FileWatcher extends Watcher {
	
	private static final Logger log = LoggerFactory.getLogger(FileWatcher.class);
	
	public enum FileWatcherType{
		MAX, MIN, STAY, LAST, PATTERN
	}

	public FileWatcher(WatcherTarget type, Map<String, Object> config) {
		super(type, config);
	}
	
	private FileWatcherType type;
	
	private String path;
		
	@Override
	public void extraInit() {
		try {
			type = FileWatcherType.valueOf(config.get("TYPE").toString());
		}catch(IllegalArgumentException e) {
			log.error("{}는 지원하지 않는 NOTICE 타입입니다.", config.get("NOTICE").toString());
			return;
		}
		
		//와처 동작여부 활성화
		isWorking=true;
	}

	@Override
	public int watch() {

		return 0;
	}

	@Override
	public void postDoing(int resultCode) {

		
	}

}
