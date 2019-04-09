package com.manager.Threads;
import com.amazonaws.services.sqs.model.*;

import com.manager.common.common;
import com.manager.common.init;
import com.manager.managers.S3Manager;
import com.manager.managers.SQSManager;
import com.manager.utils.Client;
import com.manager.utils.MissionTypes;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import static com.manager.common.common.parse_body;
import static com.manager.common.init.logger;
import static com.manager.common.init.tasks;

public class ReceiveNewClientTask implements Callable<Boolean> {
    private final String client_id;
    private final String file_key_in_s3;
    private int missions_per_instances;
    private S3Manager s3 = new S3Manager();
    private Client client;
    private Message message;
    private SQSManager sqs = new SQSManager();


    ReceiveNewClientTask(Message message){
        this.message = message;
        String[] args = parse_body(message);
        this.client_id = args[1];
        this.file_key_in_s3 = args[2];
        this.missions_per_instances = Integer.parseInt(args[3]);
        this.client = new Client(client_id);
        init.clients.put(client_id, client);
        logger.info("incremented number of clients to " + init.clients.size());
    }

    @Override
    public Boolean call() throws Exception {
        logger.info("got new client with data " + message.getBody());
        String file_path = String.format("/home/ec2-user%s", file_key_in_s3);
        s3.download_file_as_text(file_key_in_s3, file_path);
        handle_file(file_path);
        logger.info("finished handling file");
        client.setNumber_of_instances_needed(Math.max(client.getRemaining_tasks().get() / missions_per_instances, 1));
        logger.info(String.format("adding %d to needed instances", client.getNumber_of_instances_needed()));
        init.number_of_needed_instances.addAndGet(client.getNumber_of_instances_needed());
        logger.info("current number of needed instances is "+ init.number_of_needed_instances.get());
        sqs.delete_message(common.manager_queue_url, message);
        return true;
    }

    private void handle_file(String file_path) {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(file_path));
            String line = reader.readLine();
            while (line != null) {
                handle_line(line);
                // read next line
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param line: line in the document sent from the client.
     *            if line is a terminate line: setting @param should_terminate to true.
     *            else: sends an sqs message to workers and increment number of tasks for the client.
     */
    private void handle_line(String line) {
        logger.config(String.format("handling line: %s", line));
        String[] args = line.split("\t");
        MissionTypes type = MissionTypes.valueOf(args[0]);
        String file_path = args[1];
        client.getRemaining_tasks().incrementAndGet();
        Callable<Boolean> callable = new SendNewPDFTask(client_id, file_path, type);
        Future<Boolean> future = init.executor.submit(callable);
        tasks.put(future, callable);
    }
}
