package kr.uracle.ums.monit.config;

import java.util.List;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.jolbox.bonecp.BoneCPDataSource;


@ComponentScan(basePackages = {"kr.uracle.ums.monit"},useDefaultFilters = true)
@Configuration
public class DatasourceConfig {

    private Logger log = LoggerFactory.getLogger("Developer");
    
    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;
    @Value("${spring.datasource.url}")
    private List<String> url;
    @Value("${spring.datasource.username}")
    private List<String> userName;
    @Value("${spring.datasource.password}")
    private List<String> password;


    @Bean(name="dataSorce")
    public DataSource dataSource() {
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
        log.info("###[DB] dataSource completed");
        return ds;
    }

    @Bean(name="sqlSessionFactory")
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        log.info("###[DB] sqlSessionFactory start");
        String mapperSrc= "classpath:mybatis/**/*.xml";
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setConfigLocation(new PathMatchingResourcePatternResolver().getResource("classpath:config/mybatis-config.xml"));
        bean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources(mapperSrc));
        log.info("###[DB] sqlSessionFactory completed");
        return bean.getObject();
    }

    @Bean(name="sessionTemplate")
    public SqlSessionTemplate sqlSession(DataSource dataSource) throws Exception {
        return new SqlSessionTemplate(sqlSessionFactory(dataSource));
    }


}
