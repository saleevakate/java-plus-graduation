package ru.practicum.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean(name = "userDataSource")
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource.user")
    public DataSource userDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "eventDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.event")
    public DataSource eventDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "requestDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.request")
    public DataSource requestDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "statsDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.stats")
    public DataSource statsDataSource() {
        return DataSourceBuilder.create().build();
    }
}
