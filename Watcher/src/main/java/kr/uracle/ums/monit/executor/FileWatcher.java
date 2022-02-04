package kr.uracle.ums.monit.executor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class FileWatcher extends Watcher {
	
	private static final Logger log = LoggerFactory.getLogger(FileWatcher.class);
	
	public enum FileWatcherType{
		MAX, MIN, STAY, LAST, SIZE, PATTERN
	}

	public FileWatcher(WatcherTarget type, Map<String, Object> config) {
		super(type, config);
	}
	
	private FileWatcherType type;
	
	private Path path;
	
	private Object figure;
	
	private boolean isDirectory = false;
	
	Map<String, Object> replaceMap = new HashMap<String, Object>(10);
	
	
	@Override
	public boolean extraInit() {
		if(ObjectUtils.isEmpty(config.get("TYPE"))) return false;
		if(ObjectUtils.isEmpty(config.get("PATH"))) return false;
		if(ObjectUtils.isEmpty(config.get("FIGURE"))) return false;
		try {
			type = FileWatcherType.valueOf(config.get("TYPE").toString());
		}catch(IllegalArgumentException e) {
			log.error("{}는 지원하지 않는 NOTICE 타입입니다.", config.get("NOTICE").toString());
			return false;
		}
		
		path = Paths.get(config.get("PATH").toString());
		File target =path.toFile();
		if(!target.exists()) return false;
		if(target.isDirectory()) isDirectory =true;
		
		figure = config.get("FIGURE");
		
		replaceMap.put("FILENAME", path.toString());
		replaceMap.put("FILESIZE", 0);
		replaceMap.put("FILCOUNT", 0);
		
		//와처 동작여부 활성화
		return true;
	}

	@Override
	public int watch() {
		history.setStatus(WatcherState.ING);
		history.setStatusTime(now(true));
		int rslt = -1;
		switch(type) {
			case MAX:
			case MIN:
				rslt = fileCount();
				break;
			case STAY:
			case LAST:
				rslt = fileTime();
				break;
			case SIZE:
				rslt = fileSize();
				break;
			case PATTERN:
				rslt = fileCount();
				break;
		}
		
		if(rslt == 1) {
			notificater.setSendMessage(assembleMessage());
		}
		return rslt;
	}

	@Override
	public void postDoing(int resultCode) {
		//do nothing
	}

	private int fileCount() {
		int rslt = 0;
		try {
			long count= Files.walk(path).filter(p ->  p.toFile().isFile() && p.toFile().length() > 0  ).count();
			log.info("지정 타겟:{}, 현재 파일 수:{}, 제한 수치:{}", path.toAbsolutePath(), count, figure);
			if(type == FileWatcherType.MAX && count > Integer.parseInt(figure.toString())) {
				replaceMap.put("FILENAME", path.toString());
				replaceMap.put("FILECOUNT", count);
				rslt = 1;
			}
			if(type == FileWatcherType.MIN && count < Integer.parseInt(figure.toString())) {
				replaceMap.put("FILENAME", path.toString());
				replaceMap.put("FILECOUNT", count);
				rslt = 1; 
			}
		} catch (IOException e) {
			log.error("{} 감시 중 에러 발생", path.toString());
			log.error("에러상세:{}", e.getMessage());
			return -1;
		}
		return rslt;
	}
	
	// TODO STAY CASE need to implement
	private int fileTime() {
		int rslt = 0;
		try {
			List<Path> list =Files.walk(path).filter(p -> p.toFile().isFile() && p.toFile().length() > 0).collect(Collectors.toList());
			long now = System.currentTimeMillis();
			for(Path p : list) {
				File f = p.toFile();
				long modyTime = f.lastModified(); 
				if(now - modyTime > (Integer.parseInt(figure.toString())*1000) ) {
					replaceMap.put("FILENAME", f.getName());
					return 1;
				}
			}
		} catch (IOException e) {
			log.error("{} 감시 중 에러 발생", path.toString());
			log.error("에러상세:{}", e.getMessage());
			return -1;
		}
		return rslt;
	}
	
	private int fileSize() {
		int rslt = 0;
		try {
			long size= Files.walk(path).filter(p -> p.toFile().isFile()).mapToLong(p -> p.toFile().length()).sum();
			replaceMap.put("FILESIZE", size);
			if(size > (int)figure) return 1;
		} catch (IOException e) {
			log.error("{} 감시 중 에러 발생", path.toString());
			log.error("에러상세:{}", e.getMessage());
			return -1;
		}
		return rslt;
	}
	
	private int filePattern() {
		return 0;
	}
	
	private String assembleMessage() {
		String message = notificater.getLodgerMessage();
		Matcher m = pattern.matcher(message);
		while(m.find()) {
			String reple = m.group();
			String key = reple.replaceAll("(\\$|\\{|\\})", "");
			if(config.get(key) == null && replaceMap.get(key) == null) continue;
			if(ObjectUtils.isNotEmpty(config.get(key))) message = message.replace(reple, "["+config.get(key).toString()+"]");
			if(ObjectUtils.isNotEmpty(replaceMap.get(key))) message = message.replace(reple, "["+replaceMap.get(key).toString()+"]");
		}
		message = "["+watcherName+"] "+message;
		return message;
	}
}
