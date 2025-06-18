package com.team5.catdogeats.users.controller;

import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.users.dto.SellerInfoRequest;
import com.team5.catdogeats.users.dto.SellerInfoResponse;
import com.team5.catdogeats.users.exception.BusinessNumberDuplicateException;
import com.team5.catdogeats.users.exception.SellerAccessDeniedException;
import com.team5.catdogeats.users.exception.UserNotFoundException;
import com.team5.catdogeats.users.service.SellerInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/v1/sellers")
@RequiredArgsConstructor
@Tag(name = "판매자 정보 관리", description = "판매자 정보 조회 및 등록/수정 API (개발 단계 - JWT 구현 전)")
public class SellerInfoController {

    private final SellerInfoService sellerInfoService;

    /**
     * 판매자 정보 조회 (개발용 - 하드코딩된 사용자 ID 사용)
     */
    @Operation(
            summary = "판매자 정보 조회 (개발용)",
            description = """
                    현재 개발 단계에서는 하드코딩된 사용자 ID를 사용합니다.
                    
                    **개발용 사용자 ID**: 2ceb807f-586f-4450-b470-d1ece7173749
                    
                    JWT 구현 완료 후에는 토큰에서 사용자 ID를 추출하여 사용합니다.
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "판매자 정보 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "정보 있음",
                                            value = """
                        {
                          "success": true,
                          "message": "판매자 정보 조회 성공",
                          "data": {
                            "userId": "2ceb807f-586f-4450-b470-d1ece7173749",
                            "vendorName": "멍멍이네 수제간식",
                            "vendorProfileImage": "https://example.com/profile.jpg",
                            "businessNumber": "123-45-67890",
                            "settlementBank": "국민은행",
                            "settlementAcc": "123456789012",
                            "tags": "수제간식,강아지",
                            "createdAt": "2024-01-15T10:30:00",
                            "updatedAt": "2024-01-20T14:20:00"
                          },
                          "timestamp": "2024-01-20T15:30:00"
                        }
                        """
                                    ),
                                    @ExampleObject(
                                            name = "정보 없음",
                                            value = """
                        {
                          "success": true,
                          "message": "등록된 판매자 정보가 없습니다.",
                          "data": null,
                          "timestamp": "2024-01-20T15:30:00"
                        }
                        """
                                    )
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 - 하드코딩된 사용자가 판매자 권한이 없는 경우",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                    {
                      "success": false,
                      "message": "판매자 권한이 필요합니다.",
                      "data": null,
                      "timestamp": "2024-01-20T15:30:00",
                      "path": "/v1/sellers/info"
                    }
                    """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "하드코딩된 사용자를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                    {
                      "success": false,
                      "message": "존재하지 않는 사용자입니다.",
                      "data": null,
                      "timestamp": "2024-01-20T15:30:00",
                      "path": "/v1/sellers/info"
                    }
                    """
                            )
                    )
            )
    })
    @GetMapping("/info")
    public ResponseEntity<ApiResponse<SellerInfoResponse>> getSellerInfo(
            HttpServletRequest request) {

        // TODO: JWT 토큰에서 사용자 ID 추출하는 로직으로 교체 예정
        // String token = extractTokenFromHeader(request);
        // UUID userId = jwtTokenProvider.getUserIdFromToken(token);

        // 현재: 하드코딩된 사용자 ID 사용 (개발용)
        UUID tempUserId = UUID.fromString("2ceb807f-586f-4450-b470-d1ece7173749");
        log.info("판매자 정보 조회 요청 - 개발용 하드코딩 ID: {}", tempUserId);

        try {
            SellerInfoResponse response = sellerInfoService.getSellerInfo(tempUserId);

            if (response == null) {
                return ResponseEntity.ok(
                        ApiResponse.success("등록된 판매자 정보가 없습니다.", null)
                );
            }

            return ResponseEntity.ok(
                    ApiResponse.success("판매자 정보 조회 성공", response)
            );

        } catch (UserNotFoundException e) {
            log.error("사용자를 찾을 수 없음 - userId: {}", tempUserId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage(), request.getRequestURI(), null));

        } catch (SellerAccessDeniedException e) {
            log.error("판매자 권한 없음 - userId: {}", tempUserId, e);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage(), request.getRequestURI(), null));

        } catch (Exception e) {
            log.error("판매자 정보 조회 중 오류 발생 - userId: {}", tempUserId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("서버 오류가 발생했습니다.", request.getRequestURI(), null));
        }
    }

    /**
     * 판매자 정보 등록/수정 (개발용 - 하드코딩된 사용자 ID 사용)
     */
    @Operation(
            summary = "판매자 정보 등록/수정",
            description = """
                    현재 개발 단계에서는 하드코딩된 사용자 ID를 사용합니다.
                    
                    **개발용 사용자 ID**: 2ceb807f-586f-4450-b470-d1ece7173749
                    
                    기존 정보가 있으면 수정, 없으면 신규 등록됩니다.
                    JWT 구현 완료 후에는 토큰에서 사용자 ID를 추출하여 사용합니다.
                    """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "판매자 정보 등록/수정 요청",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = SellerInfoRequest.class),
                    examples = @ExampleObject(
                            name = "판매자 정보 예시",
                            value = """
                {
                  "vendorName": "멍멍이네 수제간식",
                  "vendorProfileImage": "https://example.com/profile.jpg",
                  "businessNumber": "123-45-67890",
                  "settlementBank": "국민은행",
                  "settlementAcc": "123456789012",
                  "tags": "수제간식,강아지",
                  "operatingStartTime": "09:00:00",
                  "operatingEndTime": "18:00:00",
                  "closedDays": "월요일,화요일"
                  
                }
                """
                    )
            )
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "판매자 정보 저장 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    value = """
                    {
                      "success": true,
                      "message": "판매자 정보 저장 성공",
                      "data": {
                        "userId": "2ceb807f-586f-4450-b470-d1ece7173749",
                        "vendorName": "멍멍이네 수제간식",
                        "vendorProfileImage": "https://example.com/profile.jpg",
                        "businessNumber": "123-45-67890",
                        "settlementBank": "국민은행",
                        "settlementAcc": "123456789012",
                        "tags": "수제간식,강아지",
                        "operatingStartTime": "09:00:00",
                        "operatingEndTime": "18:00:00",
                        "closedDays": "월요일,화요일",
                        "createdAt": "2024-01-15T10:30:00",
                        "updatedAt": "2024-01-20T14:20:00"
                      },
                      "timestamp": "2024-01-20T15:30:00"
                    }
                    """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "입력값 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                    {
                      "success": false,
                      "message": "입력값 검증 실패",
                      "data": null,
                      "timestamp": "2024-01-20T15:30:00",
                      "path": "/v1/sellers/info",
                      "errors": [
                        {
                          "field": "vendorName",
                          "message": "업체명은 필수 입력 항목입니다."
                        },
                        {
                          "field": "businessNumber",
                          "message": "사업자 등록번호는 숫자와 하이픈만 입력 가능합니다."
                        }
                      ]
                    }
                    """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 - 하드코딩된 사용자가 판매자 권한이 없는 경우",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                    {
                      "success": false,
                      "message": "판매자 권한이 필요합니다.",
                      "data": null,
                      "timestamp": "2024-01-20T15:30:00",
                      "path": "/v1/sellers/info"
                    }
                    """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "하드코딩된 사용자를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                    {
                      "success": false,
                      "message": "존재하지 않는 사용자입니다.",
                      "data": null,
                      "timestamp": "2024-01-20T15:30:00",
                      "path": "/v1/sellers/info"
                    }
                    """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "사업자 등록번호 중복",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                    {
                      "success": false,
                      "message": "이미 등록된 사업자 등록번호입니다.",
                      "data": null,
                      "timestamp": "2024-01-20T15:30:00",
                      "path": "/v1/sellers/info"
                    }
                    """
                            )
                    )
            )
    })
    @PatchMapping("/info")
    public ResponseEntity<ApiResponse<SellerInfoResponse>> upsertSellerInfo(
            @Valid @RequestBody SellerInfoRequest request,
            BindingResult bindingResult,
            HttpServletRequest httpRequest) {

        // TODO: JWT 토큰에서 사용자 ID 추출하는 로직으로 교체 예정
        // String token = extractTokenFromHeader(httpRequest);
        // UUID userId = jwtTokenProvider.getUserIdFromToken(token);

        // 현재: 하드코딩된 사용자 ID 사용 (개발용)
        UUID tempUserId = UUID.fromString("2ceb807f-586f-4450-b470-d1ece7173749");
        log.info("판매자 정보 등록/수정 요청 - 개발용 하드코딩 ID: {}, vendorName: {}",
                tempUserId, request.vendorName());

        // 유효성 검증 오류 처리
        if (bindingResult.hasErrors()) {
            List<ApiResponse.FieldError> fieldErrors = bindingResult.getFieldErrors().stream()
                    .map(error -> new ApiResponse.FieldError(
                            error.getField(),
                            error.getDefaultMessage()
                    ))
                    .collect(Collectors.toList());

            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("입력값 검증 실패", httpRequest.getRequestURI(), fieldErrors));
        }

        try {
            SellerInfoResponse response = sellerInfoService.upsertSellerInfo(tempUserId, request);

            return ResponseEntity.ok(
                    ApiResponse.success("판매자 정보 저장 성공", response)
            );

        } catch (UserNotFoundException e) {
            log.error("사용자를 찾을 수 없음 - userId: {}", tempUserId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage(), httpRequest.getRequestURI(), null));

        } catch (SellerAccessDeniedException e) {
            log.error("판매자 권한 없음 - userId: {}", tempUserId, e);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage(), httpRequest.getRequestURI(), null));

        } catch (BusinessNumberDuplicateException e) {
            log.error("사업자 등록번호 중복 - userId: {}, businessNumber: {}",
                    tempUserId, request.businessNumber(), e);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(e.getMessage(), httpRequest.getRequestURI(), null));

        } catch (IllegalArgumentException e) {
            // 운영시간 유효성 검증 오류 처리 추가
            log.error("운영시간 유효성 검증 실패 - userId: {}, error: {}", tempUserId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), httpRequest.getRequestURI(), null));

        } catch (Exception e) {
            log.error("판매자 정보 저장 중 오류 발생 - userId: {}", tempUserId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("서버 오류가 발생했습니다.", httpRequest.getRequestURI(), null));
        }
    }

    // TODO: JWT 구현 후 실제 토큰 추출 로직 추가 예정
    /*
    private String extractTokenFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
    }
    */
}