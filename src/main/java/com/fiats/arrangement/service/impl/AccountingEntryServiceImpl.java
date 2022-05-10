package com.fiats.arrangement.service.impl;

import com.fiats.arrangement.common.ArrangementEventCode;
import com.fiats.arrangement.constant.*;
import com.fiats.arrangement.jpa.entity.*;
import com.fiats.arrangement.jpa.repo.AccountingEntryRepo;
import com.fiats.arrangement.jpa.repo.ArrangementRelationRepo;
import com.fiats.arrangement.jpa.repo.ArrangementRepo;
import com.fiats.arrangement.jpa.specs.AccountingEntrySpecs;
import com.fiats.arrangement.payload.AccountingArrangementDTO;
import com.fiats.arrangement.payload.AccountingEntryDTO;
import com.fiats.arrangement.payload.AccountingEntryMapper;
import com.fiats.arrangement.payload.AccountingEntryPriceDTO;
import com.fiats.arrangement.payload.filter.AccountingEntryFilter;
import com.fiats.arrangement.service.*;
import com.fiats.arrangement.utils.ConverterUtils;
import com.fiats.arrangement.utils.ExcelHelper;
import com.fiats.arrangement.validator.AccountingEntryValidator;
import com.fiats.arrangement.validator.ArrangementValidator;
import com.fiats.exception.NeoFiatsException;
import com.fiats.tmgcoreutils.constant.Constant;
import com.fiats.tmgcoreutils.constant.PortfolioRetailAction;
import com.fiats.tmgcoreutils.jwt.JWTHelper;
import com.fiats.tmgcoreutils.payload.*;
import com.fiats.tmgcoreutils.utils.CommonUtils;
import com.fiats.tmgcoreutils.utils.DateHelper;
import com.fiats.tmgjpa.entity.LongIDIdentityBase;
import com.fiats.tmgjpa.entity.RecordStatus;
import com.fiats.tmgjpa.paging.PagingFilterBase;
import com.fiats.tmgjpa.payload.ResponseMessage;
import com.neo.exception.LoggingUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AccountingEntryServiceImpl implements AccountingEntryService {

    @Value("${tmp.t24.path}")
    String t24TmpPath;

    @Autowired
    AccountingEntryRepo accountingEntryRepo;

    @Autowired
    ArrangementRepo arrangementRepo;

    @Autowired
    ArrangementRelationRepo arrangementRelationRepo;

    @Autowired
    ArrangementTickingService arrTickingService;

    @Autowired
    ExcelHelper excelHelper;

    @Autowired
    AccountingEntrySpecs aeSpecs;

    @Autowired
    AccountingEntryValidator accountingEntryValidator;

    @Autowired
    ArrangementValidator arrangementValidator;

    @Autowired
    CustomerService customerService;

    @Autowired
    @Qualifier(Constant.DEFAULT_THREAD_POOL)
    TaskExecutor executor;

    @Autowired
    PortfolioService portfolioService;

    @Autowired
    MatchingService matchingService;

    @Autowired
    ArrangementNotificationService arrNotiService;

    @Autowired
    ProductDerivativeService productDerivativeService;

    @Autowired
    ArrangementService arrangementService;

    @Value("${custom.kafka.topic.prefix}")
    String KAFKA_TOPIC_PREFIX;

    @Value("${spring.application.name}")
    String applicationName;

    @Autowired
    RedisTemplate<String, String> redisTemplate;

    @Override
    public ResponseMessage<List<AccountingEntryDTO>> listAccountingEntries(
            PagingFilterBase<AccountingEntryFilter> pf) {

        log.info("Retrieving the approved reference details list based on page filter {}", pf);
        Specification<AccountingEntry> conditions = aeSpecs.buildSpec(pf.getFilter());

        List<AccountingEntry> records;
        Sort sort = Sort.by("entryTransactionDate").descending().and(Sort.by("createdDate").descending());
        if (!pf.isPageable()) {
            records = accountingEntryRepo.findAll(conditions, sort);
        } else {
            Page<AccountingEntry> page = accountingEntryRepo.findAll(conditions,
                    PageRequest.of(pf.getPageNum(), pf.getPageSize(), sort));
            pf.getPaging().setTotalRecords(page.getTotalElements());
            pf.getPaging().setTotalPages(page.getTotalPages());
            records = page.getContent();
        }

        List<AccountingEntryDTO> results = records.stream().map(r -> {
            AccountingEntryDTO dto = new AccountingEntryDTO();
            BeanUtils.copyProperties(r, dto);
            dto.setEntryAmount(CommonUtils.bigDecimalToDoubleSilently(r.getEntryAmount()));
            dto.setEntryRemainingAmount(CommonUtils.bigDecimalToDoubleSilently(r.getEntryRemainingAmount()));

            // arrangement
            if (r.getArrangement() != null) {
                dto.setArrangement(ArrangementDTO.builder()
                        .id(r.getArrangement().getId())
                        .code(r.getArrangement().getCode())
                        .build());
            }
            return dto;
        }).collect(Collectors.toList());
        return new ResponseMessage<>(results, pf.getPaging());
    }

    @Override
    public AccountingArrangementDTO findArrangementInfo(String code) {

        if (!StringUtils.hasText(code)) {
            return null;
        }

        // find arrangement by code
        Arrangement arr = arrangementValidator.validateExistence(
                arrangementRepo.findByCodeFetchParty(code), Constant.ACTIVE
        );

        // find customer info
        CustomerDTO customer = null;
        ArrangementParty arrParty = arr.getParty(ArrangementRoleEnum.OWNER);
        if (arrParty != null) {
            customer = customerService.retrieveCustomerInfo(arrParty.getCustomerId());
        }

        // casting model
        ArrangementDTO arrDTO = new ArrangementDTO();
        BeanUtils.copyProperties(arr, arrDTO);

        return AccountingArrangementDTO.builder()
                .arrangement(arrDTO)
                .customer(customer)
                .build();
    }

    @Override
    public Object manuallyMapEntry(AccountingEntryMapper dto, JWTHelper jwt) {

        log.info("Mapping manual entry for {}", dto);

        // validate rules
        accountingEntryValidator.validateBasicAttributes(dto);
        Arrangement arr = arrangementValidator.validateExistence(dto.getArrangementId(),
                Constant.ACTIVE, Constant.INACTIVE);
        List<AccountingEntry> entries = accountingEntryValidator.validateEntryExistences(dto.getIds());
        accountingEntryValidator.validateManualMapping(entries);

        // prepare models
        List<Arrangement> originalArrangements = new ArrayList<>();
        List<AccountingEntry> allEntries = new ArrayList<>();
        Map<Long, List<AccountingEntry>> previousRecords = findPreviousEntries(new ArrayList<Long>() {{
            add(arr.getId());
        }});
        log.info("Done finding previous records for arr ID {} if possible", arr.getId());

        // initialize an empty list if previous records empty
        if (CollectionUtils.isEmpty(previousRecords)) {
            previousRecords = new HashMap<>();
            List<AccountingEntry> tmp = new ArrayList<>();
            previousRecords.put(arr.getId(), tmp);
        }

        // get the list and add element
        previousRecords.get(arr.getId()).addAll(entries);

        // validate and detect here
        detectAndCollectUpdateCollections(arr, previousRecords,
                null, originalArrangements, allEntries);

        // save if possible
        saveAccountingEntries(allEntries);

        if (!CollectionUtils.isEmpty(originalArrangements)) {
            collectModelAndSendSignal(originalArrangements, jwt.getSubFromJWT());
        }

        return Constant.SUCCESS;
    }


    @Override
    public Object importAndMapEntries(MultipartFile file, JWTHelper jwt) {

        List<AccountingEntryDTO> entries;

        try {
            String fileName = file.getName();
            log.info("Preparing import for excel file {}", fileName);

            // too lazy to change all List -> Set
            entries = new ArrayList<>(readFileAndCollectModels(file));
            log.info("Done reading for file {} with entries {}",
                    fileName, LoggingUtils.objToStringIgnoreEx(entries));

            // find and filter duplicates inside db
            entries = filterDuplicates(entries);
            log.info("Done filtering duplicates inside the file {} with entries {}",
                    fileName, LoggingUtils.objToStringIgnoreEx(entries));

            // got nothing to do
            if (CollectionUtils.isEmpty(entries)) {
                return Collections.emptyList();
            }

            List<AccountingEntry> allEntries = new ArrayList<>();
            List<Arrangement> originalArrangements = new ArrayList<>();

            // map and add to 2 lists above
            mapEntriesAndSave(entries, fileName, allEntries, originalArrangements);

            // save here in @Transactional
            saveAccountingEntries(allEntries);

            // now send AFT or portfolio signal + send noti
            if (!CollectionUtils.isEmpty(originalArrangements)) {
                collectModelAndSendSignal(originalArrangements, jwt.getSubFromJWT());
            }

        } catch (IllegalStateException | IOException e) {
            log.error("Unable to parse file excel {}", file.getName());
            throw new NeoFiatsException(ArrangementErrorCode.T24_INVALID_FILE_TYPE,
                    CommonUtils.format("Cannot parse file {0}", file.getName()));
        }

        return Constant.SUCCESS;
    }

    @Override
    public void mapEntriesAndSave(List<AccountingEntryDTO> entries, String fileName,
                                  List<AccountingEntry> allEntries,
                                  List<Arrangement> updatedArrangements) {

        // this must be initialized outside
        if (allEntries == null || updatedArrangements == null) {
            throw new NeoFiatsException(ArrangementErrorCode.T24_SERVER_ERROR,
                    "allEntries and updatedArrangements must be initialized outside");
        }

        // uppercase all arrangement codes
        Map<String, List<AccountingEntryDTO>> entryMapByUpperCodes = collectUppercaseArrangementCodes(entries);
        log.info("Done parsing and casting to uppercase codes for file {}", fileName);

        // collect an unique list to search inside Arrangement
        Set<String> uppercaseCodes = entryMapByUpperCodes.entrySet().stream()
                .filter(e -> StringUtils.hasText(e.getKey()))
                .flatMap(e -> e.getValue().stream())
                .map(AccountingEntryDTO::getUpperCaseCode)
                .collect(Collectors.toSet());
        log.info("Done collecting unique uppercase codes {} for file {}",
                String.join(", ", uppercaseCodes), fileName);

        // save unparsed events first
        // actually this section can be removed since the entire map would be looped to lookup later anyway
        List<AccountingEntryDTO> unmappedEntries = entryMapByUpperCodes.get(Constant.EMPTY);
        if (!CollectionUtils.isEmpty(unmappedEntries)) {
            List<AccountingEntry> unmappedEntities = convertToEntity(unmappedEntries,
                    AccountingEntryStatusEnum.UNMAPPED.toString(), null);
            allEntries.addAll(unmappedEntities);
            log.info("Adding unparsed entries with key EMPTY[] ... to UNMAPPED status");

            // then remove unparsed key
            entryMapByUpperCodes.remove(Constant.EMPTY);
        }

        if (!CollectionUtils.isEmpty(uppercaseCodes)) {

            // finding all matching records
            Map<String, Arrangement> arrangements = findArrangementByUppercaseCodes(uppercaseCodes);
            log.info("Done finding equivalent arrangement for file {}", fileName);

            if (!CollectionUtils.isEmpty(arrangements)) {

                // now collect all findable arrangements
                Set<Long> arrangementIds = arrangements.values().stream()
                        .map(Arrangement::getId).collect(Collectors.toSet());

                Map<Long, List<AccountingEntry>> previousEntryMap = findPreviousEntries(arrangementIds);

                // map uppercase record to uppercase arrangements
                for (Map.Entry<String, List<AccountingEntryDTO>> entry : entryMapByUpperCodes.entrySet()) {

                    String uppercaseCode = entry.getKey();
                    List<AccountingEntryDTO> currentEntries = entry.getValue();
                    Arrangement arr = arrangements.get(uppercaseCode);

                    // unmapped records
                    if (arr == null || !isValidEntryType(currentEntries, arr.getType())) {
                        log.info("Found nothing for uppercase code {}. Adding all associated records to unmapped list",
                                uppercaseCode);
                        allEntries.addAll(convertToEntity(currentEntries,
                                AccountingEntryStatusEnum.UNMAPPED.toString(), null));

                    } else {
                        detectAndCollectUpdateCollections(arr, previousEntryMap,
                                currentEntries, updatedArrangements, allEntries);
                    }
                }

            } else {

                List<AccountingEntry> unmappedEntities = convertToEntity(entries,
                        AccountingEntryStatusEnum.UNMAPPED.toString(), null);
                allEntries.addAll(unmappedEntities);
                log.info("Found nothing inside db. inserting all UNMAPPED");
            }
        }
    }

    @Override
    @Transactional
    public void saveAccountingEntries(List<AccountingEntry> allEntries) {
        // insert all unmapped records
        if (!CollectionUtils.isEmpty(allEntries)) {
            allEntries = accountingEntryRepo.saveAll(allEntries);
            log.info("Done saving for unmapped entries ... ");
        }
    }

    private boolean isValidEntryType(List<AccountingEntryDTO> entries, Integer arrType) {
        return !CollectionUtils.isEmpty(entries) && arrType != null
                && entries.stream().allMatch(e -> e.getEntryType().equals(arrType));
    }

    private void detectAndCollectUpdateCollections(Arrangement arr,
                                                   Map<Long, List<AccountingEntry>> previousEntryMap,
                                                   List<AccountingEntryDTO> currentEntries,
                                                   List<Arrangement> originalArrangements,
                                                   List<AccountingEntry> allEntries) {

        log.info("Found id {} code {} for uppercase code {}",
                arr.getId(), arr.getCode(), arr.getCode().toUpperCase());

        // now getting all previous rpom.xmlecords + current records and calculate
        List<AccountingEntry> previousEntries = previousEntryMap.get(arr.getId());
        AccountingEntryPriceDTO money = new AccountingEntryPriceDTO();

        // keep track of valid status
        String status = validateAndSetStatus(previousEntries, currentEntries, money, arr);

        // found something -> TO_RETURN, MAPPED OR INSUFFICIENT_AMOUNT
        if (AccountingEntryStatusEnum.MAPPED.toString().equals(status)
                || AccountingEntryStatusEnum.TO_RETURN.toString().equals(status)
                && money.getDiff().longValue() >= 0) {
            log.info("Entry is valid to be ticked payment status with arr ID {} with status {}",
                    arr.getId(), status);
            originalArrangements.add(arr);
        } else { // INSUFFICIENT_CONDITION
            log.info("Entry is not valid to proceed for arr ID {}. Using the status {} ...",
                    arr.getId(), status);
        }

        // adding new entries to save with new status
        if (!CollectionUtils.isEmpty(currentEntries)) {
            List<AccountingEntry> currentEntities = convertToEntity(currentEntries, status, arr);
            allEntries.addAll(currentEntities);
            log.info("Done adding new entries for arr id {} with status {}",
                    arr.getId(), status);

        }

        // updating old entries to save with new status
        if (!CollectionUtils.isEmpty(previousEntries)) {
            List<AccountingEntry> previousEntities = previousEntries.stream()
                    .peek(e -> {
                        e.setStatus(status);
                        // for manual case since elements are added to this list
                        e.setArrangement(arr);
                    })
                    .collect(Collectors.toList());
            allEntries.addAll(previousEntities);
            log.info("Done updating old entries for arr id {} with status {}",
                    arr.getId(), status);
        }
    }

    private String validateAndSetStatus(List<AccountingEntry> previousEntries,
                                        List<AccountingEntryDTO> currentEntries, AccountingEntryPriceDTO money,
                                        Arrangement arr) {

        StringBuilder sb = new StringBuilder();
        sb.append("[");
        sb.append(DateHelper.formatDateSilently(new Date(), Constant.DATE_TIME_FORMAT));
        sb.append("] ");
        if (arr.getStatus() == null || !arr.getStatus().equals(ArrangementStatusEnum.ACTIVE.getStatus())) {
            log.warn("Arrangement status is not ACTIVE {}", arr.getId());
            sb.append("Bản ghi thoả thuận chưa được kích hoạt!");
            setReasonsForEntity(previousEntries, sb.toString());
            setReasonsForDTO(currentEntries, sb.toString());
            return AccountingEntryStatusEnum.INSUFFICIENT_CONDITION.toString();
        }

        if (arr.getOperation() == null) {
            log.warn("No operation info for id {}", arr.getId());
            sb.append("Lỗi không tìm thấy bản ghi trạng thái của giao dịch!");
            setReasonsForEntity(previousEntries, sb.toString());
            setReasonsForDTO(currentEntries, sb.toString());
            return AccountingEntryStatusEnum.INSUFFICIENT_CONDITION.toString();
        }

        if (arr.getOperation().getCustomerStatus() == null ||
                !arr.getOperation().getCustomerStatus().equals(RecordStatus.ACTIVE.getStatus())) {
            log.warn("Customer status is not activated yet for id {}", arr.getId());
            sb.append("Trạng thái CUSTOMER chưa được đánh dấu!");
            setReasonsForEntity(previousEntries, sb.toString());
            setReasonsForDTO(currentEntries, sb.toString());
            return AccountingEntryStatusEnum.INSUFFICIENT_CONDITION.toString();
        }

        if (arr.getOperation().getPaymentStatus() != null &&
                arr.getOperation().getPaymentStatus().equals(RecordStatus.ACTIVE.getStatus())) {
            log.warn("Payment status is already activated for id {}", arr.getId());
            sb.append("Trạng thái PAYMENT đã được đánh dấu từ trước!");
            setReasonsForEntity(previousEntries, sb.toString());
            setReasonsForDTO(currentEntries, sb.toString());
            return AccountingEntryStatusEnum.INSUFFICIENT_CONDITION.toString();
        }

        if (arr.getOperation().getDeliveryStatus() != null &&
                arr.getOperation().getDeliveryStatus().equals(RecordStatus.ACTIVE.getStatus())) {
            log.warn("Delivery status is already activated for id {}", arr.getId());
            sb.append("Trạng thái DELIVERY đã được đánh dấu từ trước!");
            setReasonsForEntity(previousEntries, sb.toString());
            setReasonsForDTO(currentEntries, sb.toString());
            return AccountingEntryStatusEnum.INSUFFICIENT_CONDITION.toString();
        }

        return calculatePayback(arr.getId(), arr.getCode(),
                arr.getTradingDate(), arr.getPricing().getPrincipal(),
                money, previousEntries, currentEntries, sb);
    }

    private String calculatePayback(Long id, String code, Timestamp tradingDate, BigDecimal principal,
                                    AccountingEntryPriceDTO money,
                                    List<AccountingEntry> previousEntries,
                                    List<AccountingEntryDTO> currentEntries, StringBuilder sb) {

        calculateMoney(tradingDate, principal, money, previousEntries, currentEntries);
        log.info("Arrangement ID {} code {} with diff {}, total receivedAmount {} and payback {}",
                id, code, money.getDiff(), money.getReceivedAmount(), money.getPayback());

        sb.append("Bút toán cho giao dich [");
        sb.append(code);
        sb.append("] có tổng tiền đầu tư [");
        sb.append(ConverterUtils.formatDecimalWithoutZero(principal));
        String status;
        if (money.getPayback().longValue() > 0) {
            status = AccountingEntryStatusEnum.TO_RETURN.toString();
            sb.append("] cần được trả lại với số tiền [");
            sb.append(ConverterUtils.formatDecimalWithoutZero(money.getPayback()));
        } else if (money.getDiff().longValue() < 0) {
            status = AccountingEntryStatusEnum.INSUFFICIENT_AMOUNT.toString();
            sb.append("] đang thiếu [");
            sb.append(ConverterUtils.formatDecimalWithoutZero(money.getDiff().abs()));
        } else {
            status = AccountingEntryStatusEnum.MAPPED.toString();
            sb.append("] đã được thanh toán với số tiền [");
            sb.append(ConverterUtils.formatDecimalWithoutZero(money.getReceivedAmount()));
        }

        sb.append("] so với tổng số tiền nhận được từ các bút toán là [");
        sb.append(ConverterUtils.formatDecimalWithoutZero(money.getReceivedAmount()));
        sb.append("]");
        setReasonsForEntity(previousEntries, sb.toString());
        setReasonsForDTO(currentEntries, sb.toString());

        return status;
    }

    private void calculateMoney(Timestamp tradingDate,
                                BigDecimal arrangementPrice,
                                AccountingEntryPriceDTO money,
                                List<AccountingEntry> previousEntries,
                                List<AccountingEntryDTO> currentEntries) {

        // prepare
        BigDecimal receivedAmount = money.getReceivedAmount();
        BigDecimal diff = money.getDiff();
        BigDecimal payback = money.getPayback();

        // for previous entries inside db
        if (!CollectionUtils.isEmpty(previousEntries)) {
            for (AccountingEntry e : previousEntries) {
                if (tradingDate.equals(e.getEntryEffectiveDate())) {
                    receivedAmount = receivedAmount.add(e.getEntryAmount());
                } else {
                    payback = payback.add(e.getEntryAmount());
                }
            }
        }

        // for current entries of this import
        if (!CollectionUtils.isEmpty(currentEntries)) {
            for (AccountingEntryDTO e : currentEntries) {
                BigDecimal entryAmount = new BigDecimal(String.valueOf(e.getEntryAmount()));
                if (tradingDate.equals(e.getEntryEffectiveDate())) {
                    receivedAmount = receivedAmount.add(entryAmount);
                } else {
                    payback = payback.add(entryAmount);
                }
            }
        }

        // set updated attributes
        money.setReceivedAmount(receivedAmount);
        diff = receivedAmount.subtract(arrangementPrice);
        money.setDiff(diff);
        money.setPayback(payback);

        if (diff.longValue() > 0) {
            money.setPayback(money.getPayback().add(diff));
        }
    }

    private Map<Long, List<AccountingEntry>> findPreviousEntries(Collection<Long> arrangementIds) {

        if (CollectionUtils.isEmpty(arrangementIds)) {
            return Collections.emptyMap();
        }

        List<AccountingEntry> previousEntries = accountingEntryRepo.findByArrangementIdIn(arrangementIds);

        if (CollectionUtils.isEmpty(previousEntries)) {
            return Collections.emptyMap();
        }

        return previousEntries.stream().collect(Collectors.groupingBy(e -> e.getArrangement().getId()));

    }

    private List<AccountingEntry> convertToEntity(List<AccountingEntryDTO> records, String status,
                                                  Arrangement arr) {

        Timestamp now = DateHelper.nowInTimestamp();
        return records.stream().map(dto -> {
            AccountingEntry entity = new AccountingEntry();
            BeanUtils.copyProperties(dto, entity, "id");
            entity.setEntryAmount(CommonUtils.doubleToBigDecimalSilently(dto.getEntryAmount()));
            entity.setEntryRemainingAmount(CommonUtils.doubleToBigDecimalSilently(dto.getEntryRemainingAmount()));
            if (arr != null && !CommonUtils.isInvalidPK(arr.getId())) {
                entity.setArrangement(arr);
            }
            entity.insertExtra(now);
            if (StringUtils.hasText(status)) {
                entity.setStatus(status);
            }
            return entity;
        }).collect(Collectors.toList());
    }

    private Map<String, Arrangement> findArrangementByUppercaseCodes(Collection<String> codes) {
        return arrangementRepo.findByUppercaseCodesInFetchPricingOperations(codes)
                .stream()
                .collect(Collectors.toMap(a -> a.getCode().toUpperCase(),
                        Function.identity(), (o, n) -> n));
    }

    private Map<String, List<AccountingEntryDTO>> collectUppercaseArrangementCodes(List<AccountingEntryDTO> entries) {

        Pattern pattern = Pattern.compile(ArrConstant.REGEX_TO_DETECT_ARRANGEMENT_CODE);

        return entries.stream()
                .peek(e -> {
                    Matcher matcher = pattern.matcher(e.getEntryDescription());
                    String upperCase = Constant.EMPTY;
                    if (matcher.matches()) {
                        upperCase = matcher.group(1)
                                .replaceAll("[^0-9A-Za-z.\\-_]", Constant.EMPTY)
                                .toUpperCase().trim(); // to map to a trading code in DB
                    }
                    e.setUpperCaseCode(upperCase);
                    e.setStatus(AccountingEntryStatusEnum.UNMAPPED.toString());
                })
                .collect(Collectors.groupingBy(AccountingEntryDTO::getUpperCaseCode));
    }


    private List<AccountingEntryDTO> filterDuplicates(List<AccountingEntryDTO> entries) {

        List<String> uniqueKeys = entries.stream().map(AccountingEntryDTO::buildUniqueKey)
                .collect(Collectors.toList());

        Map<String, AccountingEntry> existingEntries = accountingEntryRepo.findByUniqueKey(uniqueKeys)
                .stream().collect(Collectors.toMap(AccountingEntry::buildUniqueKey,
                        Function.identity(), (o, n) -> n));

        if (!CollectionUtils.isEmpty(existingEntries)) {
            entries = entries.stream()
                    .filter(e -> existingEntries.get(e.buildUniqueKey()) == null)
                    .collect(Collectors.toList());
        }

        log.info("After filtering duplicates inside database {}",
                LoggingUtils.objToStringIgnoreEx(entries));

        return entries;
    }

    private Set<AccountingEntryDTO> readFileAndCollectModels(MultipartFile mutilpartFile) throws IllegalStateException, IOException {

        String fileName = mutilpartFile.getOriginalFilename();
        String tmpPath = t24TmpPath + File.separator + fileName;
        log.info("Proceed into readFileAndTick. Begin converting [{}] to File java ...", tmpPath);

        Set<AccountingEntryDTO> entries = new HashSet<>();

        // copy content file to a physical file instead of file input stream for better memory usage
        // check apache poi for reference
        File file = new File(tmpPath);
        mutilpartFile.transferTo(file);

        // decide if this is xls or xlsx
        try (Workbook workbook = excelHelper.decideXLSXorXLS(tmpPath, file)) {

            // index of header automatically
            Map<Integer, T24ColumnEnum> header = null;

            // get first sheet
            Sheet worksheet = workbook.getSheetAt(ArrConstant.T24_SHEET);

            for (Row row : worksheet) {

                // found header first
                if (CollectionUtils.isEmpty(header)) {
                    header = excelHelper.findT24Header(row);
                } else {
                    // then filter out invalid rows and set model
                    AccountingEntryDTO entry = validateAndCastToModelIfPossible(row, header);
                    if (entry != null) {
                        boolean added = entries.add(entry);

                        if (!added) {
                            log.info("Skipping duplicate row {} inside file {}", excelHelper.rowToString(row), fileName);
                        }
                    }
                }
            }

        } finally {

            // delete after usage
            deleteFileIfExist(file);
        }

        return entries;
    }

    public AccountingEntryDTO validateAndCastToModelIfPossible(Row row, Map<Integer, T24ColumnEnum> header) {

        if (header == null) {
            throw new NeoFiatsException(ArrangementErrorCode.T24_INVALID_HEADER,
                    "Empty T24 header!");
        }

        String line = null;
        if (row == null || !StringUtils.hasText((line = excelHelper.rowToString(row)))) {
            log.warn("This line is empty {}. Skipping", line);
            return null;
        }

        Iterator<Cell> cellIterator = row.cellIterator();
        AccountingEntryDTO rowModel = new AccountingEntryDTO();

        boolean valid = true;
        while (cellIterator.hasNext()) {

            Cell cell = cellIterator.next();
            Object cellValue = excelHelper.getCellValue(cell);
            int cellIndex = cell.getColumnIndex();

            T24ColumnEnum col = header.get(cellIndex);
            if (col == null) {
                log.warn("Col number {} is not recognizable. Skipping", cellIndex);
                valid = false;
                continue;
            }

            try {
                // try casting to model. error will be ignored with non-mandatory fields
                excelHelper.lookForIndexAndSetT24Data(rowModel, col, cellValue);
            } catch (Exception e) {
                log.warn("Field {} is mandatory but data is not valid {}", col, cellValue);
                log.warn("Got error: {}", e.getMessage());
                valid = false;
                break;
            }
        }

        // last double check model + set entryAmount
        if (!valid) {
            return null;
        } else {

            // continue to check if valid
            if (ConverterUtils.isValidMoney(rowModel.getEntryDepositingAmount()) && rowModel.getEntryWithdrawingAmount() == null) {
                rowModel.setEntryAmount(rowModel.getEntryDepositingAmount());
                rowModel.setEntryType(ArrangementTypeEnum.BUY.getType());
            } else if (rowModel.getEntryDepositingAmount() == null && ConverterUtils.isValidMoney(rowModel.getEntryWithdrawingAmount())) {
                rowModel.setEntryAmount(rowModel.getEntryWithdrawingAmount());
                rowModel.setEntryType(ArrangementTypeEnum.SELL.getType());
            } else {
                log.warn("Found none or both depositing amount {} and withdrawing amount {} for line {}. Skipping",
                        rowModel.getEntryDepositingAmount(), rowModel.getEntryWithdrawingAmount(), line);
                return null;
            }
        }

        return rowModel;
    }

    public void deleteFileIfExist(File file) {

        boolean valid = Constant.ACTIVE;
        try {

            if (file.exists()) {
                valid = file.delete();
            }

        } catch (Exception e) {
            log.warn("Error while delete file {}", e.getMessage());
            valid = Constant.INACTIVE;
        }

        log.info("Deleted file result? {}", valid);
    }

    private void collectModelAndSendSignal(List<Arrangement> originalArrangements, String userAccount) {

        CompletableFuture.runAsync(() -> {

            log.info("Opening new thread {} for collecting model and sending signal for collections of arr ID [{}]",
                    Thread.currentThread().getName(), originalArrangements.stream()
                            .map(tmp -> tmp.getId().toString())
                            .collect(Collectors.joining(Constant.COMMA)));

            // finding counter arrangements and add both original and counter to 1 list
            List<Arrangement> combinedArrangements = arrangementService.findOriginalAndCounterArrangements(originalArrangements);
            log.info("collectModelAndSendSignal - Getting {}", combinedArrangements.stream()
                    .map(LongIDIdentityBase::getId)
                    .collect(Collectors.toList()));

            // get all owner and brokers
            Set<Long> customerIds = combinedArrangements.stream()
                    .flatMap(arr -> arr.getParties().stream())
                    .map(ArrangementParty::getCustomerId)
                    .collect(Collectors.toSet());
            log.info("collectModelAndSendSignal - finding info for customer ids {}", customerIds);

            List<CustomerDTO> customerDTOs = customerService.retrieveCustomerInfo(customerIds);
            if (CollectionUtils.isEmpty(customerDTOs)) {
                throw new NeoFiatsException(ArrangementErrorCode.CUSTOMER_NOT_FOUND,
                        "Empty customer info");
            }

            Map<Long, CustomerDTO> customerMap = customerDTOs.stream()
                    .collect(Collectors.toMap(CustomerDTO::getId, Function.identity(),
                            (o, n) -> n));
            log.info("collectModelAndSendSignal - Found customer info {}", LoggingUtils.objToStringIgnoreEx(customerMap));

            // find original sells
            final Map<Long, Arrangement> originalSells = arrangementService.findOriginalSellOrders(originalArrangements);

            for (Arrangement arr : combinedArrangements) {

                // finding customer account first
                log.info("collectModelAndSendSignal - Handling arr {}", arr.getId());
                ArrangementParty party = arr.getParties().stream()
                        .filter(p -> ArrangementRoleEnum.OWNER.toString().equals(p.getRole())
                                || ArrangementRoleEnum.ORGANIZATION.toString().equals(p.getRole()))
                        .findAny().orElse(null);

                CustomerDTO customerDTO;
                if (party == null || (customerDTO = customerMap.get(party.getCustomerId())) == null) {
                    log.error("collectModelAndSendSignal - Customer info for arr id {} is empty", arr.getId());
                    continue;
                }

                OrderPlacementDTO order = ConverterUtils.castArrangementToOrder(arr, customerDTO);

                // then decide which event to send to portfolio
                PortfolioRetailAction action;
                if (ArrangementTypeEnum.BUY.getType().equals(arr.getType())) {
                    action = PortfolioRetailAction.PAY;
                } else {
                    action = PortfolioRetailAction.PAID;

                    if (originalArrangements.contains(arr)) { // -> order ban goc
                        Arrangement originalSell = originalSells.get(arr.getId());
                        if (originalSell == null) {
                            log.error("Cannot find original sell of this arrangement {}", arr.getId());
                            continue;
                        }
                        order.getArrangement().setSellArrangementCode(originalSell.getCode());
                    }
                }

                log.info("collectModelAndSendSignal - About to send event to {}.{}.{} with action {} and model {}",
                        KAFKA_TOPIC_PREFIX, applicationName, ArrangementEventCode.PORTFOLIO_CONFIRMING_ACTION,
                        action, LoggingUtils.objToStringIgnoreEx(order));
                order.setPurchaserAccount(userAccount); // setting for logging
                portfolioService.sendEventToPortfolioQueue(order, action);

                // save user and action to redis for logging.
                try {
                    String key = CommonUtils.format(ArrConstant.USER_KEY_FORMAT,
                            String.valueOf(order.getArrangement().getId()), action.toString());
                    redisTemplate.opsForValue().set(key, userAccount);
                    log.info("Done saving key {} with value {}", key, userAccount);
                } catch (Exception e) {
                    log.error("Error saving user name to redis {} {} {}",
                            order.getArrangement().getId(), action, userAccount);
                    log.error(e.getMessage(), e);
                }
            }

        }, executor);

    }

    private void setReasonsForEntity(List<AccountingEntry> entries, String note) {
        if (CollectionUtils.isEmpty(entries)) {
            return;
        }
        entries.stream().peek(e -> {
            String appendNote = !StringUtils.hasText(e.getNote())
                    ? note : e.getNote().concat(ArrConstant.EOL).concat(note);
            e.setNote(appendNote);
        }).collect(Collectors.toList());
    }

    private void setReasonsForDTO(List<AccountingEntryDTO> entries, String note) {
        if (CollectionUtils.isEmpty(entries)) {
            return;
        }
        entries.stream().peek(e -> e.setNote(note)).collect(Collectors.toList());
    }

}