package com.flow.platform.agent.test;

import com.flow.platform.agent.CmdManager;
import com.flow.platform.agent.Config;
import com.flow.platform.cmd.ProcListener;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdResult;
import com.flow.platform.domain.CmdType;
import org.junit.*;
import org.junit.runners.MethodSorters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by gy@fir.im on 16/05/2017.
 * Copyright fir.im
 */
@FixMethodOrder(value = MethodSorters.NAME_ASCENDING)
public class CmdManagerTest extends TestBase {

    private CmdManager cmdManager = CmdManager.getInstance();

    private static String resourcePath;

    @BeforeClass
    public static void beforeClass() throws IOException {
        System.setProperty(Config.PROP_CONCURRENT_THREAD, "2");
        System.setProperty(Config.PROP_IS_DEBUG, "true");
        System.setProperty(Config.PROP_UPLOAD_AGENT_LOG, "false");
        System.setProperty(Config.PROP_REPORT_STATUS, "false");

        ClassLoader classLoader = CmdManagerTest.class.getClassLoader();
        resourcePath = classLoader.getResource("test.sh").getFile();
        Runtime.getRuntime().exec("chmod +x " + resourcePath);
    }

    @Before
    public void beforeEach() {
        cmdManager.getExtraProcEventListeners().clear();
        cmdManager.getRunning().clear();
        cmdManager.getFinished().clear();
        cmdManager.getRejected().clear();
    }

    @Test
    public void should_be_singleton() {
        Assert.assertEquals(cmdManager, CmdManager.getInstance());
    }

    @Test
    public void should_has_cmd_log() throws Throwable {
        Cmd cmd = new Cmd("zone1", "agent1", CmdType.RUN_SHELL, resourcePath);
        cmd.setId(UUID.randomUUID().toString());
        cmdManager.execute(cmd);

        ThreadPoolExecutor cmdExecutor = cmdManager.getCmdExecutor();
        cmdExecutor.shutdown();
        cmdExecutor.awaitTermination(60, TimeUnit.SECONDS);

//        Assert.assertTrue(Files.exists(Paths.get(TEMP_LOG_DIR.toString(), cmd.getId() + ".out.zip")));
//        Assert.assertTrue(Files.exists(Paths.get(TEMP_LOG_DIR.toString(), cmd.getId() + ".err.zip")));
    }

    @Test
    public void running_process_should_be_recorded() throws InterruptedException {
        // given:
        CountDownLatch startLatch = new CountDownLatch(2);
        CountDownLatch finishLatch = new CountDownLatch(2);
        Assert.assertEquals(2, Config.concurrentThreadNum());

        cmdManager.getExtraProcEventListeners().add(new ProcListener() {
            @Override
            public void onStarted(CmdResult result) {
                startLatch.countDown();
            }

            @Override
            public void onExecuted(CmdResult result) {

            }

            @Override
            public void onLogged(CmdResult result) {
                finishLatch.countDown();
            }

            @Override
            public void onException(CmdResult result) {

            }
        });

        // create mock cmd
        String content = String.format("source %s", resourcePath);

        Cmd cmd1 = new Cmd("zone1", "agent1", CmdType.RUN_SHELL, content);
        cmd1.setOutputEnvFilter("FLOW_AGENT");
        cmd1.setId(UUID.randomUUID().toString());

        Cmd cmd2 = new Cmd("zone1", "agent1", CmdType.RUN_SHELL, content);
        cmd2.setOutputEnvFilter("FLOW_AGENT");
        cmd2.setId(UUID.randomUUID().toString());

        Cmd cmd3 = new Cmd("zone1", "agent1", CmdType.RUN_SHELL, content);
        cmd3.setOutputEnvFilter("FLOW_AGENT");
        cmd3.setId(UUID.randomUUID().toString());

        Cmd cmd4 = new Cmd("zone1", "agent1", CmdType.RUN_SHELL, content);
        cmd4.setOutputEnvFilter("FLOW_AGENT");
        cmd4.setId(UUID.randomUUID().toString());

        // when: execute four command by thread
        cmdManager.execute(cmd1);
        cmdManager.execute(cmd2);
        cmdManager.execute(cmd3);
        cmdManager.execute(cmd4);
        startLatch.await();

        // then: check num of running proc and reject cmd
        Map<Cmd, CmdResult> runningCmd = cmdManager.getRunning();
        Assert.assertEquals(2, runningCmd.size());
        Assert.assertTrue(runningCmd.containsKey(cmd1));
        Assert.assertTrue(runningCmd.containsKey(cmd2));

        Map<Cmd, CmdResult> rejectedCmd = cmdManager.getRejected();
        Assert.assertEquals(2, rejectedCmd.size());
        Assert.assertTrue(rejectedCmd.containsKey(cmd3));
        Assert.assertTrue(rejectedCmd.containsKey(cmd4));

        // when: wait two command been finished
        finishLatch.await();

        // then: check
        Map<Cmd, CmdResult> finishedCmd = cmdManager.getFinished();
        Assert.assertEquals(2, finishedCmd.size());

        for (Map.Entry<Cmd, CmdResult> entry : finishedCmd.entrySet()) {
            CmdResult r = entry.getValue();
            Assert.assertEquals(new Integer(0), r.getExitValue());
            Assert.assertEquals(true, r.getDuration() >= 2);
            Assert.assertEquals(true, r.getTotalDuration() >= 2);
            Assert.assertEquals(0, r.getExceptions().size());
            Assert.assertEquals(2, r.getOutput().size());
        }
    }

    @Test
    public void should_be_correct_status_for_killed_process() throws Throwable {
        // given
        Cmd cmd = new Cmd("zone1", "agent1", CmdType.RUN_SHELL, resourcePath);
        cmd.setId(UUID.randomUUID().toString());

        CountDownLatch startLatch = new CountDownLatch(1);
        cmdManager.getExtraProcEventListeners().add(new ProcListener() {
            @Override
            public void onStarted(CmdResult result) {
                startLatch.countDown();
            }

            @Override
            public void onLogged(CmdResult result) {

            }

            @Override
            public void onExecuted(CmdResult result) {

            }

            @Override
            public void onException(CmdResult result) {

            }
        });

        // when: start and kill task immediately
        cmdManager.execute(cmd);
        startLatch.await();

        cmdManager.kill();

        // then: check CmdResult status
        Map<Cmd, CmdResult> finished = cmdManager.getFinished();
        Assert.assertEquals(1, finished.size());

        CmdResult result = finished.get(cmd);
        Assert.assertNotNull(result.getDuration());
        Assert.assertNotNull(result.getExecutedTime());
        Assert.assertNotNull(result.getExitValue());
        Assert.assertEquals(CmdResult.EXIT_VALUE_FOR_KILL, result.getExitValue());
    }
}
