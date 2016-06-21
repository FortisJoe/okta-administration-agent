package com.zauber.clients;

import com.fasterxml.jackson.core.type.TypeReference;
import com.okta.sdk.framework.ApiClientConfiguration;
import com.okta.sdk.framework.JsonApiClient;
import com.zauber.models.admin.Role;
import com.zauber.models.admin.RoleType;

import java.io.IOException;
import java.util.List;

/**
 * Api Client For Admin API Calls
 */
public class AdminApiClient extends JsonApiClient {

    /**
     * Creates client
     * @param config ApiClientConfiguration
     */
    public AdminApiClient(ApiClientConfiguration config) {
        super(config);
    }

    /**
     * Gets List of Admin Roles Assigned to a user
     * @param userId ID of user
     * @return List of Roles User has
     * @throws IOException
     */
    public List<Role> listRolesAssignedToUser(String userId) throws IOException {
        return this.get(this.getEncodedPath("/%s/roles", userId), new TypeReference<List<Role>>() {
        });
    }

    /**
     * Assigns a role to a user
     * @param userId ID of the user
     * @param type Type of Role
     * @return New Role assigned to user
     * @throws IOException
     */
    public Role assignRoleToUser(String userId, RoleType type) throws IOException {
        return this.post(this.getEncodedPath("/%s/roles", userId), type, new TypeReference<Role>() {
        });
    }

    /**
     * Unassigns a role from a user
     * @param userId ID of the user
     * @param roleId ID of the role
     * @throws IOException
     */
    public void unassignRoleFromUser(String userId, String roleId) throws IOException {
        this.delete(this.getEncodedPath("/%s/roles/%s", userId, roleId));
    }

    @Override
    protected String getFullPath(String relativePath) {
        return String.format("/api/v%d/users%s", this.apiVersion, relativePath);
    }
}
