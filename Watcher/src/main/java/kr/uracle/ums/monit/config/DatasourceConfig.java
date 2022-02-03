package kr.uracle.ums.monit.config;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.jolbox.bonecp.BoneCPDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import kr.uracle.ums.monit.utils.EncrytUtil;



@ComponentScan(basePackages = {"kr.uracle.ums.monit"},useDefaultFilters = true)
@Configuration
public class DatasourceConfig {

    private Logger log = LoggerFactory.getLogger("Developer");
    
    private final String CONFIG1 = "db1.properties";
    
    @Autowired
    EncrytUtil encrytManager;
    
    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;
    @Value("${spring.datasource.url}")
    private List<String> url;
    @Value("${spring.datasource.username}")
    private List<String> userName;
    @Value("${spring.datasource.password}")
    private List<String> password;


   
    public DataSource boneCpDataSource() {
        log.info("###[DB] dataSource load start");
        BoneCPDataSource ds = new BoneCPDataSource();
        ds.setDriverClass(driverClassName);
        ds.setJdbcUrl(url.get(0));
        ds.setUsername(userName.get(0));
        ds.setPassword(password.get(0));
        ds.setMinConnectionsPerPartition(5);
        ds.setMaxConnectionsPerPartition(5);
        ds.setIdleMaxAgeInSeconds(3600);
        ds.setConnectionTestStatement("select 1");
        ds.setIdleConnectionTestPeriodInMinutes(5);
        ds.setDisableConnectionTracking(true);
        log.info("###[DB] bonecp dataSource created");
        return ds;
    }

    
    public DataSource hikariDataSource() throws FileNotFoundException, IOException {
    	HikariConfig config = new HikariConfig(getProperties(CONFIG1));
    	HikariDataSource dataSource = new HikariDataSource(config);
    	log.info("###[DB] hikari dataSource created");
    	return dataSource;
    }
    
    @Bean(name="boneCpSqlSessionFactory")
    public SqlSessionFactory sqlSessionFactory1() throws Exception {
        String mapperSrc= "classpath:mybatis/**/*.xml";
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(boneCpDataSource());
        bean.setConfigLocation(new PathMatchingResourcePatternResolver().getResource("classpath:config/mybatis-config.xml"));
        bean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources(mapperSrc));
        log.info("###[DB] bonecp sqlSessionFactory created");
        return bean.getObject();
    }
    
    @Bean(name="hikariCpSqlSessionFactory")
    public SqlSessionFactory sqlSessionFactory2() throws Exception {
        String mapperSrc= "classpath:mybatis/**/*.xml";
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(hikariDataSource());
        bean.setConfigLocation(new PathMatchingResourcePatternResolver().getResource("classpath:config/mybatis-config.xml"));
        bean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources(mapperSrc));
        log.info("###[DB] hikari sqlSessionFactory created");
        return bean.getObject();
    }

    @Bean(name="bonSessionTemplate")
    public SqlSessionTemplate sqlSession1(@Qualifier("boneCpSqlSessionFactory") SqlSessionFactory sqlSessionFactory) throws Exception {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
    
    @Bean(name="hikariSessionTemplate")
    public SqlSessionTemplate sqlSession2(@Qualifier("hikariCpSqlSessionFactory") SqlSessionFactory sqlSessionFactory) throws Exception {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    
    private Properties getProperties(String path) throws FileNotFoundException, IOException {
    	Properties pro = new Properties();
    	String rootPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
    	String appConfigPath = rootPath + CONFIG1;
    	pro.load(new FileInputStream(appConfigPath));
    	
    	for(Entry<Object, Object> s:pro.entrySet()) {
    		String value = s.getValue().toString();
        	if(value.startsWith("$ENC:")) pro.setProperty(s.getKey().toString(), encrytManager.decrypt(value.replace("$ENC:", "")));
    	}
    	return pro;
    }
}
