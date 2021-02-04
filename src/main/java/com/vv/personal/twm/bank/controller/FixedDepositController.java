package com.vv.personal.twm.bank.controller;

import com.vv.personal.twm.artifactory.generated.deposit.FixedDepositProto;
import com.vv.personal.twm.bank.feign.CalcServiceFeign;
import com.vv.personal.twm.bank.feign.MongoServiceFeign;
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
@RestController("FixedDepositController")
@RequestMapping("/banking/fd")
public class FixedDepositController {
    private static final Logger LOGGER = LoggerFactory.getLogger(FixedDepositController.class);

    @Autowired
    private MongoServiceFeign mongoServiceFeign;

    @Autowired
    private CalcServiceFeign calcServiceFeign;

    @PostMapping("/addFd")
    public String addFd(@RequestBody FixedDepositProto.FixedDeposit newFixedDeposit) {
        LOGGER.info("Calculating end-date for new FD");
        String endDate = calcServiceFeign.calcEndDate(newFixedDeposit.getStartDate(), newFixedDeposit.getMonths(), newFixedDeposit.getDays());

        LOGGER.info("Calculating expected interest and amount for new FD");
        FixedDepositProto.FixedDeposit computedAmountDetails = calcServiceFeign.calcAmountAndInterest(newFixedDeposit.getDepositAmount(),
                newFixedDeposit.getRateOfInterest(), newFixedDeposit.getMonths(), newFixedDeposit.getDays());
        newFixedDeposit = compAndMerge(newFixedDeposit, computedAmountDetails, endDate);

        LOGGER.info("Received new FD to be added to Mongo: {}", newFixedDeposit);
        try {
            return mongoServiceFeign.addFd(newFixedDeposit);
        } catch (Exception e) {
            LOGGER.error("Failed to add {} to mongo! ", newFixedDeposit.getUser() + newFixedDeposit.getDepositAmount(), e);
        }
        return "FAILED";
    }

    @PostMapping("/deleteFd")
    public String deleteFd(@RequestBody String fdKey) {
        LOGGER.info("Received FD-KEY to delete: {}", fdKey);
        try {
            return mongoServiceFeign.deleteFd(fdKey);
        } catch (Exception e) {
            LOGGER.error("Failed to delete FD-KEY: {} from mongo! ", fdKey, e);
        }
        return "FAILED";
    }

    //read
    @GetMapping("/getFds")
    public FixedDepositProto.FixedDepositList getFdsForApp(@RequestParam(value = "field", defaultValue = "", required = false) String field,
                                                           @RequestParam(value = "value", required = false) String value) {
        LOGGER.info("Received {} to list for field {}", value, field);
        try {
            FixedDepositProto.FixedDepositList retrievedFdList = mongoServiceFeign.getFds(field, value);
            FixedDepositProto.FixedDepositList.Builder fdBuilderList = FixedDepositProto.FixedDepositList.newBuilder();
            fdBuilderList.mergeFrom(retrievedFdList);

            FixedDepositProto.FixedDeposit.Builder aggregateFdEntry = FixedDepositProto.FixedDeposit.newBuilder();
            double totalActiveDeposit = retrievedFdList.getFixedDepositsList().stream().mapToDouble(FixedDepositProto.FixedDeposit::getDepositAmount).sum();
            double totalExpectedInterest = retrievedFdList.getFixedDepositsList().stream().mapToDouble(FixedDepositProto.FixedDeposit::getExpectedInterest).sum();
            double totalExpectedAmount = retrievedFdList.getFixedDepositsList().stream().mapToDouble(FixedDepositProto.FixedDeposit::getExpectedAmount).sum();
            aggregateFdEntry.setDepositAmount(totalActiveDeposit);
            aggregateFdEntry.setExpectedAmount(totalExpectedAmount);
            aggregateFdEntry.setExpectedInterest(totalExpectedInterest);

            fdBuilderList.addFixedDeposits(aggregateFdEntry.build());
            return fdBuilderList.build();
        } catch (Exception e) {
            LOGGER.error("Failed to list {}: {} from mongo! ", field, value, e);
        }
        return FixedDepositProto.FixedDepositList.newBuilder().build();
    }

    @GetMapping("/manual/getFds")
    public List<String> getFdsForUser(@RequestParam(value = "field", defaultValue = "", required = false) String field,
                                      @RequestParam(value = "value", required = false) String value) {
        try {
            return getFdsForApp(field, value).getFixedDepositsList().stream()
                    .map(FixedDepositProto.FixedDeposit::toString)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LOGGER.error("Failed to list {}: {} from mongo! ", field, value, e);
        }
        return new ArrayList<>();
    }

    @GetMapping("/update")
    public String updateFd(@RequestParam String fdKey) {
        LOGGER.info("Received FD-KEY to update: {}. Calculating end-date, and expected interest & amount", fdKey);
        try {
            FixedDepositProto.FixedDeposit fixedDeposit = mongoServiceFeign.getFds("KEY", fdKey)
                    .getFixedDepositsList()
                    .get(0);

            String endDate = calcServiceFeign.calcEndDate(fixedDeposit.getStartDate(), fixedDeposit.getMonths(), fixedDeposit.getDays());
            FixedDepositProto.FixedDeposit computedAmountDetails = calcServiceFeign.calcAmountAndInterest(fixedDeposit.getDepositAmount(),
                    fixedDeposit.getRateOfInterest(), fixedDeposit.getMonths(), fixedDeposit.getDays());
            fixedDeposit = compAndMerge(fixedDeposit, computedAmountDetails, endDate);

            LOGGER.info("Going for mongo updation now for key: {}", fdKey);
            return mongoServiceFeign.updateRecordByReplacing(fixedDeposit);
        } catch (Exception e) {
            LOGGER.error("Failed to complete update op for fd key: {}", fdKey);
        }
        return "FAILED";
    }

    private FixedDepositProto.FixedDeposit compAndMerge(FixedDepositProto.FixedDeposit fixedDeposit, FixedDepositProto.FixedDeposit computedAmountDetails, String endDate) {
        FixedDepositProto.FixedDeposit.Builder builder = FixedDepositProto.FixedDeposit.newBuilder();
        builder.mergeFrom(fixedDeposit);
        builder.mergeFrom(computedAmountDetails);
        builder.setEndDate(endDate);
        return builder.build();
    }
}
