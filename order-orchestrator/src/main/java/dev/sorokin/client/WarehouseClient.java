package dev.sorokin.client;

import dev.sorokin.api.warehouse.CalculatePricingRequestDto;
import dev.sorokin.api.warehouse.CalculatePricingResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "warehouse-client", url = "${app.clients.warehouse.url}")
public interface WarehouseClient {

    @PostMapping("/warehouse/calculate-price")
    CalculatePricingResponseDto calculatePricing(@RequestBody CalculatePricingRequestDto request);
}
