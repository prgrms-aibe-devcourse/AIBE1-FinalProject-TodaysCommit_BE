package com.team5.catdogeats.users.domain.enums;

/**
 * 요일 Enum
 * 휴무일 관리를 위한 요일 정의
 */
public enum DayOfWeek {
    MONDAY("월요일"),
    TUESDAY("화요일"),
    WEDNESDAY("수요일"),
    THURSDAY("목요일"),
    FRIDAY("금요일"),
    SATURDAY("토요일"),
    SUNDAY("일요일");

    private final String koreanName;

    DayOfWeek(String koreanName) {
        this.koreanName = koreanName;
    }

    public String getKoreanName() {
        return koreanName;
    }

    /**
     * 한글 요일명으로 Enum 찾기
     */
    public static DayOfWeek fromKoreanName(String koreanName) {
        for (DayOfWeek day : values()) {
            if (day.koreanName.equals(koreanName)) {
                return day;
            }
        }
        throw new IllegalArgumentException("유효하지 않은 요일입니다: " + koreanName);
    }

    /**
     * 쉼표로 구분된 한글 요일 문자열을 파싱
     * 예: "월요일,화요일,수요일" -> [MONDAY, TUESDAY, WEDNESDAY]
     */
    public static java.util.List<DayOfWeek> parseFromString(String daysString) {
        if (daysString == null || daysString.trim().isEmpty()) {
            return java.util.Collections.emptyList();
        }

        return java.util.Arrays.stream(daysString.split(","))
                .map(String::trim)
                .filter(day -> !day.isEmpty())
                .map(DayOfWeek::fromKoreanName)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * DayOfWeek 리스트를 쉼표로 구분된 한글 문자열로 변환
     * 예: [MONDAY, TUESDAY] -> "월요일,화요일"
     */
    public static String toStringFromList(java.util.List<DayOfWeek> days) {
        if (days == null || days.isEmpty()) {
            return null;
        }

        return days.stream()
                .map(DayOfWeek::getKoreanName)
                .collect(java.util.stream.Collectors.joining(","));
    }
}
