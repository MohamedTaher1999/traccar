package org.traccar.model;

import org.traccar.storage.StorageName;

@StorageName("tc_user_group")
public class UserWithGroup extends ExtendedModel {
    private long userid;
    private long groupid;

    public long getUserid() {
        return userid;
    }

    public void setUserid(long userid) {
        this.userid = userid;
    }

    public long getGroupid() {
        return groupid;
    }

    public void setGroupid(long groupid) {
        this.groupid = groupid;
    }
}
