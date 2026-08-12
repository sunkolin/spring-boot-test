package com.starfish.test.config;

import com.xxl.job.core.biz.AdminBiz;
import com.xxl.job.core.biz.ExecutorBiz;
import com.xxl.job.core.biz.client.AdminBizClient;
import com.xxl.job.core.biz.impl.ExecutorBizImpl;
import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.glue.GlueFactory;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.core.log.XxlJobFileAppender;
import com.xxl.job.core.thread.ExecutorRegistryThread;
import com.xxl.job.core.thread.JobLogFileCleanThread;
import com.xxl.job.core.thread.JobThread;
import com.xxl.job.core.thread.TriggerCallbackThread;
import com.xxl.job.core.util.IpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.Environment;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public class XxlJobNoNettyExecutor extends XxlJobExecutor implements ApplicationContextAware,
        SmartInitializingSingleton, ApplicationListener<ApplicationReadyEvent>, DisposableBean {

    private static ApplicationContext applicationContext;
    private volatile boolean started = false;
    private ExecutorBiz executorBiz;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        XxlJobNoNettyExecutor.applicationContext = applicationContext;
    }

    @Override
    public void afterSingletonsInstantiated() {
        log.info(">>>>>>>>>>> xxl-job (no-netty) scanning @XxlJob handlers.");
        initJobHandlerMethodRepository(applicationContext);
        GlueFactory.refreshInstance(1);
        log.info(">>>>>>>>>>> xxl-job (no-netty) handler scan complete.");
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (started) {
            return;
        }
        synchronized (this) {
            if (started) {
                return;
            }
            started = true;
        }

        Environment env = event.getApplicationContext().getEnvironment();
        int serverPort = Integer.parseInt(env.getProperty("server.port", "8080"));
        String contextPath = env.getProperty("server.servlet.context-path", "");

        log.info(">>>>>>>>>>> xxl-job (no-netty) application ready, serverPort={}, contextPath={}", serverPort, contextPath);

        try {
            start(serverPort, contextPath);
        } catch (Exception e) {
            log.error(">>>>>>>>>>> xxl-job (no-netty) start failed.", e);
            throw new RuntimeException(e);
        }
    }

    public void start(int serverPort, String contextPath) throws Exception {
        String adminAddresses = getField("adminAddresses");
        String accessToken = getField("accessToken");
        String appname = getField("appname");
        String address = getField("address");
        String ip = getField("ip");
        String logPath = getField("logPath");
        int logRetentionDays = getFieldInt("logRetentionDays");

        XxlJobFileAppender.initLogPath(logPath);

        initAdminBizList(adminAddresses, accessToken);

        JobLogFileCleanThread.getInstance().start(logRetentionDays);

        TriggerCallbackThread.getInstance().start();

        executorBiz = new ExecutorBizImpl();

        String resolvedIp = (ip != null && ip.trim().length() > 0) ? ip : IpUtil.getIp();
        String resolvedAddress = address;
        if (resolvedAddress != null && !resolvedAddress.trim().isEmpty()) {
            String contextPathReplacement = (contextPath != null && !contextPath.isEmpty() && !contextPath.equals("/")) ? contextPath : "";
            resolvedAddress = resolvedAddress.replace("${server.port}", String.valueOf(serverPort));
            resolvedAddress = resolvedAddress.replace("{server.port}", String.valueOf(serverPort));
            resolvedAddress = resolvedAddress.replace("${server.servlet.context-path}", contextPathReplacement);
            resolvedAddress = resolvedAddress.replace("{server.servlet.context-path}", contextPathReplacement);
            resolvedAddress = resolvedAddress.replaceAll("/+$", "/");
        } else {
            String contextPathStr = (contextPath != null && !contextPath.isEmpty() && !contextPath.equals("/")) ? contextPath : "";
            resolvedAddress = "http://" + resolvedIp + ":" + serverPort + contextPathStr + "/";
        }

        if (accessToken == null || accessToken.trim().length() == 0) {
            log.warn(">>>>>>>>>>> xxl-job accessToken is empty. To ensure system security, please set the accessToken.");
        }

        ExecutorRegistryThread.getInstance().start(appname, resolvedAddress);

        log.info(">>>>>>>>>>> xxl-job executor (no-netty) start success, address={}", resolvedAddress);
    }

    public ExecutorBiz getExecutorBiz() {
        return executorBiz;
    }

    public String getAccessToken() {
        return getField("accessToken");
    }

    private void initAdminBizList(String adminAddresses, String accessToken) throws Exception {
        if (adminAddresses != null && adminAddresses.trim().length() > 0) {
            List<AdminBiz> adminBizList = new ArrayList<AdminBiz>();
            for (String addr : adminAddresses.trim().split(",")) {
                if (addr != null && addr.trim().length() > 0) {
                    adminBizList.add(new AdminBizClient(addr.trim(), accessToken));
                }
            }
            Field field = XxlJobExecutor.class.getDeclaredField("adminBizList");
            field.setAccessible(true);
            field.set(null, adminBizList);
        }
    }

    private String getField(String name) {
        try {
            Field f = XxlJobExecutor.class.getDeclaredField(name);
            f.setAccessible(true);
            return (String) f.get(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private int getFieldInt(String name) {
        try {
            Field f = XxlJobExecutor.class.getDeclaredField(name);
            f.setAccessible(true);
            return (int) f.get(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void initJobHandlerMethodRepository(ApplicationContext applicationContext) {
        if (applicationContext == null) {
            return;
        }
        String[] beanDefinitionNames = applicationContext.getBeanNamesForType(Object.class, false, true);
        for (String beanDefinitionName : beanDefinitionNames) {
            Object bean = applicationContext.getBean(beanDefinitionName);

            Map<Method, XxlJob> annotatedMethods;
            try {
                annotatedMethods = MethodIntrospector.selectMethods(bean.getClass(),
                        new MethodIntrospector.MetadataLookup<XxlJob>() {
                            @Override
                            public XxlJob inspect(Method method) {
                                return AnnotatedElementUtils.findMergedAnnotation(method, XxlJob.class);
                            }
                        });
            } catch (Throwable ex) {
                log.error("xxl-job method-jobhandler resolve error for bean[" + beanDefinitionName + "].", ex);
                continue;
            }
            if (annotatedMethods == null || annotatedMethods.isEmpty()) {
                continue;
            }

            for (Map.Entry<Method, XxlJob> methodXxlJobEntry : annotatedMethods.entrySet()) {
                Method executeMethod = methodXxlJobEntry.getKey();
                XxlJob xxlJob = methodXxlJobEntry.getValue();
                registJobHandler(xxlJob, bean, executeMethod);
            }
        }
    }

    @Override
    public void destroy() {
        try {
            Field jobThreadRepoField = XxlJobExecutor.class.getDeclaredField("jobThreadRepository");
            jobThreadRepoField.setAccessible(true);
            @SuppressWarnings("unchecked")
            ConcurrentMap<Integer, JobThread> jobThreadRepository = (ConcurrentMap<Integer, JobThread>) jobThreadRepoField.get(null);
            if (jobThreadRepository != null && jobThreadRepository.size() > 0) {
                for (Map.Entry<Integer, JobThread> item : jobThreadRepository.entrySet()) {
                    JobThread oldJobThread = removeJobThread(item.getKey(), "web container destroy and kill the job.");
                    if (oldJobThread != null) {
                        try {
                            oldJobThread.join();
                        } catch (InterruptedException e) {
                            log.error(">>>>>>>>>>> xxl-job, JobThread destroy(join) error, jobId:{}", item.getKey(), e);
                        }
                    }
                }
                jobThreadRepository.clear();
            }

            Field jobHandlerRepoField = XxlJobExecutor.class.getDeclaredField("jobHandlerRepository");
            jobHandlerRepoField.setAccessible(true);
            Map<String, IJobHandler> jobHandlerRepository = (Map<String, IJobHandler>) jobHandlerRepoField.get(null);
            if (jobHandlerRepository != null) {
                jobHandlerRepository.clear();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        JobLogFileCleanThread.getInstance().toStop();
        TriggerCallbackThread.getInstance().toStop();

        log.info(">>>>>>>>>>> xxl-job executor (no-netty) destroy complete.");
    }
}