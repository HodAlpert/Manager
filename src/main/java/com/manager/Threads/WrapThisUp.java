package com.manager.Threads;

import com.manager.common.common;
import com.manager.common.init;
import com.manager.managers.S3Manager;
import com.manager.managers.SQSManager;
import com.manager.utils.Client;
import com.manager.utils.CompletedMission;

import static com.manager.common.init.logger;


import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.Callable;
import java.util.logging.Level;

public class WrapThisUp implements Callable<Boolean> {
    private Client client;
    private S3Manager s3 = new S3Manager();
    private SQSManager sqs = new SQSManager();

    WrapThisUp(Client client) {
        this.client = client;
    }

    @Override
    public Boolean call() throws Exception {
        String summary_file_path = String.format("%s-summary-file.txt", client.getClient_id());
        logger.info(String.format("wrapping client %s up, creating summary file %s",
                client.getClient_id(),
                summary_file_path));
        write_summary_to_file(summary_file_path);
        logger.info(String.format("uploading file %s to S3", summary_file_path));
        String key = s3.upload_object(summary_file_path);
        logger.info("sending a message to the client");
        sqs.send_message(common.clients_queue_url,
                common.generate_new_done_task_message(key, client.getClient_id()),
                client.getClient_id(),
                common.client_consumer);
//        reducing number of instances needed
        init.number_of_needed_instances.addAndGet(-client.getNumber_of_instances_needed());
        logger.info(String.format("updated number of needed instances to %d", init.number_of_needed_instances.get()));
//        reducing number of clients
        init.clients.remove(client.getClient_id());
        logger.info(String.format("decrementing number of clients to %d", init.clients.size()));
        logger.info(String.format("finished wrapping up for client %s", client.getClient_id()));
        return true;
    }

    /**
     * writes all the data needed to the file
     *
     * @param summary_file_path path to file in which the summary should be written
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     */
    private void write_summary_to_file(String summary_file_path) throws FileNotFoundException, UnsupportedEncodingException {
        try (PrintWriter writer = new PrintWriter(summary_file_path, "UTF-8")) {
            for (CompletedMission mission : client.getCompletedMissions()) {
                String line = String.format("%s\t%s\t%s",
                        mission.getType(),
                        mission.getOld_url(),
                        mission.getNew_url());
                logger.warning(line);
                writer.println(line);
            }
        } catch (Exception exc) {
            logger.log(Level.SEVERE, "an exception aws thrown", exc);
            throw exc;
        }
    }
}
