import {
  Badge,
  BottomNavigation,
  BottomNavigationAction,
  Menu,
  MenuItem,
  Paper,
  Snackbar,
  Typography,
} from "@mui/material";
import React, { useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useLocation, useNavigate } from "react-router-dom";
import { useTranslation } from "./LocalizationProvider";
import CallToActionIcon from "@mui/icons-material/CallToAction";
import DescriptionIcon from "@mui/icons-material/Description";
import ExitToAppIcon from "@mui/icons-material/ExitToApp";
import MapIcon from "@mui/icons-material/Map";
import PersonIcon from "@mui/icons-material/Person";
import SettingsIcon from "@mui/icons-material/Settings";
import { sessionActions } from "../../store";
import { useRestriction } from "../util/permissions";
import { nativePostMessage } from "./NativeInterface";
import { snackBarDurationLongMs } from "../util/duration";

const BottomMenu = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const dispatch = useDispatch();
  const t = useTranslation();

  const readonly = useRestriction("readonly");
  const disableReports = useRestriction("disableReports");
  const user = useSelector((state) => state.session.user);
  const socket = useSelector((state) => state.session.socket);

  const [anchorEl, setAnchorEl] = useState(null);
  const [actionAnchorEl, setActionAnchorEl] = useState(null);

  // Snackbar state
  const [snackOpen, setSnackOpen] = useState(false);
  const [snackMessage, setSnackMessage] = useState("");

  const currentSelection = () => {
    if (location.pathname === `/settings/user/${user.id}`) return "account";
    if (location.pathname.startsWith("/settings")) return "settings";
    if (location.pathname.startsWith("/reports")) return "reports";
    if (location.pathname.startsWith("/actions")) return "actions";
    if (location.pathname === "/") return "map";
    return null;
  };

  const handleAccount = () => {
    setAnchorEl(null);
    navigate(`/settings/user/${user.id}`);
  };

  const handleLogout = async () => {
    setAnchorEl(null);
    const notificationToken = window.localStorage.getItem("notificationToken");
    if (notificationToken && !user.readonly) {
      window.localStorage.removeItem("notificationToken");
      const tokens = user.attributes.notificationTokens?.split(",") || [];
      if (tokens.includes(notificationToken)) {
        const updatedUser = {
          ...user,
          attributes: {
            ...user.attributes,
            notificationTokens:
              tokens.length > 1
                ? tokens.filter((it) => it !== notificationToken).join(",")
                : undefined,
          },
        };
        await fetch(`/api/users/${user.id}`, {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(updatedUser),
        });
      }
    }

    await fetch("/api/session", { method: "DELETE" });
    nativePostMessage("logout");
    navigate("/login");
    dispatch(sessionActions.updateUser(null));
  };

  const handleSOS = () => {
    setActionAnchorEl(null);
    setSnackMessage("🚨 SOS triggered!");
    setSnackOpen(true);
  };

  const handleLocate = () => {
    setActionAnchorEl(null);
    setSnackMessage("📍Your Location is being sent!");
    setSnackOpen(true);
  };

  const handleCloseSnackbar = (_, reason) => {
    if (reason === "clickaway") return;
    setSnackOpen(false);
  };

  const handleSelection = (event, value) => {
    switch (value) {
      case "map":
        navigate("/");
        break;
      case "reports":
        navigate("/reports/combined");
        break;
      case "settings":
        navigate("/settings/preferences");
        break;
      case "account":
        setAnchorEl(event.currentTarget);
        break;
      case "actions":
        setActionAnchorEl(event.currentTarget);
        break;
      case "logout":
        handleLogout();
        break;
      default:
        break;
    }
  };

  return (
    <Paper square elevation={3}>
      <BottomNavigation
        sx={{
          "& .MuiBottomNavigationAction-root": {
            minWidth: 0,
            padding: 0,
          },
        }}
        value={currentSelection()}
        onChange={handleSelection}
        showLabels
      >
        <BottomNavigationAction
          label={t("mapTitle")}
          icon={
            <Badge
              color="error"
              variant="dot"
              overlap="circular"
              invisible={socket !== false}
            >
              <MapIcon />
            </Badge>
          }
          value="map"
        />

        <BottomNavigationAction
          label={t("reportTitle")}
          icon={<DescriptionIcon />}
          value="reports"
        />

        {/* {!disableReports && (
          <BottomNavigationAction
            label={t("Actions")}
            icon={<CallToActionIcon />}
            value="actions"
          />
        )} */}

        <BottomNavigationAction
          label={t("settingsTitle")}
          icon={<SettingsIcon />}
          value="settings"
        />

        {readonly ? (
          <BottomNavigationAction
            label={t("loginLogout")}
            icon={<ExitToAppIcon />}
            value="logout"
          />
        ) : (
          <BottomNavigationAction
            label={t("settingsUser")}
            icon={<PersonIcon />}
            value="account"
          />
        )}
      </BottomNavigation>

      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={() => setAnchorEl(null)}
      >
        <MenuItem onClick={handleAccount}>
          <Typography color="textPrimary">{t("settingsUser")}</Typography>
        </MenuItem>
        <MenuItem onClick={handleLogout}>
          <Typography color="error">{t("loginLogout")}</Typography>
        </MenuItem>
      </Menu>

      <Menu
        anchorEl={actionAnchorEl}
        open={Boolean(actionAnchorEl)}
        onClose={() => setActionAnchorEl(null)}
      >
        <MenuItem onClick={handleSOS}>
          <Typography color="error">🚨 SOS</Typography>
        </MenuItem>
        <MenuItem onClick={handleLocate}>
          <Typography color="textPrimary">📍 Locate</Typography>
        </MenuItem>
      </Menu>

      <Snackbar
        anchorOrigin={{ vertical: "bottom", horizontal: "center" }}
        open={snackOpen}
        onClose={handleCloseSnackbar}
        autoHideDuration={snackBarDurationLongMs}
        message={<span style={{ fontWeight: "bold" }}>{snackMessage}</span>}
      />
    </Paper>
  );
};

export default BottomMenu;
