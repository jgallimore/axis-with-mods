/*
 * Copyright 2001-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.axis.transport.http;

import org.apache.axis.components.logger.LogFactory;
import org.apache.axis.components.threadpool.ThreadPool;
import org.apache.axis.server.AxisServer;
import org.apache.axis.session.Session;
import org.apache.axis.session.SimpleSession;
import org.apache.axis.utils.Messages;
import org.apache.axis.utils.Options;
import org.apache.axis.utils.NetworkUtils;
import org.apache.axis.collections.LRUMap;
import org.apache.axis.EngineConfiguration;
import org.apache.axis.management.ServiceAdmin;
import org.apache.axis.configuration.EngineConfigurationFactoryFinder;
import org.apache.commons.logging.Log;

import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.io.IOException;
import java.io.File;

/**
 * This is a simple implementation of an HTTP server for processing
 * SOAP requests via Apache's xml-axis.  This is not intended for production
 * use.  Its intended uses are for demos, debugging, and performance
 * profiling.
 *
 * Note this classes uses static objects to provide a thread pool, so you should
 * not use multiple instances of this class in the same JVM/classloader unless
 * you want bad things to happen at shutdown.
 * @author Sam Ruby (ruby@us.ibm.com)
 * @author Rob Jellinghaus (robj@unrealities.com)
 * @author Alireza Taherkordi (a_taherkordi@users.sourceforge.net)
 */
public class SimpleAxisServer implements Runnable {
    protected static Log log =
            LogFactory.getLog(SimpleAxisServer.class.getName());

    // session state.
    // This table maps session keys (random numbers) to SimpleAxisSession objects.
    //
    // There is a simple LRU based session cleanup mechanism, but if clients are not
    // passing cookies, then a new session will be created for *every* request.
    private Map sessions;
    //Maximum capacity of the LRU Map used for session cleanup
    private int maxSessions;
    public static final int MAX_SESSIONS_DEFAULT = 100;

    /**
     * get the thread pool
     * @return
     */
    public static ThreadPool getPool() {
        return pool;
    }

    /**
     * pool of threads
     */
    private static ThreadPool pool;

    /** Are we doing threads?
     * */
    private static boolean doThreads = true;

    /* Are we doing sessions?
     Set this to false if you don't want any session overhead.
     */
    private static boolean doSessions = true;

    /**
     * create a server with the default threads and sessions.
     */
    public SimpleAxisServer() {
        this(ThreadPool.DEFAULT_MAX_THREADS);
    }

    /**
     * Create a server with a configurable pool side; sessions set to the default
     * limit
     * @param maxPoolSize maximum thread pool size
     */
    public SimpleAxisServer(int maxPoolSize) {
        this(maxPoolSize, MAX_SESSIONS_DEFAULT);
    }
    
    /**
     * Constructor
     * @param maxPoolSize max number of threads
     * @param maxSessions maximum sessions
     */
    public SimpleAxisServer(int maxPoolSize, int maxSessions) {
        this.maxSessions = maxSessions;
        sessions = new LRUMap(maxSessions);
        pool = new ThreadPool(maxPoolSize);
    }


    /**
     * stop the server if not already told to.
     * @throws Throwable
     */
    protected void finalize() throws Throwable {
        stop();
        super.finalize();
    }

    /**
     * get max session count
     * @return
     */
    public int getMaxSessions() {
        return maxSessions;
    }

    /**
     * Resize the session map
     * @param maxSessions maximum sessions
     */
    public void setMaxSessions(int maxSessions) {
        this.maxSessions = maxSessions;
        ((LRUMap)sessions).setMaximumSize(maxSessions);
    }
     //---------------------------------------------------

    protected boolean isSessionUsed() {
        return doSessions;
    }

    /**
     * turn threading on or off. This sets a static value
     * @param value
     */
    public void setDoThreads(boolean value) {
        doThreads = value ;
    }

    public boolean getDoThreads() {
        return doThreads ;
    }

    public EngineConfiguration getMyConfig() {
        return myConfig;
    }

    public void setMyConfig(EngineConfiguration myConfig) {
        this.myConfig = myConfig;
    }

    /**
     * demand create a session if there is not already one for the string
     * @param cooky
     * @return a session.
     */
    protected Session createSession(String cooky) {

        // is there a session already?
        Session session = null;
        if (sessions.containsKey(cooky)) {
            session = (Session) sessions.get(cooky);
        } else {
            // no session for this cooky, bummer
            session = new SimpleSession();

            // ADD CLEANUP LOGIC HERE if needed
            sessions.put(cooky, session);
        }
        return session;
    }

    // What is our current session index?
    // This is a monotonically increasing, non-thread-safe integer
    // (thread safety not considered crucial here)
    public static int sessionIndex = 0;

    // Axis server (shared between instances)
    private static AxisServer myAxisServer = null;

    private EngineConfiguration myConfig = null;

    /**
     * demand create an axis server; return an existing one if one exists.
     * The configuration for the axis server is derived from #myConfig if not null,
     * the default config otherwise.
     * @return
     */
    public synchronized AxisServer getAxisServer() {
        if (myAxisServer == null) {
            if (myConfig == null) {
                myConfig = EngineConfigurationFactoryFinder.newFactory().getServerEngineConfig();
            }
            myAxisServer = new AxisServer(myConfig);
            ServiceAdmin.setEngine(myAxisServer, NetworkUtils.getLocalHostname() + "@" + serverSocket.getLocalPort());
        }
        return myAxisServer;
    }

    /**
    are we stopped?
    latch to true if stop() is called
     */
    private boolean stopped = false;

    /**
     * Accept requests from a given TCP port and send them through the
     * Axis engine for processing.
     */
    public void run() {
        log.info(Messages.getMessage("start01", "SimpleAxisServer",
                new Integer(getServerSocket().getLocalPort()).toString(),getCurrentDirectory()));

        // Accept and process requests from the socket
        while (!stopped) {
            Socket socket = null;
            try {
                socket = serverSocket.accept();
            } catch (java.io.InterruptedIOException iie) {
            } catch (Exception e) {
                log.debug(Messages.getMessage("exception00"), e);
                break;
            }
            if (socket != null) {
                SimpleAxisWorker worker = new SimpleAxisWorker(this, socket);
                if (doThreads) {
                    pool.addWorker(worker);
                } else {
                    worker.run();
                }
            }
        }
        log.info(Messages.getMessage("quit00", "SimpleAxisServer"));
    }

    /**
     * Gets the current directory
     * @return current directory
     */ 
    private String getCurrentDirectory() {
        return System.getProperty("user.dir");
    }

    /**
     * per thread socket information
     */

    private ServerSocket serverSocket;

    /**
     * Obtain the serverSocket that that SimpleAxisServer is listening on.
     */
    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    /**
     * Set the serverSocket this server should listen on.
     * (note : changing this will not affect a running server, but if you
     *  stop() and then start() the server, the new socket will be used).
     */
    public void setServerSocket(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    /**
     * Start this server.
     *
     * Spawns a worker thread to listen for HTTP requests.
     *
     * @param daemon a boolean indicating if the thread should be a daemon.
     */
    public void start(boolean daemon) throws Exception {
        stopped=false;
        if (doThreads) {
            Thread thread = new Thread(this);
            thread.setDaemon(daemon);
            thread.start();
        } else {
            run();
        }
    }

    /**
     * Start this server as a NON-daemon.
     */
    public void start() throws Exception {
        start(false);
    }

    /**
     * Stop this server. Can be called safely if the system is already stopped,
     * or if it was never started.
     *
     * This will interrupt any pending accept().
     */
    public void stop() {
        //recognise use before we are live
        if(stopped ) {
            return;
        }
        /*
         * Close the server socket cleanly, but avoid fresh accepts while
         * the socket is closing.
         */
        stopped = true;

        try {
            if(serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            log.info(Messages.getMessage("exception00"), e);
        } finally {
            serverSocket=null;
        }

        log.info(Messages.getMessage("quit00", "SimpleAxisServer"));

        //shut down the pool
        pool.shutdown();
    }

    /**
     * Server process.
     */
    public static void main(String args[]) {

        Options opts = null;
        try {
            opts = new Options(args);
        } catch (MalformedURLException e) {
            log.error(Messages.getMessage("malformedURLException00"), e);
            return;
        }

        String maxPoolSize = opts.isValueSet('t');
        if (maxPoolSize==null) maxPoolSize = ThreadPool.DEFAULT_MAX_THREADS + "";

        String maxSessions = opts.isValueSet('m');
        if (maxSessions==null) maxSessions = MAX_SESSIONS_DEFAULT + "";

        SimpleAxisServer sas = new SimpleAxisServer(Integer.parseInt(maxPoolSize),
                                                        Integer.parseInt(maxSessions));

        try {
            doThreads = (opts.isFlagSet('t') > 0);

            int port = opts.getPort();
            ServerSocket ss = null;
            // Try five times
            final int retries = 5;
            for (int i = 0; i < retries; i++) {
                try {
                    ss = new ServerSocket(port);
                    break;
                } catch (java.net.BindException be){
                    log.debug(Messages.getMessage("exception00"), be);
                    if (i < (retries-1)) {
                        // At 3 second intervals.
                        Thread.sleep(3000);
                    } else {
                        throw new Exception(Messages.getMessage("unableToStartServer00",
                                           Integer.toString(port)));
                    }
                }
            }
            sas.setServerSocket(ss);
            sas.start();
        } catch (Exception e) {
            log.error(Messages.getMessage("exception00"), e);
            return;
        }
    }
}
