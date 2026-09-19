package com.lifeadmin.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmailDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(EmailDeliveryService.class);

    public void sendEmail(String toAddress, String subject, String body) {
        // Stub implementation - do not fake email delivery via real SMTP if no credentials exist
        log.info("================ EMAIL STUB ================");
        log.info("TO: {}", toAddress);
        log.info("SUBJECT: {}", subject);
        log.info("BODY:\n{}", body);
        log.info("============================================");
    }
}
