package com.example.email.smtp.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sk.solodev.notify.Notifier;
import sk.solodev.notify.email.EmailRequest;

/**
 * Sends email through {@link Notifier}. For the sake of a compact example the {@link EmailRequest}
 * is bound straight from the request body — normally you would map an inbound DTO onto it rather
 * than expose the library type at the web edge. Jackson deserializes the record (and its nested
 * {@code EmailAddress}/{@code Attachment} records, with attachment bytes as base64) directly, so one
 * endpoint covers every shape: plain text, multipart/alternative (HTML), attachments, cc/bcc,
 * reply-to, and custom headers.
 */
@RestController
@RequestMapping("/emails")
class EmailController {

    private final Notifier notifier;

    EmailController(Notifier notifier) {
        this.notifier = notifier;
    }

    @PostMapping
    String send(@RequestBody EmailRequest request) {
        return notifier.notify(request);
    }
}
