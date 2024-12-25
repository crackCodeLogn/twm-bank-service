package com.vv.personal.twm.bank.controller;

import com.vv.personal.twm.artifactory.generated.bank.BankProto;
import com.vv.personal.twm.bank.feign.CrdbServiceFeign;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author Vivek
 * @since 2024-12-24
 * Simple request forwarder
 */
@RestController("BankAccountCrdbController")
@RequestMapping("/banking/bank-account/crdb")
public class BankAccountController {

    @Autowired
    private CrdbServiceFeign crdbServiceFeign;

    @PostMapping("/bank-account")
    String addBankAccount(@RequestBody BankProto.BankAccount newBankAccount) {
        return crdbServiceFeign.addBankAccount(newBankAccount);
    }

    @PostMapping("/bank-accounts")
    String addBankAccounts(@RequestBody BankProto.BankAccounts newBankAccounts) {
        return crdbServiceFeign.addBankAccounts(newBankAccounts);
    }

    @GetMapping("/bank-accounts")
    BankProto.BankAccounts getBankAccounts(@RequestParam("field") String field,
                                           @RequestParam("value") String value) {
        return crdbServiceFeign.getBankAccounts(field, value);
    }

    @GetMapping("/bank-account/{id}")
    BankProto.BankAccount getBankAccount(@PathVariable("id") String id) {
        return crdbServiceFeign.getBankAccount(id);
    }

    @GetMapping("/bank-account/{id}/balance")
    BankProto.BankAccount getBankAccountBalance(@PathVariable("id") String id) {
        return crdbServiceFeign.getBankAccountBalance(id);
    }

    @PatchMapping("/bank-account/{id}/balance")
    boolean updateBankAccountBalance(@PathVariable("id") String id,
                                     @RequestBody BankProto.BankAccount bankAccount) {
        return crdbServiceFeign.updateBankAccountBalance(id, bankAccount);
    }

    @DeleteMapping("/bank-account/{id}")
    String deleteBankAccount(@PathVariable("id") String id) {
        return crdbServiceFeign.deleteBankAccount(id);
    }
}
