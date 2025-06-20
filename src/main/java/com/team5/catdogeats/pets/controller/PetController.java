package com.team5.catdogeats.pets.controller;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import com.team5.catdogeats.pets.domain.dto.*;
import com.team5.catdogeats.pets.service.PetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/v1/buyers")
@RequiredArgsConstructor
@Tag(name = "Pet", description = "펫 정보 관련 API")
public class PetController {

    private final PetService petService;

    @Operation(summary = "펫 등록", description = "새로운 펫 정보를 등록합니다.")
    @PostMapping("/pet")
    public ResponseEntity<ApiResponse<Void>> registerPet(@AuthenticationPrincipal UserPrincipal userPrincipal, @RequestBody @Valid @Parameter(description = "등록할 펫 정보", required = true) PetCreateRequestDto dto) {
        try {
            UUID petId = petService.registerPet(userPrincipal, dto);
            return ResponseEntity
                    .created(URI.create("/v1/buyers/pet/" + petId))
                    .body(ApiResponse.success(ResponseCode.CREATED));
        } catch (NoSuchElementException e) {
            return ResponseEntity
                    .status(ResponseCode.ENTITY_NOT_FOUND.getStatus())
                    .body(ApiResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, e.getMessage()));
        }
    }

    @Operation(summary = "내 펫 목록 조회", description = "로그인한 사용자의 펫 목록을 조회합니다.")
    @GetMapping("/pet")
    public ResponseEntity<ApiResponse<List<PetResponseDto>>> getMyPets(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            List<PetResponseDto> pets = petService.getMyPets(userPrincipal);
            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, pets));
        } catch (NoSuchElementException e) {
            return ResponseEntity
                    .status(ResponseCode.ENTITY_NOT_FOUND.getStatus())
                    .body(ApiResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, e.getMessage()));
        }
    }

    @Operation(summary = "펫 정보 수정", description = "기존 펫 정보를 수정합니다.")
    @PatchMapping("/pet")
    public ResponseEntity<ApiResponse<Void>> updatePet(@RequestBody @Valid @Parameter(description = "수정할 펫 정보", required = true) PetUpdateRequestDto dto) {
        try {
            petService.updatePet(dto);
            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS));
        } catch (NoSuchElementException e) {
            return ResponseEntity
                    .status(ResponseCode.ENTITY_NOT_FOUND.getStatus())
                    .body(ApiResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, e.getMessage()));
        }
    }

    @Operation(summary = "펫 삭제", description = "펫 정보를 삭제합니다.")
    @DeleteMapping("/pet")
    public ResponseEntity<ApiResponse<Void>> deletePet(@RequestBody @Valid @Parameter(description = "삭제할 펫 id", required = true) PetDeleteRequestDto dto) {
        try {
            petService.deletePet(dto);
            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS));
        } catch (NoSuchElementException e) {
            return ResponseEntity
                    .status(ResponseCode.ENTITY_NOT_FOUND.getStatus())
                    .body(ApiResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, e.getMessage()));
        }
    }
}
