package com.dway.dwaybackend.service.admin;

import com.dway.dwaybackend.common.exception.motivation.MotivationNotFoundException;
import com.dway.dwaybackend.dto.request.motivation.CreateMotivationRequest;
import com.dway.dwaybackend.dto.request.motivation.UpdateMotivationRequest;
import com.dway.dwaybackend.dto.response.motivation.MotivationResponse;
import com.dway.dwaybackend.entity.DailyMotivation;
import com.dway.dwaybackend.mapper.MotivationMapper;
import com.dway.dwaybackend.repository.DailyMotivationRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminMotivationService")
class AdminMotivationServiceTest {

    @Mock private DailyMotivationRepository motivationRepository;
    @Mock private MotivationMapper motivationMapper;

    @InjectMocks private AdminMotivationService adminMotivationService;

    private static final UUID ID    = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final LocalDate TODAY = LocalDate.now();


    private DailyMotivation entity() {
        return DailyMotivation.builder()
                .id(ID)
                .quote("Do or do not.")
                .author("Yoda")
                .lastShownDate(null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private MotivationResponse response(DailyMotivation m) {
        return MotivationResponse.builder()
                .id(m.getId())
                .quote(m.getQuote())
                .author(m.getAuthor())
                .lastShownDate(m.getLastShownDate())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }

    private CreateMotivationRequest createReq(String quote, String author) {
        CreateMotivationRequest r = new CreateMotivationRequest();
        r.setQuote(quote);
        r.setAuthor(author);
        return r;
    }

    @Nested
    @DisplayName("getAllMotivations()")
    class GetAllMotivations {

        @Test
        @DisplayName("calls the queue-ordered repository method")
        void callsQueueOrderedMethod() {
            Pageable pageable = PageRequest.of(0, 20);
            when(motivationRepository.findAllByOrderByLastShownDateAscCreatedAtAsc(pageable))
                    .thenReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));

            adminMotivationService.getAllMotivations(pageable);

            verify(motivationRepository).findAllByOrderByLastShownDateAscCreatedAtAsc(pageable);
        }

        @Test
        @DisplayName("returns mapped page of responses")
        void returnsMappedPage() {
            DailyMotivation m = entity();
            Pageable pageable = PageRequest.of(0, 20);
            when(motivationRepository.findAllByOrderByLastShownDateAscCreatedAtAsc(pageable))
                    .thenReturn(new PageImpl<>(List.of(m), pageable, 1));
            when(motivationMapper.toResponse(m)).thenReturn(response(m));

            Page<MotivationResponse> result = adminMotivationService.getAllMotivations(pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getId()).isEqualTo(ID);
        }

        @Test
        @DisplayName("returns empty page when no motivations exist")
        void returnsEmptyPage() {
            Pageable pageable = PageRequest.of(0, 20);
            when(motivationRepository.findAllByOrderByLastShownDateAscCreatedAtAsc(pageable))
                    .thenReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));

            Page<MotivationResponse> result = adminMotivationService.getAllMotivations(pageable);

            assertThat(result.getTotalElements()).isZero();
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("maps each entity through MotivationMapper")
        void mapsEachEntityThroughMapper() {
            DailyMotivation m1 = entity();
            DailyMotivation m2 = entity();
            m2.setId(UUID.randomUUID());
            Pageable pageable = PageRequest.of(0, 20);
            when(motivationRepository.findAllByOrderByLastShownDateAscCreatedAtAsc(pageable))
                    .thenReturn(new PageImpl<>(List.of(m1, m2), pageable, 2));
            when(motivationMapper.toResponse(any())).thenReturn(response(m1));

            adminMotivationService.getAllMotivations(pageable);

            verify(motivationMapper, times(2)).toResponse(any());
        }

        @Test
        @DisplayName("passes pageable to repository unchanged")
        void passesPagableToRepository() {
            Pageable pageable = PageRequest.of(2, 5);
            when(motivationRepository.findAllByOrderByLastShownDateAscCreatedAtAsc(pageable))
                    .thenReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));

            adminMotivationService.getAllMotivations(pageable);

            verify(motivationRepository).findAllByOrderByLastShownDateAscCreatedAtAsc(pageable);
        }
    }

    @Nested
    @DisplayName("getMotivationById()")
    class GetMotivationById {

        @Test
        @DisplayName("returns mapped response when motivation is found")
        void returnsResponseWhenFound() {
            DailyMotivation m = entity();
            when(motivationRepository.findById(ID)).thenReturn(Optional.of(m));
            when(motivationMapper.toResponse(m)).thenReturn(response(m));

            MotivationResponse result = adminMotivationService.getMotivationById(ID);

            assertThat(result.getId()).isEqualTo(ID);
            assertThat(result.getQuote()).isEqualTo("Do or do not.");
        }

        @Test
        @DisplayName("throws MotivationNotFoundException when motivation does not exist")
        void throwsWhenNotFound() {
            when(motivationRepository.findById(ID)).thenReturn(Optional.empty());

            assertThrows(MotivationNotFoundException.class,
                    () -> adminMotivationService.getMotivationById(ID));
        }

        @Test
        @DisplayName("does not call mapper when motivation is not found")
        void doesNotMapWhenNotFound() {
            when(motivationRepository.findById(ID)).thenReturn(Optional.empty());

            assertThrows(MotivationNotFoundException.class,
                    () -> adminMotivationService.getMotivationById(ID));

            verify(motivationMapper, never()).toResponse(any());
        }

        @Test
        @DisplayName("passes the correct id to findById")
        void passesCorrectId() {
            UUID otherId = UUID.randomUUID();
            when(motivationRepository.findById(otherId)).thenReturn(Optional.empty());

            assertThrows(MotivationNotFoundException.class,
                    () -> adminMotivationService.getMotivationById(otherId));

            verify(motivationRepository).findById(otherId);
        }
    }


    @Nested
    @DisplayName("createMotivation()")
    class CreateMotivation {

        @Test
        @DisplayName("saves motivation and returns mapped response")
        void savesAndReturnsResponse() {
            CreateMotivationRequest req = createReq("New quote", "Author");
            DailyMotivation m = entity();
            when(motivationMapper.toEntity(req)).thenReturn(m);
            when(motivationMapper.toResponse(m)).thenReturn(response(m));

            MotivationResponse result = adminMotivationService.createMotivation(req);

            assertThat(result.getId()).isEqualTo(ID);
            verify(motivationRepository).save(m);
        }

        @Test
        @DisplayName("sets lastShownDate = today so the new motivation joins the BACK of the queue")
        void setsLastShownDateToTodayBeforeSaving() {
            CreateMotivationRequest req = createReq("Quote", "Author");
            DailyMotivation m = entity(); // lastShownDate starts null
            when(motivationMapper.toEntity(req)).thenReturn(m);
            when(motivationMapper.toResponse(m)).thenReturn(response(m));

            adminMotivationService.createMotivation(req);

            // Must be set to today before save — verified by checking the entity state
            assertThat(m.getLastShownDate()).isEqualTo(TODAY);
        }

        @Test
        @DisplayName("sets lastShownDate BEFORE calling save")
        void setsLastShownDateBeforeSave() {
            CreateMotivationRequest req = createReq("Quote", "Author");
            DailyMotivation m = entity();
            when(motivationMapper.toEntity(req)).thenReturn(m);
            when(motivationMapper.toResponse(m)).thenReturn(response(m));

            // Capture the entity at the moment save() is called
            doAnswer(invocation -> {
                DailyMotivation saved = invocation.getArgument(0);
                assertThat(saved.getLastShownDate()).isEqualTo(TODAY);
                return null;
            }).when(motivationRepository).save(m);

            adminMotivationService.createMotivation(req);
        }

        @Test
        @DisplayName("does NOT call findNextInQueue or updateLastShownDate — queue unaffected")
        void doesNotTouchQueue() {
            CreateMotivationRequest req = createReq("Quote", "Author");
            DailyMotivation m = entity();
            when(motivationMapper.toEntity(req)).thenReturn(m);
            when(motivationMapper.toResponse(m)).thenReturn(response(m));

            adminMotivationService.createMotivation(req);

            verify(motivationRepository, never()).findNextInQueue();
            verify(motivationRepository, never()).updateLastShownDate(any(), any());
        }

        @Test
        @DisplayName("does NOT call any existsBy check — every upload is independent")
        void noConflictCheck() {
            CreateMotivationRequest req = createReq("Quote", "Author");
            DailyMotivation m = entity();
            when(motivationMapper.toEntity(req)).thenReturn(m);
            when(motivationMapper.toResponse(m)).thenReturn(response(m));

            adminMotivationService.createMotivation(req);

            // save and findById are the only allowed repository calls
            verify(motivationRepository).save(m);
            verify(motivationRepository, never()).findById(any());
            verify(motivationRepository, never()).findAllByOrderByLastShownDateAscCreatedAtAsc(any());
        }

        @Test
        @DisplayName("null author is valid — motivation without attribution is allowed")
        void nullAuthorIsValid() {
            CreateMotivationRequest req = createReq("Anonymous quote", null);
            DailyMotivation m = entity();
            m.setAuthor(null);
            when(motivationMapper.toEntity(req)).thenReturn(m);
            when(motivationMapper.toResponse(m)).thenReturn(response(m));

            adminMotivationService.createMotivation(req);

            verify(motivationRepository).save(m);
        }

        @Test
        @DisplayName("toEntity is called before save")
        void toEntityCalledBeforeSave() {
            CreateMotivationRequest req = createReq("Quote", "Author");
            DailyMotivation m = entity();
            when(motivationMapper.toEntity(req)).thenReturn(m);
            when(motivationMapper.toResponse(m)).thenReturn(response(m));

            adminMotivationService.createMotivation(req);

            InOrder order = inOrder(motivationMapper, motivationRepository);
            order.verify(motivationMapper).toEntity(req);
            order.verify(motivationRepository).save(m);
        }
    }

    @Nested
    @DisplayName("updateMotivation()")
    class UpdateMotivation {

        @Test
        @DisplayName("updates quote when provided")
        void updatesQuoteWhenProvided() {
            DailyMotivation m = entity();
            UpdateMotivationRequest req = new UpdateMotivationRequest();
            req.setQuote("Updated quote");
            when(motivationRepository.findById(ID)).thenReturn(Optional.of(m));
            when(motivationMapper.toResponse(m)).thenReturn(response(m));

            adminMotivationService.updateMotivation(ID, req);

            assertThat(m.getQuote()).isEqualTo("Updated quote");
        }

        @Test
        @DisplayName("updates author when provided")
        void updatesAuthorWhenProvided() {
            DailyMotivation m = entity();
            UpdateMotivationRequest req = new UpdateMotivationRequest();
            req.setAuthor("New Author");
            when(motivationRepository.findById(ID)).thenReturn(Optional.of(m));
            when(motivationMapper.toResponse(m)).thenReturn(response(m));

            adminMotivationService.updateMotivation(ID, req);

            assertThat(m.getAuthor()).isEqualTo("New Author");
        }

        @Test
        @DisplayName("updates both fields when both provided")
        void updatesBothFields() {
            DailyMotivation m = entity();
            UpdateMotivationRequest req = new UpdateMotivationRequest();
            req.setQuote("New quote");
            req.setAuthor("New Author");
            when(motivationRepository.findById(ID)).thenReturn(Optional.of(m));
            when(motivationMapper.toResponse(m)).thenReturn(response(m));

            adminMotivationService.updateMotivation(ID, req);

            assertThat(m.getQuote()).isEqualTo("New quote");
            assertThat(m.getAuthor()).isEqualTo("New Author");
        }

        @Test
        @DisplayName("null quote keeps existing value — PATCH semantics")
        void nullQuoteKeepsExisting() {
            DailyMotivation m = entity();
            String original = m.getQuote();
            UpdateMotivationRequest req = new UpdateMotivationRequest(); // quote = null
            when(motivationRepository.findById(ID)).thenReturn(Optional.of(m));
            when(motivationMapper.toResponse(m)).thenReturn(response(m));

            adminMotivationService.updateMotivation(ID, req);

            assertThat(m.getQuote()).isEqualTo(original);
        }

        @Test
        @DisplayName("null author keeps existing value — PATCH semantics")
        void nullAuthorKeepsExisting() {
            DailyMotivation m = entity();
            String original = m.getAuthor();
            UpdateMotivationRequest req = new UpdateMotivationRequest(); // author = null
            when(motivationRepository.findById(ID)).thenReturn(Optional.of(m));
            when(motivationMapper.toResponse(m)).thenReturn(response(m));

            adminMotivationService.updateMotivation(ID, req);

            assertThat(m.getAuthor()).isEqualTo(original);
        }

        @Test
        @DisplayName("empty request leaves both fields unchanged")
        void emptyRequestChangesNothing() {
            DailyMotivation m = entity();
            String originalQuote  = m.getQuote();
            String originalAuthor = m.getAuthor();
            when(motivationRepository.findById(ID)).thenReturn(Optional.of(m));
            when(motivationMapper.toResponse(m)).thenReturn(response(m));

            adminMotivationService.updateMotivation(ID, new UpdateMotivationRequest());

            assertThat(m.getQuote()).isEqualTo(originalQuote);
            assertThat(m.getAuthor()).isEqualTo(originalAuthor);
        }

        @Test
        @DisplayName("does NOT modify lastShownDate — queue position is preserved after update")
        void doesNotChangeLastShownDate() {
            DailyMotivation m = entity();
            m.setLastShownDate(TODAY.minusDays(5));
            UpdateMotivationRequest req = new UpdateMotivationRequest();
            req.setQuote("Fixed typo");
            when(motivationRepository.findById(ID)).thenReturn(Optional.of(m));
            when(motivationMapper.toResponse(m)).thenReturn(response(m));

            adminMotivationService.updateMotivation(ID, req);

            assertThat(m.getLastShownDate()).isEqualTo(TODAY.minusDays(5));
        }

        @Test
        @DisplayName("saves the entity after applying changes")
        void savesAfterUpdate() {
            DailyMotivation m = entity();
            UpdateMotivationRequest req = new UpdateMotivationRequest();
            req.setQuote("Updated");
            when(motivationRepository.findById(ID)).thenReturn(Optional.of(m));
            when(motivationMapper.toResponse(m)).thenReturn(response(m));

            adminMotivationService.updateMotivation(ID, req);

            verify(motivationRepository).save(m);
        }

        @Test
        @DisplayName("throws MotivationNotFoundException when motivation does not exist")
        void throwsWhenNotFound() {
            when(motivationRepository.findById(ID)).thenReturn(Optional.empty());

            assertThrows(MotivationNotFoundException.class,
                    () -> adminMotivationService.updateMotivation(ID, new UpdateMotivationRequest()));
        }

        @Test
        @DisplayName("does not call save when motivation is not found")
        void doesNotSaveWhenNotFound() {
            when(motivationRepository.findById(ID)).thenReturn(Optional.empty());

            assertThrows(MotivationNotFoundException.class,
                    () -> adminMotivationService.updateMotivation(ID, new UpdateMotivationRequest()));

            verify(motivationRepository, never()).save(any());
        }

        @Test
        @DisplayName("returns mapped response of the updated entity")
        void returnsMappedResponse() {
            DailyMotivation m = entity();
            UpdateMotivationRequest req = new UpdateMotivationRequest();
            req.setQuote("Updated");
            when(motivationRepository.findById(ID)).thenReturn(Optional.of(m));
            MotivationResponse expectedResponse = response(m);
            when(motivationMapper.toResponse(m)).thenReturn(expectedResponse);

            MotivationResponse result = adminMotivationService.updateMotivation(ID, req);

            assertThat(result).isSameAs(expectedResponse);
        }
    }

    @Nested
    @DisplayName("deleteMotivation()")
    class DeleteMotivation {

        @Test
        @DisplayName("deletes the motivation from the repository")
        void deletesMotivation() {
            DailyMotivation m = entity();
            when(motivationRepository.findById(ID)).thenReturn(Optional.of(m));

            adminMotivationService.deleteMotivation(ID);

            verify(motivationRepository).delete(m);
        }

        @Test
        @DisplayName("uses physical delete, not soft delete — save is never called")
        void neverCallsSave() {
            DailyMotivation m = entity();
            when(motivationRepository.findById(ID)).thenReturn(Optional.of(m));

            adminMotivationService.deleteMotivation(ID);

            verify(motivationRepository, never()).save(any());
        }

        @Test
        @DisplayName("fetches the entity before deleting — cannot delete unknown ids")
        void fetchesBeforeDeleting() {
            DailyMotivation m = entity();
            when(motivationRepository.findById(ID)).thenReturn(Optional.of(m));

            adminMotivationService.deleteMotivation(ID);

            InOrder order = inOrder(motivationRepository);
            order.verify(motivationRepository).findById(ID);
            order.verify(motivationRepository).delete(m);
        }

        @Test
        @DisplayName("throws MotivationNotFoundException when motivation does not exist")
        void throwsWhenNotFound() {
            when(motivationRepository.findById(ID)).thenReturn(Optional.empty());

            assertThrows(MotivationNotFoundException.class,
                    () -> adminMotivationService.deleteMotivation(ID));
        }

        @Test
        @DisplayName("does not call delete when motivation is not found")
        void doesNotDeleteWhenNotFound() {
            when(motivationRepository.findById(ID)).thenReturn(Optional.empty());

            assertThrows(MotivationNotFoundException.class,
                    () -> adminMotivationService.deleteMotivation(ID));

            verify(motivationRepository, never()).delete(any());
        }
    }
}