package com.example.demo.services.task.data;

import com.example.demo.services.task.TaskException;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static com.example.demo.services.Storage.storage;
import static com.example.demo.services.task.TaskException.Type.ACCESS_DENIED;
import static com.example.demo.services.task.TaskException.Type.WRONG_FORMAT;
import static com.example.demo.services.task.data.Command.Type.LIST_TASK;
import static com.example.demo.services.task.data.TaskStatus.CREATED;
import static com.example.demo.services.task.data.TaskStatus.DELETED;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class User {

    private String title;
    private final List<Task> taskList = new ArrayList<>();

    private User() {
    }

    public User(String title) {
        this.setTitle(title);
    }

    public String getUserTitle() {
        return title;
    }

    private void setTitle(@NotNull String title) {
        checkTitle(title);

        this.title = title;
    }

    private List<Task> getTaskList() {
        return taskList;
    }

    public String getTaskListByString() {
        return taskList.toString();
    }

    private void checkTitle(String title) throws TaskException {
        if (isBlank(title))
            throw new TaskException(WRONG_FORMAT);
    }

    public Task checkTask(String title, List<TaskStatus> taskStatusList) throws TaskException {
        checkTitle(title);

        return findTask(title, taskStatusList)
                .findFirst()
                .orElseThrow(() -> {
                            if (isOpenTaskByOtherUsers(title))
                                return new TaskException(ACCESS_DENIED);
                            else return new TaskException();
                        }
                );
    }

    private Stream<Task> findTask(String title, List<TaskStatus> taskStatusList) {
        return getTaskList()
                .stream()
                .filter(task -> task.getTaskTitle().equals(title))
                .filter(Task::isAvailableLifeCycle)
                .filter(task -> task.checkStatus(taskStatusList));
    }

    private boolean isOpenTaskByOtherUsers(String taskTitle) {
        AtomicBoolean result = new AtomicBoolean(false);

        storage()
                .getAllUser()
                .stream()
                .filter(user -> !user.getUserTitle().equals(this.getUserTitle()))
                .forEach(user -> user.getTaskList()
                        .stream()
                        .filter(task -> !task.getTaskStatus().equals(DELETED))
                        .forEach(task -> result.set(result.get() || task.getTaskTitle().equals(taskTitle))));

        return result.get();
    }

    public boolean isNotAddedToStorage() {
        return storage()
                .getAllUser()
                .stream()
                .noneMatch(user -> user.getUserTitle().equals(this.getUserTitle()));
    }

    public String addTask(Task task) throws TaskException {
        Optional<Task> findTask = findTask(task.getTaskTitle(), LIST_TASK.getStatusList()).findFirst();

        if (findTask.isPresent() || !task.getTaskStatus().getTaskStatusTitle().equals(CREATED.getTaskStatusTitle()))
            throw new TaskException();
        else
            getTaskList().add(task);

        return task.getTaskStatus().getTaskStatusTitle();
    }

}