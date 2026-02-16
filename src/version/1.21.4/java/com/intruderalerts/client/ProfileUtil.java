package com.intruderalerts.client;

import com.mojang.authlib.GameProfile;

import java.util.UUID;

public class ProfileUtil {

    public static String getName(GameProfile profile) {
        return profile.getName();
    }

    public static UUID getId(GameProfile profile) {
        return profile.getId();
    }
}
