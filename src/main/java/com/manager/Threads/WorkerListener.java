package com.manager.Threads;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.sqs.model.*;
import com.manager.common.common;
import com.manager.common.init;
import com.manager.managers.EC2Manager;
import com.manager.managers.S3Manager;
import com.manager.managers.SQSManager;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import static com.manager.common.common.parse_body;
import static com.manager.common.init.*;

public class WorkerListener implements Callable {
    private SQSManager sqs = new SQSManager();
    private EC2Manager ec2 = new EC2Manager();
    private S3Manager s3 = new S3Manager();


    @Override
    public Object call() throws Exception {
        logger.info("handling workers");
        handle_workers();
        logger.info("waiting for last clients");
        terminate();
        return null;
    }

    /**
     * @throws InterruptedException in case it gets interrupted while asleep
     *                              flow:
     *                              waits for remaining pdf messages to finish and than terminates.
     */
    private void terminate() throws InterruptedException {
        logger.warning("clients = " + clients.values());
        while (!init.clients.isEmpty()) {
            Message message = null;
            while (message == null) {
                message = sqs.recieve_message(common.worker_to_manager_queue_url, null, common.manager_main_thread_consumer);
                if (init.clients.isEmpty())
                    break;
            }
            if (init.clients.isEmpty())
                break;
            String[] body_splited = parse_body(message);
            String message_type = body_splited[0];
            if (common.done_pdf_task.equals(message_type)) {
                handle_done_pdf_task(message);
                sqs.delete_message(common.worker_to_manager_queue_url, message);
            }
        }
        logger.info("terminating workers");
        terminate_instances();
        while (!has_client_manager_finished) {
            Thread.sleep(common.time_to_sleep);
        }
        logger.info("uploading log");
        s3.upload_object("/home/ec2-user/log");
        logger.info("stopping manager");
        executor.shutdown();
        ec2.stop_manager();
    }

    /**
     * @throws InterruptedException in case it gets interrupted while asleep
     *                              flow:
     *                              creates additional instances if needed (see New ClientTask)
     *                              receive message from clients and handle them according to their type
     */
    private void handle_workers() throws InterruptedException {
        while (!init.should_terminate.get()) {
            Message message = null;
            while (message == null) {
                message = sqs.recieve_message(common.worker_to_manager_queue_url, null, common.manager_main_thread_consumer);
                if (init.should_terminate.get())
                    break;
            }
            if (init.should_terminate.get())
                break;
            String message_type = parse_body(message)[0];
            logger.config("got new message with type " + message_type);
            if (message_type.equals(common.done_pdf_task)) {
                logger.config("handling done_pdf_task");
                handle_done_pdf_task(message);
                logger.config("deleting message");
                sqs.delete_message(common.worker_to_manager_queue_url, message);
            }
        }
        logger.info("terminating");
    }

    /**
     * handles the task by submitting a callable to the executor
     *
     * @param message message which contains the task
     */
    private void handle_done_pdf_task(Message message) {
        logger.info(String.format("new Done-PDF-task accepted: %s", message.getBody()));
        Callable<Boolean> callable = new ReceiveNewDonePDFTask(message);
        Future<Boolean> future = init.executor.submit(callable);
        tasks.put(future, callable);

    }

    /**
     * terminates all instances with id different than the manager-id
     */
    private void terminate_instances() {
        List<Reservation> reservationList = ec2.list();
        for (Reservation reservation : reservationList){
        for (Instance instance : reservation.getInstances()) {
            logger.config("instance found: " + instance.toString());
            if (!instance.getInstanceId().equals(ec2.manager_instance_id) && instance.getState().getName().equals("running")) {
                logger.config("terminating instance");
                ec2.terminate_instance(instance.getInstanceId());
            }
        }
        }
    }

}
