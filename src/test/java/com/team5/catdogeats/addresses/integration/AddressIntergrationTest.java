package com.team5.catdogeats.addresses.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team5.catdogeats.addresses.domain.Addresses;
import com.team5.catdogeats.addresses.domain.enums.AddressType;
import com.team5.catdogeats.addresses.dto.AddressRequestDto;
import com.team5.catdogeats.addresses.dto.AddressUpdateRequestDto;
import com.team5.catdogeats.addresses.repository.AddressRepository;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Address 통합 테스트")
class AddressIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Users testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성 및 저장
        testUser = Users.builder()
                .name("테스트사용자")
                .role(Role.ROLE_BUYER)
                .accountDisable(false)
                .userNameAttribute("test")
                .provider("test")
                .providerId("test")
                .build();

        testUser = userRepository.save(testUser);
        userId = testUser.getId();
    }

    @Test
    @DisplayName("주소 생성부터 삭제까지 전체 플로우 테스트")
    void completeAddressFlow() throws Exception {
        // 1. 주소 생성
        AddressRequestDto createRequest = AddressRequestDto.builder()
                .title("집")
                .city("서울특별시")
                .district("강남구")
                .neighborhood("역삼동")
                .streetAddress("테헤란로 123")
                .postalCode("06234")
                .detailAddress("456호")
                .phoneNumber("010-1234-5678")
                .addressType(AddressType.PERSONAL)
                .isDefault(true)
                .build();

        String createResponse = mockMvc.perform(post("/v1/buyers/address")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("집"))
                .andExpect(jsonPath("$.isDefault").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 생성된 주소 ID 추출
        UUID addressId = UUID.fromString(
                objectMapper.readTree(createResponse).get("id").asText()
        );

        // 2. 주소 목록 조회 - 생성된 주소 확인
        mockMvc.perform(get("/v1/buyers/address")
                        .header("X-User-Id", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addresses").isArray())
                .andExpect(jsonPath("$.addresses[0].title").value("집"))
                .andExpect(jsonPath("$.totalElements").value(1));

        // 3. 기본 주소 조회
        mockMvc.perform(get("/v1/buyers/address/default")
                        .header("X-User-Id", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(addressId.toString()))
                .andExpect(jsonPath("$.isDefault").value(true));

        // 4. 두 번째 주소 생성
        AddressRequestDto secondAddressRequest = AddressRequestDto.builder()
                .title("회사")
                .city("경기도")
                .district("성남시")
                .neighborhood("분당구")
                .streetAddress("판교역로 789")
                .postalCode("13494")
                .detailAddress("101동 202호")
                .phoneNumber("010-9876-5432")
                .addressType(AddressType.PERSONAL)
                .isDefault(false)
                .build();

        String secondCreateResponse = mockMvc.perform(post("/v1/buyers/address")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondAddressRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("회사"))
                .andExpect(jsonPath("$.isDefault").value(false))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID secondAddressId = UUID.fromString(
                objectMapper.readTree(secondCreateResponse).get("id").asText()
        );

        // 5. 주소 목록 조회 - 2개 주소 확인 (기본 주소 먼저)
        mockMvc.perform(get("/v1/buyers/address")
                        .header("X-User-Id", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addresses").isArray())
                .andExpect(jsonPath("$.addresses").value(org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$.addresses[0].isDefault").value(true)) // 기본 주소가 먼저
                .andExpect(jsonPath("$.addresses[1].isDefault").value(false))
                .andExpect(jsonPath("$.totalElements").value(2));

        // 6. 주소 수정
        AddressUpdateRequestDto updateRequest = AddressUpdateRequestDto.builder()
                .title("수정된 회사")
                .city("서울특별시")
                .district("서초구")
                .build();

        mockMvc.perform(patch("/v1/buyers/address/{addressId}", secondAddressId)
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("수정된 회사"))
                .andExpect(jsonPath("$.city").value("서울특별시"))
                .andExpect(jsonPath("$.district").value("서초구"));

        // 7. 기본 주소 변경
        mockMvc.perform(patch("/v1/buyers/address/{addressId}/default", secondAddressId)
                        .header("X-User-Id", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(secondAddressId.toString()))
                .andExpect(jsonPath("$.isDefault").value(true));

        // 8. 변경된 기본 주소 확인
        mockMvc.perform(get("/v1/buyers/address/default")
                        .header("X-User-Id", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(secondAddressId.toString()))
                .andExpect(jsonPath("$.title").value("수정된 회사"))
                .andExpect(jsonPath("$.isDefault").value(true));

        // 9. 주소 상세 조회
        mockMvc.perform(get("/v1/buyers/address/{addressId}", addressId)
                        .header("X-User-Id", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(addressId.toString()))
                .andExpect(jsonPath("$.title").value("집"))
                .andExpect(jsonPath("$.isDefault").value(false)); // 더 이상 기본 주소가 아님

        // 10. 주소 삭제
        mockMvc.perform(delete("/v1/buyers/address/{addressId}", addressId)
                        .header("X-User-Id", userId))
                .andDo(print())
                .andExpect(status().isNoContent());

        // 11. 삭제 확인 - 1개 주소만 남음
        mockMvc.perform(get("/v1/buyers/address")
                        .header("X-User-Id", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addresses").value(org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.addresses[0].id").value(secondAddressId.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));

        // 12. 데이터베이스에서 직접 확인
        List<Addresses> remainingAddresses = addressRepository.findByUserIdAndAddressTypeOrderByIsDefaultDescCreatedAtDesc(
                userId, AddressType.PERSONAL);

        assertThat(remainingAddresses).hasSize(1);
        assertThat(remainingAddresses.get(0).getId()).isEqualTo(secondAddressId);
        assertThat(remainingAddresses.get(0).getTitle()).isEqualTo("수정된 회사");
        assertThat(remainingAddresses.get(0).isDefault()).isTrue();
    }

    @Test
    @DisplayName("주소 개수 제한 테스트")
    void addressLimitTest() throws Exception {
        // 개인 주소 10개 생성
        for (int i = 1; i <= 10; i++) {
            AddressRequestDto request = AddressRequestDto.builder()
                    .title("주소" + i)
                    .city("서울특별시")
                    .district("강남구")
                    .neighborhood("역삼동")
                    .streetAddress("테헤란로 " + (100 + i))
                    .postalCode("0623" + i)
                    .detailAddress(i + "호")
                    .phoneNumber("010-1234-567" + i)
                    .addressType(AddressType.PERSONAL)
                    .isDefault(i == 1)
                    .build();

            mockMvc.perform(post("/v1/buyers/address")
                            .header("X-User-Id", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk());
        }

        // 11번째 주소 생성 시도 - 실패해야 함
        AddressRequestDto eleventhRequest = AddressRequestDto.builder()
                .title("주소11")
                .city("서울특별시")
                .district("강남구")
                .neighborhood("역삼동")
                .streetAddress("테헤란로 111")
                .postalCode("06235")
                .detailAddress("11호")
                .phoneNumber("010-1234-5670")
                .addressType(AddressType.PERSONAL)
                .isDefault(false)
                .build();

        mockMvc.perform(post("/v1/buyers/address")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eleventhRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("개인주소은(는) 최대 10개까지만 등록할 수 있습니다."));

        // 데이터베이스에서 주소 개수 확인
        long addressCount = addressRepository.countByUserIdAndAddressType(userId, AddressType.PERSONAL);
        assertThat(addressCount).isEqualTo(10);
    }

    @Test
    @DisplayName("기본 주소 변경 시나리오 테스트")
    void defaultAddressChangeScenario() throws Exception {
        // 첫 번째 주소 생성 (기본 주소)
        AddressRequestDto firstRequest = AddressRequestDto.builder()
                .title("첫번째주소")
                .city("서울특별시")
                .district("강남구")
                .neighborhood("역삼동")
                .streetAddress("테헤란로 123")
                .postalCode("06234")
                .detailAddress("456호")
                .phoneNumber("010-1234-5678")
                .addressType(AddressType.PERSONAL)
                .isDefault(true)
                .build();

        String firstResponse = mockMvc.perform(post("/v1/buyers/address")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID firstAddressId = UUID.fromString(
                objectMapper.readTree(firstResponse).get("id").asText()
        );

        // 두 번째 주소 생성 (기본 주소로 설정)
        AddressRequestDto secondRequest = AddressRequestDto.builder()
                .title("두번째주소")
                .city("부산광역시")
                .district("해운대구")
                .neighborhood("우동")
                .streetAddress("해운대로 789")
                .postalCode("48094")
                .detailAddress("101호")
                .phoneNumber("051-1234-5678")
                .addressType(AddressType.PERSONAL)
                .isDefault(true) // 새로운 기본 주소
                .build();

        String secondResponse = mockMvc.perform(post("/v1/buyers/address")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID secondAddressId = UUID.fromString(
                objectMapper.readTree(secondResponse).get("id").asText()
        );

        // 기본 주소 확인 - 두 번째 주소가 기본 주소여야 함
        mockMvc.perform(get("/v1/buyers/address/default")
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(secondAddressId.toString()))
                .andExpect(jsonPath("$.title").value("두번째주소"));

        // 데이터베이스에서 직접 확인
        List<Addresses> addresses = addressRepository.findByUserIdAndAddressTypeOrderByIsDefaultDescCreatedAtDesc(
                userId, AddressType.PERSONAL);

        assertThat(addresses).hasSize(2);

        Addresses defaultAddress = addresses.stream()
                .filter(Addresses::isDefault)
                .findFirst()
                .orElseThrow();

        assertThat(defaultAddress.getId()).isEqualTo(secondAddressId);
        assertThat(defaultAddress.getTitle()).isEqualTo("두번째주소");

        // 첫 번째 주소는 더 이상 기본 주소가 아님
        Addresses firstAddress = addressRepository.findById(firstAddressId).orElseThrow();
        assertThat(firstAddress.isDefault()).isFalse();
    }

    @Test
    @DisplayName("권한 검증 테스트 - 다른 사용자의 주소 접근")
    void accessControlTest() throws Exception {
        // 다른 사용자 생성
        Users otherUser = Users.builder()
                .name("다른사용자")
                .role(Role.ROLE_BUYER)
                .accountDisable(false)
                .userNameAttribute("other")
                .provider("test")
                .providerId("other")
                .build();

        otherUser = userRepository.save(otherUser);
        UUID otherUserId = otherUser.getId();

        // 첫 번째 사용자의 주소 생성
        AddressRequestDto request = AddressRequestDto.builder()
                .title("내주소")
                .city("서울특별시")
                .district("강남구")
                .neighborhood("역삼동")
                .streetAddress("테헤란로 123")
                .postalCode("06234")
                .detailAddress("456호")
                .phoneNumber("010-1234-5678")
                .addressType(AddressType.PERSONAL)
                .isDefault(true)
                .build();

        String response = mockMvc.perform(post("/v1/buyers/address")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID addressId = UUID.fromString(
                objectMapper.readTree(response).get("id").asText()
        );

        // 다른 사용자가 주소 상세 조회 시도 - 실패해야 함
        mockMvc.perform(get("/v1/buyers/address/{addressId}", addressId)
                        .header("X-User-Id", otherUserId))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("해당 주소에 접근할 권한이 없습니다."));

        // 다른 사용자가 주소 수정 시도 - 실패해야 함
        AddressUpdateRequestDto updateRequest = AddressUpdateRequestDto.builder()
                .title("해킹시도")
                .build();

        mockMvc.perform(patch("/v1/buyers/address/{addressId}", addressId)
                        .header("X-User-Id", otherUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("해당 주소에 접근할 권한이 없습니다."));

        // 다른 사용자가 주소 삭제 시도 - 실패해야 함
        mockMvc.perform(delete("/v1/buyers/address/{addressId}", addressId)
                        .header("X-User-Id", otherUserId))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("해당 주소에 접근할 권한이 없습니다."));
    }
}