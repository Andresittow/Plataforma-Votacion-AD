package com.votingplatform.voterregistrationservice.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.votingplatform.common.domain.Region;
import com.votingplatform.common.domain.VoterId;
import com.votingplatform.common.domain.VoterRecord;
import com.votingplatform.voterregistrationservice.application.port.EligibleVoterPublisher;
import com.votingplatform.voterregistrationservice.application.port.RawVoter;
import com.votingplatform.voterregistrationservice.application.port.VoterSource;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VoterRegistrationServiceTest {
    @Mock
    private VoterSource source;

    @Mock
    private EligibleVoterPublisher publisher;

    @Test
    void shouldPublishOnlyValidVoters() {
        // Arrange
        when(source.readAll()).thenReturn(List.of(
                new RawVoter("u-001", "norte"),
                new RawVoter("u-002", "sur"),
                new RawVoter("", "este")));
        VoterRegistrationService service = new VoterRegistrationService(source, publisher);

        // Act
        RegistrationReport report = service.registerAll();

        // Assert
        assertThat(report.publicados()).isEqualTo(2);
        ArgumentCaptor<VoterRecord> records = ArgumentCaptor.forClass(VoterRecord.class);
        verify(publisher, org.mockito.Mockito.times(2)).publish(records.capture());
        assertThat(records.getAllValues()).containsExactly(
                new VoterRecord(new VoterId("u-001"), new Region("norte"), true),
                new VoterRecord(new VoterId("u-002"), new Region("sur"), true));
    }

    @Test
    void shouldCountInvalidVotersInReportWithoutPublishingThem() {
        // Arrange
        when(source.readAll()).thenReturn(List.of(
                new RawVoter("", "norte"),
                new RawVoter("u-002", "region-invalida")));
        VoterRegistrationService service = new VoterRegistrationService(source, publisher);

        // Act
        RegistrationReport report = service.registerAll();

        // Assert
        assertThat(report).isEqualTo(new RegistrationReport(0, 2));
        verify(publisher, never()).publish(org.mockito.ArgumentMatchers.any(VoterRecord.class));
    }

    @Test
    void shouldDeduplicateByUserIdAndKeepLastRecord() {
        // Arrange
        when(source.readAll()).thenReturn(List.of(
                new RawVoter("u-001", "norte"),
                new RawVoter("u-002", "sur"),
                new RawVoter("u-001", "centro")));
        VoterRegistrationService service = new VoterRegistrationService(source, publisher);

        // Act
        RegistrationReport report = service.registerAll();

        // Assert
        assertThat(report).isEqualTo(new RegistrationReport(2, 1));
        ArgumentCaptor<VoterRecord> records = ArgumentCaptor.forClass(VoterRecord.class);
        verify(publisher, org.mockito.Mockito.times(2)).publish(records.capture());
        assertThat(records.getAllValues()).containsExactly(
                new VoterRecord(new VoterId("u-001"), new Region("centro"), true),
                new VoterRecord(new VoterId("u-002"), new Region("sur"), true));
    }

    @Test
    void shouldNotPublishWhenSourceIsEmpty() {
        // Arrange
        when(source.readAll()).thenReturn(List.of());
        VoterRegistrationService service = new VoterRegistrationService(source, publisher);

        // Act
        RegistrationReport report = service.registerAll();

        // Assert
        assertThat(report).isEqualTo(new RegistrationReport(0, 0));
        verify(publisher, never()).publish(org.mockito.ArgumentMatchers.any(VoterRecord.class));
    }

    @Test
    void shouldPublishExactlyOnceForEachValidVoter() {
        // Arrange
        when(source.readAll()).thenReturn(List.of(
                new RawVoter("u-001", "norte"),
                new RawVoter("u-002", "sur"),
                new RawVoter("u-003", "este")));
        VoterRegistrationService service = new VoterRegistrationService(source, publisher);

        // Act
        RegistrationReport report = service.registerAll();

        // Assert
        assertThat(report).isEqualTo(new RegistrationReport(3, 0));
        verify(publisher, org.mockito.Mockito.times(3))
                .publish(org.mockito.ArgumentMatchers.any(VoterRecord.class));
        verifyNoMoreInteractions(publisher);
    }
}
