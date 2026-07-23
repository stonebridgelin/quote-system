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
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
@MapperScan(
        basePackages = "com.stonebridge.quotesystem.business.mapper",
        sqlSessionFactoryRef = "businessSqlSessionFactory",
        sqlSessionTemplateRef = "businessSqlSessionTemplate"
)
public class BusinessDataSourceConfig {

    @Primary
    @Bean(name = "businessDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.druid.business")
    public DataSource businessDataSource() {
        return new DruidDataSource();
    }

    @Primary
    @Bean(name = "businessSqlSessionFactory")
    public SqlSessionFactory businessSqlSessionFactory(
            @Qualifier("businessDataSource") DataSource businessDataSource,
            @Qualifier("businessMybatisPlusInterceptor") Interceptor businessMybatisPlusInterceptor
    ) throws Exception {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(businessDataSource);

        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setJdbcTypeForNull(JdbcType.NULL);
        factoryBean.setConfiguration(configuration);
        factoryBean.setPlugins(businessMybatisPlusInterceptor);
        return factoryBean.getObject();
    }

    @Primary
    @Bean(name = "businessSqlSessionTemplate")
    public SqlSessionTemplate businessSqlSessionTemplate(
            @Qualifier("businessSqlSessionFactory") SqlSessionFactory businessSqlSessionFactory
    ) {
        return new SqlSessionTemplate(businessSqlSessionFactory);
    }
}
