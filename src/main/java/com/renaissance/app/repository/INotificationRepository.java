package com.renaissance.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.renaissance.app.model.Notification;
import com.renaissance.app.model.User;

public interface INotificationRepository extends JpaRepository<Notification, Long> {

	// CORRECT: Use 'user.id' instead of 'userId'
	List<Notification> findByUser_UserIdOrderByCreatedAtDesc(Long userId);

	@Query("SELECT n FROM Notification n WHERE n.user = :user AND n.isRead = false ORDER BY n.createdAt DESC")
	List<Notification> findUnreadByUser(@Param("user") User user);

	@Query("SELECT COUNT(n) FROM Notification n WHERE n.user = :user AND n.isRead = false")
	long countUnreadByUser(@Param("user") User user);
	
	@Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.userId = :userId AND n.isRead = false")
    void markAllAsReadForUser(@Param("userId") Long userId);
}
