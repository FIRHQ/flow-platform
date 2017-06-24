package com.flow.platform.dao.test;

import com.flow.platform.dao.AgentDao;
import com.flow.platform.dao.CmdDao;
import com.flow.platform.dao.CmdResultDao;
import org.junit.After;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by gy@fir.im on 23/06/2017.
 * Copyright fir.im
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {HibernateConfig.class})
@Transactional
public abstract class TestBase {

    @Autowired
    protected AgentDao agentDao;

    @Autowired
    protected CmdDao cmdDao;

    @Autowired
    protected CmdResultDao cmdResultDao;

    @After
    public void after() {
        cmdDao.baseDelete("1=1");
        cmdResultDao.baseDelete("1=1");
        agentDao.baseDelete("1=1");
    }
}
