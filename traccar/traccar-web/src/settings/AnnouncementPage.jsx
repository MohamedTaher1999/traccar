import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import {
    Accordion,
    AccordionSummary,
    AccordionDetails,
    Typography,
    Container,
    TextField,
    Button,
} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import { useTranslation } from "../common/components/LocalizationProvider";
import PageLayout from "../common/components/PageLayout";
import SettingsMenu from "./components/SettingsMenu";
import { useCatchCallback } from "../reactHelper";
import useSettingsStyles from "./common/useSettingsStyles";
import SelectField from "../common/components/SelectField";
import { prefixString } from "../common/util/stringUtils";
import Loader from '../common/components/Loader';
const AnnouncementPage = () => {
    const navigate = useNavigate();
    const styleClasses = useSettingsStyles();
    const t = useTranslation();
    const [users, setUsers] = useState([]);
    const [notificator, setNotificator] = useState();
    const [message, setMessage] = useState({});
    const [classes, setClasses] = useState([]);
    const [devices, setDevices] = useState([]);
    const [loading, setLoading] = useState(false);

    const handleSend = useCatchCallback(async () => {
        setLoading(true)
        const query = new URLSearchParams();
        if (classes.length > 0) {
            classes.forEach((classId) => query.append("classId", classId));
            const response = await fetch(
                `/api/notifications/sendToClass/${notificator}?${query.toString()}`,
                {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(message)
                }
            );
            if (response.ok) {
                navigate(-1);
            } else {
                throw Error(await response.text());
            }
        } else if (devices.length > 0) {
            devices.forEach((uniqueId) =>{ console.log(uniqueId);return query.append("deviceId", uniqueId)});
            const response = await fetch(
                `/api/notifications/sendToDevices/${notificator}?${query.toString()}`,
                {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(message)
                }
            );
            if (response.ok) {
              navigate(-1);
          } else {
              throw Error(await response.text());
          }
        } else {
            users.forEach((userId) => query.append("userId", userId));
            const response = await fetch(
                `/api/notifications/send/${notificator}?${query.toString()}`,
                {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(message)
                }
            );
            if (response.ok) {
                navigate(-1);
            } else {
                throw Error(await response.text());
            }
        }
        setLoading(false)
    }, [users, notificator, message, navigate]);

    return (
        <PageLayout menu={<SettingsMenu />} breadcrumbs={["serverAnnouncement"]}>
            {loading?<Loader/>:
            <Container maxWidth="xs" className={styleClasses.container}>
                <Accordion defaultExpanded>
                    <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                        <Typography variant="subtitle1">{t("sharedRequired")}</Typography>
                    </AccordionSummary>
                    <AccordionDetails className={styleClasses.details}>
                        <SelectField
                            multiple
                            value={classes}
                            onChange={(e) => {
                                setClasses(e.target.value);
                            }}
                            endpoint="/api/classes?all=true"
                            label={"Classes"}
                            disabled={users.length > 0 || devices.length > 0}
                        />
                        <SelectField
                            multiple
                            value={users}
                            onChange={(e) => {
                                setUsers(e.target.value);
                            }}
                            endpoint="/api/users"
                            label={t("settingsUsers")}
                            disabled={classes.length > 0 || devices.length > 0}
                        />
                        <SelectField
                            multiple
                            value={devices}
                            onChange={(e) => {
                                console.log(e.target.value,devices)
                                setDevices(e.target.value);
                            }}
                            keyGetter={(it) => it.uniqueId}
                            endpoint="/api/devices/all"
                            label={t("statisticsActiveDevices")}
                            disabled={classes.length > 0 || users.length > 0}
                        />
                        <SelectField
                            value={notificator}
                            onChange={(e) => setNotificator(e.target.value)}
                            endpoint="/api/notifications/notificators?announcement=true"
                            keyGetter={(it) => it.type}
                            titleGetter={(it) => t(prefixString("notificator", it.type))}
                            label={t("notificationNotificators")}
                        />
                        <TextField
                            value={message.subject}
                            onChange={(e) => setMessage({ ...message, subject: e.target.value })}
                            label={t("sharedSubject")}
                        />
                        <TextField
                            label="Message"
                            variant="outlined"
                            fullWidth
                            multiline
                            minRows={4}
                            value={message.body}
                            onChange={(e) => setMessage({ ...message, body: e.target.value })}
                            />
                    </AccordionDetails>
                </Accordion>
                <div className={styleClasses.buttons}>
                    <Button
                        type="button"
                        color="primary"
                        variant="outlined"
                        onClick={() => navigate(-1)}
                    >
                        {t("sharedCancel")}
                    </Button>
                    <Button
                        type="button"
                        color="primary"
                        variant="contained"
                        onClick={handleSend}
                        disabled={!notificator || !message.subject || !message.body||loading}
                    >
                      {t("commandSend")}
                    </Button>
                </div>
            </Container>}
        </PageLayout>
    );
};

export default AnnouncementPage;
