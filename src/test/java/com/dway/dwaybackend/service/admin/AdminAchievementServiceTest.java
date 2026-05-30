package com.dway.dwaybackend.service.admin;

import com.dway.dwaybackend.common.exception.achievement.AchievementNotFoundException;
import com.dway.dwaybackend.dto.request.achievement.CreateAchievementRequest;
import com.dway.dwaybackend.dto.request.achievement.UpdateAchievementRequest;
import com.dway.dwaybackend.dto.response.achievement.AchievementResponse;
import com.dway.dwaybackend.entity.Achievement;
import com.dway.dwaybackend.entity.enums.AchievementType;
import com.dway.dwaybackend.mapper.AchievementMapper;
import com.dway.dwaybackend.repository.AchievementRepository;
import com.dway.dwaybackend.repository.UserAchievementRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAchievementService Unit Tests")
class AdminAchievementServiceTest {

    @Mock private AchievementRepository achievementRepository;
    @Mock private UserAchievementRepository userAchievementRepository;
    @Mock private AchievementMapper achievementMapper;

    @InjectMocks private AdminAchievementService service;

    private static final UUID ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private Achievement inactiveAchievement() {
        return Achievement.builder()
                .id(ID).title("Task Starter").description("Complete 10 tasks")
                .icon("🏅").type(AchievementType.TASK_COUNT)
                .threshold(10).isActive(false).createdAt(LocalDateTime.now())
                .build();
    }

    private Achievement activeAchievement() {
        Achievement a = inactiveAchievement();
        a.setActive(true);
        return a;
    }

    private AchievementResponse response(Achievement a) {
        return AchievementResponse.builder()
                .id(a.getId()).title(a.getTitle()).description(a.getDescription())
                .icon(a.getIcon()).type(a.getType())
                .threshold(a.getThreshold())
                .isActive(a.isActive()).createdAt(a.getCreatedAt())
                .build();
    }

    private CreateAchievementRequest createRequest() {
        CreateAchievementRequest r = new CreateAchievementRequest();
        r.setTitle("Task Starter"); r.setDescription("Complete 10 tasks");
        r.setIcon("🏅"); r.setType(AchievementType.TASK_COUNT); r.setThreshold(10);
        return r;
    }

    // ── getAllAchievements ───────────────────────────────────────────────────

    @Nested @DisplayName("getAllAchievements()")
    class GetAllAchievements {

        @Test @DisplayName("returns all when isActive filter is null")
        void noFilter_returnsAll() {
            Achievement a = inactiveAchievement();
            Page<Achievement> page = new PageImpl<>(List.of(a), PageRequest.of(0, 20), 1);
            when(achievementRepository.findAll(any(Pageable.class))).thenReturn(page);
            when(achievementMapper.toResponse(a)).thenReturn(response(a));

            assertThat(service.getAllAchievements(null, PageRequest.of(0, 20)).getTotalElements()).isEqualTo(1);
            verify(achievementRepository).findAll(any(Pageable.class));
            verify(achievementRepository, never()).findByIsActive(anyBoolean(), any());
        }

        @Test @DisplayName("filters by isActive=true")
        void filterTrue() {
            Achievement a = activeAchievement();
            Page<Achievement> page = new PageImpl<>(List.of(a), PageRequest.of(0, 20), 1);
            when(achievementRepository.findByIsActive(true, PageRequest.of(0, 20))).thenReturn(page);
            when(achievementMapper.toResponse(a)).thenReturn(response(a));

            assertThat(service.getAllAchievements(true, PageRequest.of(0, 20)).getContent().get(0).getIsActive()).isTrue();
        }

        @Test @DisplayName("filters by isActive=false")
        void filterFalse() {
            Achievement a = inactiveAchievement();
            Page<Achievement> page = new PageImpl<>(List.of(a), PageRequest.of(0, 20), 1);
            when(achievementRepository.findByIsActive(false, PageRequest.of(0, 20))).thenReturn(page);
            when(achievementMapper.toResponse(a)).thenReturn(response(a));

            assertThat(service.getAllAchievements(false, PageRequest.of(0, 20)).getContent().get(0).getIsActive()).isFalse();
        }

        @Test @DisplayName("returns empty page")
        void empty() {
            when(achievementRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());
            assertThat(service.getAllAchievements(null, PageRequest.of(0, 20)).getContent()).isEmpty();
        }
    }

    // ── getAchievementById ──────────────────────────────────────────────────

    @Nested @DisplayName("getAchievementById()")
    class GetAchievementById {

        @Test @DisplayName("returns response when found")
        void found() {
            Achievement a = inactiveAchievement();
            when(achievementRepository.findById(ID)).thenReturn(Optional.of(a));
            when(achievementMapper.toResponse(a)).thenReturn(response(a));
            assertThat(service.getAchievementById(ID).getType()).isEqualTo(AchievementType.TASK_COUNT);
        }

        @Test @DisplayName("throws when not found")
        void notFound() {
            when(achievementRepository.findById(ID)).thenReturn(Optional.empty());
            assertThrows(AchievementNotFoundException.class, () -> service.getAchievementById(ID));
        }
    }

    // ── createAchievement ───────────────────────────────────────────────────

    @Nested @DisplayName("createAchievement()")
    class CreateAchievement {

        @Test @DisplayName("saves entity and returns response")
        void saves() {
            CreateAchievementRequest req = createRequest();
            Achievement entity = inactiveAchievement();
            when(achievementMapper.toEntity(req)).thenReturn(entity);
            when(achievementMapper.toResponse(entity)).thenReturn(response(entity));

            service.createAchievement(req);

            verify(achievementRepository).save(entity);
        }

        @Test @DisplayName("new achievement is inactive by default")
        void inactiveByDefault() {
            Achievement entity = inactiveAchievement();
            when(achievementMapper.toEntity(any())).thenReturn(entity);
            when(achievementMapper.toResponse(entity)).thenReturn(response(entity));

            service.createAchievement(createRequest());

            assertThat(entity.isActive()).isFalse();
        }

        @Test @DisplayName("CHALLENGE_COUNT type is saved correctly")
        void challengeType() {
            CreateAchievementRequest req = createRequest();
            req.setType(AchievementType.CHALLENGE_COUNT);
            Achievement entity = inactiveAchievement();
            entity.setType(AchievementType.CHALLENGE_COUNT);
            when(achievementMapper.toEntity(req)).thenReturn(entity);
            when(achievementMapper.toResponse(entity)).thenReturn(response(entity));

            service.createAchievement(req);

            assertThat(entity.getType()).isEqualTo(AchievementType.CHALLENGE_COUNT);
        }
    }

    // ── updateAchievement ───────────────────────────────────────────────────

    @Nested @DisplayName("updateAchievement()")
    class UpdateAchievement {

        private UpdateAchievementRequest req(String title, Integer threshold) {
            UpdateAchievementRequest r = new UpdateAchievementRequest();
            r.setTitle(title); r.setThreshold(threshold);
            return r;
        }

        @Test @DisplayName("updates title") void updatesTitle() {
            Achievement a = inactiveAchievement();
            when(achievementRepository.findById(ID)).thenReturn(Optional.of(a));
            when(achievementMapper.toResponse(a)).thenReturn(response(a));
            service.updateAchievement(ID, req("New", null));
            assertThat(a.getTitle()).isEqualTo("New");
        }

        @Test @DisplayName("null fields unchanged — PATCH semantics") void patchSemantics() {
            Achievement a = inactiveAchievement();
            when(achievementRepository.findById(ID)).thenReturn(Optional.of(a));
            when(achievementMapper.toResponse(a)).thenReturn(response(a));
            service.updateAchievement(ID, req(null, null));
            assertThat(a.getTitle()).isEqualTo("Task Starter");
            assertThat(a.getThreshold()).isEqualTo(10);
        }

        @Test @DisplayName("throws when not found") void notFound() {
            when(achievementRepository.findById(ID)).thenReturn(Optional.empty());
            assertThrows(AchievementNotFoundException.class,
                    () -> service.updateAchievement(ID, req("x", null)));
        }
    }

    // ── toggleActive ────────────────────────────────────────────────────────

    @Nested @DisplayName("toggleActive()")
    class ToggleActive {

        @Test @DisplayName("false → true") void activates() {
            Achievement a = inactiveAchievement();
            when(achievementRepository.findById(ID)).thenReturn(Optional.of(a));
            when(achievementMapper.toResponse(a)).thenReturn(response(a));
            service.toggleActive(ID);
            assertThat(a.isActive()).isTrue();
        }

        @Test @DisplayName("true → false") void deactivates() {
            Achievement a = activeAchievement();
            when(achievementRepository.findById(ID)).thenReturn(Optional.of(a));
            when(achievementMapper.toResponse(a)).thenReturn(response(a));
            service.toggleActive(ID);
            assertThat(a.isActive()).isFalse();
        }

        @Test @DisplayName("calling twice restores original") void twice() {
            Achievement a = inactiveAchievement();
            when(achievementRepository.findById(ID)).thenReturn(Optional.of(a));
            when(achievementMapper.toResponse(a)).thenReturn(response(a));
            service.toggleActive(ID);
            service.toggleActive(ID);
            assertThat(a.isActive()).isFalse();
        }

        @Test @DisplayName("throws when not found") void notFound() {
            when(achievementRepository.findById(ID)).thenReturn(Optional.empty());
            assertThrows(AchievementNotFoundException.class, () -> service.toggleActive(ID));
        }
    }

    // ── deleteAchievement ───────────────────────────────────────────────────

    @Nested @DisplayName("deleteAchievement()")
    class DeleteAchievement {

        @Test @DisplayName("deletes user achievements first, then achievement")
        void deletesInOrder() {
            Achievement a = inactiveAchievement();
            when(achievementRepository.findById(ID)).thenReturn(Optional.of(a));
            service.deleteAchievement(ID);
            InOrder order = inOrder(userAchievementRepository, achievementRepository);
            order.verify(userAchievementRepository).deleteAllByAchievementId(ID);
            order.verify(achievementRepository).delete(a);
        }

        @Test @DisplayName("throws when not found") void notFound() {
            when(achievementRepository.findById(ID)).thenReturn(Optional.empty());
            assertThrows(AchievementNotFoundException.class, () -> service.deleteAchievement(ID));
            verify(achievementRepository, never()).delete(any());
        }
    }
}