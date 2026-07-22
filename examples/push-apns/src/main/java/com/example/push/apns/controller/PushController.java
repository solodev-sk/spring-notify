package com.example.push.apns.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sk.solodev.notify.Notifier;
import sk.solodev.notify.push.PushRequest;

/**
 * Sends push notifications through {@link Notifier}. For the sake of a compact example the
 * {@link PushRequest} is bound straight from the request body — normally you would map an inbound
 * DTO onto it rather than expose the library type at the web edge. The request's {@code to} is the
 * device token, {@code title}/{@code body} the alert; provider-specific extras (sound, badge, …)
 * ride in {@code attributes}.
 */
@RestController
@RequestMapping("/push")
class PushController {

    private final Notifier notifier;

    PushController(Notifier notifier) {
        this.notifier = notifier;
    }

    @PostMapping
    String send(@RequestBody PushRequest request) {
        return notifier.notify(request);
    }
}
