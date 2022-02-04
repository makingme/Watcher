package kr.uracle.ums.monit.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kr.uracle.ums.monit.common.Notice;
import lombok.Data;

@Data
public class Notificater extends Notice {
	
	private static final Logger log = LoggerFactory.getLogger(Notificater.class);

	public int sendNotification() {
		return 1;
	}
}
