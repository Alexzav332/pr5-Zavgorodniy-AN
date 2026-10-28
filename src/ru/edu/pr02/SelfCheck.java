package ru.edu.pr02;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/** Единственная точка входа: unit-проверки, data-driven режим, интерактивный режим. */
public class SelfCheck {
    private static final String DATA_FILE = "data/sample.csv";
    private static int unitTotal = 0;
    private static int unitPassed = 0;

    /** Без аргументов выполняются все режимы; можно указать: unit | data | interactive. */
    public static void main(String[] args) {
        String mode = args.length > 0 ? args[0] : "all";
        boolean all = mode.equals("all");
        boolean ok = true;
        if (all || mode.equals("unit")) {
            ok = runUnitChecks() && ok;
        }
        if (all || mode.equals("data")) {
            ok = runDataDriven() && ok;
        }
        if (!ok) {
            System.out.println("Есть FAIL – исправьте до защиты.");
            System.exit(1);
        }
        if (all || mode.equals("interactive")) {
            runInteractive();
        }
    }

    // ---------------------------------------------------------------- Режим 1
    private static boolean runUnitChecks() {
        System.out.println("=== Режим 1: unit-проверки ===");
        try {
            checkParseHours();
            checkTotal();
            checkAverage();
            checkOvertimeDays();
            checkWeeklyOvertime();
            checkVariantPartA();
            checkVariantPartB();
        } catch (RuntimeException e) {
            check("неожиданное исключение: " + e, false);
        }
        System.out.println("Unit: " + unitPassed + "/" + unitTotal + " passed.");
        return unitPassed == unitTotal;
    }

    private static void check(String name, boolean ok) {
        unitTotal++;
        if (ok) {
            unitPassed++;
        }
        System.out.println((ok ? "PASS " : "FAIL ") + name);
    }

    private static String parseError(String line) {
        try {
            TimesheetAnalyzer.parseHours(line);
            return null;
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    private static boolean has(String message, String part) {
        return message != null && message.contains(part);
    }

    private static void checkParseHours() {
        // parseHours: корректная строка (7 значений)
        check("parseHours: корректная строка", Arrays.equals(
                TimesheetAnalyzer.parseHours("8;9;10;8;8;0;0"), new int[]{8, 9, 10, 8, 8, 0, 0}));
        // parseHours: пустая строка -> ошибка структуры
        check("parseHours: пустая строка", has(parseError(""), "структура"));
        // parseHours: значение 17 на позиции 3 (граница диапазона)
        check("parseHours: значение 17", has(parseError("8;8;17;8;8;0;0"), "позиция 3"));
        // parseHours: не число на позиции 2
        check("parseHours: не число", has(parseError("8;abc;8;8;8;0;0"), "позиция 2"));
        // parseHours: 4 значения -> ошибка структуры
        check("parseHours: 4 значения", has(parseError("8;8;8;8"), "структура"));
        // parseHours: trailing ';' даёт 8 элементов -> ошибка структуры
        check("parseHours: trailing ;", has(parseError("8;8;8;8;8;0;0;"), "структура"));
        // parseHours: ведущий ноль на позиции 1
        check("parseHours: ведущий ноль", has(parseError("08;8;8;8;8;0;0"), "позиция 1"));
        // parseHours (доп.): пробел, пустое поле, знак +, дробное, 16 допустимо
        check("parseHours: пробел после ;", has(parseError("8; 9;10;8;8;0;0"), "позиция 2"));
        check("parseHours: пустое поле", has(parseError("8;;9;10;8;0;0"), "позиция 2"));
        check("parseHours: знак +", has(parseError("+8;8;8;8;8;0;0"), "позиция 1"));
        check("parseHours: дробное 8.5", has(parseError("8;8.5;8;8;8;0;0"), "позиция 2"));
        check("parseHours: 16 допустимо", parseError("16;16;16;16;16;16;16") == null);
        check("parseHours: null", has(parseError(null), "структура"));
    }

    private static void checkTotal() {
        // total: типовое, нижняя и верхняя граница
        check("total: типовое {8,9,10,8,8,0,0} = 43",
                TimesheetAnalyzer.total(new int[]{8, 9, 10, 8, 8, 0, 0}) == 43);
        check("total: нижняя граница = 0", TimesheetAnalyzer.total(new int[7]) == 0);
        check("total: верхняя граница = 112",
                TimesheetAnalyzer.total(new int[]{16, 16, 16, 16, 16, 16, 16}) == 112);
        // total: null -> IAE "hours is null"
        check("total: null", has(totalError(null), "hours is null"));
    }

    private static String totalError(int[] hours) {
        try {
            TimesheetAnalyzer.total(hours);
            return null;
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    private static boolean near(double actual, double expected) {
        return Math.abs(actual - expected) < 1e-9;
    }

    private static void checkAverage() {
        // average: типовое 43/7 = 6.14, целое 8.0, дробное 55/7 = 7.86
        check("average: типовое = 6.14",
                near(TimesheetAnalyzer.average(new int[]{8, 9, 10, 8, 8, 0, 0}), 6.14));
        check("average: целое = 8.0",
                near(TimesheetAnalyzer.average(new int[]{8, 8, 8, 8, 8, 8, 8}), 8.0));
        check("average: дробное = 7.86",
                near(TimesheetAnalyzer.average(new int[]{7, 8, 9, 8, 7, 8, 8}), 7.86));
        // average: длина != 7 -> IAE
        String error = null;
        try {
            TimesheetAnalyzer.average(new int[]{8, 8, 8, 8, 8});
        } catch (IllegalArgumentException e) {
            error = e.getMessage();
        }
        check("average: длина 5 -> ошибка", has(error, "hours length must be 7, got 5"));
    }

    private static void checkOvertimeDays() {
        // overtimeDays: нет переработки, два дня, граница 8 (8 не переработка)
        check("overtimeDays: нет переработки",
                TimesheetAnalyzer.overtimeDays(new int[]{8, 8, 8, 8, 8, 8, 8}) == 0);
        check("overtimeDays: два дня",
                TimesheetAnalyzer.overtimeDays(new int[]{9, 8, 7, 10, 8, 0, 0}) == 2);
        check("overtimeDays: граница 8",
                TimesheetAnalyzer.overtimeDays(new int[]{8, 9, 8, 8, 8, 0, 0}) == 1);
    }

    private static void checkWeeklyOvertime() {
        // weeklyOvertime: сумма 40, сумма 50, сумма < 40
        check("weeklyOvertime: сумма 40 -> 0",
                TimesheetAnalyzer.weeklyOvertime(new int[]{8, 8, 8, 8, 8, 0, 0}) == 0);
        check("weeklyOvertime: сумма 50 -> 10",
                TimesheetAnalyzer.weeklyOvertime(new int[]{10, 10, 10, 10, 10, 0, 0}) == 10);
        check("weeklyOvertime: сумма 30 -> 0",
                TimesheetAnalyzer.weeklyOvertime(new int[]{6, 6, 6, 6, 6, 0, 0}) == 0);
        // согласованность: weeklyOvertime == max(0, total - 40)
        int[] h = {9, 9, 9, 9, 9, 9, 9};
        check("weeklyOvertime: согласован с total",
                TimesheetAnalyzer.weeklyOvertime(h) == Math.max(0, TimesheetAnalyzer.total(h) - 40));
    }

    private static void checkVariantPartA() {
        // Часть А: 7 значений разбираются корректно
        int[] parsed = TimesheetAnalyzer.parseHours("1;2;3;4;5;6;7");
        check("Часть А: 7 значений -> int[7]", parsed.length == 7 && parsed[6] == 7);
        // Часть А: 5 значений -> ошибка структуры (ожидалось 7)
        check("Часть А: 5 значений -> ожидалось 7, получено 5",
                has(parseError("8;8;8;8;8"), "структура – ожидалось 7 значений, получено 5"));
    }

    private static void checkVariantPartB() {
        // Часть Б: addWeekendHours на {8,8,8,8,8,2,3} -> 5, на {8,8,8,8,8,0,0} -> 0
        check("Часть Б: addWeekendHours {..,2,3} = 5",
                TimesheetAnalyzer.addWeekendHours(new int[]{8, 8, 8, 8, 8, 2, 3}) == 5);
        check("Часть Б: addWeekendHours {..,0,0} = 0",
                TimesheetAnalyzer.addWeekendHours(new int[]{8, 8, 8, 8, 8, 0, 0}) == 0);
        // Часть Б: защита null и длины
        String nullError = null;
        String lengthError = null;
        try {
            TimesheetAnalyzer.addWeekendHours(null);
        } catch (IllegalArgumentException e) {
            nullError = e.getMessage();
        }
        try {
            TimesheetAnalyzer.addWeekendHours(new int[5]);
        } catch (IllegalArgumentException e) {
            lengthError = e.getMessage();
        }
        check("Часть Б: null -> IAE", has(nullError, "hours is null"));
        check("Часть Б: длина 5 -> IAE", has(lengthError, "hours length must be 7, got 5"));
    }

    // ---------------------------------------------------------------- Режим 2
    private static boolean runDataDriven() {
        System.out.println("=== Режим 2: data-driven (" + DATA_FILE + ") ===");
        List<String> lines;
        try {
            lines = Files.readAllLines(Paths.get(DATA_FILE), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.println("FAIL data: не удалось прочитать " + DATA_FILE
                    + " (запускайте из корня проекта)");
            return false;
        }
        int n = 0;
        int passed = 0;
        for (String raw : lines) {
            String line = raw.replace("\uFEFF", "");
            if (line.trim().isEmpty() || line.startsWith("#")) {
                continue;
            }
            n++;
            String reason = checkCase(line);
            System.out.println(reason == null ? "PASS data/" + n : "FAIL data/" + n + ": " + reason);
            if (reason == null) {
                passed++;
            }
        }
        System.out.println("Data-driven: " + passed + "/" + n + " passed.");
        return passed == n;
    }

    /** Возвращает null, если случай пройден, иначе причину. */
    private static String checkCase(String line) {
        String[] parts = line.split("\\|", -1);
        if (parts.length < 3) {
            return "строка файла должна иметь вид <вход>|<ожидание>|<комментарий>";
        }
        String input = parts[0].equals("EMPTY") ? "" : parts[0];
        if (parts[1].startsWith("OK:")) {
            return checkOkCase(input, parts[1].substring(3));
        }
        if (parts[1].startsWith("ERROR:")) {
            return checkErrorCase(input, parts[1].substring(6));
        }
        return "неизвестное ожидание: " + parts[1];
    }

    private static String checkOkCase(String input, String expected) {
        try {
            int[] h = TimesheetAnalyzer.parseHours(input);
            String actual = "sum=" + TimesheetAnalyzer.total(h)
                    + ",avg=" + String.format(Locale.US, "%.2f", TimesheetAnalyzer.average(h))
                    + ",od=" + TimesheetAnalyzer.overtimeDays(h)
                    + ",wo=" + TimesheetAnalyzer.weeklyOvertime(h);
            return actual.equals(expected) ? null : "ожидалось " + expected + ", получено " + actual;
        } catch (IllegalArgumentException e) {
            return "неожиданная ошибка: " + e.getMessage();
        }
    }

    private static String checkErrorCase(String input, String expected) {
        String message = parseError(input);
        if (message == null) {
            return "ожидалась ошибка \"" + expected + "\", но разбор прошёл";
        }
        return message.contains(expected) ? null
                : "ожидалось \"" + expected + "\", сообщение: " + message;
    }

    // ---------------------------------------------------------------- Режим 3
    private static void runInteractive() {
        System.out.println("=== Режим 3: интерактивный (exit или пустая строка – выход) ===");
        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
        while (true) {
            System.out.print("Табель (7 значений через ;): ");
            if (!scanner.hasNextLine()) {
                break;
            }
            String line = scanner.nextLine();
            if (line.isEmpty() || line.trim().equals("exit")) {
                break;
            }
            printReport(line);
        }
        System.out.println("Выход.");
    }

    private static void printReport(String line) {
        try {
            int[] h = TimesheetAnalyzer.parseHours(line);
            System.out.println("Сумма: " + TimesheetAnalyzer.total(h));
            System.out.println("Среднее: " + String.format(Locale.US, "%.2f", TimesheetAnalyzer.average(h)));
            System.out.println("Дней с переработкой: " + TimesheetAnalyzer.overtimeDays(h));
            System.out.println("Недельная переработка: " + TimesheetAnalyzer.weeklyOvertime(h));
            System.out.println("Часов в выходные: " + TimesheetAnalyzer.addWeekendHours(h));
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
    }
}
