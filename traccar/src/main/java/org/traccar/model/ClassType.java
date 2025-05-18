package org.traccar.model;

import org.traccar.storage.StorageName;

@StorageName("tc_classes")
public class ClassType extends ExtendedModel {
    private String name;
    private String description;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
