package org.traccar.model;

import org.traccar.storage.StorageName;

@StorageName("tc_user_classtype")
public class UserWithClass extends ExtendedModel {
    private long userid;
    private long classid;

    public long getUserid() {
        return userid;
    }

    public void setUserid(long userid) {
        this.userid = userid;
    }

    public long getClassid() {
        return classid;
    }

    public void setClassid(long classid) {
        this.classid = classid;
    }
}
