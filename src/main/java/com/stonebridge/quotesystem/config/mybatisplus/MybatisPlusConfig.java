package com.stonebridge.quotesystem.config.mybatisplus;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.plugin.Interceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * MyBatis-Plus 插件配置。
 *
 * 注意：
 * 1. 这里不要再写 @MapperScan；
 * 2. MapperScan 分别放在 BusinessDataSourceConfig 和 SystemDataSourceConfig；
 * 3. 两个数据源分别注入自己的分页插件。
 */
@Configuration
@EnableTransactionManagement
public class MybatisPlusConfig {

    @Bean(name = "businessMybatisPlusInterceptor")
    public Interceptor businessMybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    @Bean(name = "systemMybatisPlusInterceptor")
    public Interceptor systemMybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}