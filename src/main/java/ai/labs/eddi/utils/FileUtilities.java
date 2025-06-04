package ai.labs.eddi.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public final class FileUtilities {
    private static final String lineSeparator = System.getProperty("line.separator");

    private FileUtilities() {
        //utility class
    }

    public static String readTextFromFile(File file) throws IOException {
        final int MAX_LINE_LENGTH = 4096;
        final int MAX_TOTAL_LENGTH = 1024 * 1024 * 10;
        BufferedReader rd = null;
        StringBuilder ret = new StringBuilder(MAX_TOTAL_LENGTH);
        int totalLength = 0;
        try {
            rd = new BufferedReader(new FileReader(file));
            String line;
            while ((line = rd.readLine()) != null) {
                if (line.length() > MAX_LINE_LENGTH) {
                    throw new IOException("Line too long in file: " + file.getName());
                }
                totalLength += line.length();
                if (totalLength > MAX_TOTAL_LENGTH) {
                    throw new IOException("File too large: " + file.getName());
                }
                ret.append(line);
                ret.append(lineSeparator);
            }
            return ret.toString();
        } finally {
            if (rd != null) {
                try {
                    rd.close();
                } catch (IOException e) {
                    //do nothing
                }
            }
        }
    }

    public static String buildPath(String... directories) {
        int capacity = 0;
        for (String directory : directories) {
            if (directory != null) capacity += directory.length();
        }
        // Considera anche i separatori
        capacity += directories.length * 2;
        StringBuilder ret = new StringBuilder(Math.max(capacity, 64));
        for (String directory : directories) {
            ret.append(directory);
            if (!endsWith(ret, File.separator)) {
                ret.append(File.separator);
            }
        }

        if (directories.length > 0 && endsWith(ret, File.separator)) {
            ret.deleteCharAt(ret.length() - 1);
        }

        if (!ret.substring(ret.lastIndexOf(File.separator)).contains(".")) {
            ret.append(File.separatorChar);
        }

        return ret.toString();
    }

    private static boolean endsWith(StringBuilder sb, String lookup) {
        return sb.substring(sb.length() - lookup.length()).equals(lookup);
    }
}