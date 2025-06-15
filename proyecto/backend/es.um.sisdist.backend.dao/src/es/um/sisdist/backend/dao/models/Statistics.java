package es.um.sisdist.backend.dao.models;

import java.sql.Timestamp;

public class Statistics {
    private int numLogins;
    private int numPrompts;
    private Timestamp lastActivity;

    public int getNumLogins() {
        return numLogins;
    }

    public void setNumLogins(int numLogins) {
        this.numLogins = numLogins;
    }

    public int getNumPrompts() {
        return numPrompts;
    }

    public void setNumPrompts(int numPrompts) {
        this.numPrompts = numPrompts;
    }

    public Timestamp getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(Timestamp lastActivity) {
        this.lastActivity = lastActivity;
    }
}
