package org.junolabs.activiti.example.application;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.ManagementService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.impl.persistence.entity.VariableInstanceEntity;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {

	public static void main(String[] args) {

		Logger log = LoggerFactory.getLogger(Application.class);

		log.debug("Hola");
		log.warn("Un mensaje de advertencia");

		// Se crea el motor de activiti
		// ProcessEngine processEngine =
		// ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration().buildProcessEngine();
		ProcessEngines.init();
		ProcessEngineConfiguration processEngineConfiguration = ProcessEngineConfiguration.createStandaloneProcessEngineConfiguration();
		processEngineConfiguration.setJdbcUrl("jdbc:mysql://localhost:3306/activiti_test?autoReconnect=true");
		processEngineConfiguration.setJdbcDriver("com.mysql.jdbc.Driver");
		processEngineConfiguration.setJdbcUsername("root");
		processEngineConfiguration.setJdbcPassword("");
		processEngineConfiguration.setJobExecutorActivate(true);

		// .setAsyncExecutorEnabled(true)
		// .setAsyncExecutorActivate(false)

		ProcessEngine processEngine = processEngineConfiguration.buildProcessEngine();

		RuntimeService runtimeService = processEngine.getRuntimeService();
		RepositoryService repositoryService = processEngine.getRepositoryService();

		// Se deploya la definicion del proceso
		repositoryService.createDeployment().addClasspathResource("bookorder.simple.bpmn20.xml").deploy();

		// Se crea una instancia del proceso
		ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("simplebookorder");

		Assert.assertNotNull(processInstance.getId());

		System.out.println(">>>> Id de Proceso >>>> " + processInstance.getId() + " >>>> Id de Definición de Proceso >>>>" + processInstance.getProcessDefinitionId());

		// ====================
		// Starting a process instance

		repositoryService.createDeployment().addClasspathResource("VacationRequest.bpmn20.xml").deploy();

		Map<String, Object> variables = new HashMap<String, Object>();
		variables.put("employeeName", "Kermit");
		variables.put("numberOfDays", new Integer(4));
		variables.put("vacationMotivation", "I'm really tired!");

		RuntimeService runtimeServiceV = processEngine.getRuntimeService();
		ProcessInstance processInstanceV = runtimeServiceV.startProcessInstanceByKey("vacationRequest", variables);

		// Verify that we started a new process instance
		System.out.println(">>>> Id de Proceso >>>> " + processInstanceV.getId() + ">>>> Number of process instances: >>>> " + runtimeServiceV.createProcessInstanceQuery().count());

		// ====================
		// Get Tasks

		TaskService taskService = processEngine.getTaskService();
		List<Task> tasks = taskService.createTaskQuery().taskCandidateGroup("management").list();
		for (Task task : tasks) {
			System.out.println(">>>> Task available: >>>> " + task.getName() + " - " + task.getId());
		}

		// ====================
		// Completing tasks

		Task task = tasks.get(0);

		Map<String, Object> taskVariables = new HashMap<String, Object>();
		taskVariables.put("vacationApproved", "false");
		taskVariables.put("managerMotivation", "We have a tight deadline!");
		taskService.complete(task.getId(), taskVariables);

		// ====================
		// Suspending and activating a process

		repositoryService.suspendProcessDefinitionByKey("vacationRequest");
		try {
			runtimeService.startProcessInstanceByKey("vacationRequest");
		} catch (ActivitiException e) {
			e.printStackTrace();
		}

		// ====================
		// Query API

		List<Task> tasksTaskQuery = taskService.createTaskQuery().taskAssignee("kermit").processVariableValueEquals("orderId", "0815").orderByDueDate().asc().list();

		ManagementService managementService = processEngine.getManagementService();
		List<Task> tasksNativeTaskQuery = taskService.createNativeTaskQuery().sql("SELECT count(*) FROM " + managementService.getTableName(Task.class) + " T WHERE T.NAME_ = #{taskName}")
				.parameter("taskName", "gonzoTask").list();

		long count = taskService.createNativeTaskQuery()
				.sql("SELECT count(*) FROM " + managementService.getTableName(Task.class) + " T1, " + managementService.getTableName(VariableInstanceEntity.class) + " V1 WHERE V1.TASK_ID_ = T1.ID_")
				.count();

	}

}
