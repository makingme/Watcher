package kr.uracle.ums.monit.mng;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

@Order(1)
@Component
public class SpecManager implements ApplicationRunner{
	
	private Logger log = LoggerFactory.getLogger("Developer");
	
	public final Gson gson = new GsonBuilder().setPrettyPrinting().create();

	
	public final String targetSpecPath;
	
	public static Map<String, Object> specMap = new HashMap<String, Object>();

	public SpecManager(@Value("${spec.target-path}")String targetSpecPath) throws Exception {
		this.targetSpecPath = targetSpecPath;
	}
	
    public Object specMap(String target) {
        return specMap.get(target);
    }
    
    public Map<String, Object> getSpecMap() {
        return specMap;
    }
	
	public void loadJsonToMap(String path) throws Exception {
		Resource resource=new ClassPathResource(path);
		try(	InputStream is = resource.getInputStream();
				BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")))	) {
			StringBuilder sb=new StringBuilder();
			String val=null;
			while((val=br.readLine()) !=null) sb.append(val);
			
			specMap = gson.fromJson(sb.toString(),  new TypeToken<Map<String, Object>>(){}.getType());
			log.info("============================================================");
			log.info("Target Spec {} ?????? ?????? ??????", path);
			//log.debug(gson.toJson(specMap));
			log.info("============================================================");
		}catch (FileNotFoundException e) {
			log.error("{} ????????? Targer ?????? ????????? ????????????.", path);
			throw new FileNotFoundException();
		}catch (IOException e) {
			log.error("{} ?????? ????????? IO ?????? ??????", path);
			throw new IOException();
		}catch(JsonSyntaxException e) {
			log.error("{} ?????? JSON ?????? ??????", path);
			throw new JsonSyntaxException("???????????? ?????? JSON ??????");
			
		}
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		loadJsonToMap(targetSpecPath);
	}

}
