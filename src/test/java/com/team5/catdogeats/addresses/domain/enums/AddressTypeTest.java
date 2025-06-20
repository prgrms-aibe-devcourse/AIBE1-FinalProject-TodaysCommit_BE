package com.team5.catdogeats.addresses.domain.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AddressType Enum 테스트")
class AddressTypeTest {

    @Test
    @DisplayName("개인주소 타입의 설명이 올바르게 반환된다")
    void getDescription_Personal_ReturnsCorrectDescription() {
        // given
        AddressType addressType = AddressType.PERSONAL;

        // when
        String description = addressType.getDescription();

        // then
        assertThat(description).isEqualTo("개인주소");
    }

    @Test
    @DisplayName("사업자주소 타입의 설명이 올바르게 반환된다")
    void getDescription_Business_ReturnsCorrectDescription() {
        // given
        AddressType addressType = AddressType.BUSINESS;

        // when
        String description = addressType.getDescription();

        // then
        assertThat(description).isEqualTo("사업자주소");
    }

    @Test
    @DisplayName("모든 AddressType 열거형 값이 존재한다")
    void values_ReturnsAllEnumValues() {
        // when
        AddressType[] addressTypes = AddressType.values();

        // then
        assertThat(addressTypes).hasSize(2);
        assertThat(addressTypes).containsExactly(AddressType.PERSONAL, AddressType.BUSINESS);
    }

    @Test
    @DisplayName("문자열로부터 AddressType을 올바르게 변환한다")
    void valueOf_ValidString_ReturnsCorrectEnum() {
        // when & then
        assertThat(AddressType.valueOf("PERSONAL")).isEqualTo(AddressType.PERSONAL);
        assertThat(AddressType.valueOf("BUSINESS")).isEqualTo(AddressType.BUSINESS);
    }
}