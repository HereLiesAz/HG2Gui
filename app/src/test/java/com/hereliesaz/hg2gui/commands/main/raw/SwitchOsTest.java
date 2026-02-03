package com.hereliesaz.hg2gui.commands.main.raw;

import org.junit.Test;
import static org.junit.Assert.*;

import com.hereliesaz.hg2gui.commands.ExecutePack;
import com.hereliesaz.hg2gui.managers.SystemContext;

public class SwitchOsTest {

    @Test
    public void testSwitchToMac() throws Exception {
        switchos cmd = new switchos();
        ExecutePack pack = new ExecutePack(null) {};
        pack.args = new Object[]{"macos"};

        String result = cmd.exec(pack);

        assertNotNull(result);
        assertTrue(result.contains("KERNEL SWITCHED TO: MACOS"));
        assertEquals(SystemContext.OSType.MACOS, SystemContext.getInstance().getOs());
    }

    @Test
    public void testSwitchToWindows() throws Exception {
        switchos cmd = new switchos();
        ExecutePack pack = new ExecutePack(null) {};
        pack.args = new Object[]{"windows"};

        String result = cmd.exec(pack);

        assertTrue(result.contains("KERNEL SWITCHED TO: WINDOWS"));
        assertEquals(SystemContext.OSType.WINDOWS, SystemContext.getInstance().getOs());
    }

    @Test
    public void testInvalidOs() throws Exception {
        switchos cmd = new switchos();
        ExecutePack pack = new ExecutePack(null) {};
        pack.args = new Object[]{"templeos"};

        String result = cmd.exec(pack);

        assertTrue(result.contains("NOT FOUND"));
    }
}
