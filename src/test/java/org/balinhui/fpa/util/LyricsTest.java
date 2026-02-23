package org.balinhui.fpa.util;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LyricsTest {

    @Test
    void read() {
        String content = "世界第一的公主殿下";
        Map<String, String> metadata = Map.of("LYRICS", "[01:54.51]" + content, "Others","杂项");
        TreeMap<Long, String> read = Lyrics.read(metadata);
        assertEquals(1, read.size());
        assertEquals(content, read.get(114510L));
    }

    @Test
    void changeTimeFormat() {
        assertEquals(114510L, Lyrics.changeTimeFormat("[01:54.51]"));
    }
}