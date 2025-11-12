package com.renaissance.app.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.renaissance.app.model.Notification;
import com.renaissance.app.model.User;
import com.renaissance.app.payload.NotificationResponseDTO;
import com.renaissance.app.repository.INotificationRepository;
import com.renaissance.app.repository.IUserRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Transactional
//@CrossOrigin(origins = "http://192.168.0.183:4200") // Your Angular IP
public class NotificationController {

	private final INotificationRepository notificationRepository;
	private final IUserRepository userRepository;

	@GetMapping("/unread")
	public ResponseEntity<List<NotificationResponseDTO>> getUnreadNotifications() {
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

		List<Notification> unread = notificationRepository.findUnreadByUser(user);

		List<NotificationResponseDTO> dtos = unread.stream()
				.map(n -> new NotificationResponseDTO(n.getId(), n.getTaskId(), n.getType(), n.getMessage(),
						n.getIsRead(), n.getCreatedAt(), user.getUsername(), // safe
						n.getType() // or format it
						)).toList();

		return ResponseEntity.ok(dtos);
	}

	//MARK AS READ (single)
	@PatchMapping("/{id}/read")
	public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

		Notification notification = notificationRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Notification not found"));

		// Security: user can only mark their own notifications
		if (!notification.getUser().getUserId().equals(user.getUserId())) {
			return ResponseEntity.status(403).build(); // Forbidden
		}

		notification.setIsRead(true);
		notificationRepository.save(notification);

		return ResponseEntity.ok().build();
	}

	// MARK ALL AS READ
	@PatchMapping("/read-all")
	public ResponseEntity<Void> markAllAsRead() {
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

		notificationRepository.markAllAsReadForUser(user.getUserId());

		return ResponseEntity.ok().build();
	}

	@GetMapping("/unread/count")
	public ResponseEntity<Long> getUnreadCount() {
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

		long count = notificationRepository.countUnreadByUser(user);
		return ResponseEntity.ok(count);
	}
}
