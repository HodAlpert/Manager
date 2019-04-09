package com.manager.common;

import com.amazonaws.services.sqs.model.Message;
import com.manager.managers.EC2Manager;
import com.manager.utils.CompletedMission;
import com.manager.utils.MissionTypes;

import java.util.List;

public class common {
    private static List<String> initialization_script(){
        List<String> script = EC2Manager.init_script();
        script.add("cd /home/ec2-user/");
//        installing git
//        script.add("sudo yum -y install git");
//        installing maven
//        script.add("sudo wget http://repos.fedorapeople.org/repos/dchen/apache-maven/epel-apache-maven.repo -O /etc/yum.repos.d/epel-apache-maven.repo");
//        script.add("sudo sed -i s/\\$releasever/6/g /etc/yum.repos.d/epel-apache-maven.repo");
//        script.add("sudo yum install -y apache-maven");
//        creating log file
        return script;
    }

    public static List<String> worker_script(){
        List<String> script = initialization_script();
//        cloning git repo
        script.add("sudo git clone https://github.com/hod246/worker.git");
        script.add("cd worker");
//        building maven project
        script.add("sudo mvn compile package");
//        running the manager script
        script.add("sudo java -jar target/worker-1.0-SNAPSHOT.jar");
    return script;
    }
//    SQS message types
    public static final String new_task = "new task";
    public static final String done_task = "done task";
    public static final String new_pdf_task = "new pdf task";
    public static final String done_pdf_task = "done pdf task";
    public static final String terminate_task = "terminate";
//    SQS messages

    public static String generate_new_pdf_task_message(String client_id, MissionTypes type, String file_path){
        return String.format("%s\t%s\t%s\t%s",
                new_pdf_task,
                client_id,
                type.name(),
                file_path);
    }
    public static String generate_new_done_task_message(String key, String client_id){
        return String.format("%s\t%s\t%s", common.done_task, client_id, key);
    }
    //    consumer types
    public static String manager_main_thread_consumer = "MANAGER-MAIN_THREAD";
    public static String client_consumer= "CLIENT";
    public static String worker_consumer= "WORKER";

    public static String[] parse_body(Message message) {
        String body = message.getBody();
        return body.split("\t");
    }

    //    queues url
    public static String clients_queue_url = "https://sqs.us-east-2.amazonaws.com/606249488880/clients-queue.fifo";
    public static String manager_queue_url = "https://sqs.us-east-2.amazonaws.com/606249488880/manager-queue.fifo";
    public static String worker_queue_url = "https://sqs.us-east-2.amazonaws.com/606249488880/worker-queue.fifo";
    public static String worker_to_manager_queue_url = "https://sqs.us-east-2.amazonaws.com/606249488880/manager-queue-from-worker.fifo";

    public static int time_to_sleep = 5000;
}
