package kr.uracle.ums.monit.mng;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NotificationManager {
	@Autowired
	SpecManager specManager;
}
