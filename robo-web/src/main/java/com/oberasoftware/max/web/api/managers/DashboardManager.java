package com.oberasoftware.max.web.api.managers;


import com.oberasoftware.max.web.api.model.Dashboard;

import java.util.List;

/**
 * @author Renze de Vries
 */
public interface DashboardManager {
    List<Dashboard> findDashboards();

    Dashboard findDefaultDashboard();

    Dashboard findDashboard(String dashboardId);

    Dashboard store(Dashboard dashboard);

    void delete(String dashboardId);
}
