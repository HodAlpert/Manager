package com.manager.Threads;

import com.manager.common.common;
import com.manager.managers.SQSManager;
import com.manager.utils.MissionTypes;

import java.util.concurrent.Callable;
import static com.manager.common.init.logger;

/**
 * a callable object designated to send message of a pdf file to the workers
 */
public class SendNewPDFTask implements Callable<Boolean> {
    private SQSManager sqs = new SQSManager();
    private String client_id;
    private String file_path;
    private MissionTypes type;

    public SendNewPDFTask(String client_id, String file_path, MissionTypes type) {
        this.client_id = client_id;
        this.file_path = file_path;
        this.type = type;
    }

    @Override
    public Boolean call() throws Exception {
        logger.info(String.format("sending %s message for client %s with file %s",
                type.name(),
                client_id,
                file_path));
        sqs.send_message(common.worker_queue_url,
                common.generate_new_pdf_task_message(client_id, type, file_path),
                client_id,
                common.worker_consumer);

        logger.info(String.format("sending %s message for client %s with file %s- COMPLETED",
                type.name(),
                client_id,
                file_path));
        return true;
    }
}
