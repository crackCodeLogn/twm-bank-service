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

        FixedDepositProto.FixedDeposit.Builder builder = FixedDepositProto.FixedDeposit.newBuilder();
        builder.mergeFrom(newFixedDeposit);
        builder.mergeFrom(computedAmountDetails);
        builder.setEndDate(endDate);
        newFixedDeposit = builder.build();

        LOGGER.info("Received new FD to be added to Mongo: {}", newFixedDeposit);
        try {
            return mongoServiceFeign.addFd(newFixedDeposit);
        } catch (Exception e) {
            LOGGER.error("Failed to add {} to mongo! ", newFixedDeposit.getUser() + newFixedDeposit.getDepositAmount(), e);
        }
        return "FAILED";
    }

    @PostMapping("/deleteFd")
    public String deleteBank(@RequestBody String fdKey) {
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
            return mongoServiceFeign.getFds(field, value);
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
}
