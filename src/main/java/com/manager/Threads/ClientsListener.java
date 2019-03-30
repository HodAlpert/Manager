package com.manager.Threads;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.sqs.model.*;
import com.manager.common.common;
import com.manager.common.init;
import com.manager.managers.EC2Manager;
import com.manager.managers.SQSManager;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;

import static com.manager.common.common.parse_body;
import static com.manager.common.init.*;

public class ClientsListener implements Callable {
    private SQSManager sqs = new SQSManager();
    private EC2Manager ec2 = new EC2Manager();
    private String queue_url = common.manager_queue_url;

    @Override
    public Object call() throws Exception {
        logger.fine("handling clients");
        handle_clients();
        logger.info("terminating");
        while (!clients.isEmpty()) {
            create_instance_if_needed();
            Thread.sleep(common.time_to_sleep);
        }
        has_client_manager_finished = true;
        return null;
    }

    /**
     * @throws InterruptedException in case it gets interrupted while asleep
     * flow:
     *  creates additional instances if needed (see New ClientTask)
     *  receive message from clients and handle them according to their type
     */
    private void handle_clients() throws InterruptedException {
        Message message = null;
        while(!init.should_terminate.get()){
            while(message == null){
                create_instance_if_needed();
                Thread.sleep(common.time_to_sleep);
                message = sqs.recieve_message(queue_url, null, common.manager_main_thread_consumer);
            }
            String message_type = parse_body(message)[0];
            sqs.delete_message(queue_url, message);
            if (message_type.equals(common.new_task)) {
                handle_new_client_task(message);
            }
            else if (message_type.equals(common.terminate_task)) {
                should_terminate.set(true);
            }
            message = null;
        }
    }


    private void create_instance_if_needed() {
        logger.config("needed workers = " + number_of_instances+ " current workers= "+number_of_instances.get());
        if (number_of_needed_instances.get() > number_of_instances.get()){
            logger.config("creating instances");
            while (number_of_instances.get() < number_of_needed_instances.get()) {
                create_worker();
                number_of_instances.incrementAndGet();
            }
        }
    }

    private void handle_new_client_task(Message message) {
        logger.info("submitting new client");
        init.executor.submit(new ReceiveNewClientTask(message));
    }

    private void create_worker(){
        List<String> user_data = common.worker_script();
        ec2.create(user_data);
    }

    private void monitor_tasks(){
        Enumeration<Future<Boolean>> futures = tasks.keys();
        while (futures.hasMoreElements()){
            Future<Boolean> task = futures.nextElement();
                if (task.isDone()){
                    try {
                        if (task.get())
                            tasks.remove(task);
                        else
                        {
                            Callable<Boolean> callable = tasks.remove(task);
                            Future<Boolean> new_task = executor.submit(callable);
                            tasks.put(new_task, callable);
                        }
                    } catch (InterruptedException | ExecutionException exc) {
                        logger.log(Level.SEVERE, "an exception was thrown" + exc.getMessage() + exc.getCause() + Arrays.toString(exc.getStackTrace()), exc);

                    }
                }
        }
    }



}
