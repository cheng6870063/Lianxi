package com.example.activitydemo.controller;

import com.example.activitydemo.entity.LeaveInfo;
import com.example.activitydemo.service.LeaveService;
import com.example.activitydemo.service.TestLeaveService;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.engine.*;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.image.ProcessDiagramGenerator;
import org.activiti.image.impl.DefaultProcessDiagramGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.List;


@RestController
public class LeaveController {

	private static Logger logger = LoggerFactory.getLogger(LeaveController.class);

	@Autowired
	private LeaveService leaveService;

	@Autowired
	private RepositoryService repositoryService;

	@Autowired
	private TaskService taskService;

	@Autowired
	private TestLeaveService testLeaveService;
	@Autowired
	private RuntimeService runtimeService;

	@Autowired
	private HistoryService historyService;

	/**
	 * 发起申请，新增信息
	 * @param msg
	 * @param request
	 * @param model
	 * @return
	 */
	@RequestMapping("/addLeaveInfo") 
	public String addLeaveInfo(String msg,HttpServletRequest request,Model model) {
		leaveService.addLeaveAInfo(msg);
		return "新增成功...";
	}
	
	/**
	 * 查询当前用户的任务列表
	 * @param userId
	 * @param request
	 * @return
	 */
	@RequestMapping("/getTaskByUserId")
	public Object getTaskByUserId(String userId,HttpServletRequest request) {
		//System.out.println();
		return leaveService.getByUserId(userId);
	}
	
	/**
	 * 处理完成任务
	 * @param taskId
	 * @param userId
	 * @param audit
	 * @param request
	 * @return
	 */
	@RequestMapping("/completeTask")
	public String completeTask(String taskId,String userId,String audit,HttpServletRequest request) {
		leaveService.completeTaskByUser(taskId, userId, audit);
		return "审批完成...";
	}


	/*
	获取流程图片（页面显示，有乱码）
	 */
	@RequestMapping("/showImg")
	public void showImg(String deploymentId,HttpServletRequest request,HttpServletResponse response) {
	
		try {
			InputStream inputStream = leaveService.findProcessPic(deploymentId);
			byte[] b = new byte[1024];
			int len = -1;
			while((len = inputStream.read(b, 0, 1024)) != -1) {
				response.getOutputStream().write(b, 0, len);
			}
		} catch (IOException e) {
			logger.error("读取流程图片出错:{" + e + "}");
		}
		
	}

	/*
	获取流程图片（页面显示，有乱码）
	 */
	@RequestMapping("/readImage")
	public void deployment(String deploymentId,HttpServletRequest request,HttpServletResponse response) {

		ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();
		ProcessDefinition result = query.deploymentId(deploymentId).singleResult();
		String name = result.getDiagramResourceName();
		InputStream inputStream = repositoryService.getResourceAsStream(result.getDeploymentId(), name);
		/*ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();
		List<ProcessDefinition> list = query.list();
		ProcessDefinition result = list.get(0);
 		String name = result.getDiagramResourceName();
		InputStream inputStream = repositoryService.getResourceAsStream(result.getDeploymentId(), name);*/
		byte[] b = new byte[1024];
		int len = -1;
		try {
			while((len = inputStream.read(b, 0, 1024)) != -1) {
                response.getOutputStream().write(b, 0, len);
            }
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	获取流程图片（页面显示，无乱码）
	 */
	@RequestMapping("/readtopage")
	public void readToPage(String deploymentId,HttpServletRequest request,HttpServletResponse response) {

		//ProcessInstance pi = runtimeService.createProcessInstanceQuery().deploymentId(deploymentId).singleResult();
		ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(deploymentId).singleResult();
		BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
		ProcessDiagramGenerator p = new DefaultProcessDiagramGenerator();
		InputStream inputStream = p.generateDiagram(bpmnModel, "png","宋体", "宋体",
				"宋体", null,0.0);

		byte[] b = new byte[1024];
		int len = -1;
		try {
			while((len = inputStream.read(b, 0, 1024)) != -1) {
				response.getOutputStream().write(b, 0, len);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	/*
	获取流程图片到文件夹（无乱码）
	 */
	@RequestMapping("/readandwriteImage")
	public void readandwriteImage(String deploymentId,HttpServletRequest request,HttpServletResponse response) {

		ProcessInstance pi = runtimeService.createProcessInstanceQuery().deploymentId(deploymentId).singleResult();
		BpmnModel bpmnModel = repositoryService.getBpmnModel(pi.getProcessDefinitionId());
		List<String> activeIds = runtimeService.getActiveActivityIds(pi.getId());
		ProcessDiagramGenerator p = new DefaultProcessDiagramGenerator();
		InputStream is = p.generateDiagram(bpmnModel, "png","宋体", "宋体",
				"宋体", null,0.0);

		File file = new File("F:\\activiti\\process.png");
		OutputStream os = null;
		try {
			os = new FileOutputStream(file);
			byte[] buffer = new byte[1024];
			int len = 0;
			while ((len = is.read(buffer)) != -1) {
				os.write(buffer, 0, len);
			}
			os.close();
			is.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	/*
	高亮显示已审批节点
	 */
	@RequestMapping("/hightLightImag")
	public void hightLightImag(String taskId,HttpServletRequest request,HttpServletResponse response) {

			//使用任务ID，查询任务对象
		Task task = taskService.createTaskQuery()//
				    .taskId(taskId)//使用任务ID查询
				    .singleResult();
		//创建核心引擎流程对象processEngine
		ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
		//流程定义
		BpmnModel bpmnModel = processEngine.getRepositoryService().getBpmnModel(task.getProcessDefinitionId());

		//正在活动节点
		List<String> activeActivityIds = processEngine.getRuntimeService().getActiveActivityIds(task.getExecutionId());

		ProcessDiagramGenerator pdg = processEngine.getProcessEngineConfiguration().getProcessDiagramGenerator();

		//生成流图片
		InputStream inputStream = pdg.generateDiagram(bpmnModel, "PNG", activeActivityIds, activeActivityIds,
				"宋体",
				"宋体",
				"宋体",
				processEngine.getProcessEngineConfiguration().getProcessEngineConfiguration().getClassLoader(), 1.0);
		try {
				byte[] b = new byte[1024];
				int len = -1;
				while((len = inputStream.read(b, 0, 1024)) != -1) {
					response.getOutputStream().write(b, 0, len);
				}
			} catch (IOException e) {
				logger.error("读取流程图片出错:{" + e + "}");
			}

		}

	/**
	 * 更改请假单状态
	 * @return
	 */
	@RequestMapping("/updateLeaveInfo")
	public String updateLeaveInfo(HttpServletRequest request) {
		LeaveInfo entity = new LeaveInfo();
		entity.setId("6b53f5fd-53d8-46dd-9571-9d202c5a573d");
		entity.setLeaveMsg("请事假");
		entity.setStatus("passes");
		leaveService.updateLeaveInfo(entity);
		return "更改完成...";
	}
	
}
