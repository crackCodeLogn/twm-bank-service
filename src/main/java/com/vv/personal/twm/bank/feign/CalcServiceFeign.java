package com.vv.personal.twm.bank.feign;

import com.vv.personal.twm.artifactory.generated.deposit.FixedDepositProto;
import com.vv.personal.twm.ping.remote.feign.PingFeign;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * @author Vivek
 * @since 04/02/21
 */
@FeignClient("twm-calc-service")
public interface CalcServiceFeign extends PingFeign {

    @GetMapping("/calc/bank/fd/amount-interest?depositAmount={depositAmount}&rateOfInterest={rateOfInterest}&months={months}&days={days}&accountType={accountType}")
    FixedDepositProto.FixedDeposit calcFixedDepositAmountAndInterest(@PathVariable Double depositAmount,
                                                                     @PathVariable Double rateOfInterest,
                                                                     @PathVariable Integer months,
                                                                     @PathVariable Integer days,
                                                                     @PathVariable Integer accountType);

    @GetMapping("/calc/bank/fd/end-date?startDate={startDate}&months={months}&days={days}")
    String calcEndDate(@PathVariable("startDate") String startDate,
                       @PathVariable("months") Integer months,
                       @PathVariable("days") Integer days);

    @GetMapping("/calc/bank/fd/amount-interest/annual?depositAmount={depositAmount}&rateOfInterest={rateOfInterest}&startDate={startDate}&endDate={endDate}")
    FixedDepositProto.AnnualBreakdownList calcFixedDepositAnnualBreakdown(@PathVariable("depositAmount") Double depositAmount,
                                                                          @PathVariable("rateOfInterest") Double rateOfInterest,
                                                                          @PathVariable("startDate") String startDate,
                                                                          @PathVariable("endDate") String endDate);

}
