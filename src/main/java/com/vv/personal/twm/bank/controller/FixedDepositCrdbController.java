package com.vv.personal.twm.bank.controller;

import com.vv.personal.twm.artifactory.generated.deposit.FixedDepositProto;
import com.vv.personal.twm.bank.feign.CalcServiceFeign;
import com.vv.personal.twm.bank.feign.CrdbServiceFeign;
import com.vv.personal.twm.ping.processor.Pinger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Vivek
 * @since 17/11/20
 */
@RestController("FixedDepositCrdbController")
@RequestMapping("/banking/fd/crdb")
public class FixedDepositCrdbController {
    private static final Logger LOGGER = LoggerFactory.getLogger(FixedDepositCrdbController.class);

    @Autowired
    private CrdbServiceFeign crdbServiceFeign;

    @Autowired
    private CalcServiceFeign calcServiceFeign;

    @Autowired
    private Pinger pinger;

    @PostMapping("/addFd")
    public String addFd(@RequestBody FixedDepositProto.FixedDeposit newFixedDeposit) {
        LOGGER.info("Calculating end-date for new FD");
        if (!pinger.allEndPointsActive(calcServiceFeign, crdbServiceFeign)) {
            LOGGER.error("All end-points not active. Will not trigger op! Check log");
            return "END-POINTS NOT READY!";
        }
        String endDate = calcServiceFeign.calcEndDate(newFixedDeposit.getStartDate(), newFixedDeposit.getMonths(), newFixedDeposit.getDays());

        LOGGER.info("Calculating expected interest and amount for new FD");
        FixedDepositProto.FixedDeposit computedAmountDetails = calcServiceFeign.calcFixedDepositAmountAndInterest(newFixedDeposit.getDepositAmount(),
                newFixedDeposit.getRateOfInterest(), newFixedDeposit.getMonths(), newFixedDeposit.getDays(),
                newFixedDeposit.getAccountTypeValue());
        newFixedDeposit = compAndMerge(newFixedDeposit, computedAmountDetails, endDate);

        LOGGER.info("Received new FD to be added to crdb: {}", newFixedDeposit);
        try {
            return crdbServiceFeign.addFd(newFixedDeposit);
        } catch (Exception e) {
            LOGGER.error("Failed to add {} to crdb! ", newFixedDeposit.getUser() + newFixedDeposit.getDepositAmount(), e);
        }
        return "FAILED";
    }

    @PostMapping("/deleteFd")
    public String deleteFd(@RequestBody String fdKey) {
        LOGGER.info("Received FD-KEY to delete: {}", fdKey);
        if (!pinger.allEndPointsActive(crdbServiceFeign)) {
            LOGGER.error("All end-points not active. Will not trigger op! Check log");
            return "END-POINTS NOT READY!";
        }
        try {
            return crdbServiceFeign.deleteFixedDeposit(fdKey);
        } catch (Exception e) {
            LOGGER.error("Failed to delete FD-KEY: {} from crdb! ", fdKey, e);
        }
        return "FAILED";
    }

    //read
    @GetMapping("/getFds")
    public FixedDepositProto.FixedDepositList getFdsForApp(@RequestParam(value = "field", defaultValue = "", required = false) String field,
                                                           @RequestParam(value = "value", required = false) String value,
                                                           @RequestParam(value = "considerActiveFdOnly", defaultValue = "true", required = false) boolean considerActiveFdOnly) {
        LOGGER.info("Received {} to list for field {}", value, field);
        if (!pinger.allEndPointsActive(crdbServiceFeign)) {
            LOGGER.error("All end-points not active. Will not trigger op! Check log");
            return FixedDepositProto.FixedDepositList.newBuilder().build();
        }
        try {
            FixedDepositProto.FixedDepositList retrievedFdList = crdbServiceFeign.getFds(field, value);
            List<FixedDepositProto.FixedDeposit> fixedDeposits = new ArrayList<>(retrievedFdList.getFixedDepositList());
            if (considerActiveFdOnly)
                fixedDeposits = fixedDeposits.stream().filter(FixedDepositProto.FixedDeposit::getIsFdActive).collect(Collectors.toList());
            FixedDepositProto.FixedDepositList.Builder fdBuilderList = FixedDepositProto.FixedDepositList.newBuilder();
            fdBuilderList.addAllFixedDeposit(fixedDeposits);

            FixedDepositProto.FixedDeposit.Builder aggregateFdEntry = FixedDepositProto.FixedDeposit.newBuilder();
            double totalActiveDeposit = fixedDeposits.stream().mapToDouble(FixedDepositProto.FixedDeposit::getDepositAmount).sum();
            double totalExpectedInterest = fixedDeposits.stream().mapToDouble(FixedDepositProto.FixedDeposit::getExpectedInterest).sum();
            double totalExpectedAmount = fixedDeposits.stream().mapToDouble(FixedDepositProto.FixedDeposit::getExpectedAmount).sum();
            aggregateFdEntry.setDepositAmount(totalActiveDeposit);
            aggregateFdEntry.setExpectedAmount(totalExpectedAmount);
            aggregateFdEntry.setExpectedInterest(totalExpectedInterest);

            fdBuilderList.addFixedDeposit(aggregateFdEntry.build());
            return fdBuilderList.build();
        } catch (Exception e) {
            LOGGER.error("Failed to list {}: {} from crdb! ", field, value, e);
        }
        return FixedDepositProto.FixedDepositList.newBuilder().build();
    }

    @GetMapping("/manual/getFds")
    public List<String> getFdsForUser(@RequestParam(value = "field", defaultValue = "", required = false) String field,
                                      @RequestParam(value = "value", required = false) String value,
                                      @RequestParam(value = "considerActiveFdOnly", defaultValue = "true", required = false) boolean considerActiveFdOnly) {
        try {
            return getFdsForApp(field, value, considerActiveFdOnly).getFixedDepositList().stream()
                    .map(FixedDepositProto.FixedDeposit::toString)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LOGGER.error("Failed to list {}: {} from crdb! ", field, value, e);
        }
        return new ArrayList<>();
    }

    @GetMapping("/update")
    public String updateFd(@RequestParam String fdKey) {
        LOGGER.info("Received FD-KEY to update: {}. Calculating end-date, and expected interest & amount", fdKey);
        if (!pinger.allEndPointsActive(crdbServiceFeign, calcServiceFeign)) {
            LOGGER.error("All end-points not active. Will not trigger op! Check log");
            return "END-POINTS NOT READY!";
        }
        try {
            FixedDepositProto.FixedDeposit fixedDeposit = crdbServiceFeign.getFds("KEY", fdKey)
                    .getFixedDepositList()
                    .get(0);

            String endDate = calcServiceFeign.calcEndDate(fixedDeposit.getStartDate(), fixedDeposit.getMonths(), fixedDeposit.getDays());
            if (fixedDeposit.getFreeze() == 0) { // if total Amount and interest calc is NOT FROZEN
                FixedDepositProto.FixedDeposit computedAmountDetails = calcServiceFeign.calcFixedDepositAmountAndInterest(fixedDeposit.getDepositAmount(),
                        fixedDeposit.getRateOfInterest(), fixedDeposit.getMonths(), fixedDeposit.getDays(), fixedDeposit.getAccountTypeValue());
                fixedDeposit = compAndMerge(fixedDeposit, computedAmountDetails, endDate);
            }

            LOGGER.info("Going for crdb updation now for key: {}", fdKey);
            return crdbServiceFeign.updateRecordByReplacing(fixedDeposit);
        } catch (Exception e) {
            LOGGER.error("Failed to complete update op for fd key: {}", fdKey);
        }
        return "FAILED";
    }

    @GetMapping("/update/active")
    public String updateFdActiveStatus(@RequestParam String fdKey,
                                       @RequestParam Boolean isActive) {
        LOGGER.info("Received FD-KEY {} to update active status to {}", fdKey, isActive);
        if (!pinger.allEndPointsActive(crdbServiceFeign)) {
            LOGGER.error("All end-points not active. Will not trigger op! Check log");
            return "END-POINTS NOT READY!";
        }
        try {
            return crdbServiceFeign.updateRecordActiveStatus(fdKey, isActive);
        } catch (Exception e) {
            LOGGER.error("Failed to complete active status update op for fd key: {}", fdKey);
        }
        return "FAILED";
    }

    @GetMapping("/freeze/totalAmount")
    public String freezeTotalAmount(@RequestParam String fdKey,
                                    @RequestParam Double totalAmount) {
        LOGGER.info("Received FD-KEY {} to freeze total amount to {}", fdKey, totalAmount);
        if (!pinger.allEndPointsActive(crdbServiceFeign)) {
            LOGGER.error("All end-points not active. Will not trigger op! Check log");
            return "END-POINTS NOT READY!";
        }
        try {
            return crdbServiceFeign.freezeTotalAmount(fdKey, totalAmount);
        } catch (Exception e) {
            LOGGER.error("Failed to complete freeze total amount op for fd key: {}", fdKey);
        }
        return "FAILED";
    }

    @GetMapping("/expire/nr")
    public String freezeTotalAmount(@RequestParam String fdKey) {
        LOGGER.info("Received FD-KEY {} to mark for expiry", fdKey);
        if (!pinger.allEndPointsActive(crdbServiceFeign)) {
            LOGGER.error("All end-points not active. Will not trigger op! Check log");
            return "END-POINTS NOT READY!";
        }
        try {
            return crdbServiceFeign.expireNrFd(fdKey);
        } catch (Exception e) {
            LOGGER.error("Failed to complete expiry op for fd key: {}", fdKey);
        }
        return "FAILED";
    }

    @GetMapping("/annual-breakdown")
    public FixedDepositProto.FixedDepositList generateAnnualBreakdownForExistingFds(@RequestParam(value = "field", defaultValue = "", required = false) String field,
                                                                                    @RequestParam(value = "value", required = false) String value,
                                                                                    @RequestParam(value = "excludeOnBankIfsc", required = false, defaultValue = "") String excludeOnBankIfsc,
                                                                                    @RequestParam(value = "considerActiveFdOnly", defaultValue = "true", required = false) boolean considerActiveFdOnly) {
        LOGGER.info("Will be generating annual breakdown for FDs matching {} x {} and exempting [{}]", field, value, excludeOnBankIfsc);
        if (!pinger.allEndPointsActive(crdbServiceFeign, calcServiceFeign)) {
            LOGGER.error("All end-points not active. Will not trigger op! Check log");
            return FixedDepositProto.FixedDepositList.newBuilder().build();
        }
        try {
            FixedDepositProto.FixedDepositList retrievedFdList = crdbServiceFeign.getFds(field, value);
            List<FixedDepositProto.FixedDeposit> fixedDeposits = new ArrayList<>(retrievedFdList.getFixedDepositList());
            if (considerActiveFdOnly)
                fixedDeposits = fixedDeposits.stream().filter(FixedDepositProto.FixedDeposit::getIsFdActive).collect(Collectors.toList());
            FixedDepositProto.FixedDepositList.Builder fdBuilderList = FixedDepositProto.FixedDepositList.newBuilder();

            fixedDeposits.forEach(fixedDeposit -> {
                if (!fixedDeposit.getBankIFSC().matches(excludeOnBankIfsc)) {
                    FixedDepositProto.FixedDeposit.Builder fdBuilder = FixedDepositProto.FixedDeposit.newBuilder();
                    fdBuilder.mergeFrom(fixedDeposit);
                    FixedDepositProto.AnnualBreakdownList annualBreakdownList = calcServiceFeign.calcFixedDepositAnnualBreakdown(fixedDeposit.getDepositAmount(),
                            fixedDeposit.getRateOfInterest(), fixedDeposit.getStartDate(), fixedDeposit.getEndDate());
                    fdBuilder.setAnnualBreakdownList(annualBreakdownList);
                    fdBuilderList.addFixedDeposit(fdBuilder.build());
                } else {
                    LOGGER.info("Exempting FD '{}' as it's IFSC '{}' is to be exempted on basis of [{}]", fixedDeposit.getFdNumber(), fixedDeposit.getBankIFSC(), excludeOnBankIfsc);
                }
            });

            LOGGER.info("Computed annual breakdown for {} FDs", fdBuilderList.getFixedDepositCount());
            LOGGER.info(fdBuilderList.toString()); //TODO - demote to debug later
            return fdBuilderList.build();
        } catch (Exception e) {
            LOGGER.error("Failed to list {}: {} from crdb! ", field, value, e);
        }
        return FixedDepositProto.FixedDepositList.newBuilder().build();
    }

    @GetMapping("/manual/annual-breakdown")
    public List<String> generateAnnualBreakdownForExistingFdsManually(@RequestParam(value = "field", defaultValue = "", required = false) String field,
                                                                      @RequestParam(value = "value", required = false) String value,
                                                                      @RequestParam(value = "excludeOnBankIfsc", required = false, defaultValue = "") String excludeOnBankIfsc,
                                                                      @RequestParam(value = "considerActiveFdOnly", defaultValue = "true", required = false) boolean considerActiveFdOnly) {
        return generateAnnualBreakdownForExistingFds(field, value, excludeOnBankIfsc, considerActiveFdOnly).getFixedDepositList().stream()
                .map(FixedDepositProto.FixedDeposit::toString).collect(Collectors.toList());
    }

    private FixedDepositProto.FixedDeposit compAndMerge(FixedDepositProto.FixedDeposit fixedDeposit, FixedDepositProto.FixedDeposit computedAmountDetails, String endDate) {
        FixedDepositProto.FixedDeposit.Builder builder = FixedDepositProto.FixedDeposit.newBuilder();
        builder.mergeFrom(fixedDeposit);
        builder.mergeFrom(computedAmountDetails);
        builder.setEndDate(endDate);
        return builder.build();
    }
}
