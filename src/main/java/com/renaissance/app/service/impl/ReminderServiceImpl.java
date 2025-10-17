//package com.renaissance.app.service.impl;
//
//import java.time.LocalDateTime;
//import java.time.ZoneId;
//import java.util.List;
//import java.util.stream.Collectors;
//
//import org.modelmapper.ModelMapper;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.scheduling.TaskScheduler;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import com.renaissance.app.model.Task;
//import com.renaissance.app.model.TaskReminder;
//import com.renaissance.app.payload.TaskReminderDTO;
//import com.renaissance.app.repository.IReminderRepository;
//import com.renaissance.app.repository.TaskRepository;
//import com.renaissance.app.service.interfaces.IReminderService;
//
//@Service
//@Transactional
//public class ReminderServiceImpl implements IReminderService {
//
//    private final IReminderRepository reminderRepository;
//    private final TaskRepository taskRepository;
//    private final TaskScheduler taskScheduler;
//    private final ModelMapper modelMapper;
//
//    @Autowired
//    public ReminderServiceImpl(IReminderRepository reminderRepository,
//                               TaskRepository taskRepository,
//                               TaskScheduler taskScheduler,
//                               ModelMapper modelMapper) {
//        this.reminderRepository = reminderRepository;
//        this.taskRepository = taskRepository;
//        this.taskScheduler = taskScheduler;
//        this.modelMapper = modelMapper;
//    }
//
//    @Override
//    @PreAuthorize("hasAnyRole('ADMIN', 'HOD')")
//    public void scheduleActivationReminder(Long taskId) {
//        Task task = taskRepository.findById(taskId)
//                .orElseThrow(() -> new RuntimeException("Task not found"));
//
//        LocalDateTime reminderDate = LocalDateTime.now().plusDays(1);
//        if (reminderDate.isBefore(LocalDateTime.now())) {
//            throw new IllegalArgumentException("Reminder date cannot be in the past");
//        }
//
//        TaskReminder reminder = TaskReminder.builder()
//                .task(task)
//                .reminderDate(reminderDate)
//                .sent(false)
//                .build();
//
//        TaskReminder savedReminder = reminderRepository.save(reminder);
//        taskScheduler.schedule(() -> sendReminder(savedReminder),
//                savedReminder.getReminderDate().atZone(ZoneId.systemDefault()).toInstant());
//    }
//
//    @Override
//    @PreAuthorize("hasAnyRole('ADMIN', 'HOD')")
//    public void scheduleDeadlineReminder(Long taskId) {
//        Task task = taskRepository.findById(taskId)
//                .orElseThrow(() -> new RuntimeException("Task not found"));
//
//        LocalDateTime reminderDate = task.getDueDate().minusDays(1);
//        if (reminderDate.isBefore(LocalDateTime.now())) {
//            throw new IllegalArgumentException("Deadline reminder date cannot be in the past");
//        }
//
//        TaskReminder reminder = TaskReminder.builder()
//                .task(task)
//                .reminderDate(reminderDate)
//                .sent(false)
//                .build();
//
//        TaskReminder savedReminder = reminderRepository.save(reminder);
//        taskScheduler.schedule(() -> sendReminder(savedReminder),
//                savedReminder.getReminderDate().atZone(ZoneId.systemDefault()).toInstant());
//    }
//
//    @Override
//    @PreAuthorize("permitAll()")
//    @Transactional(readOnly = true)
//    public List<TaskReminderDTO> getRemindersForTask(Long taskId) {
//        if (taskId == null || taskId <= 0) {
//            throw new IllegalArgumentException("Invalid Task ID");
//        }
//
//        return reminderRepository.findByTask_TaskId(taskId)
//                .stream()
//                .map(reminder -> modelMapper.map(reminder, TaskReminderDTO.class))
//                .collect(Collectors.toList());
//    }
//
//    private void sendReminder(TaskReminder reminder) {
//        // ✅ Instead of calling NotificationService (removed), 
//        // you can later integrate email/Push/WS notifications here.
//        System.out.println("Reminder triggered for task: " + reminder.getTask().getTitle());
//
//        reminder.setSent(true);
//        reminderRepository.save(reminder);
//    }
//}
