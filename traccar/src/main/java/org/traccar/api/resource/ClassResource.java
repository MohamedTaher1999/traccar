package org.traccar.api.resource;

import jakarta.annotation.security.PermitAll;
import org.traccar.api.BaseObjectResource;
import org.traccar.model.Class;
import org.traccar.model.Device;
import org.traccar.model.User;
import org.traccar.storage.StorageException;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Order;
import org.traccar.storage.query.Request;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

@Path("classes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ClassResource extends BaseObjectResource<Class> {


    public ClassResource() {
        super(Class.class);
    }

    @GET
    @PermitAll
    public Collection<Class> get(
            @QueryParam("all") boolean all,
            @QueryParam("userId") long userId) throws StorageException {
        var conditions = new LinkedList<Condition>();
        if (all) {
            if (permissionsService.notAdmin(getUserId())) {
                conditions.add(new Condition.Permission(User.class, getUserId(), baseClass));
            }
        } else {
            if (userId == 0) {
                conditions.add(new Condition.Permission(User.class, getUserId(), baseClass));
            } else {
                permissionsService.checkUser(getUserId(), userId);
                conditions.add(new Condition.Permission(User.class, userId, baseClass).excludeGroups());
            }
        }

        return storage.getObjects(baseClass, new Request(
                new Columns.All(), Condition.merge(conditions), new Order("name")));
    }


}