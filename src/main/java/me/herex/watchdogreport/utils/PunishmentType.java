package me.herex.watchdogreport.utils;

public enum PunishmentType {
    BAN("Ban"),
    TEMPBAN("Temp Ban"),
    MUTE("Mute"),
    TEMPMUTE("Temp Mute"),
    WARN("Warning"),
    KICK("Kick");

    private final String name;

    PunishmentType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}