package com.manager.managers;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;
import org.apache.commons.codec.binary.Base64;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class EC2Manager extends BaseManager{
    public String manager_instance_id = "i-0125d68ff955b4461";
    private AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard()
            .withRegion("us-east-2")
            .build();

    /**
     * @return List of all Instances currently available is the domain
     */
    public List<Reservation> list(){
        try {
            DescribeInstancesRequest req = new DescribeInstancesRequest();
            List<Reservation> answer = ec2.describeInstances(req).getReservations();
            logger.fine("returned " + answer);
            return answer;
        }
        catch (Exception exc) {
            handle_exception(exc);
            return null;
        }
    }

    /**
     * @param id of Instance to get
     * @return Instance requested or null if failed
     */
    public Instance get(String id) {
        try {
            DescribeInstancesRequest req = new DescribeInstancesRequest().withInstanceIds(id);
            Instance answer = ec2.describeInstances(req).getReservations().get(0).getInstances().get(0);
            logger.fine("returned " + answer);
            return answer;
        } catch (Exception exc) {
            handle_exception(exc);
            return null;
        }
    }

    /**
     * @param user_data user data to create the instance with
     * @return Instance created
     */
    public Instance create(List<String> user_data){
        IamInstanceProfileSpecification instance_profile = new IamInstanceProfileSpecification()
                .withArn("arn:aws:iam::606249488880:instance-profile/worker_role");
    try {
        RunInstancesRequest request = new RunInstancesRequest("ami-07e720151f18c6342", 1, 1)
                .withKeyName("my_first_keypair")
                .withSecurityGroups("launch-wizard-1")
                .withIamInstanceProfile(instance_profile)
                .withUserData(getUserDataScript(user_data))
                .withTagSpecifications();
        request.setInstanceType(InstanceType.T2Micro.toString());
        logger.fine(String.format("Creating instance with user-data %s", user_data.toString()));
        Instance answer = ec2.runInstances(request).getReservation().getInstances().get(0);
        logger.fine("returned " + answer);
        return answer;
    }
    catch (Exception exc) {
        handle_exception(exc);
        return null;
    }
    }

    /**
     * @return true if manager is in state running, false otherwise and null if it fails
     */
    public Boolean is_manager_up(){
        try {
            boolean answer = get(manager_instance_id).getState().getName().equals("running");
            logger.fine("returned " + answer);
            return answer;
        }
        catch (Exception exc) {
            handle_exception(exc);
            return null;
        }
    }

    private String getUserDataScript(List <String> script){
        return new String(Base64.encodeBase64(join(script).getBytes()));
    }

    private static String join(Collection<String> s) {
        StringBuilder builder = new StringBuilder();
        Iterator<String> iter = s.iterator();
        while (iter.hasNext()) {
            builder.append(iter.next());
            if (!iter.hasNext()) {
                break;
            }
            builder.append("\n");
        }
        return builder.toString();
    }

    public static List<String> init_script(){
        List<String> list = new ArrayList<>();
        list.add("#! /bin/bash");
        return list;
    }
    public StartInstancesResult power_up_manager(){
        try {
            StartInstancesRequest startInstancesRequest = new StartInstancesRequest().withInstanceIds(manager_instance_id);
            StartInstancesResult answer = ec2.startInstances(startInstancesRequest);
            logger.fine("returned " + answer);
            return answer;
        }
        catch (Exception exc) {
            handle_exception(exc);
            return null;
        }
    }

    /**
     * stopping manager instance
     */
    public void stop_manager(){
        logger.fine("stopping manager");
        try{
            StopInstancesRequest stopInstancesRequest = new StopInstancesRequest()
                    .withInstanceIds(manager_instance_id);

            ec2.stopInstances(stopInstancesRequest)
                    .getStoppingInstances()
                    .get(0)
                    .getPreviousState()
                    .getName();
        }
        catch (Exception exc) {
            handle_exception(exc);
        }
    }

    /**
     * @param id of the instance that should be terminated
     * @return result of the termination or null if fails
     */
    public TerminateInstancesResult terminate_instance(String id){
        try{
            TerminateInstancesRequest request = new TerminateInstancesRequest().withInstanceIds(id);
            TerminateInstancesResult answer = ec2.terminateInstances(request);
            logger.fine("returned " + answer);
            return answer;
        }
        catch (Exception exc) {
            handle_exception(exc);
            return null;
        }
    }

}