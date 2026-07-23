package com.stonebridge.quotesystem.config.datasource;

import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.JdbcType;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@MapperScan(
        basePackages = "com.stonebridge.quotesystem.system.mapper",
        sqlSessionFactoryRef = "systemSqlSessionFactory",
        sqlSessionTemplateRef = "systemSqlSessionTemplate"
)
public class SystemDataSourceConfig {

    @Bean(name = "systemDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.druid.system")
    public DataSource systemDataSource() {
        return new DruidDataSource();
    }

    @Bean(name = "systemSqlSessionFactory")
    public SqlSessionFactory systemSqlSessionFactory(
            @Qualifier("systemDataSource") DataSource systemDataSource,
            @Qualifier("systemMybatisPlusInterceptor") Interceptor systemMybatisPlusInterceptor
    ) throws Exception {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(systemDataSource);

        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setJdbcTypeForNull(JdbcType.NULL);
        factoryBean.setConfiguration(configuration);
        factoryBean.setPlugins(systemMybatisPlusInterceptor);
        return factoryBean.getObject();
    }

    @Bean(name = "systemSqlSessionTemplate")
    public SqlSessionTemplate systemSqlSessionTemplate(
            @Qualifier("systemSqlSessionFactory") SqlSessionFactory systemSqlSessionFactory
    ) {
        return new SqlSessionTemplate(systemSqlSessionFactory);
    }
}
