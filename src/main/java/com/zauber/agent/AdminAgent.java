package com.zauber.agent;

import com.okta.sdk.clients.UserApiClient;
import com.okta.sdk.clients.UserGroupApiClient;
import com.okta.sdk.framework.ApiClientConfiguration;
import com.okta.sdk.framework.FilterBuilder;
import com.okta.sdk.framework.PagedResults;
import com.okta.sdk.models.usergroups.UserGroup;
import com.okta.sdk.models.users.User;
import com.zauber.clients.AdminApiClient;
import com.zauber.domain.OktaServer;
import com.zauber.models.admin.Role;
import com.zauber.models.admin.RoleType;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * Admin Agent Class - Runs against a number of Okta Servers and manages Admin roles on each according to group membership.
 */
public class AdminAgent {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminAgent.class);

    private List<OktaServer> servers;

    private String readOnlyAdminGroupName;

    private String mobileAdminGroupName;

    private String userAdminGroupName;

    private String appAdminGroupName;

    private String orgAdminGroupName;

    private String superAdminGroupName;

    private Set<String> whitelistedUsers;

    public static void main(String[] args) {
        try {
            AdminAgent agent = new AdminAgent();
            agent.runAgent();
        } catch (IOException e) {
            LOGGER.error("Failed to load Admin Agent Properties", e);
        }
    }

    /**
     * AdminAgent()
     * @throws IOException Exception if properties cannot be found
     */
    private AdminAgent() throws IOException {
        Properties properties = loadProperties();
        servers = getOktaServers(properties);
        readOnlyAdminGroupName = properties.getProperty("okta.readonlyadmin.group");
        mobileAdminGroupName = properties.getProperty("okta.mobileadmin.group");
        userAdminGroupName = properties.getProperty("okta.useradmin.group");
        appAdminGroupName = properties.getProperty("okta.appadmin.group");
        orgAdminGroupName = properties.getProperty("okta.orgadmin.group");
        superAdminGroupName = properties.getProperty("okta.superadmin.group");
        whitelistedUsers = getWhiteListedUsers(properties);
    }

    /**
     * runAgent()
     * Runs against a number of Okta Servers and manages Admin roles on each according to group membership.
     */
    private void runAgent()  {
        for (OktaServer server : servers) {
            ApiClientConfiguration config = new ApiClientConfiguration(server.getUrl(), server.getApiKey());
            try {
                Map<RoleType, Set<User>> roleTypeToUsersMap = getUsersWithAdminRoles(config);
                // Perform admin provisioning from lowest to highest privileges to ensure that if a user is in several
                // groups they receive the highest privilege level.

                // Read Only Admins
                try {
                    Set<User> readOnlyUsers = getGroupMembers(config, readOnlyAdminGroupName);
                    for (User user : readOnlyUsers) {
                        try {
                            provisionRoleMembership(config, user, RoleType.READ_ONLY_ADMIN);
                            removeUserFromRoleToUserMap(roleTypeToUsersMap, user);
                        } catch (IOException e) {
                            LOGGER.error("Could not provision Read Only Admin Role correctly on " + server.getName(), e);
                        }
                    }
                } catch (IOException e) {
                    LOGGER.error("On " + server.getName() + " Could not get Read Only Admin Role Group for group name " + readOnlyAdminGroupName, e);
                }

                // Mobile Admins
                try {
                    Set<User> mobileUsers = getGroupMembers(config, mobileAdminGroupName);
                    for (User user : mobileUsers) {
                        try {
                            provisionRoleMembership(config, user, RoleType.MOBILE_ADMIN);
                        } catch (IOException e) {
                            LOGGER.error("Could not provision Mobile Admin Role correctly on " + server.getName(), e);
                        }
                    }
                } catch (IOException e) {
                    LOGGER.error("On " + server.getName() + " Could not get Mobile Admin Role Group for group name " + mobileAdminGroupName, e);
                }

                // User Admin
                try {
                    Set<User> userAdminUsers = getGroupMembers(config, userAdminGroupName);
                    for (User user : userAdminUsers) {
                        try {
                            provisionRoleMembership(config, user, RoleType.USER_ADMIN);
                        } catch (IOException e) {
                            LOGGER.error("Could not provision User Admin Role correctly on " + server.getName(), e);
                        }
                    }
                } catch (IOException e) {
                    LOGGER.error("On " + server.getName() + " Could not get User Admin Role Group for group name " + userAdminGroupName, e);
                }

                // App Admin
                try {
                    Set<User> appAdminUsers = getGroupMembers(config, appAdminGroupName);
                    for (User user : appAdminUsers) {
                        try {
                            provisionRoleMembership(config, user, RoleType.APP_ADMIN);
                        } catch (IOException e) {
                            LOGGER.error("Could not provision App Admin Role correctly on " + server.getName(), e);
                        }
                    }
                } catch (IOException e) {
                    LOGGER.error("On " + server.getName() + " Could not get App Admin Role Group for group name " + appAdminGroupName, e);
                }

                //Org Admin
                try {
                    Set<User> orgAdminUsers = getGroupMembers(config, orgAdminGroupName);
                    for (User user : orgAdminUsers) {
                        try {
                            provisionRoleMembership(config, user, RoleType.ORG_ADMIN);
                        } catch (IOException e) {
                            LOGGER.error("Could not provision Org Admin Role correctly on " + server.getName(), e);
                        }
                    }
                } catch (IOException e) {
                    LOGGER.error("On " + server.getName() + " Could not get Org Admin Role Group for group name " + orgAdminGroupName, e);
                }

                //Super Admins
                try {
                    Set<User> superAdminUsers = getGroupMembers(config, superAdminGroupName);
                    for (User user : superAdminUsers) {
                        try {
                            provisionRoleMembership(config, user, RoleType.SUPER_ADMIN);
                        } catch (IOException e) {
                            LOGGER.error("Could not provision Super Admin Role correctly on " + server.getName(), e);
                        }
                    }
                } catch (IOException e) {
                    LOGGER.error("On " + server.getName() + " Could not get Super Admin Role Group for group name " + superAdminGroupName, e);
                }

                //Remaining users have admin roles they should not have (unless they are whitelisted)
                for (Map.Entry<RoleType, Set<User>> entry : roleTypeToUsersMap.entrySet()) {
                    RoleType type = entry.getKey();
                    Set<User> users = entry.getValue();
                    for (User user : users) {
                        if (!whitelistedUsers.contains(user.getProfile().getLogin())) {
                            try {
                                deprovisionRoleMembership(config, user, type);
                            } catch (IOException e) {
                                LOGGER.error("Could not deprovision " + user.getProfile().getLogin() + " from role type " + type.getType() + " on " + server.getName(), e);
                            }
                        }
                    }

                }
            } catch (IOException e) {
                LOGGER.error("Could not get list of existing Administrators from " + server.getName() + ", skipping this server.");
            }
        }
    }

    private void removeUserFromRoleToUserMap(Map<RoleType, Set<User>> roleTypeToUsersMap, User user) {
        for (Set<User> users : roleTypeToUsersMap.values()){
            users.remove(user);
        }
    }

    private Properties loadProperties() throws IOException {
        Properties properties = new Properties();
        properties.load(new FileInputStream("application.properties"));
        return properties;
    }

    private List<OktaServer> getOktaServers(Properties properties){
        List<OktaServer> servers;
        String urls = properties.getProperty("oktaserver.urls");
        String apiKeys = properties.getProperty("oktaserver.apikeys");
        String names = properties.getProperty("oktaserver.names");
        if (urls == null){
            throw new IllegalArgumentException("Okta Server URLs must have a value.");
        }
        else if (apiKeys == null) {
            throw new IllegalArgumentException("Okta Server API keys must have a value.");
        }
        else if (names == null)
        {
            throw new IllegalArgumentException("Okta Server names must have a value.");
        }
        String[] urlArray = urls.split("\\|");
        String[] keyArray = apiKeys.split("\\|");
        String[] nameArray = names.split("\\|");
        if (urlArray.length != keyArray.length || urlArray.length != nameArray.length || keyArray.length != nameArray.length) {
            throw new IllegalArgumentException("Okta Server URLs and API Keys and names must be same length.");
        }
        servers = new ArrayList<>(urlArray.length);
        for (int i = 0; i < urlArray.length; i++) {
            String url = urlArray[i];
            String key = keyArray[i];
            String name = nameArray[i];
            if (StringUtils.isEmpty(url)) {
                throw new IllegalArgumentException("Okta Server URLs must not be blank or empty.");
            }
            else if (StringUtils.isEmpty(key)) {
                throw new IllegalArgumentException("Okta Server API Keys must not be blank or empty.");
            }
            else if (StringUtils.isEmpty(name)){
                throw new IllegalArgumentException("Okta Server names must not be blank or empty.");
            }
            servers.add(new OktaServer(name, url, key));
        }
        return servers;
    }

    private Set<String> getWhiteListedUsers(Properties properties) {
        Set<String> whitelistedUsers = new HashSet<>();
        String users = properties.getProperty("okta.whitelist");
        if (users != null){
            String[] userArray = users.split("\\|");
            whitelistedUsers.addAll(Arrays.asList(userArray));
        }
        return whitelistedUsers;
    }

    private Set<User> getGroupMembers(ApiClientConfiguration config, String groupName) throws IOException {
        Set<User> users = new HashSet<>();
        if (!StringUtils.isEmpty(groupName)) {
            UserGroupApiClient client = new UserGroupApiClient(config);
            FilterBuilder filter = new FilterBuilder();
            filter.attr("profile.name").equalTo(groupName);
            List<UserGroup> groups = client.getUserGroupsWithFilter(filter);

            for (UserGroup group : groups) {
                users.addAll(client.getUsers(group.getId()));
            }
        }
        return users;
    }

    private void provisionRoleMembership(ApiClientConfiguration config, User user, RoleType type) throws IOException {
        AdminApiClient client = new AdminApiClient(config);
        List<Role> roles = client.listRolesAssignedToUser(user.getId());
        boolean notFound = true;
        for (Role role : roles){
            if (type.getType().equalsIgnoreCase(role.getType())) {
                notFound = false;
            }
            else {
                client.unassignRoleFromUser(user.getId(), role.getId());
            }
        }
        if (notFound) {
            client.assignRoleToUser(user.getId(), type);
        }
    }

    private void deprovisionRoleMembership(ApiClientConfiguration config, User user, RoleType type) throws IOException {
        AdminApiClient client = new AdminApiClient(config);
        List<Role> roles = client.listRolesAssignedToUser(user.getId());
        for (Role role : roles) {
            if (type.getType().equalsIgnoreCase(role.getType())) {
                client.unassignRoleFromUser(user.getId(), role.getId());
                break;
            }
        }
    }
    
    private Map<RoleType, Set<User>> getUsersWithAdminRoles(ApiClientConfiguration config) throws IOException {
        Map<RoleType, Set<User>> roleTypeToUserMap = new HashMap<>();
        roleTypeToUserMap.put(RoleType.READ_ONLY_ADMIN, new HashSet<User>());
        roleTypeToUserMap.put(RoleType.MOBILE_ADMIN, new HashSet<User>());
        roleTypeToUserMap.put(RoleType.USER_ADMIN, new HashSet<User>());
        roleTypeToUserMap.put(RoleType.APP_ADMIN, new HashSet<User>());
        roleTypeToUserMap.put(RoleType.ORG_ADMIN, new HashSet<User>());
        roleTypeToUserMap.put(RoleType.SUPER_ADMIN, new HashSet<User>());
        UserApiClient userApiClient = new UserApiClient(config);
        AdminApiClient adminApiClient = new AdminApiClient(config);
        PagedResults<User> userPagedResults = userApiClient.getUsersPagedResults();
        while (true){
            List<User> usersInBatch = userPagedResults.getResult();
            for (User user : usersInBatch){
                List<Role> roles = adminApiClient.listRolesAssignedToUser(user.getId());
                for (Role role : roles) {
                    try {
                        RoleType roleType = RoleType.valueOf(role.getType());
                        Set<User> usersForType = roleTypeToUserMap.get(roleType);
                        if (usersForType != null) {
                            usersForType.add(user);
                        }
                    }
                    catch (IllegalArgumentException e){
                        LOGGER.error("Unexpected Role Type: " + role.getType(), e);
                    }

                }
            }
            if (!userPagedResults.isLastPage())
            {
                userPagedResults = userApiClient.getUsersPagedResultsByUrl(userPagedResults.getNextUrl());
            }
            else {
                break;
            }
        }
        return roleTypeToUserMap;
    }
}
