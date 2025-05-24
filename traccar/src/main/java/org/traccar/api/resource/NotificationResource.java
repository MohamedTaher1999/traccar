/*
 * Copyright 2016 - 2024 Anton Tananaev (anton@traccar.org)
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

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.api.ExtendedObjectResource;
import org.traccar.model.*;
import org.traccar.notification.MessageException;
import org.traccar.notification.NotificationMessage;
import org.traccar.notification.NotificatorManager;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

@Path("notifications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NotificationResource extends ExtendedObjectResource<Notification> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationResource.class);
    @Inject
    private AnnouncementResource announcementResource;
    @Inject
    private NotificatorManager notificatorManager;

    public NotificationResource() {
        super(Notification.class, "description");
    }

    @GET
    @Path("types")
    public Collection<Typed> get() {
        List<Typed> types = new LinkedList<>();
        Field[] fields = Event.class.getDeclaredFields();
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers()) && field.getName().startsWith("TYPE_")) {
                try {
                    types.add(new Typed(field.get(null).toString()));
                } catch (IllegalArgumentException | IllegalAccessException error) {
                    LOGGER.warn("Get event types error", error);
                }
            }
        }
        return types;
    }

    @GET
    @Path("notificators")
    public Collection<Typed> getNotificators(@QueryParam("announcement") boolean announcement) {
        Set<String> announcementsUnsupported = Set.of("command", "web");
        return notificatorManager.getAllNotificatorTypes().stream()
                .filter(typed -> !announcement || !announcementsUnsupported.contains(typed.type()))
                .collect(Collectors.toUnmodifiableSet());
    }

    @POST
    @Path("test")
    public Response testMessage() throws MessageException, StorageException {
        User user = permissionsService.getUser(getUserId());
        for (Typed method : notificatorManager.getAllNotificatorTypes()) {
            notificatorManager.getNotificator(method.type()).send(null, user, new Event("test", 0), null);
        }
        return Response.noContent().build();
    }

    @POST
    @Path("test/{notificator}")
    public Response testMessage(@PathParam("notificator") String notificator)
            throws MessageException, StorageException {
        User user = permissionsService.getUser(getUserId());
        notificatorManager.getNotificator(notificator).send(null, user, new Event("test", 0), null);
        return Response.noContent().build();
    }

    @POST
    @Path("send/{notificator}")
    public Response sendMessage(
            @PathParam("notificator") String notificator, @QueryParam("userId") List<Long> userIds,
            NotificationMessage message) throws MessageException, StorageException {
        permissionsService.checkManager(getUserId());
        List<User> users;

        users = new ArrayList<>();
        for (long userId : userIds) {
            var conditions = new LinkedList<Condition>();
            conditions.add(new Condition.Equals("id", userId));
            if (permissionsService.notAdmin(getUserId())) {
                conditions.add(new Condition.Permission(
                        User.class, getUserId(), ManagedUser.class).excludeGroups());
            }
            users.add(storage.getObject(
                    User.class, new Request(new Columns.All(), Condition.merge(conditions))));
        }

        for (User user : users) {
            if (!user.getTemporary()) {
                Announcement announcement = new Announcement();
                announcement.setSenderId(getUserId());
                announcement.setReceiverId(user.getId());
                announcement.setMessage(message.getBody());
                announcement.setSubject(message.getSubject());
                announcement.setNotificator(notificator);
                announcement.setDate(new Date());
                announcementResource.createAnnouncement(announcement);
                notificatorManager.getNotificator(notificator).send(user, message, null, null);
            }
        }
        return Response.noContent().build();
    }

    @POST
    @Path("sendToDevices/{notificator}")
    public Response sendDevicesMessage(
            @PathParam("notificator") String notificator, @QueryParam("deviceId") List<String> deviceIds,
            NotificationMessage message) throws MessageException, StorageException {
        permissionsService.checkManager(getUserId());
        List<Device> devices;

        devices = new ArrayList<>();
        for (String deviceId : deviceIds) {

            devices.add(storage.getObject(
                    Device.class, new Request(new Columns.All(),new Condition.Equals("uniqueid", deviceId) )));
        }

        for (Device device : devices) {

                Announcement announcement = new Announcement();
                announcement.setSenderId(getUserId());
                announcement.setReceiverId(getUserId());
                announcement.setMessage(message.getBody());
                announcement.setSubject(message.getSubject());
                announcement.setNotificator(notificator);
                announcement.setDeviceId(device.getUniqueId());
                announcement.setDate(new Date());
                announcementResource.createAnnouncement(announcement);
                notificatorManager.getNotificator(notificator).send(device, message, null, null);

        }
        return Response.noContent().build();
    }
    @POST
    @Path("sendToGroup/{notificator}")
    public Response sendMessageToGroup(
            @PathParam("notificator") String notificator,
            @QueryParam("groupId") List<Long> groupIds,
            NotificationMessage message) throws MessageException, StorageException {

        permissionsService.checkManager(getUserId());
        int count = 0;

        for (long groupId : groupIds) {
            List<UserWithGroup> usersIdWithGroupID;
            List<User> usersList = new ArrayList<>();

            usersIdWithGroupID = storage.getObjects(UserWithGroup.class, new Request(
                    new Columns.All(),
                    new Condition.Equals("groupid", groupId)));


            List<Long> userIds = new ArrayList<>();
            for (UserWithGroup userWithGroup : usersIdWithGroupID)
                userIds.add(userWithGroup.getUserid());

            for (long userId : userIds) {

                usersList.add(storage.getObject(
                        User.class, new Request(new Columns.All(), new Condition.Equals("id", userId))));
            }
            for (User user : usersList) {
                if (!user.getTemporary()) {
                    Announcement announcement = new Announcement();
                    announcement.setSenderId(getUserId());
                    announcement.setReceiverId(user.getId());
                    announcement.setMessage(message.getBody());
                    announcement.setSubject(message.getSubject());
                    announcement.setNotificator(notificator);
                    announcement.setDate(new Date());
                    announcementResource.createAnnouncement(announcement);
                    notificatorManager.getNotificator(notificator).send(user, message, null, null);
                    count++;
                }
            }
        }
        return Response.ok(Map.of("sent", count)).build();

    }

    @POST
    @Path("sendToClass/{notificator}")
    public Response sendMessageToClass(
            @PathParam("notificator") String notificator,
            @QueryParam("classId") List<Long> classIds,
            NotificationMessage message) throws MessageException, StorageException {

        permissionsService.checkManager(getUserId());
        int count = 0;

        for (long classId : classIds) {
            List<UserWithClass> usersIdWithClassID;
            List<User> usersList = new ArrayList<>();

            usersIdWithClassID = storage.getObjects(UserWithClass.class, new Request(
                    new Columns.All(),
                    new Condition.Equals("classTypeId", classId)));


            List<Long> userIds = new ArrayList<>();
            for (UserWithClass userWithClass : usersIdWithClassID)
                userIds.add(userWithClass.getUserid());

            for (long userId : userIds) {

                usersList.add(storage.getObject(
                        User.class, new Request(new Columns.All(), new Condition.Equals("id", userId))));
            }
            for (User user : usersList) {
                if (!user.getTemporary()) {
                    Announcement announcement = new Announcement();
                    announcement.setSenderId(getUserId());
                    announcement.setReceiverId(user.getId());
                    announcement.setMessage(message.getBody());
                    announcement.setSubject(message.getSubject());
                    announcement.setNotificator(notificator);
                    announcement.setDate(new Date());
                    announcementResource.createAnnouncement(announcement);
                    notificatorManager.getNotificator(notificator).send(user, message, null, null);
                    count++;
                }
            }
        }
        return Response.ok(Map.of("sent", count)).build();

    }
}
