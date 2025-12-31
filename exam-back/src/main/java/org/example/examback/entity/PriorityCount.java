package org.example.examback.entity;

import lombok.Data;

@Data
public class PriorityCount {
    private String priority;
    private Integer count;

    // 构造器
    public PriorityCount() {}

    // getters and setters
    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }
}
