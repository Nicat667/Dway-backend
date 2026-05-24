package com.dway.dwaybackend.service.mobile;

import com.dway.dwaybackend.common.exception.motivation.MotivationNotFoundException;
import com.dway.dwaybackend.dto.response.motivation.MotivationResponse;
import com.dway.dwaybackend.entity.DailyMotivation;
import com.dway.dwaybackend.mapper.MotivationMapper;
import com.dway.dwaybackend.repository.DailyMotivationRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MotivationService")
class MotivationServiceTest {

    @Mock private DailyMotivationRepository motivationRepository;
    @Mock private MotivationMapper motivationMapper;

    @InjectMocks private MotivationService motivationService;

    private static final UUID ID    = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final LocalDate TODAY = LocalDate.now();


    private DailyMotivation entity(LocalDate lastShownDate) {
        return DailyMotivation.builder()
                .id(ID)
                .quote("Press forward.")
                .author("Lincoln")
                .lastShownDate(lastShownDate)
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

    private void stubQueue(DailyMotivation m) {
        when(motivationRepository.findNextInQueue()).thenReturn(Optional.of(m));
        when(motivationMapper.toResponse(m)).thenReturn(response(m));
    }


    @Nested
    @DisplayName("getTodayMotivation() — queue is not empty")
    class QueueNotEmpty {

        @Test
        @DisplayName("returns mapped response of the next motivation in the queue")
        void returnsNextInQueue() {
            DailyMotivation m = entity(null);
            stubQueue(m);

            MotivationResponse result = motivationService.getTodayMotivation();

            assertThat(result.getId()).isEqualTo(ID);
            assertThat(result.getQuote()).isEqualTo("Press forward.");
        }

        @Test
        @DisplayName("stamps today's date on the picked motivation (moves it to back of queue)")
        void stampsLastShownDateWithToday() {
            stubQueue(entity(null));

            motivationService.getTodayMotivation();

            verify(motivationRepository).updateLastShownDate(ID, TODAY);
        }

        @Test
        @DisplayName("stamp uses exactly today — not yesterday, not tomorrow")
        void stampUsesExactToday() {
            stubQueue(entity(null));

            motivationService.getTodayMotivation();

            verify(motivationRepository).updateLastShownDate(eq(ID), eq(LocalDate.now()));
        }

        @Test
        @DisplayName("operations happen in correct order: findNextInQueue → updateLastShownDate → toResponse")
        void operationsInCorrectOrder() {
            DailyMotivation m = entity(null);
            stubQueue(m);

            motivationService.getTodayMotivation();

            InOrder order = inOrder(motivationRepository, motivationMapper);
            order.verify(motivationRepository).findNextInQueue();
            order.verify(motivationRepository).updateLastShownDate(ID, TODAY);
            order.verify(motivationMapper).toResponse(m);
        }

        @Test
        @DisplayName("delegates mapping to MotivationMapper, not internal logic")
        void delegatesMappingToMapper() {
            DailyMotivation m = entity(null);
            stubQueue(m);

            motivationService.getTodayMotivation();

            verify(motivationMapper, times(1)).toResponse(m);
        }

        @Test
        @DisplayName("works for a motivation that was never shown (lastShownDate = null)")
        void worksForNeverShownMotivation() {
            DailyMotivation m = entity(null);
            stubQueue(m);

            MotivationResponse result = motivationService.getTodayMotivation();

            assertThat(result.getLastShownDate()).isNull();
            verify(motivationRepository).updateLastShownDate(ID, TODAY);
        }

        @Test
        @DisplayName("works for a motivation being cycled (lastShownDate = past date)")
        void worksForCycledMotivation() {
            // This is the 'cycle restart' scenario — all motivations have been shown,
            // the oldest one (shown 30 days ago) is picked again.
            DailyMotivation m = entity(TODAY.minusDays(30));
            stubQueue(m);

            MotivationResponse result = motivationService.getTodayMotivation();

            assertThat(result.getId()).isEqualTo(ID);
            // Stamp is still called — moves it to the back of the new cycle
            verify(motivationRepository).updateLastShownDate(ID, TODAY);
        }

        @Test
        @DisplayName("returns null author when motivation has no author set")
        void nullAuthorPassedThrough() {
            DailyMotivation m = entity(null);
            m.setAuthor(null);
            when(motivationRepository.findNextInQueue()).thenReturn(Optional.of(m));
            when(motivationMapper.toResponse(m)).thenReturn(response(m));

            MotivationResponse result = motivationService.getTodayMotivation();

            assertThat(result.getAuthor()).isNull();
        }

        @Test
        @DisplayName("calls findNextInQueue exactly once per invocation")
        void callsFindNextInQueueExactlyOnce() {
            stubQueue(entity(null));

            motivationService.getTodayMotivation();

            verify(motivationRepository, times(1)).findNextInQueue();
        }

        @Test
        @DisplayName("calls updateLastShownDate exactly once per invocation")
        void callsUpdateLastShownDateExactlyOnce() {
            stubQueue(entity(null));

            motivationService.getTodayMotivation();

            verify(motivationRepository, times(1)).updateLastShownDate(any(), any());
        }
    }

    @Nested
    @DisplayName("getTodayMotivation() — queue is empty (no motivations uploaded yet)")
    class QueueEmpty {

        @Test
        @DisplayName("throws MotivationNotFoundException when no motivations exist at all")
        void throwsWhenQueueEmpty() {
            when(motivationRepository.findNextInQueue()).thenReturn(Optional.empty());

            assertThrows(MotivationNotFoundException.class,
                    () -> motivationService.getTodayMotivation());
        }

        @Test
        @DisplayName("does NOT call updateLastShownDate when queue is empty")
        void doesNotStampWhenEmpty() {
            when(motivationRepository.findNextInQueue()).thenReturn(Optional.empty());

            assertThrows(MotivationNotFoundException.class,
                    () -> motivationService.getTodayMotivation());

            verify(motivationRepository, never()).updateLastShownDate(any(), any());
        }

        @Test
        @DisplayName("does NOT call mapper when queue is empty")
        void doesNotMapWhenEmpty() {
            when(motivationRepository.findNextInQueue()).thenReturn(Optional.empty());

            assertThrows(MotivationNotFoundException.class,
                    () -> motivationService.getTodayMotivation());

            verify(motivationMapper, never()).toResponse(any());
        }
    }
}