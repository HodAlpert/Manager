package com.manager.utils;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class Client {
    private int number_of_instances_needed;
    private String client_id;
    private CopyOnWriteArrayList<CompletedMission> completedMissions;
    private AtomicInteger remaining_tasks;

    public Client(String client_id) {
        this.client_id = client_id;
        this.completedMissions = new CopyOnWriteArrayList<>();
        this.remaining_tasks = new AtomicInteger(0);
        this.number_of_instances_needed = 0;
    }

    public AtomicInteger getRemaining_tasks() {
        return remaining_tasks;
    }

    public CopyOnWriteArrayList<CompletedMission> getCompletedMissions() {
        return completedMissions;
    }

    public String getClient_id() {
        return client_id;
    }

    public int getNumber_of_instances_needed() {
        return number_of_instances_needed;
    }
    public void setNumber_of_instances_needed(int value){
        this.number_of_instances_needed = value;
    }
}
