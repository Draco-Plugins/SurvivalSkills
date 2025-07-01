package sir_draco.survivalskills.Utils;

import org.bukkit.Color;

import java.util.ArrayList;
import java.util.List;

public class ColorParser {
    public static String colorizeString(String words, List<String> colors, boolean bold) {
        if (words.length() != colors.size()) {
            return words;
        }

        StringBuilder word = new StringBuilder();
        for (int i = 0; i < words.length(); i++) {
            if (bold) {
                word.append(hexToChatColor(colors.get(i))).append(org.bukkit.ChatColor.BOLD).append(words.charAt(i));
            }
            else {
                word.append(hexToChatColor(colors.get(i))).append(words.charAt(i));
            }
        }
        return word.toString();
    }

    public static List<String> gradientConnector(List<List<String>> lists) {
        List<String> gradient = new ArrayList<>();
        for (List<String> list : lists) {
            gradient.addAll(list);
        }
        return gradient;
    }

    public static List<String> generateGradient(String color1, String color2, int length) {
        length = length - 1;
        Color startColor = hexToColor(color1);
        Color endColor = hexToColor(color2);

        List<String> gradient = new ArrayList<>();
        for (int i = 0; i <= length; i++) {

            int r = interpolate(startColor.getRed(), endColor.getRed(), i, length);
            int g = interpolate(startColor.getGreen(), endColor.getGreen(), i, length);
            int b = interpolate(startColor.getBlue(), endColor.getBlue(), i, length);

            String hexColor = "#" + intToString(r) + intToString(g) + intToString(b);
            gradient.add(hexColor);
        }
        return gradient;
    }

    public static List<String> generateGradient(String color1, String color2, String string) {
        int length = string.length() - 1;
        Color startColor = hexToColor(color1);
        Color endColor = hexToColor(color2);

        List<String> gradient = new ArrayList<>();
        for (int i = 0; i <= length; i++) {

            int r = interpolate(startColor.getRed(), endColor.getRed(), i, length);
            int g = interpolate(startColor.getGreen(), endColor.getGreen(), i, length);
            int b = interpolate(startColor.getBlue(), endColor.getBlue(), i, length);

            String hexColor = "#" + intToString(r) + intToString(g) + intToString(b);
            gradient.add(hexColor);
        }
        return gradient;
    }

    private static String hexToChatColor(String hex) {
        return net.md_5.bungee.api.ChatColor.of(hex) + "";
    }

    private static int interpolate(int start, int end, int currentStep, int totalSteps) {
        float ratio = (float) currentStep / totalSteps;
        int range = end - start;
        return Math.round(start + ratio * range);
    }

    public static Color hexToColor(String colorStr) {
        return Color.fromRGB(hexToInt(colorStr.substring(1, 3)),
                hexToInt(colorStr.substring(3, 5)),
                hexToInt(colorStr.substring(5)));
    }

    public static int hexToInt(String hex) {
        if (hex.length() != 2) return 0;
        int first = charToInt(hex.charAt(0));
        int second = charToInt(hex.charAt(1));
        return (first * 16) + second;
    }

    public static int charToInt(char l) {
        return switch (l) {
            case '1' -> 1;
            case '2' -> 2;
            case '3' -> 3;
            case '4' -> 4;
            case '5' -> 5;
            case '6' -> 6;
            case '7' -> 7;
            case '8' -> 8;
            case '9' -> 9;
            case 'A' -> 10;
            case 'B' -> 11;
            case 'C' -> 12;
            case 'D' -> 13;
            case 'E' -> 14;
            case 'F' -> 15;
            default -> 0;
        };
    }

    public static char intToChar(int i) {
        return switch (i) {
            case 1 -> '1';
            case 2 -> '2';
            case 3 -> '3';
            case 4 -> '4';
            case 5 -> '5';
            case 6 -> '6';
            case 7 -> '7';
            case 8 -> '8';
            case 9 -> '9';
            case 10 -> 'A';
            case 11 -> 'B';
            case 12 -> 'C';
            case 13 -> 'D';
            case 14 -> 'E';
            case 15 -> 'F';
            default -> '0';
        };
    }

    public static String intToString(int value) {
        int first = value / 16;
        int second = value - (first * 16);
        return String.valueOf(intToChar(first)) + intToChar(second);
    }

    public static String rgbToHex(int r, int g, int b) {
        return "#" + intToString(r) + intToString(g) + intToString(b);
    }
}
