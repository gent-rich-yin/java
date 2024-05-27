package com.example.thread;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SimpleServer implements Server {
    private final String name;

    private final ShutdownableThread thread;

    public SimpleServer(String name) {
        this.name = name;
        this.thread = new ShutdownableThread(name) {
            @Override
            public void doWork() throws Exception {
                while (true) {
                    log.info("Doing some work for you.");
                    Thread.sleep(1000L);
                }
            }
        };
    }

    @Override
    public void start() {
        log.info(name + " is starting up");
        thread.start();
        log.info(name + " has started up");
    }

    @Override
    public void shutdown() {
        log.info(name + " is shutting down");
        try {
            thread.shutdown();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log.info(name + " is down now");
    }

    public static void main(String[] args) {

        Server simpleServer = new SimpleServer("SimpleServer");
        Runtime.getRuntime().addShutdownHook(new Thread(simpleServer::shutdown));

        simpleServer.start();
    }
}
