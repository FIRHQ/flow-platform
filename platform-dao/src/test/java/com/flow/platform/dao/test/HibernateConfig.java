package com.flow.platform.dao.test;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Created by gy@fir.im on 24/06/2017.
 * Copyright fir.im
 */
@Configuration
@ComponentScan({"com.flow.platform.dao"})
@ImportResource({"classpath:hibernate-mysql.config.xml"})
@EnableTransactionManagement
public class HibernateConfig {

}
