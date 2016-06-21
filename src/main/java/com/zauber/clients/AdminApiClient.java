package com.zauber.clients;

import com.fasterxml.jackson.core.type.TypeReference;
import com.okta.sdk.framework.ApiClientConfiguration;
import com.okta.sdk.framework.JsonApiClient;
import com.zauber.models.admin.Role;
import com.zauber.models.admin.RoleType;

import java.io.IOException;
import java.util.List;

/**
 * Created by Zauber Ltd on 21/06/2016.
 */
public class AdminApiClient extends JsonApiClient {

    public AdminApiClient(ApiClientConfiguration config) {
        super(config);
    }

    public List<Role> listRolesAssignedToUser(String userId) throws IOException {
        return this.get(this.getEncodedPath("/%s/roles", userId), new TypeReference<List<Role>>() {
        });
    }

    public Role assignRoleToUser(String userId, RoleType type) throws IOException {
        return this.post(this.getEncodedPath("/%s/roles", userId), type, new TypeReference<Role>() {
        });
    }

    public void unassignRoleFromUser(String userId, String roleId) throws IOException {
        this.delete(this.getEncodedPath("/%s/roles/%s", userId, roleId));
    }

    @Override
    protected String getFullPath(String relativePath) {
        return String.format("/api/v%d/users%s", this.apiVersion, relativePath);
    }
}
