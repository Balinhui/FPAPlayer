package org.balinhui.fpa.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArrayLoopTest {

    @Test
    void ShortReSize() {
        short[] s1 = {9, 3, 5, 6};
        short[] s2 = {4, 5, 6, 3};
        short[] s3 = {2, 4, 5, 3};

        short[] ss1 = ArrayLoop.reSize(s1, s1[0]);
        assertEquals(9, ss1.length);
        short[] ss2 = ArrayLoop.reSize(s2, s2[0]);
        assertEquals(s2, ss2);
        short[] ss3 = ArrayLoop.reSize(s3, s3[0]);
        for (int i = 0; i < 2; i++)
            assertEquals(s3[i], ss3[i]);
        for (int i = 2; i < s3.length; i++)
            assertEquals(0, ss3[i]);
    }

    @Test
    void FloatReSize() {
        float[] f1 = {9, 3, 5, 6};
        float[] f2 = {4, 5, 6, 3};
        float[] f3 = {2, 4, 5, 3};

        float[] ff1 = ArrayLoop.reSize(f1, 9);
        assertEquals(9, ff1.length);
        float[] ff2 = ArrayLoop.reSize(f2, 4);
        assertEquals(f2, ff2);
        float[] ff3 = ArrayLoop.reSize(f3, 2);
        for (int i = 0; i < 2; i++)
            assertEquals(f3[i], ff3[i]);
        for (int i = 2; i < f3.length; i++)
            assertEquals(0, ff3[i]);
    }
}