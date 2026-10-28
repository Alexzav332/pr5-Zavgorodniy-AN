package ru.edu.pr02;

/**
 * Анализ табеля учёта рабочего времени (вариант 12: неделя из 7 дней, пн-вс).
 * Класс stateless: только статические методы, без ввода-вывода.
 */
public class TimesheetAnalyzer {
    private static final int DAYS_COUNT = 7;
    private static final int MIN_HOURS = 0;
    private static final int MAX_HOURS = 16;
    private static final int OVERTIME_THRESHOLD = 8;
    private static final int WEEKLY_NORM = 40;
    private static final int FIRST_WEEKEND_INDEX = 5;
    private static final String HOURS_REGEX = "^(0|[1-9][0-9]*)$";

    /** Единственная точка валидации строкового ввода (R1-R3, R7-R9). */
    public static int[] parseHours(String line) {
        if (line == null) {
            throw new IllegalArgumentException("Ошибка: структура – строка не задана (null)");
        }
        String[] parts = line.split(";", -1);
        if (parts.length != DAYS_COUNT) {
            throw new IllegalArgumentException("Ошибка: структура – ожидалось "
                    + DAYS_COUNT + " значений, получено " + parts.length);
        }
        int[] result = new int[DAYS_COUNT];
        for (int i = 0; i < DAYS_COUNT; i++) {
            result[i] = parseValue(parts[i], i + 1);
        }
        return result;
    }

    private static int parseValue(String text, int position) {
        if (text.isEmpty()) {
            throw positionError(position, "пустое поле");
        }
        if (!text.matches(HOURS_REGEX)) {
            throw positionError(position, "значение \"" + text
                    + "\" не является целым числом без знака и ведущих нулей");
        }
        int value;
        try {
            value = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw rangeError(text, position);
        }
        if (value < MIN_HOURS || value > MAX_HOURS) {
            throw rangeError(text, position);
        }
        return value;
    }

    private static IllegalArgumentException positionError(int position, String description) {
        return new IllegalArgumentException("Ошибка: позиция " + position + " – " + description);
    }

    private static IllegalArgumentException rangeError(String text, int position) {
        return positionError(position, "значение " + text
                + " вне диапазона [" + MIN_HOURS + ", " + MAX_HOURS + "]");
    }

    /** Защита от программистских ошибок (не пользовательский ввод). */
    private static void checkHours(int[] hours) {
        if (hours == null) {
            throw new IllegalArgumentException("hours is null");
        }
        if (hours.length != DAYS_COUNT) {
            throw new IllegalArgumentException("hours length must be "
                    + DAYS_COUNT + ", got " + hours.length);
        }
    }

    public static int total(int[] hours) {
        checkHours(hours);
        int sum = 0;
        for (int i = 0; i < hours.length; i++) {
            sum += hours[i];
        }
        return sum;
    }

    public static double average(int[] hours) {
        double value = (double) total(hours) / DAYS_COUNT;
        return Math.round(value * 100.0) / 100.0;
    }

    /** Количество дней, где часов строго больше порога (R5). */
    public static int overtimeDays(int[] hours) {
        checkHours(hours);
        int count = 0;
        for (int i = 0; i < hours.length; i++) {
            if (hours[i] > OVERTIME_THRESHOLD) {
                count++;
            }
        }
        return count;
    }

    /** Недельная переработка = max(0, сумма - норма) (R6). */
    public static int weeklyOvertime(int[] hours) {
        return Math.max(0, total(hours) - WEEKLY_NORM);
    }

    /** Вариант 12, часть Б: сумма часов в субботу и воскресенье (позиции 6 и 7). */
    public static int addWeekendHours(int[] hours) {
        checkHours(hours);
        return hours[FIRST_WEEKEND_INDEX] + hours[FIRST_WEEKEND_INDEX + 1];
    }
}
