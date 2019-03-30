package com.manager.utils;

public class CompletedMission {
    private Enum<MissionTypes> type;
    private String old_url;
    private String new_url;

    public CompletedMission(Enum<MissionTypes> type, String old_url, String new_url) {
        this.type = type;
        this.old_url = old_url;
        this.new_url = new_url;
    }

    public Enum<MissionTypes> getType() {
        return type;
    }

    public String getOld_url() {
        return old_url;
    }

    public String getNew_url() {
        return new_url;
    }

    @Override
    public String toString() {
        return String.format("type: %s, old-url: %s, new-url: %s", type.name(), old_url, new_url);
    }
}
