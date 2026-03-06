package com.danapple.spicep.dao;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;

import javax.sql.DataSource;

@Configuration
class DataSources {
    @Bean("spicepDatabaseConfiguration")
    @ConfigurationProperties("spicep.database.spicep")
    DatabaseConfiguration spiecepDatabaseConfiguration() {
        return new DatabaseConfiguration();
    }

    @Bean("spicepDataSource")
    @ConfigurationProperties("spring.datasource.spicep")
    DataSource spicepDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean("spicepJdbcClient")
    @Primary
    JdbcClient spicepJdbcClient(@Qualifier("spicepDataSource") DataSource spicepDataSource,
                                       @Qualifier("spicepDatabaseConfiguration") DatabaseConfiguration databaseConfiguration) {
        FluentConfiguration flywayConfig =
                new org.flywaydb.core.api.configuration.FluentConfiguration()
                        .dataSource(spicepDataSource)
                        .locations(databaseConfiguration.getMigrationsLocation());
        Flyway flyway = new Flyway(flywayConfig);
        if (databaseConfiguration.isRepair()) {
            flyway.repair();
        }
        flyway.migrate();

        return JdbcClient.create(spicepDataSource);
    }
}


