package com.medcare.app.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "logs", indices = @Index(value = "timestamp"))
public class LogEntry {
    public static final String ACTION_CREATE = "CREATE";
    public static final String ACTION_UPDATE = "UPDATE";
    public static final String ACTION_DELETE = "DELETE";
    public static final String ACTION_LOGIN = "LOGIN";
    public static final String ACTION_LOGOUT = "LOGOUT";
    public static final String ACTION_EXPORT = "EXPORT";
    public static final String ACTION_IMPORT = "IMPORT";
    public static final String ACTION_BACKUP = "BACKUP";
    public static final String ACTION_RESTORE = "RESTORE";

    @PrimaryKey(autoGenerate = true)
    private long id;
    private long timestamp;
    @ColumnInfo(name = "user_id")
    private long userId;
    private String action;
    @ColumnInfo(name = "entity_type")
    private String entityType;
    @ColumnInfo(name = "entity_id")
    private Long entityId;
    private String detail;

    public LogEntry(long timestamp, long userId, String action, String entityType, Long entityId, String detail) {
        this.timestamp = timestamp;
        this.userId = userId;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.detail = detail;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
}