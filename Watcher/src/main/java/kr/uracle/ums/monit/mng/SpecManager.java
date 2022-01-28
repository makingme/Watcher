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
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

@Component
public class SpecManager {
	
	private Logger log = LoggerFactory.getLogger("Developer");
	
	public final Gson gson = new GsonBuilder().setPrettyPrinting().create();

	
	public final String targetSpecPath;
	
	public static Map<String, Object> specMap = new HashMap<String, Object>();

	public SpecManager(@Value("${spec.target-path}")String targetSpecPath) throws Exception {
		this.targetSpecPath = targetSpecPath;
		loadJsonToMap(targetSpecPath);
	}
	
    public Object specMap(String target) {
        return specMap.get(target);
    }
	
	public void loadJsonToMap(String path) throws Exception {
		Resource resource=new ClassPathResource(path);
		try(	InputStream is = resource.getInputStream();
				BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")))	) {
			StringBuilder sb=new StringBuilder();
			String val=null;
			while((val=br.readLine()) !=null) {
				sb.append(val);
				val=null;
			}
			
			specMap = gson.fromJson(sb.toString(),  new TypeToken<Map<String, Object>>(){}.getType());
			log.info("============================================================");
			log.info("Target Spec {} 파일 로딩 완료", path);
			log.debug(gson.toJson(specMap));
			log.info("============================================================");
		}catch (FileNotFoundException e) {
			log.error("{} 위치에 Targer 설정 파일이 없습니다.", path);
			throw new FileNotFoundException();
		}catch (IOException e) {
			log.error("{} 파일 처리중 IO 에러 발생", path);
			throw new IOException();
		}catch(JsonSyntaxException e) {
			log.error("{} 파일 JSON 포맷 이상", path);
			throw new JsonSyntaxException("유효하지 않은 JSON 포맷");
			
		}
	}

}
