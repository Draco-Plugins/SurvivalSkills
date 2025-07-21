package sir_draco.survivalskills;

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

        List<String> gradiant = new ArrayList<>();
        for (int i = 0; i <= length; i++) {

            int r = interpolate(startColor.getRed(), endColor.getRed(), i, length);
            int g = interpolate(startColor.getGreen(), endColor.getGreen(), i, length);
            int b = interpolate(startColor.getBlue(), endColor.getBlue(), i, length);

            String hexColor = "#" + intToString(r) + intToString(g) + intToString(b);
            gradiant.add(hexColor);
        }
        return gradiant;
    }

    public static List<String> generateGradient(String color1, String color2, String string) {
        int length = string.length() - 1;
        Color startColor = hexToColor(color1);
        Color endColor = hexToColor(color2);

        List<String> gradiant = new ArrayList<>();
        for (int i = 0; i <= length; i++) {

            int r = interpolate(startColor.getRed(), endColor.getRed(), i, length);
            int g = interpolate(startColor.getGreen(), endColor.getGreen(), i, length);
            int b = interpolate(startColor.getBlue(), endColor.getBlue(), i, length);

            String hexColor = "#" + intToString(r) + intToString(g) + intToString(b);
            gradiant.add(hexColor);
        }
        return gradiant;
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
        switch (l) {
            case '1':
                return 1;
            case '2':
                return 2;
            case '3':
                return 3;
            case '4':
                return 4;
            case '5':
                return 5;
            case '6':
                return 6;
            case '7':
                return 7;
            case '8':
                return 8;
            case '9':
                return 9;
            case 'A':
                return 10;
            case 'B':
                return 11;
            case 'C':
                return 12;
            case 'D':
                return 13;
            case 'E':
                return 14;
            case 'F':
                return 15;
            default:
                return 0;
        }
    }

    public static char intToChar(int i) {
        switch (i) {
            case 1:
                return '1';
            case 2:
                return '2';
            case 3:
                return '3';
            case 4:
                return '4';
            case 5:
                return '5';
            case 6:
                return '6';
            case 7:
                return '7';
            case 8:
                return '8';
            case 9:
                return '9';
            case 10:
                return 'A';
            case 11:
                return 'B';
            case 12:
                return 'C';
            case 13:
                return 'D';
            case 14:
                return 'E';
            case 15:
                return 'F';
            default:
                return '0';
        }
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
