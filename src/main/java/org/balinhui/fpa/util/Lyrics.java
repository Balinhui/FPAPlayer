package org.balinhui.fpa.util;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lyrics {
    /**
     * 专门匹配类似: [00:00:00] 时间格式，这种格式很大可能是因为打错
     */
    private static final Pattern pattern1 = Pattern.compile("\\[\\d{2}:\\d{2}:\\d{2}]");
    /**
     * 专门匹配类似: [00:00.00] 时间格式，正常格式
     */
    private static final Pattern pattern2 = Pattern.compile("\\[\\d{2}:\\d{2}.\\d{2}]");
    private static final Pattern lyricsPattern = Pattern.compile("lyrics", java.util.regex.Pattern.CASE_INSENSITIVE);

    private Lyrics() {}

    public static TreeMap<Long, String> read(@NotNull Map<String, String> metadata) {
        String getLyrics = "[00:00.00]纯音乐，请欣赏";
        for (String s : metadata.keySet()) {
            Matcher matcher = lyricsPattern.matcher(s);
            if (matcher.find()) {
                getLyrics = metadata.get(s);
                break;
            }
        }
        return read(getLyrics);
    }

    public static TreeMap<Long, String> read(@NotNull String lyrics) {
        TreeMap<Long, String> lyricsMap = new TreeMap<>();

        //分割不同段歌词，依据换行符(临时)，例: [00:00.00]歌词\n[00:00.00]lyrics 一般的歌词都有换行，特例到时考虑
        String[] split = lyrics.replaceAll("\\r", "").split("\n");//有的歌词结尾会同时具有\r\n,已去掉\r
        for (String s : split) {
            Matcher matcher1 = pattern1.matcher(s);
            Matcher matcher2 = pattern2.matcher(s);

            //对事件进行匹配，有时可能会出现第二个“.”打成“:”，只对这种情况适配，感觉有点怪怪的，依然特例到时考虑
            if (matcher1.find()) {
                String lyricOnly = s.substring(10);//没有时间的纯歌词
                if (noSpace(lyricOnly)) {
                    long t = changeTimeFormat(matcher1.group());
                    if (lyricsMap.containsKey(t)) {
                        lyricsMap.put(t, lyricsMap.get(t) + "\n" + lyricOnly);
                    } else {
                        lyricsMap.put(t, lyricOnly);
                    }
                }
            } else if (matcher2.find()) {
                String lyricOnly = s.substring(10);//没有时间的纯歌词
                if (noSpace(lyricOnly)) {
                    long t = changeTimeFormat(matcher2.group());
                    if (lyricsMap.containsKey(t)) {
                        lyricsMap.put(t, lyricsMap.get(t) + "\n" + lyricOnly);
                    } else {
                        lyricsMap.put(t, lyricOnly);
                    }
                }
            }
        }
        return lyricsMap;
    }

    /**
     * 针对空行歌词判断，目的是想要去除它
     * @param s 纯歌词部分，不包含时间点
     * @return 空行返回{@code false}，反之返回{@code true}
     */
    private static boolean noSpace(String s) {

        for (int i = 0; i < s.length(); i++)
            if (s.charAt(i) != ' ')
                return true;
        return false;
    }

    /**
     * 将字符串的时间点，如: [MM:SS.NS] 转化为毫秒值
     * @param timeStr 字符串时间
     * @return 转化的毫秒值
     */
    public static long changeTimeFormat(String timeStr) {
        String cleanStr = timeStr.substring(1, timeStr.length() - 1);

        long minutes = Long.parseLong(cleanStr.substring(0, 2));
        long seconds = Long.parseLong(cleanStr.substring(3, 5));
        long milliseconds = Long.parseLong(cleanStr.substring(6, 8)) * 10;
        return minutes * 60000 + seconds * 1000 + milliseconds;
    }
}
