package com.counseling.api.adapter.outbound.external

import com.counseling.api.config.NotificationProperties
import com.counseling.api.port.outbound.EmailMessage
import com.counseling.api.port.outbound.EmailPort
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@Component
@Profile("!test")
class EmailAdapter(
    private val mailSender: JavaMailSender,
    private val notificationProperties: NotificationProperties,
) : EmailPort {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun send(message: EmailMessage): Mono<Void> =
        Mono
            .fromCallable {
                val mimeMessage = mailSender.createMimeMessage()
                val helper = MimeMessageHelper(mimeMessage, true, "UTF-8")
                helper.setFrom(notificationProperties.mailFrom)
                helper.setTo(message.to)
                helper.setSubject(message.subject)
                helper.setText(message.htmlBody, true)
                mailSender.send(mimeMessage)
            }.subscribeOn(Schedulers.boundedElastic())
            .doOnError { e -> log.error("Failed to send email to {}: {}", message.to, e.message) }
            .then()
}
