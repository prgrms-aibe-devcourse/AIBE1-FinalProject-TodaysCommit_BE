package com.team5.catdogeats.addresses.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team5.catdogeats.addresses.domain.enums.AddressType;
import com.team5.catdogeats.addresses.dto.*;
import com.team5.catdogeats.addresses.exception.AddressAccessDeniedException;
import com.team5.catdogeats.addresses.exception.AddressNotFoundException;
import com.team5.catdogeats.addresses.exception.UserNotFoundException;
import com.team5.catdogeats.addresses.service.AddressService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BuyerAddressController.class)
@DisplayName("BuyerAddressController 테스트")
class BuyerAddressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AddressService addressService;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID userId;
    private UUID addressId;
    private AddressResponseDto addressResponseDto;
    private AddressListResponseDto addressListResponseDto;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        addressId = UUID.randomUUID();

        addressResponseDto = AddressResponseDto.builder()
                .id(addressId)
                .title("집")
                .city("서울특별시")
                .district("강남구")
                .neighborhood("역삼동")
                .streetAddress("테헤란로 123")
                .postalCode("06234")
                .detailAddress("456호")
                .phoneNumber("010-1234-5678")
                .addressType(AddressType.PERSONAL)
                .isDefault(false)
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();

        addressListResponseDto = AddressListResponseDto.builder()
                .addresses(Arrays.asList(addressResponseDto))
                .totalElements(1)
                .totalPages(1)
                .currentPage(0)
                .pageSize(10)
                .hasNext(false)
                .hasPrevious(false)
                .build();
    }

    @DisplayName("주소 목록 조회 테스트")
    class GetAddressesTest {

        @Test
        @DisplayName("주소 목록 조회 - 성공")
        void getAddresses_Success() throws Exception {
            // given
            given(addressService.getAddressesByUserAndType(
                    eq(userId), eq(AddressType.PERSONAL), any(PageRequest.class)))
                    .willReturn(addressListResponseDto);

            // when & then
            mockMvc.perform(get("/v1/buyers/address")
                            .header("X-User-Id", userId)
                            .param("page", "0")
                            .param("size", "10"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.addresses").isArray())
                    .andExpect(jsonPath("$.addresses[0].title").value("집"))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.currentPage").value(0));
        }

        @Test
        @DisplayName("주소 목록 조회 - 기본 페이징 값 적용")
        void getAddresses_DefaultPaging() throws Exception {
            // given
            given(addressService.getAddressesByUserAndType(
                    eq(userId), eq(AddressType.PERSONAL), any(PageRequest.class)))
                    .willReturn(addressListResponseDto);

            // when & then
            mockMvc.perform(get("/v1/buyers/address")
                            .header("X-User-Id", userId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.currentPage").value(0))
                    .andExpect(jsonPath("$.pageSize").value(10));
        }

        @Test
        @DisplayName("사용자가 존재하지 않으면 404 에러")
        void getAddresses_UserNotFound() throws Exception {
            // given
            given(addressService.getAddressesByUserAndType(
                    eq(userId), eq(AddressType.PERSONAL), any(PageRequest.class)))
                    .willThrow(new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));

            // when & then
            mockMvc.perform(get("/v1/buyers/address")
                            .header("X-User-Id", userId))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다: " + userId));
        }
    }

    @DisplayName("전체 주소 목록 조회 테스트")
    class GetAllAddressesTest {

        @Test
        @DisplayName("전체 주소 목록 조회 - 성공")
        void getAllAddresses_Success() throws Exception {
            // given
            List<AddressResponseDto> addresses = Arrays.asList(addressResponseDto);
            given(addressService.getAllAddressesByUserAndType(userId, AddressType.PERSONAL))
                    .willReturn(addresses);

            // when & then
            mockMvc.perform(get("/v1/buyers/address/all")
                            .header("X-User-Id", userId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].title").value("집"));
        }
    }

    @DisplayName("주소 상세 조회 테스트")
    class GetAddressByIdTest {

        @Test
        @DisplayName("주소 상세 조회 - 성공")
        void getAddressById_Success() throws Exception {
            // given
            given(addressService.getAddressById(addressId, userId))
                    .willReturn(addressResponseDto);

            // when & then
            mockMvc.perform(get("/v1/buyers/address/{addressId}", addressId)
                            .header("X-User-Id", userId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(addressId.toString()))
                    .andExpect(jsonPath("$.title").value("집"));
        }

        @Test
        @DisplayName("주소가 존재하지 않으면 404 에러")
        void getAddressById_NotFound() throws Exception {
            // given
            given(addressService.getAddressById(addressId, userId))
                    .willThrow(new AddressNotFoundException("주소를 찾을 수 없습니다: " + addressId));

            // when & then
            mockMvc.perform(get("/v1/buyers/address/{addressId}", addressId)
                            .header("X-User-Id", userId))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("주소를 찾을 수 없습니다: " + addressId));
        }

        @Test
        @DisplayName("다른 사용자의 주소 조회 시 403 에러")
        void getAddressById_AccessDenied() throws Exception {
            // given
            given(addressService.getAddressById(addressId, userId))
                    .willThrow(new AddressAccessDeniedException("해당 주소에 접근할 권한이 없습니다."));

            // when & then
            mockMvc.perform(get("/v1/buyers/address/{addressId}", addressId)
                            .header("X-User-Id", userId))
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("해당 주소에 접근할 권한이 없습니다."));
        }
    }

    @DisplayName("주소 생성 테스트")
    class CreateAddressTest {

        @Test
        @DisplayName("주소 생성 - 성공")
        void createAddress_Success() throws Exception {
            // given
            AddressRequestDto requestDto = AddressRequestDto.builder()
                    .title("새주소")
                    .city("서울특별시")
                    .district("강남구")
                    .neighborhood("역삼동")
                    .streetAddress("테헤란로 456")
                    .postalCode("06234")
                    .detailAddress("789호")
                    .phoneNumber("010-9999-8888")
                    .addressType(AddressType.BUSINESS) // 컨트롤러에서 PERSONAL로 강제 변경
                    .isDefault(false)
                    .build();

            given(addressService.createAddress(any(AddressRequestDto.class), eq(userId)))
                    .willReturn(addressResponseDto);

            // when & then
            mockMvc.perform(post("/v1/buyers/address")
                            .header("X-User-Id", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(addressId.toString()))
                    .andExpect(jsonPath("$.title").value("집"));
        }

        @Test
        @DisplayName("유효하지 않은 입력 데이터 - 400 에러")
        void createAddress_InvalidInput() throws Exception {
            // given
            AddressRequestDto invalidRequestDto = AddressRequestDto.builder()
                    .title("") // 빈 제목
                    .city("") // 빈 시
                    .build();

            // when & then
            mockMvc.perform(post("/v1/buyers/address")
                            .header("X-User-Id", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequestDto)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
        }

        @Test
        @DisplayName("주소 개수 제한 초과 - 400 에러")
        void createAddress_ExceedsLimit() throws Exception {
            // given
            AddressRequestDto requestDto = AddressRequestDto.builder()
                    .title("새주소")
                    .city("서울특별시")
                    .district("강남구")
                    .neighborhood("역삼동")
                    .streetAddress("테헤란로 456")
                    .postalCode("06234")
                    .detailAddress("789호")
                    .phoneNumber("010-9999-8888")
                    .addressType(AddressType.PERSONAL)
                    .build();

            given(addressService.createAddress(any(AddressRequestDto.class), eq(userId)))
                    .willThrow(new IllegalArgumentException("개인주소은(는) 최대 10개까지만 등록할 수 있습니다."));

            // when & then
            mockMvc.perform(post("/v1/buyers/address")
                            .header("X-User-Id", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("개인주소은(는) 최대 10개까지만 등록할 수 있습니다."));
        }
    }

    @DisplayName("주소 수정 테스트")
    class UpdateAddressTest {

        @Test
        @DisplayName("주소 수정 - 성공")
        void updateAddress_Success() throws Exception {
            // given
            AddressUpdateRequestDto updateDto = AddressUpdateRequestDto.builder()
                    .title("수정된 제목")
                    .city("부산광역시")
                    .build();

            AddressResponseDto updatedAddress = AddressResponseDto.builder()
                    .id(addressId)
                    .title("수정된 제목")
                    .city("부산광역시")
                    .district("강남구")
                    .neighborhood("역삼동")
                    .streetAddress("테헤란로 123")
                    .postalCode("06234")
                    .detailAddress("456호")
                    .phoneNumber("010-1234-5678")
                    .addressType(AddressType.PERSONAL)
                    .isDefault(false)
                    .createdAt(ZonedDateTime.now())
                    .updatedAt(ZonedDateTime.now())
                    .build();

            given(addressService.updateAddress(addressId, updateDto, userId))
                    .willReturn(updatedAddress);

            // when & then
            mockMvc.perform(patch("/v1/buyers/address/{addressId}", addressId)
                            .header("X-User-Id", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(addressId.toString()))
                    .andExpect(jsonPath("$.title").value("수정된 제목"))
                    .andExpect(jsonPath("$.city").value("부산광역시"));
        }

        @Test
        @DisplayName("주소가 존재하지 않으면 404 에러")
        void updateAddress_NotFound() throws Exception {
            // given
            AddressUpdateRequestDto updateDto = AddressUpdateRequestDto.builder()
                    .title("수정된 제목")
                    .build();

            given(addressService.updateAddress(addressId, updateDto, userId))
                    .willThrow(new AddressNotFoundException("주소를 찾을 수 없습니다: " + addressId));

            // when & then
            mockMvc.perform(patch("/v1/buyers/address/{addressId}", addressId)
                            .header("X-User-Id", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("주소를 찾을 수 없습니다: " + addressId));
        }
    }

    @DisplayName("주소 삭제 테스트")
    class DeleteAddressTest {

        @Test
        @DisplayName("주소 삭제 - 성공")
        void deleteAddress_Success() throws Exception {
            // given
            willDoNothing().given(addressService).deleteAddress(addressId, userId);

            // when & then
            mockMvc.perform(delete("/v1/buyers/address/{addressId}", addressId)
                            .header("X-User-Id", userId))
                    .andDo(print())
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("주소가 존재하지 않으면 404 에러")
        void deleteAddress_NotFound() throws Exception {
            // given
            willThrow(new AddressNotFoundException("주소를 찾을 수 없습니다: " + addressId))
                    .given(addressService).deleteAddress(addressId, userId);

            // when & then
            mockMvc.perform(delete("/v1/buyers/address/{addressId}", addressId)
                            .header("X-User-Id", userId))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("주소를 찾을 수 없습니다: " + addressId));
        }
    }

    @DisplayName("기본 주소 설정 테스트")
    class SetDefaultAddressTest {

        @Test
        @DisplayName("기본 주소 설정 - 성공")
        void setDefaultAddress_Success() throws Exception {
            // given
            AddressResponseDto defaultAddress = AddressResponseDto.builder()
                    .id(addressId)
                    .title("집")
                    .city("서울특별시")
                    .district("강남구")
                    .neighborhood("역삼동")
                    .streetAddress("테헤란로 123")
                    .postalCode("06234")
                    .detailAddress("456호")
                    .phoneNumber("010-1234-5678")
                    .addressType(AddressType.PERSONAL)
                    .isDefault(true) // 기본 주소로 설정됨
                    .createdAt(ZonedDateTime.now())
                    .updatedAt(ZonedDateTime.now())
                    .build();

            given(addressService.setDefaultAddress(addressId, userId))
                    .willReturn(defaultAddress);

            // when & then
            mockMvc.perform(patch("/v1/buyers/address/{addressId}/default", addressId)
                            .header("X-User-Id", userId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(addressId.toString()))
                    .andExpect(jsonPath("$.isDefault").value(true));
        }
    }

    @DisplayName("기본 주소 조회 테스트")
    class GetDefaultAddressTest {

        @Test
        @DisplayName("기본 주소 조회 - 성공")
        void getDefaultAddress_Success() throws Exception {
            // given
            AddressResponseDto defaultAddress = AddressResponseDto.builder()
                    .id(addressId)
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
                    .createdAt(ZonedDateTime.now())
                    .updatedAt(ZonedDateTime.now())
                    .build();

            given(addressService.getDefaultAddress(userId, AddressType.PERSONAL))
                    .willReturn(defaultAddress);

            // when & then
            mockMvc.perform(get("/v1/buyers/address/default")
                            .header("X-User-Id", userId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(addressId.toString()))
                    .andExpect(jsonPath("$.isDefault").value(true));
        }

        @Test
        @DisplayName("기본 주소가 없는 경우 204 응답")
        void getDefaultAddress_NoContent() throws Exception {
            // given
            given(addressService.getDefaultAddress(userId, AddressType.PERSONAL))
                    .willReturn(null);

            // when & then
            mockMvc.perform(get("/v1/buyers/address/default")
                            .header("X-User-Id", userId))
                    .andDo(print())
                    .andExpect(status().isNoContent());
        }
    }

    @Test
    @DisplayName("X-User-Id 헤더 누락 시 에러")
    void missingUserIdHeader() throws Exception {
        // when & then
        mockMvc.perform(get("/v1/buyers/address"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}