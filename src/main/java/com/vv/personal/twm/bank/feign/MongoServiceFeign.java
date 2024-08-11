package com.vv.personal.twm.bank.feign;

import com.vv.personal.twm.artifactory.generated.bank.BankProto;
import com.vv.personal.twm.artifactory.generated.deposit.FixedDepositProto;
import com.vv.personal.twm.ping.remote.feign.PingFeign;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author Vivek
 * @since 16/11/20
 */
@FeignClient("twm-mongo-service")
public interface MongoServiceFeign extends PingFeign {

    @PostMapping("/mongo/bank/addBank")
    String addBank(@RequestBody BankProto.Bank newBank);

    @PostMapping("/mongo/bank/deleteBank")
    String deleteBank(@RequestBody String ifscToDelete);

    @GetMapping("/mongo/bank/getBanks?field={field}&value={value}")
    BankProto.BankList getBanks(@PathVariable("field") String field,
                                @PathVariable("value") String value);

    @PostMapping("/mongo/fd/addFd")
    String addFd(@RequestBody FixedDepositProto.FixedDeposit newFd);

    @PostMapping("/mongo/fd/deleteFd")
    String deleteFd(@RequestBody String fdKey);

    @PostMapping("/mongo/fd/update-by-replacing")
    String updateRecordByReplacing(@RequestBody FixedDepositProto.FixedDeposit fixedDeposit);

    @GetMapping("/mongo/fd/getFds?field={field}&value={value}")
    FixedDepositProto.FixedDepositList getFds(@PathVariable("field") String field,
                                              @PathVariable("value") String value);
}
