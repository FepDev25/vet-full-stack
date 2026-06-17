package com.veterinaria.ai.features.soap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;

import com.veterinaria.ai.audit.AiFeedback;
import com.veterinaria.ai.audit.AiInteractionLog;
import com.veterinaria.ai.audit.AiInteractionLogRepository;
import com.veterinaria.ai.audit.AiStatus;
import com.veterinaria.ai.provider.AiRequest;
import com.veterinaria.ai.provider.AiResponse;
import com.veterinaria.application.dto.request.ConsultationPatchRequest;
import com.veterinaria.application.dto.request.DiagnosisRequest;
import com.veterinaria.application.dto.request.PrescriptionRequest;
import com.veterinaria.application.dto.response.ConsultationResponse;
import com.veterinaria.application.service.ConsultationService;
import com.veterinaria.domain.entity.Appointment;
import com.veterinaria.domain.entity.Breed;
import com.veterinaria.domain.entity.Consultation;
import com.veterinaria.domain.entity.Diagnosis;
import com.veterinaria.domain.entity.Patient;
import com.veterinaria.domain.entity.Prescription;
import com.veterinaria.domain.entity.Species;
import com.veterinaria.domain.entity.Staff;
import com.veterinaria.domain.enums.DiagnosisSeverity;
import com.veterinaria.domain.enums.PatientSex;
import com.veterinaria.domain.repository.ConsultationRepository;
import com.veterinaria.domain.repository.DiagnosisRepository;
import com.veterinaria.domain.repository.PrescriptionRepository;
import com.veterinaria.domain.repository.ProductRepository;
import com.veterinaria.domain.repository.StaffRepository;
import com.veterinaria.exception.BusinessRuleException;
import com.veterinaria.exception.ResourceNotFoundException;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SoapAssistantServiceTest {

    @Mock private com.veterinaria.ai.provider.AiProvider aiProvider;
    @Mock private ConsultationService consultationService;
    @Mock private ConsultationRepository consultationRepo;
    @Mock private DiagnosisRepository diagnosisRepo;
    @Mock private PrescriptionRepository prescriptionRepo;
    @Mock private ProductRepository productRepo;
    @Mock private AiInteractionLogRepository logRepo;
    @Mock private StaffRepository staffRepo;
    @Mock private PlatformTransactionManager txManager;

    private ObjectMapper objectMapper;
    private SoapAssistantService service;

    private static final UUID CONSULTATION_ID = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID VET_ID = UUID.randomUUID();
    private static final UUID INTERACTION_ID = UUID.randomUUID();
    private static final UUID PRODUCT_IN_CATALOG = UUID.randomUUID();
    private static final UUID PRODUCT_NOT_IN_CATALOG = UUID.randomUUID();

    @BeforeEach
    void setUp() throws IOException {
        objectMapper = new ObjectMapper();
        when(txManager.getTransaction(any())).thenReturn(null);

        Resource sysPrompt = new ByteArrayResource("system".getBytes());
        Resource userPrompt = new ByteArrayResource(
                ("Contexto del paciente:\n- Nombre: {name}\n- Especie: {species}\n" +
                 "- Raza: {breed}\n- Edad: {ageYears} anios\n- Sexo: {sex}\n" +
                 "- Peso actual: {weightKg} kg\n- Esterilizado: {isSterilized}\n" +
                 "\nHistoria reciente:\n{history_block}\n" +
                 "\nCatalogo de productos:\n{products_block}\n" +
                 "\nNotas:\n\"\"\"{freeText}\"\"\"\n").getBytes());

        service = new SoapAssistantService(
                aiProvider, consultationService, consultationRepo,
                diagnosisRepo, prescriptionRepo, productRepo,
                logRepo, objectMapper, txManager,
                sysPrompt, userPrompt,
                "claude-haiku-4-5", 0.3, 4096);
    }

    private Patient patient() {
        Species species = new Species();
        species.setId(UUID.randomUUID());
        species.setName("Perro");
        Breed breed = new Breed();
        breed.setId(UUID.randomUUID());
        breed.setName("Labrador");
        Patient p = new Patient();
        p.setId(PATIENT_ID);
        p.setName("Firulais");
        p.setSpecies(species);
        p.setBreed(breed);
        p.setBirthDate(LocalDate.of(2020, 1, 1));
        p.setSex(PatientSex.M);
        p.setWeightKg(new BigDecimal("25.0"));
        p.setSterilized(true);
        return p;
    }

    private Consultation consultation(Patient p) {
        Appointment appt = new Appointment();
        appt.setPatient(p);
        Consultation c = new Consultation();
        c.setId(CONSULTATION_ID);
        c.setAppointment(appt);
        return c;
    }

    private String validSoapJson() {
        return "{\"subjective\":\"S\",\"objective\":\"O\",\"assessment\":\"A\",\"plan\":\"P\"," +
                "\"suggestedDiagnoses\":[{\"description\":\"dx1\",\"cieCode\":\"K52.9\"," +
                "\"severity\":\"MODERATE\",\"isPrimary\":true,\"confidence\":0.8," +
                "\"rationale\":\"r\"}]," +
                "\"suggestedPrescriptions\":[{\"productId\":\"" + PRODUCT_IN_CATALOG + "\"," +
                "\"productNameHint\":\"med\",\"dosage\":\"1mg\",\"frequency\":\"c/24h\"," +
                "\"durationDays\":3,\"instructions\":\"x\"}]," +
                "\"warnings\":[],\"followUp\":null,\"disclaimers\":[]}";
    }

    private AiResponse successResponse(String text) {
        return AiResponse.success(text, 100, 50, new BigDecimal("0.000250"), 1500L);
    }

    @Test
    void suggest_validJson_returnsSuggestion() {
        when(aiProvider.complete(any(AiRequest.class))).thenReturn(successResponse(validSoapJson()));
        when(consultationRepo.findByIdWithDetails(CONSULTATION_ID)).thenReturn(Optional.of(consultation(patient())));
        when(productRepo.findByIsActiveTrueOrderByName()).thenReturn(List.of());
        when(consultationRepo.findByPatientId(any(), any())).thenReturn(org.springframework.data.domain.Page.empty());
        when(diagnosisRepo.findByConsultationId(any())).thenReturn(List.of());
        when(prescriptionRepo.findByConsultationId(any())).thenReturn(List.of());
        when(logRepo.save(any())).thenAnswer(inv -> {
            AiInteractionLog log = inv.getArgument(0);
            log.setId(INTERACTION_ID);
            return log;
        });

        SoapAssistantService.SoapSuggestionResult result = service.suggest(
                CONSULTATION_ID, "test", false, VET_ID);

        assertNotNull(result);
        assertNotNull(result.suggestion());
        assertEquals(INTERACTION_ID, result.interactionId());
        assertEquals("S", result.suggestion().subjective());
        verify(logRepo).save(any());
    }

    @Test
    void suggest_emptyResponse_throwsBusinessRule() {
        when(aiProvider.complete(any(AiRequest.class))).thenReturn(AiResponse.success("", null, null, null, 100L));
        when(consultationRepo.findByIdWithDetails(CONSULTATION_ID)).thenReturn(Optional.of(consultation(patient())));
        when(productRepo.findByIsActiveTrueOrderByName()).thenReturn(List.of());
        when(consultationRepo.findByPatientId(any(), any())).thenReturn(org.springframework.data.domain.Page.empty());
        when(diagnosisRepo.findByConsultationId(any())).thenReturn(List.of());
        when(prescriptionRepo.findByConsultationId(any())).thenReturn(List.of());
        when(logRepo.save(any())).thenAnswer(inv -> {
            AiInteractionLog log = inv.getArgument(0);
            log.setId(INTERACTION_ID);
            return log;
        });

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> service.suggest(CONSULTATION_ID, "test", false, VET_ID));
        assertEquals("AI_SUGGESTION_FAILED", ex.getCode());
    }

    @Test
    void suggest_invalidCieCode_setsToNullAndAddsWarning() {
        String json = "{\"subjective\":\"S\",\"objective\":\"O\",\"assessment\":\"A\",\"plan\":\"P\"," +
                "\"suggestedDiagnoses\":[{\"description\":\"dx1\",\"cieCode\":\"INVALIDO\"," +
                "\"severity\":\"MILD\",\"isPrimary\":true,\"confidence\":0.5,\"rationale\":\"r\"}]," +
                "\"suggestedDiagnoses2\":[],\"suggestedPrescriptions\":[]," +
                "\"warnings\":[],\"followUp\":null,\"disclaimers\":[]}";

        when(aiProvider.complete(any(AiRequest.class))).thenReturn(successResponse(json));
        when(consultationRepo.findByIdWithDetails(CONSULTATION_ID)).thenReturn(Optional.of(consultation(patient())));
        when(productRepo.findByIsActiveTrueOrderByName()).thenReturn(List.of());
        when(consultationRepo.findByPatientId(any(), any())).thenReturn(org.springframework.data.domain.Page.empty());
        when(diagnosisRepo.findByConsultationId(any())).thenReturn(List.of());
        when(prescriptionRepo.findByConsultationId(any())).thenReturn(List.of());
        when(logRepo.save(any())).thenAnswer(inv -> {
            AiInteractionLog log = inv.getArgument(0);
            log.setId(INTERACTION_ID);
            return log;
        });

        SoapAssistantService.SoapSuggestionResult result = service.suggest(
                CONSULTATION_ID, "test", false, VET_ID);

        assertNotNull(result.suggestion().suggestedDiagnoses());
        assertEquals(1, result.suggestion().suggestedDiagnoses().size());
        assertNull(result.suggestion().suggestedDiagnoses().get(0).cieCode());
        assertTrue(result.suggestion().warnings().stream()
                .anyMatch(w -> w.contains("CIE-10") && w.contains("INVALIDO")));
    }

    @Test
    void suggest_productNotInCatalog_setsToNullAndAddsWarning() {
        String json = "{\"subjective\":\"S\",\"objective\":\"O\",\"assessment\":\"A\",\"plan\":\"P\"," +
                "\"suggestedDiagnoses\":[]," +
                "\"suggestedPrescriptions\":[{\"productId\":\"" + PRODUCT_NOT_IN_CATALOG + "\"," +
                "\"productNameHint\":\"med\",\"dosage\":\"1mg\",\"frequency\":\"c/24h\"," +
                "\"durationDays\":3,\"instructions\":\"x\"}]," +
                "\"warnings\":[],\"followUp\":null,\"disclaimers\":[]}";

        when(aiProvider.complete(any(AiRequest.class))).thenReturn(successResponse(json));
        when(consultationRepo.findByIdWithDetails(CONSULTATION_ID)).thenReturn(Optional.of(consultation(patient())));
        when(productRepo.findByIsActiveTrueOrderByName()).thenReturn(List.of());
        when(consultationRepo.findByPatientId(any(), any())).thenReturn(org.springframework.data.domain.Page.empty());
        when(diagnosisRepo.findByConsultationId(any())).thenReturn(List.of());
        when(prescriptionRepo.findByConsultationId(any())).thenReturn(List.of());
        when(logRepo.save(any())).thenAnswer(inv -> {
            AiInteractionLog log = inv.getArgument(0);
            log.setId(INTERACTION_ID);
            return log;
        });

        SoapAssistantService.SoapSuggestionResult result = service.suggest(
                CONSULTATION_ID, "test", false, VET_ID);

        assertNotNull(result.suggestion().suggestedPrescriptions());
        assertEquals(1, result.suggestion().suggestedPrescriptions().size());
        assertNull(result.suggestion().suggestedPrescriptions().get(0).productId());
        assertTrue(result.suggestion().warnings().stream()
                .anyMatch(w -> w.contains("no encontrado en catalogo")));
    }

    @Test
    void suggest_omittedDisclaimers_injectsStandardOnes() {
        when(aiProvider.complete(any(AiRequest.class))).thenReturn(successResponse(validSoapJson()));
        when(consultationRepo.findByIdWithDetails(CONSULTATION_ID)).thenReturn(Optional.of(consultation(patient())));
        when(productRepo.findByIsActiveTrueOrderByName()).thenReturn(List.of());
        when(consultationRepo.findByPatientId(any(), any())).thenReturn(org.springframework.data.domain.Page.empty());
        when(diagnosisRepo.findByConsultationId(any())).thenReturn(List.of());
        when(prescriptionRepo.findByConsultationId(any())).thenReturn(List.of());
        when(logRepo.save(any())).thenAnswer(inv -> {
            AiInteractionLog log = inv.getArgument(0);
            log.setId(INTERACTION_ID);
            return log;
        });

        SoapAssistantService.SoapSuggestionResult result = service.suggest(
                CONSULTATION_ID, "test", false, VET_ID);

        assertTrue(result.suggestion().disclaimers().stream()
                .anyMatch(d -> d.contains("validada por un veterinario")));
        assertTrue(result.suggestion().disclaimers().stream()
                .anyMatch(d -> d.contains("juicio clinico")));
    }

    @Test
    void suggest_aiFailure_throwsBusinessRuleAndLogsFailure() {
        when(aiProvider.complete(any(AiRequest.class)))
                .thenReturn(AiResponse.failure("API timeout", 30000L));
        when(consultationRepo.findByIdWithDetails(CONSULTATION_ID)).thenReturn(Optional.of(consultation(patient())));
        when(productRepo.findByIsActiveTrueOrderByName()).thenReturn(List.of());
        when(consultationRepo.findByPatientId(any(), any())).thenReturn(org.springframework.data.domain.Page.empty());
        when(diagnosisRepo.findByConsultationId(any())).thenReturn(List.of());
        when(prescriptionRepo.findByConsultationId(any())).thenReturn(List.of());
        when(logRepo.save(any())).thenAnswer(inv -> {
            AiInteractionLog log = inv.getArgument(0);
            log.setId(INTERACTION_ID);
            return log;
        });

        assertThrows(BusinessRuleException.class,
                () -> service.suggest(CONSULTATION_ID, "test", false, VET_ID));

        org.mockito.ArgumentCaptor<AiInteractionLog> captor =
                org.mockito.ArgumentCaptor.forClass(AiInteractionLog.class);
        verify(logRepo).save(captor.capture());
        assertEquals(AiStatus.FAILURE, captor.getValue().getStatus());
        assertTrue(captor.getValue().getErrorMessage().contains("API timeout"));
    }

    @Test
    void apply_validSuggestion_updatesAndAddsEntities() {
        SoapSuggestion suggestion = new SoapSuggestion(
                "S", "O", "A", "P",
                List.of(new SuggestedDiagnosis("dx1", "K52.9", DiagnosisSeverity.MILD, true, 0.8, "r")),
                List.of(new SuggestedPrescription(PRODUCT_IN_CATALOG, "med", "1mg", "c/24h", 3, "x")),
                List.of(), "follow", List.of());

        UUID diagnosisId = UUID.randomUUID();
        UUID prescriptionId = UUID.randomUUID();
        when(consultationService.getConsultation(CONSULTATION_ID)).thenReturn(sampleConsultationResponse());
        when(consultationService.updateConsultation(eq(CONSULTATION_ID), any(ConsultationPatchRequest.class)))
                .thenReturn(sampleConsultationResponse());
        when(consultationService.addDiagnosis(eq(CONSULTATION_ID), any(DiagnosisRequest.class)))
                .thenReturn(null);
        when(consultationService.addPrescription(eq(CONSULTATION_ID), any(PrescriptionRequest.class)))
                .thenReturn(null);
        when(consultationService.getConsultation(CONSULTATION_ID))
                .thenReturn(sampleConsultationResponse());

        ConsultationResponse result = service.apply(CONSULTATION_ID, suggestion, VET_ID);

        assertNotNull(result);
        verify(consultationService).updateConsultation(eq(CONSULTATION_ID), any(ConsultationPatchRequest.class));
        verify(consultationService).addDiagnosis(eq(CONSULTATION_ID), any(DiagnosisRequest.class));
        verify(consultationService).addPrescription(eq(CONSULTATION_ID), any(PrescriptionRequest.class));
        verify(diagnosisRepo).clearPrimary(CONSULTATION_ID);
    }

    @Test
    void apply_emptyDescription_skipsDiagnosis() {
        SoapSuggestion suggestion = new SoapSuggestion(
                "S", "O", "A", "P",
                List.of(new SuggestedDiagnosis("", null, DiagnosisSeverity.MILD, false, 0.5, "r")),
                List.of(),
                List.of(), null, List.of());

        when(consultationService.getConsultation(CONSULTATION_ID)).thenReturn(sampleConsultationResponse());
        when(consultationService.updateConsultation(eq(CONSULTATION_ID), any(ConsultationPatchRequest.class)))
                .thenReturn(sampleConsultationResponse());
        when(consultationService.getConsultation(CONSULTATION_ID))
                .thenReturn(sampleConsultationResponse());

        service.apply(CONSULTATION_ID, suggestion, VET_ID);

        verify(consultationService, never()).addDiagnosis(any(), any());
        verify(diagnosisRepo, never()).clearPrimary(any());
    }

    @Test
    void apply_nullProductId_skipsPrescription() {
        SoapSuggestion suggestion = new SoapSuggestion(
                "S", "O", "A", "P",
                List.of(),
                List.of(new SuggestedPrescription(null, "h", "1mg", "c/24h", 3, "x")),
                List.of(), null, List.of());

        when(consultationService.getConsultation(CONSULTATION_ID)).thenReturn(sampleConsultationResponse());
        when(consultationService.updateConsultation(eq(CONSULTATION_ID), any(ConsultationPatchRequest.class)))
                .thenReturn(sampleConsultationResponse());
        when(consultationService.getConsultation(CONSULTATION_ID))
                .thenReturn(sampleConsultationResponse());

        service.apply(CONSULTATION_ID, suggestion, VET_ID);

        verify(consultationService, never()).addPrescription(any(), any());
    }

    @Test
    void recordFeedback_existingInteraction_updatesWithCorrectShort() {
        AiInteractionLog entry = new AiInteractionLog();
        entry.setId(INTERACTION_ID);
        entry.setEntityId(CONSULTATION_ID);
        when(logRepo.findById(INTERACTION_ID)).thenReturn(Optional.of(entry));

        service.recordFeedback(CONSULTATION_ID, INTERACTION_ID, AiFeedback.UP);

        verify(logRepo).updateFeedback(INTERACTION_ID, (short) 1);
    }

    @Test
    void recordFeedback_downFeedback_updatesNegativeShort() {
        AiInteractionLog entry = new AiInteractionLog();
        entry.setId(INTERACTION_ID);
        entry.setEntityId(CONSULTATION_ID);
        when(logRepo.findById(INTERACTION_ID)).thenReturn(Optional.of(entry));

        service.recordFeedback(CONSULTATION_ID, INTERACTION_ID, AiFeedback.DOWN);

        verify(logRepo).updateFeedback(INTERACTION_ID, (short) -1);
    }

    @Test
    void recordFeedback_mismatchedEntity_throwsBusinessRule() {
        AiInteractionLog entry = new AiInteractionLog();
        entry.setId(INTERACTION_ID);
        entry.setEntityId(UUID.randomUUID());
        when(logRepo.findById(INTERACTION_ID)).thenReturn(Optional.of(entry));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> service.recordFeedback(CONSULTATION_ID, INTERACTION_ID, AiFeedback.UP));
        assertEquals("AI_INTERACTION_MISMATCH", ex.getCode());
        verify(logRepo, never()).updateFeedback(any(), any());
    }

    @Test
    void recordFeedback_notFound_throwsResourceNotFound() {
        when(logRepo.findById(INTERACTION_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.recordFeedback(CONSULTATION_ID, INTERACTION_ID, AiFeedback.UP));
        verify(logRepo, never()).updateFeedback(any(), any());
    }

    private static <T> T eq(T value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }

    private ConsultationResponse sampleConsultationResponse() {
        return new ConsultationResponse(
                CONSULTATION_ID, UUID.randomUUID(), PATIENT_ID, "Firulais",
                VET_ID, "Carlos Mendoza", "S", "O", "P",
                new BigDecimal("25.0"), new BigDecimal("38.5"),
                List.of(), List.of(), OffsetDateTime.now(), null);
    }
}
