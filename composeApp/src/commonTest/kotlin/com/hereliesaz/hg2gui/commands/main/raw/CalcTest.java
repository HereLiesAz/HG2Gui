package com.hereliesaz.hg2gui.commands.main.raw;

import org.junit.Test;
import static org.junit.Assert.*;

import com.hereliesaz.hg2gui.commands.ExecutePack;

public class CalcTest {

    @Test
    public void testSimpleAddition() throws Exception {
        calc cmd = new calc();
        ExecutePack pack = new ExecutePack(null) {};
        pack.args = new Object[]{"2+2"};

        String result = cmd.exec(pack);
        assertEquals("4.0", result);
    }

    @Test
    public void testMultiplication() throws Exception {
        calc cmd = new calc();
        ExecutePack pack = new ExecutePack(null) {};
        pack.args = new Object[]{"3*5"};

        String result = cmd.exec(pack);
        assertEquals("15.0", result);
    }

    @Test
    public void testSqrt() throws Exception {
        calc cmd = new calc();
        ExecutePack pack = new ExecutePack(null) {};
        pack.args = new Object[]{"sqrt(9)"};

        String result = cmd.exec(pack);
        assertEquals("3.0", result);
    }

    @Test
    public void testComplex() throws Exception {
        calc cmd = new calc();
        ExecutePack pack = new ExecutePack(null) {};
        pack.args = new Object[]{"2+3*4"};

        String result = cmd.exec(pack);
        assertEquals("14.0", result);
    }
}
