package com.vv.personal.twm.bank.controller;

import com.vv.personal.twm.artifactory.generated.deposit.FixedDepositProto;
import com.vv.personal.twm.bank.feign.MongoServiceFeign;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/addFd")
    public String addFd(@RequestBody FixedDepositProto.FixedDeposit newFd) {
        LOGGER.info("Received new FD to be added to Mongo: {}", newFd);
        try {
            return mongoServiceFeign.addFd(newFd);
        } catch (Exception e) {
            LOGGER.error("Failed to add {} to mongo! ", newFd.getUser() + newFd.getDepositAmount(), e);
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
    public String getFds(@RequestParam("field") String field,
                         @RequestParam(value = "value", required = false) String value) {
        LOGGER.info("Received {} to list for field {}", value, field);
        try {
            return mongoServiceFeign.getFds(field, value);
        } catch (Exception e) {
            LOGGER.error("Failed to list {}: {} from mongo! ", field, value, e);
        }
        return "FAILED";
    }
}
