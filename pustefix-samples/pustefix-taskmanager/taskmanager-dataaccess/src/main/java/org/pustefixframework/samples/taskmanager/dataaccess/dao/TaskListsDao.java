package org.pustefixframework.samples.taskmanager.dataaccess.dao;

import java.util.List;

import org.pustefixframework.samples.taskmanager.model.TaskList;

public interface TaskListsDao {

    public List<TaskList> getTaskListsByUser(int userId);
    public void addTaskList(TaskList taskList);
    public void updateTaskList(TaskList taskList);
    public void deleteTaskList(TaskList taskList);
    public TaskList getTaskList(int id);
    
}
