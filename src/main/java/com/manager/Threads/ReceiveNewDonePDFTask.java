package com.manager.Threads;

import com.amazonaws.services.sqs.model.Message;
import com.manager.common.init;
import com.manager.managers.SQSManager;
import com.manager.utils.Client;
import com.manager.utils.CompletedMission;
import com.manager.utils.MissionTypes;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import static com.manager.common.common.manager_queue_url;
import static com.manager.common.common.parse_body;
import static com.manager.common.init.logger;
import static com.manager.common.init.tasks;

public class ReceiveNewDonePDFTask implements Callable<Boolean> {
    private String old_file_url;
    private String new_file_url;
    private MissionTypes type;
    private Client client;
    private SQSManager sqs = new SQSManager();

    public ReceiveNewDonePDFTask(Message message) {
        String[] body_splited = parse_body(message);
        String client_id = body_splited[1];
        this.type = MissionTypes.valueOf(body_splited[2]);
        this.old_file_url = body_splited[3];
        this.new_file_url = body_splited[4];
        this.client = init.clients.get(client_id);
    }

    @Override
    public Boolean call() throws Exception {
        logger.info(String.format("completed mission accepted for client %s. type: %s, old-url: %s, new-url: %s",
                client.getClient_id(),
                type.name(),
                old_file_url,
                new_file_url));
        CompletedMission completedMission = new CompletedMission(type, old_file_url, new_file_url);
        logger.config("adding completed mission to client: "+ completedMission);
        client.getCompletedMissions().add(completedMission);
        logger.config("decrementing remaining tasks of client");
        if (client.getRemaining_tasks().decrementAndGet() == 0){
            logger.config("wrapping it up");
            Callable<Boolean> callable = new WrapThisUp(client);
            Future<Boolean> future = init.executor.submit(callable);
            tasks.put(future, callable);
        }
        logger.config(client.getRemaining_tasks().get() + " tasks remains for client");

        return true;
    }
}
