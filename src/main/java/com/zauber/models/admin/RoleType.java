package com.zauber.models.admin;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Created by Zauber Ltd on 21/06/2016.
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum RoleType {
    SUPER_ADMIN("SUPER_ADMIN"),
    ORG_ADMIN("ORG_ADMIN"),
    APP_ADMIN("APP_ADMIN"),
    USER_ADMIN("USER_ADMIN"),
    MOBILE_ADMIN("MOBILE_ADMIN"),
    READ_ONLY_ADMIN("READ_ONLY_ADMIN");

    private String type;

    RoleType(final String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
