/*
 * Copyright 2021 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.api.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.traccar.api.BaseObjectResource;
import org.traccar.api.SimpleObjectResource;
import org.traccar.model.Announcement;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;

@Path("announcements")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AnnouncementResource extends BaseObjectResource<Announcement> {

    public AnnouncementResource() {
        super(Announcement.class);
    }
    @GET
    public Collection<Announcement> get(
            @QueryParam("receiverID") long userId
            ) throws StorageException {


        return storage.getObjects(baseClass, new Request(
                new Columns.All(), new Condition.Equals("receiverid", userId)));
    }

    @POST
    public Response add(Announcement entity) throws StorageException {
        // Use the create method and return the response
        return Response.ok(createAnnouncement(entity)).build();
    }

    // Method that can be called internally from other classes
    public Announcement createAnnouncement(Announcement announcement) throws StorageException {
        // Set the date if not already set
        if (announcement.getDate() == null) {
            announcement.setDate(new Date());
        }

        // Add the announcement to storage
        storage.addObject(announcement, new Request(new Columns.Exclude("id")));

        // Return the created announcement with its ID
        return announcement;
    }
}
