package kr.uracle.ums.monit.api;


import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;




@RestController
@RequestMapping("/store")
public class APIController {
	private static final Logger log = LoggerFactory.getLogger(APIController.class);
	
	private static String rootPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
	private static CacheManager cm = CacheManager.create(rootPath +"ehcache.xml");
	
	@RequestMapping(value ="/test" , method = RequestMethod.POST , consumes="application/json;charset=UTF-8", produces = "application/json;charset=UTF-8")
	public String send(@RequestBody Map<String, Object> params) {
		log.info("API TEST CALLED, params:{}", params);
		
		Cache c = cm.getCache("MEMBER_CACHE");
		String name = findByNameCache("name");
		name = findByNameCache("name");

		log.info("Cache Value:{}, Cache Size:{}", c.get(name), c.getKeys().size());
		
		return params!=null?params.toString() : "test";
	}
	
	@Cacheable(value="MEMBER_CACHE", key="#name")
	private String findByNameCache(String name) {
		return name;
	}
}
