package com.TownyDiscordChat.TownyDiscordChat.Core;

import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.requests.restaction.RoleAction;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class TDCManagerTest {
    @Test
    void createdAccessRolesAlwaysSpecifyZeroPermissions() {
        AtomicReference<Long> permissions = new AtomicReference<>();
        AtomicReference<String> name = new AtomicReference<>();
        AtomicInteger permissionCalls = new AtomicInteger();

        RoleAction roleAction = (RoleAction) Proxy.newProxyInstance(
                RoleAction.class.getClassLoader(), new Class<?>[]{RoleAction.class},
                (proxy, method, arguments) -> {
                    if ("setPermissions".equals(method.getName())) {
                        permissionCalls.incrementAndGet();
                        permissions.set((Long) arguments[0]);
                    }
                    if ("setName".equals(method.getName())) {
                        name.set((String) arguments[0]);
                    }
                    return proxy;
                });
        Guild guild = (Guild) Proxy.newProxyInstance(
                Guild.class.getClassLoader(), new Class<?>[]{Guild.class},
                (proxy, method, arguments) -> {
                    if ("createRole".equals(method.getName())) {
                        return roleAction;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });

        assertSame(roleAction, TDCManager.newMembershipRole(
                guild, "town-TestLab", Color.BLUE));
        assertEquals("town-TestLab", name.get());
        assertEquals(0L, permissions.get().longValue());
        assertEquals(1, permissionCalls.get());

        assertSame(roleAction, TDCManager.newMembershipRole(
                guild, "nation-TestLab", Color.GREEN));
        assertEquals("nation-TestLab", name.get());
        assertEquals(0L, permissions.get().longValue());
        assertEquals(2, permissionCalls.get());
    }
}
