package com.vv.personal.twm.bank.feign;

import com.vv.personal.twm.artifactory.generated.bank.BankProto;
import com.vv.personal.twm.artifactory.generated.deposit.FixedDepositProto;
import com.vv.personal.twm.ping.feign.HealthFeign;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * @author Vivek
 * @since 10/3/22
 */
@FeignClient("twm-bank-crdb-service")
public interface CrdbServiceFeign extends HealthFeign {

    @PostMapping("/crdb/bank/bank")
    String addBank(@RequestBody BankProto.Bank newBank);

    @DeleteMapping("/crdb/bank/banks/{ifscToDelete}")
    String deleteBank(@PathVariable("ifscToDelete") String ifscToDelete);

    @DeleteMapping("/crdb/bank/banks")
    String deleteAllBanks();

    @GetMapping("/crdb/bank/banks?field={field}&value={value}")
    BankProto.BankList getBanks(@PathVariable("field") String field,
                                @PathVariable("value") String value);


    @PostMapping("/crdb/bank/fixed-deposit")
    String addFd(@RequestBody FixedDepositProto.FixedDeposit newFd);

    @DeleteMapping("/crdb/bank/fixed-deposits/{fd-number}")
    String deleteFixedDeposit(@PathVariable("fd-number") String fdNumber);

    @DeleteMapping("/crdb/bank/fixed-deposits")
    String deleteAllFixedDeposits();

    @PutMapping("/crdb/bank/fixed-deposits/{fd-number}?active={isActive}")
    String updateRecordActiveStatus(@PathVariable("fd-number") String fdNumber,
                                    @PathVariable("isActive") Boolean isActive);

    @PutMapping("/crdb/bank/fixed-deposits")
    String updateRecordByReplacing(@RequestBody FixedDepositProto.FixedDeposit fixedDeposit);

    @GetMapping("/crdb/bank/fixed-deposits?field={field}&value={value}")
    FixedDepositProto.FixedDepositList getFds(@PathVariable("field") String field,
                                              @PathVariable("value") String value);
}