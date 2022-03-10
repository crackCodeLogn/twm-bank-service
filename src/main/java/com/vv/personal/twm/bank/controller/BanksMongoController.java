package com.vv.personal.twm.bank.controller;

import com.vv.personal.twm.artifactory.generated.bank.BankProto;
import com.vv.personal.twm.bank.feign.MongoServiceFeign;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Vivek
 * @since 17/11/20
 */
@RestController("BankMongoController")
@RequestMapping("/banking/banks/mongo")
public class BanksMongoController {
    private static final Logger LOGGER = LoggerFactory.getLogger(BanksMongoController.class);

    @Autowired
    private MongoServiceFeign mongoServiceFeign;

    @PostMapping("/addBank")
    public String addBank(@RequestBody BankProto.Bank newBank) {
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
    public BankProto.BankList getBanksForApp(@RequestParam("field") String field,
                                             @RequestParam(value = "value", required = false) String value) {
        LOGGER.info("Received {} to list for field {}", value, field);
        try {
            return mongoServiceFeign.getBanks(field, value);
        } catch (Exception e) {
            LOGGER.error("Failed to list {}: {} from mongo! ", field, value, e);
        }
        return BankProto.BankList.newBuilder().build();
    }

    //shifting from BankList to List<String> instead for web ui access - manual
    @GetMapping("/manual/getBanks")
    public List<String> getBanksForUser(@RequestParam(value = "field", defaultValue = "", required = false) String field,
                                        @RequestParam(value = "value", required = false) String value) {
        try {
            return getBanksForApp(field, value).getBanksList().stream()
                    .map(BankProto.Bank::toString)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LOGGER.error("Failed to list {}: {} from mongo! ", field, value, e);
        }
        return new ArrayList<>();
    }
}
