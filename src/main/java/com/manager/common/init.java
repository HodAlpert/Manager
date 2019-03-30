package com.manager.common;

import com.manager.utils.Client;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class init {
    public static String log_name = "my_logger";
    public static String bucket_name = "123456789-hod-application-bucket";
    public static Logger logger = Logger.getLogger(log_name);
    public static ConcurrentHashMap<String, Client> clients = new ConcurrentHashMap<String, Client>();
    public static ConcurrentHashMap<Future<Boolean>, Callable<Boolean>> tasks = new ConcurrentHashMap<>();
    public static AtomicInteger number_of_instances = new AtomicInteger(0);
    public static AtomicInteger number_of_needed_instances = new AtomicInteger(0);
    public static ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    public static AtomicBoolean should_terminate = new AtomicBoolean(false);
    public static volatile boolean has_client_manager_finished = false;


    public static void main(){
        FileHandler handler = null;
        try {
            handler = new FileHandler("/home/ec2-user/log");
            handler.setLevel(Level.FINER);
            handler.setFormatter(new LogFormatter());
            logger.addHandler(handler);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "an exception aws thrown", e);
        }
        logger.setLevel(Level.FINE);
        logger.info("STARTING MANAGER");
//        logger.setUseParentHandlers(false);

    }
}
