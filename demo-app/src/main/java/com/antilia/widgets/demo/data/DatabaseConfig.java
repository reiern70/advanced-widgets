/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.data;

import java.util.Map;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import com.antilia.widgets.demo.account.Accounts;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import jakarta.persistence.EntityManagerFactory;

/**
 * The Spring configuration of the {@link Database}: a pool of connections, Flyway making the
 * tables from {@code db/migration}, JPA on Hibernate, which only checks the tables against the
 * entities, and the repositories with the {@link Accounts} and the {@link Accesses} on them; the
 * accesses move to the old ones every night, see {@link Accesses#archiveOld()}.
 */
@Configuration
@EnableTransactionManagement
@EnableScheduling
@EnableJpaRepositories(basePackageClasses = IUserRepository.class)
public class DatabaseConfig
{
	@Bean(destroyMethod = "close")
	public HikariDataSource dataSource(Database.Settings settings)
	{
		HikariConfig config = new HikariConfig();
		config.setPoolName("antilia-widgets");
		config.setJdbcUrl(settings.url());
		config.setUsername(settings.user());
		config.setPassword(settings.password());
		// a small instance: a few connections are plenty for the demo
		config.setMaximumPoolSize(5);
		return new HikariDataSource(config);
	}

	@Bean
	public Flyway flyway(DataSource dataSource)
	{
		// never clean: migrations only add to the data, which upgrading keeps (see MigrationsTest)
		Flyway flyway = Flyway.configure().dataSource(dataSource).cleanDisabled(true).load();
		flyway.migrate();
		return flyway;
	}

	@Bean
	@DependsOn("flyway")
	public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource)
	{
		LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
		factory.setDataSource(dataSource);
		factory.setPackagesToScan(User.class.getPackageName());
		factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
		// Flyway owns the tables
		factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "validate"));
		return factory;
	}

	@Bean
	public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory)
	{
		return new JpaTransactionManager(entityManagerFactory);
	}

	@Bean
	public Accounts accounts(IUserRepository users)
	{
		return new Accounts(users);
	}

	@Bean
	public Accesses accesses(IAccessRepository accesses, IOldAccessRepository oldAccesses,
		JpaTransactionManager transactionManager)
	{
		return new Accesses(accesses, oldAccesses, new TransactionTemplate(transactionManager));
	}

	/**
	 * @return what runs the nightly tasks, on a thread that keeps neither the JVM nor the server
	 *         from stopping
	 */
	@Bean
	public ThreadPoolTaskScheduler taskScheduler()
	{
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setThreadNamePrefix("antilia-widgets-tasks-");
		scheduler.setDaemon(true);
		return scheduler;
	}
}
