package com.example.monitor.controller;

import com.example.monitor.domain.StatusSubscription;
import com.example.monitor.domain.StatusSubscriptionRequest;
import com.example.monitor.service.SubscriptionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/status/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StatusSubscription subscribe(@Valid @RequestBody StatusSubscriptionRequest request) {
        return subscriptionService.subscribe(request.getEmail());
    }
}
