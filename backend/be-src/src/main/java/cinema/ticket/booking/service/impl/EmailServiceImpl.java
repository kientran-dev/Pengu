package cinema.ticket.booking.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import cinema.ticket.booking.service.EmailService;

@Service
public class EmailServiceImpl implements EmailService {
	
	@Value("${app.default_sender}")
	private String default_sender;
	
	@Autowired
	private JavaMailSender mailSender;

	@Override
	public void sendMail(String toMail, String subject, String body) {
		SimpleMailMessage mail = new SimpleMailMessage();
		mail.setFrom(default_sender);
		mail.setTo(toMail);
		mail.setSubject(subject);
		mail.setText(body);
		
		mailSender.send(mail);
	}

	@Override
	public void sendHtmlMail(String toMail, String subject, String htmlBody) {
		MimeMessage message = mailSender.createMimeMessage();
		try {
			// true nghĩa là multipart (có thể đính kèm file), "UTF-8" để hỗ trợ tiếng Việt
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

			helper.setFrom(default_sender);
			helper.setTo(toMail);
			helper.setSubject(subject);

			// Tham số true ở đây xác định nội dung là HTML
			helper.setText(htmlBody, true);

			mailSender.send(message);
		} catch (MessagingException e) {
			e.printStackTrace();
			// Có thể log lỗi hoặc throw exception tùy logic của bạn
		}
	}
}
