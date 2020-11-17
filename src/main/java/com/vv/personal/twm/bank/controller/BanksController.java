package com.vv.personal.twm.bank.controller;

import com.vv.personal.twm.artifactory.bank.Bank;
import com.vv.personal.twm.bank.feign.MongoServiceFeign;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author Vivek
 * @since 17/11/20
 */
@RestController("BankController")
@RequestMapping("/banking/banks")
public class BanksController {
    private static final Logger LOGGER = LoggerFactory.getLogger(BanksController.class);

    @Autowired
    private MongoServiceFeign mongoServiceFeign;

    @PostMapping("/addBank")
    public String addBank(@RequestBody Bank newBank) {
        LOGGER.info("Received new bank to be added to Mongo: {}", newBank);
        try {
            return mongoServiceFeign.addBank(newBank);
        } catch (Exception e) {
            LOGGER.error("Failed to add {} to mongo! ", newBank.getName(), e);
        }
        return "FAILED";
    }

    @PostMapping("/deleteBank")
    public String deleteBank(@RequestBody String ifscToDelete) {
        LOGGER.info("Received IFSC to delete: {}", ifscToDelete);
        try {
            return mongoServiceFeign.deleteBank(ifscToDelete);
        } catch (Exception e) {
            LOGGER.error("Failed to delete IFSC: {} from mongo! ", ifscToDelete, e);
        }
        return "FAILED";
    }

    //read
    @GetMapping("/getBanks")
    public String getBanks(@RequestParam("field") String field,
                           @RequestParam(value = "value", required = false) String value) {
        LOGGER.info("Received {} to list for field {}", value, field);
        try {
            return mongoServiceFeign.getBanks(field, value);
        } catch (Exception e) {
            LOGGER.error("Failed to list {}: {} from mongo! ", field, value, e);
        }
        return "FAILED";
    }
}
