package dev.sorokin.client;

import dev.sorokin.api.payment.AuthorizePaymentRequestDto;
import dev.sorokin.api.payment.AuthorizePaymentResponseDto;
import dev.sorokin.api.payment.CapturePaymentRequestDto;
import dev.sorokin.api.payment.CapturePaymentResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "payment-client", url = "${app.clients.payment.url}")
public interface PaymentClient {

    @PostMapping("/payment/authorize")
    AuthorizePaymentResponseDto authorizePayment(@RequestBody AuthorizePaymentRequestDto request);

    @PostMapping("/payment/capture")
    CapturePaymentResponseDto capturePayment(@RequestBody CapturePaymentRequestDto request);
}
