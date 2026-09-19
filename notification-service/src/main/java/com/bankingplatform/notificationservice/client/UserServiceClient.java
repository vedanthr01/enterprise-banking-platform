package com.bankingplatform.notificationservice.client;

import com.bankingplatform.notificationservice.config.InternalFeignClientConfig;
import com.bankingplatform.notificationservice.dto.UserContactResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", path = "/users/internal", configuration = InternalFeignClientConfig.class)
public interface UserServiceClient {

    @GetMapping("/{userId}/contact")
    UserContactResponse getContact(@PathVariable("userId") String userId);
}
