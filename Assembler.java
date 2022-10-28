import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.Scanner;
import java.util.regex.Pattern;


public class Assembler {
    static final Hashtable<String, String> COMP;
    static final Hashtable<String, String> DEST;
    static final Hashtable<String, String> JUMP;
    static Hashtable<String, Integer> table;
    static {
        COMP = new Hashtable<>();
        COMP.put("0", "0101010");
        COMP.put("1", "0111111");
        COMP.put("-1", "0111010");
        COMP.put("D", "0001100");
        COMP.put("A", "0110000");
        COMP.put("!D", "0001101");
        COMP.put("!A", "0110001");
        COMP.put("-D", "0001111");
        COMP.put("-A", "0110011");
        COMP.put("D+1", "0011111");
        COMP.put("A+1", "0110111");
        COMP.put("D-1", "0001110");
        COMP.put("A-1", "0110010");
        COMP.put("D+A", "0000010");
        COMP.put("D-A", "0010011");
        COMP.put("A-D", "0000111");
        COMP.put("D&A", "0000000");
        COMP.put("D|A", "0010101");
        COMP.put("M", "1110000");
        COMP.put("!M", "1110001");
        COMP.put("-M", "1110011");
        COMP.put("M+1", "1110111");
        COMP.put("M-1", "1110010");
        COMP.put("D+M", "1000010");
        COMP.put("D-M", "1010011");
        COMP.put("M-D", "1000111");
        COMP.put("D&M", "1000000");
        COMP.put("D|M", "1010101");

        DEST = new Hashtable<>();
        DEST.put("null", "000");
        DEST.put("M", "001");
        DEST.put("D", "010");
        DEST.put("A", "100");
        DEST.put("MD", "011");
        DEST.put("AM", "101");
        DEST.put("AD", "110");
        DEST.put("AMD", "111");

        JUMP = new Hashtable<>();
        JUMP.put("null", "000");
        JUMP.put("JGT", "001");
        JUMP.put("JEQ", "010");
        JUMP.put("JGE", "011");
        JUMP.put("JLT", "100");
        JUMP.put("JNE", "101");
        JUMP.put("JLE", "110");
        JUMP.put("JMP", "111");

        table = new Hashtable<>();
        table.put("SP", 0);
        table.put("LCL", 1);
        table.put("ARG", 2);
        table.put("THIS", 3);
        table.put("THAT", 4);
        table.put("SCREEN", 16384);
        table.put("KBD", 24576);
        for (int i = 0; i < 16; i++) {
            table.put("R"+i, i);
        }
    }
    static int variableCursor = 16;
    public static void main(String[] args) throws IOException {
        String root = args[0];
        Scanner inputFile = new Scanner(new File(root+".asm"));
        PrintWriter tempFile = new PrintWriter(root+".temp");
        int lineNum = 0;
        while (inputFile.hasNextLine()) {
            String line = inputFile.nextLine();
            if (line.length() > 0) {
                if (line.charAt(0) == '/' || line.charAt(0) == '\n') {
                    line = inputFile.nextLine();
                    continue;
                }
                line = remove(line);
                if (line.charAt(0) == '(') {
                    line = line.replaceAll("[()]", "");
                    table.put(line, lineNum);
                } else {
                    lineNum++;
                    tempFile.printf("%s\n", line);
                }
            }
        }
        tempFile.close();

        inputFile = new Scanner(new File(root+".temp"));
        PrintWriter outputFile = new PrintWriter(root+".hack");
        while (inputFile.hasNextLine()) {
            String line = inputFile.nextLine();
            if (line.length() > 0) {
                if (line.charAt(0) == '@') {
                    line = aTranslate(line.substring(1));
                } else {
                    line = cTranslate(line);
                }
            }
            outputFile.printf("%s\n", line);
        }
        outputFile.close();
        inputFile.close();
        (new File(root+".temp")).delete();

    }
    // allocate memory location
    public static void addVariable(String line) {
        table.put(line, variableCursor);
        variableCursor++;
    }

    // remove space and
    public static String remove(String line) {
        line = line.replaceAll(" ", "");
        line = line.split("/", 2)[0];
        return line;
    }
    // aTranslate
    public static String aTranslate(String line) {
        int numVal = 0;
        if (isNumeric(line)) {
            try {
                numVal = Integer.parseInt(line);
            } catch (NumberFormatException e) {
                numVal = 0;
                System.out.printf("ERROR - Number format exception, line (%s)\n", line);
            }
            line = (String.format("%16s", Integer.toBinaryString(numVal)).replaceAll(" ", "0"));

        } else {
            if (!table.containsKey(line)) {
                addVariable(line);
            }
            try {
                numVal = table.get(line);
            } catch (NullPointerException e) {
                numVal = 0;
                System.out.printf("ERROR - Null pointer exception, line (%s)\n", line);
            }
            line = (String.format("%16s", Integer.toBinaryString(numVal))).replaceAll(" ", "0");
        }
        return line;
    }

    // cTranslate
    public static String cTranslate(String line) {
        String destCode = "null";
        String compCode = "null";
        String jumpCode = "null";
        if (line.length() > 0) {
            if (line.contains("=")) {
                destCode =  DEST.get(line.split("=",2)[0]);
                compCode = COMP.get(line.split("=",2)[1]);
                jumpCode = JUMP.get("null");
            } else if (line.contains(";")) {
                destCode = DEST.get("null");
                compCode = COMP.get(line.split(";",2)[0]);
                jumpCode = JUMP.get(line.split(";",2)[1]);
            }
        }
        return "111" + compCode + destCode + jumpCode;
    }
    // check if number
    public static boolean isNumeric(String string) {
        String regex = "[0-9]+[\\.]?[0-9]*";
        return Pattern.matches(regex, string);
    }
}