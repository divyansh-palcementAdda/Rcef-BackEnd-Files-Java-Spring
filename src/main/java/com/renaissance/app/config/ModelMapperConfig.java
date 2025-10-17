//package com.renaissance.app.config;
//
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.time.format.DateTimeParseException;
//
//import org.hibernate.proxy.HibernateProxy;
//import org.modelmapper.Converter;
//import org.modelmapper.ModelMapper;
//import org.modelmapper.convention.MatchingStrategies;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import com.renaissance.app.model.BulkUploadLog;
//import com.renaissance.app.model.Rating;
//import com.renaissance.app.model.Task;
//import com.renaissance.app.model.TaskReminder;
//import com.renaissance.app.model.User;
//import com.renaissance.app.payload.BulkUploadLogDTO;
//import com.renaissance.app.payload.RatingDTO;
//import com.renaissance.app.payload.TaskDTO;
//import com.renaissance.app.payload.TaskReminderDTO;
//import com.renaissance.app.payload.UserDTO;
//
//@Configuration
//public class ModelMapperConfig {
//
//	@Bean
//	public ModelMapper modelMapper() {
//		ModelMapper modelMapper = new ModelMapper();
//		modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT).setFieldMatchingEnabled(true)
//		.setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE).setSkipNullEnabled(true);
//
//		// === Common Converters ===
//		DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
//
//		Converter<LocalDateTime, String> dateToStringConverter = ctx -> ctx.getSource() == null ? null
//				: ctx.getSource().format(formatter);
//
//		Converter<String, LocalDateTime> stringToDateConverter = ctx -> {
//			if (ctx.getSource() == null)
//				return null;
//			try {
//				return LocalDateTime.parse(ctx.getSource(), formatter);
//			} catch (DateTimeParseException e) {
//				System.err.println("Invalid LocalDateTime: " + ctx.getSource());
//				return null;
//			}
//		};
//
//		Converter<User, String> userToStringConverter = ctx -> {
//			if (ctx.getSource() == null)
//				return null;
//			try {
//				User user = ctx.getSource();
//				if (user instanceof HibernateProxy) {
//					user = (User) ((HibernateProxy) user).getHibernateLazyInitializer().getImplementation();
//				}
//				return user.getFullName() != null ? user.getFullName() : user.getUsername();
//			} catch (Exception e) {
//				return null;
//			}
//		};
//
//		// === User Mapping ===
//		// Configure User -> UserDTO mapping
//		modelMapper.typeMap(User.class, UserDTO.class).addMappings(mapper -> {
//		    mapper.map(User::getUserId, UserDTO::setUserId);
//		    mapper.map(User::getUsername, UserDTO::setUsername);
//		    mapper.map(User::getEmail, UserDTO::setEmail);
//		    mapper.map(User::getFullName, UserDTO::setFullName);
//		    mapper.map(User::getRole, UserDTO::setRole);
//		    mapper.map(User::getStatus, UserDTO::setStatus);
//		    mapper.map(User::isEmailVerified, UserDTO::setEmailVerified);
//
//		    // 🚨 Skip department completely to stop ModelMapper from auto-mapping it
//		    mapper.skip(UserDTO::setDepartmentId);
//		    mapper.skip(UserDTO::setDepartmentName);
//		});
//
//		// Now handle department manually after mapping
//		modelMapper.typeMap(User.class, UserDTO.class).setPostConverter(context -> {
//		    User source = context.getSource();
//		    UserDTO dest = context.getDestination();
//
//		    if (source.getDepartment() != null) {
//		        dest.setDepartmentId(source.getDepartment().getDepartmentId());
//		        dest.setDepartmentName(source.getDepartment().getName());
//		    }
//
//		    return dest;
//		});
//
//
//		// === Task Mapping ===
//		modelMapper.typeMap(Task.class, TaskDTO.class).addMappings(mapper -> {
//			mapper.map(Task::getTaskId, TaskDTO::setTaskId);
//			mapper.map(Task::getTitle, TaskDTO::setTitle);
//			mapper.map(Task::getDescription, TaskDTO::setDescription);
//			mapper.map(Task::getDueDate, TaskDTO::setDueDate);
//			mapper.map(Task::getStatus, TaskDTO::setStatus);
//
//			mapper.map(src -> src.getCreatedBy() != null ? src.getCreatedBy().getUserId() : null,
//					TaskDTO::setCreatedById);
//			mapper.map(src -> src.getCreatedBy() != null ? src.getCreatedBy().getFullName() : null,
//					TaskDTO::setCreatedByName);
//
//			mapper.map(src -> src.getAssignedTo() != null ? src.getAssignedTo().getUserId() : null,
//					TaskDTO::setAssignedToIds);
//			mapper.map(src -> src.getAssignedTo() != null ? src.getAssignedTo().getFullName() : null,
//					TaskDTO::setAssignedToNames);
//
//			mapper.map(src -> src.getDepartments() != null ? src.getDepartments().getDepartmentId() : null,
//					TaskDTO::setDepartmentIds);
//			mapper.map(src -> src.getDepartments() != null ? src.getDepartments().getName() : null,
//					TaskDTO::setDepartmentNames);
//
//			mapper.map(Task::isRequiresApproval, TaskDTO::setRequiresApproval);
//			mapper.map(Task::isApproved, TaskDTO::setApproved);
//			mapper.map(Task::getRfcCompletedAt, TaskDTO::setRfcCompletedAt);
//		});
//
//		// === TaskReminder Mapping ===
//		modelMapper.typeMap(TaskReminder.class, TaskReminderDTO.class).addMappings(mapper -> {
//			mapper.map(TaskReminder::getReminderId, TaskReminderDTO::setReminderId);
//			mapper.map(src -> src.getTask() != null ? src.getTask().getTaskId() : null, TaskReminderDTO::setTaskId);
//			mapper.map(TaskReminder::getReminderDate, TaskReminderDTO::setReminderDate);
//			mapper.map(TaskReminder::isSent, TaskReminderDTO::setSent);
//		});
//
//		// === Rating Mapping ===
//		modelMapper.typeMap(Rating.class, RatingDTO.class).addMappings(mapper -> {
//			mapper.map(Rating::getRatingId, RatingDTO::setRatingId);
//			mapper.map(Rating::getType, RatingDTO::setType);
//			mapper.map(Rating::getScore, RatingDTO::setScore);
//			mapper.map(Rating::getFeedback, RatingDTO::setFeedback);
//
//			mapper.map(src -> src.getRatedUser() != null ? src.getRatedUser().getUserId() : null,
//					RatingDTO::setRatedUserId);
//			mapper.map(src -> src.getRatedUser() != null ? src.getRatedUser().getFullName() : null,
//					RatingDTO::setRatedUserName);
//
//			mapper.map(src -> src.getRatedDepartment() != null ? src.getRatedDepartment().getDepartmentId() : null,
//					RatingDTO::setRatedDepartmentId);
//			mapper.map(src -> src.getRatedDepartment() != null ? src.getRatedDepartment().getName() : null,
//					RatingDTO::setRatedDepartmentName);
//
//			mapper.map(src -> src.getTask() != null ? src.getTask().getTaskId() : null, RatingDTO::setTaskId);
//			mapper.map(src -> src.getTask() != null ? src.getTask().getTitle() : null, RatingDTO::setTaskTitle);
//
//			mapper.map(src -> src.getGivenBy() != null ? src.getGivenBy().getUserId() : null, RatingDTO::setGivenById);
//			mapper.map(src -> src.getGivenBy() != null ? src.getGivenBy().getFullName() : null,
//					RatingDTO::setGivenByName);
//
//			mapper.map(Rating::getCreatedAt, RatingDTO::setCreatedAt);
//		});
//
//		// === BulkUploadLog Mapping ===
//		modelMapper.typeMap(BulkUploadLog.class, BulkUploadLogDTO.class).addMappings(mapper -> {
//			mapper.map(BulkUploadLog::getLogId, BulkUploadLogDTO::setLogId);
//			mapper.map(BulkUploadLog::getFileName, BulkUploadLogDTO::setFileName);
//			mapper.map(BulkUploadLog::getTotalRecords, BulkUploadLogDTO::setTotalRecords);
//			mapper.map(BulkUploadLog::getSuccessCount, BulkUploadLogDTO::setSuccessCount);
//			mapper.map(BulkUploadLog::getFailureCount, BulkUploadLogDTO::setFailureCount);
//			mapper.map(BulkUploadLog::getErrorReport, BulkUploadLogDTO::setErrorReport);
//			mapper.map(src -> src.getUploadedBy() != null ? src.getUploadedBy().getUserId() : null,
//					BulkUploadLogDTO::setUploadedById);
//			mapper.map(src -> src.getUploadedBy() != null ? src.getUploadedBy().getFullName() : null,
//					BulkUploadLogDTO::setUploadedByName);
//			mapper.map(BulkUploadLog::getUploadedAt, BulkUploadLogDTO::setUploadedAt);
//		});
//
//		return modelMapper;
//	}
//}
