package com.veterinaria.ai.features.soap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.veterinaria.ai.audit.AiFeature;
import com.veterinaria.ai.audit.AiFeedback;
import com.veterinaria.ai.audit.AiInteractionLog;
import com.veterinaria.ai.audit.AiInteractionLogRepository;
import com.veterinaria.ai.audit.AiStatus;
import com.veterinaria.ai.provider.AiOptions;
import com.veterinaria.ai.provider.AiProvider;
import com.veterinaria.ai.provider.AiRequest;
import com.veterinaria.ai.provider.AiResponse;
import com.veterinaria.application.dto.request.ConsultationPatchRequest;
import com.veterinaria.application.dto.request.DiagnosisRequest;
import com.veterinaria.application.dto.request.PrescriptionRequest;
import com.veterinaria.application.dto.response.ConsultationResponse;
import com.veterinaria.application.service.ConsultationService;
import com.veterinaria.domain.entity.Consultation;
import com.veterinaria.domain.entity.Patient;
import com.veterinaria.domain.entity.Product;
import com.veterinaria.domain.repository.ConsultationRepository;
import com.veterinaria.domain.repository.DiagnosisRepository;
import com.veterinaria.domain.repository.PrescriptionRepository;
import com.veterinaria.domain.repository.ProductRepository;
import com.veterinaria.exception.BusinessRuleException;
import com.veterinaria.exception.ResourceNotFoundException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class SoapAssistantService {

    private static final Logger log = LoggerFactory.getLogger(SoapAssistantService.class);
    private static final Pattern CIE_CODE_PATTERN = Pattern.compile("^[A-Z]\\d{2}(\\.\\d{1,2})?$");
    private static final String DISCLAIMER_VALIDATION = "Sugerencia generada por IA. Debe ser validada por un veterinario antes de aplicarse.";
    private static final String DISCLAIMER_JUDGMENT = "Esta sugerencia no reemplaza el juicio clinico profesional.";
    private static final int RECENT_HISTORY_LIMIT = 3;

    private final AiProvider aiProvider;
    private final ConsultationService consultationService;
    private final ConsultationRepository consultationRepo;
    private final DiagnosisRepository diagnosisRepo;
    private final PrescriptionRepository prescriptionRepo;
    private final ProductRepository productRepo;
    private final AiInteractionLogRepository logRepo;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate readOnlyTx;
    private final String systemPrompt;
    private final String userPromptTemplate;
    private final String defaultModel;
    private final double defaultTemperature;
    private final int defaultMaxTokens;

    public SoapAssistantService(
            AiProvider aiProvider,
            ConsultationService consultationService,
            ConsultationRepository consultationRepo,
            DiagnosisRepository diagnosisRepo,
            PrescriptionRepository prescriptionRepo,
            ProductRepository productRepo,
            AiInteractionLogRepository logRepo,
            ObjectMapper objectMapper,
            PlatformTransactionManager txManager,
            @Value("classpath:prompts/soap/system.txt") Resource systemPromptResource,
            @Value("classpath:prompts/soap/user.txt") Resource userPromptResource,
            @Value("${spring.ai.anthropic.chat.model}") String defaultModel,
            @Value("${spring.ai.anthropic.chat.temperature:0.3}") double defaultTemperature,
            @Value("${spring.ai.anthropic.chat.max-tokens:4096}") int defaultMaxTokens
    ) throws IOException {
        this.aiProvider = aiProvider;
        this.consultationService = consultationService;
        this.consultationRepo = consultationRepo;
        this.diagnosisRepo = diagnosisRepo;
        this.prescriptionRepo = prescriptionRepo;
        this.productRepo = productRepo;
        this.logRepo = logRepo;
        this.objectMapper = objectMapper;
        this.readOnlyTx = new TransactionTemplate(txManager);
        this.readOnlyTx.setReadOnly(true);
        this.systemPrompt = new String(systemPromptResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        this.userPromptTemplate = new String(userPromptResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        this.defaultModel = defaultModel;
        this.defaultTemperature = defaultTemperature;
        this.defaultMaxTokens = defaultMaxTokens;
    }

    public SoapSuggestionResult suggest(UUID consultationId, String freeText, boolean includeHistory, UUID userId) {
        SoapContext ctx = readOnlyTx.execute(status -> loadContext(consultationId, includeHistory));

        String userPrompt = renderUserPrompt(ctx, freeText);
        AiRequest request = new AiRequest(
                systemPrompt,
                userPrompt,
                new AiOptions(defaultModel, defaultTemperature, defaultMaxTokens)
        );
        AiResponse response = aiProvider.complete(request);

        SoapSuggestion suggestion = null;
        AiStatus status = response.success() ? AiStatus.SUCCESS : AiStatus.FAILURE;
        String error = response.error();

        if (response.success()) {
            try {
                SoapSuggestion parsed = parseJson(response.text());
                suggestion = validate(parsed, ctx.validProductIds());
            } catch (BusinessRuleException e) {
                status = AiStatus.FAILURE;
                error = e.getMessage();
            } catch (Exception e) {
                status = AiStatus.FAILURE;
                error = "Parse error: " + e.getMessage();
            }
        }

        UUID interactionId = saveLog(consultationId, userId, request, response, suggestion, status, error);

        if (status == AiStatus.FAILURE) {
            throw new BusinessRuleException("AI_SUGGESTION_FAILED",
                    error != null ? error : "El LLM no respondio", 502);
        }
        return new SoapSuggestionResult(suggestion, interactionId);
    }

    @Transactional
    public ConsultationResponse apply(UUID consultationId, SoapSuggestion suggestion, UUID vetId) {
        consultationService.getConsultation(consultationId);

        ConsultationPatchRequest patch = new ConsultationPatchRequest(
                suggestion.subjective(),
                suggestion.objective(),
                suggestion.plan(),
                null,
                null
        );
        consultationService.updateConsultation(consultationId, patch);

        boolean hasPrimary = suggestion.suggestedDiagnoses() != null
                && suggestion.suggestedDiagnoses().stream().anyMatch(SuggestedDiagnosis::isPrimary);
        if (hasPrimary) {
            diagnosisRepo.clearPrimary(consultationId);
        }

        if (suggestion.suggestedDiagnoses() != null) {
            for (SuggestedDiagnosis d : suggestion.suggestedDiagnoses()) {
                if (d.description() == null || d.description().isBlank()) continue;
                DiagnosisRequest req = new DiagnosisRequest(
                        d.cieCode(),
                        d.description(),
                        d.severity(),
                        d.isPrimary()
                );
                consultationService.addDiagnosis(consultationId, req);
            }
        }

        if (suggestion.suggestedPrescriptions() != null) {
            for (SuggestedPrescription p : suggestion.suggestedPrescriptions()) {
                if (p.productId() == null) continue;
                if (p.dosage() == null || p.dosage().isBlank()) continue;
                if (p.frequency() == null || p.frequency().isBlank()) continue;
                PrescriptionRequest req = new PrescriptionRequest(
                        p.productId(),
                        p.dosage(),
                        p.frequency(),
                        p.durationDays(),
                        p.instructions()
                );
                consultationService.addPrescription(consultationId, req);
            }
        }

        return consultationService.getConsultation(consultationId);
    }

    @Transactional
    public void recordFeedback(UUID consultationId, UUID interactionId, AiFeedback rating) {
        AiInteractionLog entry = logRepo.findById(interactionId)
                .orElseThrow(() -> new ResourceNotFoundException("AI_INTERACTION_NOT_FOUND",
                        "Interaccion IA no encontrada: " + interactionId));

        if (!consultationId.equals(entry.getEntityId())) {
            throw new BusinessRuleException("AI_INTERACTION_MISMATCH",
                    "La interaccion no pertenece a esta consulta", 400);
        }

        short fb = (short) (rating == AiFeedback.UP ? 1 : -1);
        logRepo.updateFeedback(interactionId, fb);
    }

    private SoapContext loadContext(UUID consultationId, boolean includeHistory) {
        Consultation consultation = consultationRepo.findByIdWithDetails(consultationId)
                .orElseThrow(() -> new ResourceNotFoundException("CONSULTATION_NOT_FOUND",
                        "Consulta no encontrada: " + consultationId));

        Patient patient = consultation.getAppointment().getPatient();

        List<PatientHistorySnippet> history = includeHistory
                ? loadHistory(patient.getId())
                : List.of();

        List<Product> activeProducts = productRepo.findByIsActiveTrueOrderByName();
        List<ProductCatalogSnippet> catalog = activeProducts.stream()
                .map(p -> new ProductCatalogSnippet(p.getId(), p.getName(), p.getType()))
                .toList();
        Set<UUID> validProductIds = activeProducts.stream()
                .map(Product::getId)
                .collect(Collectors.toSet());

        return new SoapContext(patient, history, catalog, validProductIds);
    }

    private List<PatientHistorySnippet> loadHistory(UUID patientId) {
        List<Consultation> recent = consultationRepo
                .findByPatientId(patientId, PageRequest.of(0, RECENT_HISTORY_LIMIT))
                .getContent();
        List<PatientHistorySnippet> snippets = new ArrayList<>();
        for (Consultation c : recent) {
            String anamnesisSnippet = Optional.ofNullable(c.getAnamnesis())
                    .map(s -> s.length() > 200 ? s.substring(0, 200) + "..." : s)
                    .orElse("");
            List<String> dx = diagnosisRepo.findByConsultationId(c.getId()).stream()
                    .map(d -> d.getDescription() + (d.getCieCode() != null ? " (" + d.getCieCode() + ")" : ""))
                    .toList();
            List<String> rx = prescriptionRepo.findByConsultationId(c.getId()).stream()
                    .map(p -> p.getProduct().getName() + " (" + p.getDosage() + " " + p.getFrequency() + ")")
                    .toList();
            snippets.add(new PatientHistorySnippet(
                    c.getCreatedAt().toLocalDate(),
                    anamnesisSnippet,
                    dx,
                    rx
            ));
        }
        return snippets;
    }

    private String renderUserPrompt(SoapContext ctx, String freeText) {
        String historyBlock = ctx.history().isEmpty()
                ? "Sin consultas previas."
                : renderHistoryBlock(ctx.history());
        String productsBlock = renderProductsBlock(ctx.products());

        return userPromptTemplate
                .replace("{name}", nullSafe(ctx.patient().getName()))
                .replace("{species}", ctx.patient().getSpecies() != null ? ctx.patient().getSpecies().getName() : "desconocida")
                .replace("{breed}", ctx.patient().getBreed() != null ? ctx.patient().getBreed().getName() : "desconocida")
                .replace("{ageYears}", computeAgeYears(ctx.patient().getBirthDate()))
                .replace("{sex}", ctx.patient().getSex().name())
                .replace("{weightKg}", ctx.patient().getWeightKg() != null ? ctx.patient().getWeightKg().toPlainString() : "desconocido")
                .replace("{isSterilized}", ctx.patient().isSterilized() ? "si" : "no")
                .replace("{history_block}", historyBlock)
                .replace("{products_block}", productsBlock)
                .replace("{freeText}", freeText != null ? freeText : "");
    }

    private String renderHistoryBlock(List<PatientHistorySnippet> history) {
        StringBuilder sb = new StringBuilder();
        for (PatientHistorySnippet h : history) {
            sb.append("- ").append(h.date()).append(" | ").append(h.anamnesisSnippet()).append("\n");
            sb.append("  Diagnosticos: ").append(String.join("; ", h.diagnoses())).append("\n");
            sb.append("  Prescripciones: ").append(String.join("; ", h.prescriptions())).append("\n");
        }
        return sb.toString().trim();
    }

    private String renderProductsBlock(List<ProductCatalogSnippet> products) {
        StringBuilder sb = new StringBuilder();
        for (ProductCatalogSnippet p : products) {
            sb.append("- ").append(p.id()).append(" | ").append(p.name()).append(" | ").append(p.type()).append("\n");
        }
        return sb.toString().trim();
    }

    private String computeAgeYears(LocalDate birthDate) {
        if (birthDate == null) return "desconocida";
        return String.valueOf(Period.between(birthDate, LocalDate.now()).getYears());
    }

    private String nullSafe(String s) {
        return s != null ? s : "";
    }

    private SoapSuggestion parseJson(String text) {
        if (text == null || text.isBlank()) {
            throw new BusinessRuleException("AI_RESPONSE_EMPTY",
                    "El LLM devolvio una respuesta vacia", 502);
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end < 0 || start > end) {
            throw new BusinessRuleException("AI_RESPONSE_NO_JSON",
                    "No se encontro JSON en la respuesta del LLM", 502);
        }
        String json = text.substring(start, end + 1);
        try {
            return objectMapper.readValue(json, SoapSuggestion.class);
        } catch (JacksonException e) {
            throw new BusinessRuleException("AI_RESPONSE_INVALID",
                    "No se pudo parsear la respuesta del LLM: " + e.getOriginalMessage(), 502);
        }
    }

    private SoapSuggestion validate(SoapSuggestion s, Set<UUID> validProductIds) {
        List<String> warnings = s.warnings() != null ? new ArrayList<>(s.warnings()) : new ArrayList<>();
        List<String> disclaimers = s.disclaimers() != null ? new ArrayList<>(s.disclaimers()) : new ArrayList<>();
        if (!disclaimers.contains(DISCLAIMER_VALIDATION)) disclaimers.add(DISCLAIMER_VALIDATION);
        if (!disclaimers.contains(DISCLAIMER_JUDGMENT)) disclaimers.add(DISCLAIMER_JUDGMENT);

        List<SuggestedDiagnosis> validDiagnoses = new ArrayList<>();
        boolean primarySeen = false;
        List<SuggestedDiagnosis> inputDiagnoses = s.suggestedDiagnoses() != null ? s.suggestedDiagnoses() : List.of();
        for (SuggestedDiagnosis d : inputDiagnoses) {
            String cieCode = d.cieCode();
            if (cieCode != null && !CIE_CODE_PATTERN.matcher(cieCode).matches()) {
                warnings.add("Codigo CIE-10 invalido descartado: " + cieCode);
                cieCode = null;
            }
            boolean isPrimary = d.isPrimary();
            if (isPrimary && primarySeen) {
                warnings.add("Multiples diagnosticos primarios detectados; se mantiene solo el primero");
                isPrimary = false;
            }
            if (isPrimary) primarySeen = true;
            double confidence = Math.max(0.0, Math.min(1.0, d.confidence()));
            validDiagnoses.add(new SuggestedDiagnosis(
                    d.description(), cieCode, d.severity(), isPrimary, confidence, d.rationale()));
        }

        List<SuggestedPrescription> validPrescriptions = new ArrayList<>();
        List<SuggestedPrescription> inputPrescriptions = s.suggestedPrescriptions() != null ? s.suggestedPrescriptions() : List.of();
        for (SuggestedPrescription p : inputPrescriptions) {
            UUID productId = p.productId();
            if (productId != null && !validProductIds.contains(productId)) {
                warnings.add("Producto no encontrado en catalogo, descartado: " + productId);
                productId = null;
            }
            validPrescriptions.add(new SuggestedPrescription(
                    productId, p.productNameHint(), p.dosage(), p.frequency(), p.durationDays(), p.instructions()));
        }

        return new SoapSuggestion(
                s.subjective(), s.objective(), s.assessment(), s.plan(),
                validDiagnoses, validPrescriptions, warnings, s.followUp(), disclaimers);
    }

    private UUID saveLog(UUID consultationId, UUID userId, AiRequest request, AiResponse response,
                         SoapSuggestion suggestion, AiStatus status, String error) {
        AiInteractionLog logEntry = new AiInteractionLog();
        logEntry.setFeature(AiFeature.SOAP_ASSISTANT);
        logEntry.setEntityType("consultation");
        logEntry.setEntityId(consultationId);
        logEntry.setUserId(userId);
        logEntry.setModel(defaultModel);
        logEntry.setPromptHash(sha256(request.systemPrompt() + "\n---\n" + request.userPrompt()));
        logEntry.setOutputHash(sha256(response.text() != null ? response.text() : ""));
        logEntry.setPromptTokens(response.promptTokens());
        logEntry.setCompletionTokens(response.completionTokens());
        logEntry.setCostUsd(response.costUsd());
        logEntry.setLatencyMs(Math.toIntExact(Math.min(response.latencyMs(), Integer.MAX_VALUE)));
        logEntry.setStatus(status);
        logEntry.setErrorMessage(error);
        return logRepo.save(logEntry).getId();
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public record SoapSuggestionResult(SoapSuggestion suggestion, UUID interactionId) {}

    private record SoapContext(
            Patient patient,
            List<PatientHistorySnippet> history,
            List<ProductCatalogSnippet> products,
            Set<UUID> validProductIds
    ) {}
}
