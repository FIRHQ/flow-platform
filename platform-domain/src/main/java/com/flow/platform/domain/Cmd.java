package com.flow.platform.domain;

import com.google.gson.Gson;

import java.util.Date;

/**
 * Command object to communicate between c/s
 *
 * Created by gy@fir.im on 12/05/2017.
 * Copyright fir.im
 */
public class Cmd extends CmdBase {

    /**
     * Get zk cmd from json of byte[] format
     *
     * @param raw
     * @return Cmd or null if any exception
     */
    public static Cmd parse(byte[] raw) {
        String json = new String(raw);
        Gson gson = new Gson();
        return gson.fromJson(json, Cmd.class);
    }

    public enum Status {

        /**
         * Init status when cmd send to agent
         */
        PENDING("PENDING"),

        /**
         * Cmd running
         */
        RUNNING("RUNNING"),

        /**
         * Cmd executed
         */
        EXECUTED("EXECUTED"),

        /**
         * Log uploaded
         */
        LOGGED("LOGGED"),

        /**
         * Got exception when running
         */
        EXCEPTION("EXCEPTION");

        private String name;

        Status(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * Server generated command id
     */
    private String id;

    /**
     * Cmd status
     */
    private Cmd.Status status = Status.PENDING;

    /**
     * Cmd execution result
     */
    private CmdResult result;

    /**
     * Created date
     */
    private Date createdDate;

    /**
     * Updated date
     */
    private Date updatedDate;


    public Cmd() {
    }

    public Cmd(CmdBase cmdBase) {
        super(cmdBase.getAgentPath(),
                cmdBase.getType(),
                cmdBase.getCmd());
    }

    public Cmd(String zone, String agent, Type type, String cmd) {
        super(zone, agent, type, cmd);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public CmdResult getResult() {
        return result;
    }

    public void setResult(CmdResult result) {
        this.result = result;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(Date updatedDate) {
        this.updatedDate = updatedDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Cmd cmd = (Cmd) o;

        return id != null ? id.equals(cmd.id) : cmd.id == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (id != null ? id.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Cmd{" +
                "id='" + id + '\'' +
                ", status=" + status +
                ", createdDate=" + createdDate +
                ", updatedDate=" + updatedDate +
                '}';
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
