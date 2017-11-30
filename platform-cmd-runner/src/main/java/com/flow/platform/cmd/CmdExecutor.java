/*
 * Copyright 2017 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flow.platform.cmd;

import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdResult;
import com.flow.platform.util.DateUtil;
import com.flow.platform.util.Logger;
import com.flow.platform.util.SystemUtil;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author gy@fir.im
 */
public final class CmdExecutor {

    /**
     * Class for null proc listener
     */
    private final class NullProcListener implements ProcListener {

        @Override
        public void onStarted(CmdResult result) {

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
    }

    /**
     * Class for null log listener
     */
    private final class NullLogListener implements LogListener {

        @Override
        public void onLog(Log log) {

        }

        @Override
        public void onFinish() {

        }
    }

    private final static Logger LOGGER = new Logger(CmdExecutor.class);

    private final static File DEFAULT_WORKING_DIR = new File(
        System.getProperty("user.home", System.getProperty("user.dir")));

    // process timeout in seconds, default is 2 hour
    private final static Integer DEFAULT_TIMEOUT = new Integer(3600 * 2);

    // 1 mb buffer for std reader
    private final static int DEFAULT_BUFFER_SIZE = 1024 * 1024 * 1;

    private final static int DEFAULT_LOGGING_WAITTING_SECONDS = 30;

    private final static int DEFAULT_SHUTDING_WATTING_SECONDS = 30;

    private final ConcurrentLinkedQueue<Log> loggingQueue = new ConcurrentLinkedQueue<>();

    private final String endTerm = String.format("=====EOF-%s=====", UUID.randomUUID());

    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(4, 4, 0L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(),
        new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = Executors.defaultThreadFactory().newThread(r);
                t.setDaemon(true);
                return t;
            }
        }
    );

    private final CountDownLatch stdThreadCountDown = new CountDownLatch(2);
    private final CountDownLatch logThreadCountDown = new CountDownLatch(1);

    private ProcessBuilder pBuilder;
    private ProcListener procListener = new NullProcListener();
    private LogListener logListener = new NullLogListener();
    private List<String> cmdList;
    private String outputEnvFilter;
    private CmdResult outputResult;
    private Integer timeout;

    /**
     * @param procListener nullable
     * @param logListener nullable
     * @param inputs nullable input env
     * @param workingDir nullable, for set cmd working directory, default is user.dir
     * @param outputEnvFilter nullable, for start_with env to cmd result output
     * @param cmd exec cmd
     */
    public CmdExecutor(final ProcListener procListener,
                       final LogListener logListener,
                       final Map<String, String> inputs,
                       final String workingDir,
                       final String outputEnvFilter,
                       final Integer timeout,
                       final List<String> cmds) {

        if (procListener != null) {
            this.procListener = procListener;
        }

        if (logListener != null) {
            this.logListener = logListener;
        }

        this.outputEnvFilter = outputEnvFilter;

        cmds.add(0, "set -e"); // exit bash when command error

        this.cmdList = cmds;
        this.pBuilder = new ProcessBuilder("/bin/bash").directory(DEFAULT_WORKING_DIR);

        // check and init working dir
        if (workingDir != null) {
            Path dir = SystemUtil.replacePathWithEnv(workingDir);

            if (!Files.exists(dir)) {
                try {
                    Files.createDirectories(dir);
                } catch (IOException e) {
                    throw new IllegalArgumentException("Unable to create working dir: " + dir);
                }
            }

            this.pBuilder.directory(dir.toFile());
        }

        // init inputs env
        if (inputs != null && inputs.size() > 0) {
            this.pBuilder.environment().putAll(inputs);
        }

        // init timeout
        if (timeout == null) {
            this.timeout = DEFAULT_TIMEOUT;
        } else {
            this.timeout = timeout;
        }
    }

    public CmdExecutor(final String workingDir, final List<String> cmds) {
        this(null, null, null, workingDir, null, null, cmds);
    }

    public void setProcListener(ProcListener procListener) {
        this.procListener = procListener;
    }

    public void setLogListener(LogListener logListener) {
        this.logListener = logListener;
    }


    public CmdResult run() {
        outputResult = new CmdResult();

        long startTime = System.currentTimeMillis();
        outputResult.setStartTime(DateUtil.now());

        try {
            Process p = pBuilder.start();
            outputResult.setProcessId(getPid(p));
            outputResult.setProcess(p);

            procListener.onStarted(outputResult);

            // thread to send cmd list to bash
            executor.execute(createCmdListExec(p.getOutputStream(), cmdList));

            // thread to read stdout and stderr stream and put log to logging queue
            executor.execute(createStdStreamReader(Log.Type.STDOUT, p.getInputStream()));
            executor.execute(createStdStreamReader(Log.Type.STDERR, p.getErrorStream()));

            // thread to make consume logging queue
            executor.execute(createCmdLoggingReader());

            // wait for max process timeout
            if (p.waitFor(timeout.longValue(), TimeUnit.SECONDS)) {
                outputResult.setExitValue(p.exitValue());
            } else {
                outputResult.setExitValue(CmdResult.EXIT_VALUE_FOR_TIMEOUT);
            }

            outputResult.setExecutedTime(DateUtil.now());
            procListener.onExecuted(outputResult);
            LOGGER.trace("====== 1. Process executed : %s ======", outputResult.getExitValue());

            // wait for log thread with max 30 seconds to continue upload log
            logThreadCountDown.await(DEFAULT_LOGGING_WAITTING_SECONDS, TimeUnit.SECONDS);
            executor.shutdown();

            // try to shutdown all threads with max 30 seconds waitting time
            if (!executor.awaitTermination(DEFAULT_SHUTDING_WATTING_SECONDS, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }

            outputResult.setFinishTime(DateUtil.now());
            procListener.onLogged(outputResult);
            LOGGER.trace("====== 2. Logging executed ======");

        } catch (InterruptedException ignore) {
            LOGGER.warn(ignore.getMessage());
        } catch (Throwable e) {
            outputResult.getExceptions().add(e);
            outputResult.setFinishTime(DateUtil.now());
            procListener.onException(outputResult);
            LOGGER.warn(e.getMessage());
        } finally {
            LOGGER.trace("====== 3. Process Done ======");
        }

        return outputResult;
    }

    /**
     * Get process id
     */
    private int getPid(Process process) {
        try {
            Class<?> cProcessImpl = process.getClass();
            Field fPid = cProcessImpl.getDeclaredField("pid");
            if (!fPid.isAccessible()) {
                fPid.setAccessible(true);
            }
            return fPid.getInt(process);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Make runnable to exec each cmd
     */
    private Runnable createCmdListExec(final OutputStream outputStream, final List<String> cmdList) {

        return () -> {
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
                for (String cmd : cmdList) {
                    writer.write(cmd + Cmd.NEW_LINE);
                    writer.flush();
                }

                // find env and set to result output if output filter is not null or empty
                if (!Strings.isNullOrEmpty(outputEnvFilter)) {
                    writer.write(String.format("echo %s" + Cmd.NEW_LINE, endTerm));
                    writer.write("env" + Cmd.NEW_LINE);
                    writer.flush();
                }

            } catch (IOException e) {
                LOGGER.warn("Exception on write cmd: " + e.getMessage());
            }
        };
    }

    private Runnable createCmdLoggingReader() {
        return () -> {
            try {
                while (true) {
                    if (stdThreadCountDown.getCount() == 0 && loggingQueue.size() == 0) {
                        break;
                    }

                    Log log = loggingQueue.poll();
                    if (log == null) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException ignored) {
                        }
                    } else {
                        logListener.onLog(log);
                    }
                }
            } finally {
                logListener.onFinish();
                logThreadCountDown.countDown();
                LOGGER.trace(" ===== Logging Reader Thread Finish =====");
            }
        };
    }

    private Runnable createStdStreamReader(final Log.Type type, final InputStream is) {
        return () -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is), DEFAULT_BUFFER_SIZE)) {
                String line;
                Integer count = 0;
                while ((line = reader.readLine()) != null) {
                    if (Objects.equals(line, endTerm)) {
                        if (outputResult != null) {
                            readEnv(reader, outputResult.getOutput(), outputEnvFilter);
                        }
                        break;
                    }
                    count += 1;
                    loggingQueue.add(new Log(type, line, count));
                }
            } catch (IOException ignore) {

            } finally {
                stdThreadCountDown.countDown();
                LOGGER.trace(" ===== %s Stream Reader Thread Finish =====", type);
            }
        };
    }

    /**
     * Start when find log match 'endTerm', and load all env,
     * put env item which match 'start with filter' to CmdResult.output map
     */
    private void readEnv(final BufferedReader reader,
                         final Map<String, String> output,
                         final String filter) throws IOException {
        String line;
        String currentKey = null;
        StringBuilder value = null;

        while ((line = reader.readLine()) != null) {
            int index = line.indexOf('=');

            // reset value builder and current key
            if (index != -1 && !line.startsWith(filter)) {
                if (value != null && currentKey != null) {
                    output.put(currentKey, value.toString());
                }

                currentKey = null;
                value = null;
                continue;
            }

            if (line.startsWith(filter)) {

                // put previous env to output and reset
                if (value != null && currentKey != null) {
                    output.put(currentKey, value.toString());
                    value = null;
                    currentKey = null;
                }

                value = new StringBuilder();
                currentKey = line.substring(0, index);
                value.append(line.substring(index + 1));
                continue;
            }

            if (index == -1 && value != null) {
                value.append(Cmd.NEW_LINE + line);
            }
        }
    }
}
