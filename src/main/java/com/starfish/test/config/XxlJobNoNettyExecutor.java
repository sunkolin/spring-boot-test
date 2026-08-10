package com.starfish.test.config;

import com.xxl.job.core.biz.AdminBiz;
import com.xxl.job.core.biz.ExecutorBiz;
import com.xxl.job.core.biz.client.AdminBizClient;
import com.xxl.job.core.biz.impl.ExecutorBizImpl;
import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.glue.GlueFactory;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.core.handler.impl.MethodJobHandler;
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
public class XxlJobNoNettyExecutor implements ApplicationContextAware, SmartInitializingSingleton,
        ApplicationListener<ApplicationReadyEvent>, DisposableBean {

    private String adminAddresses;
    private String accessToken;
    private String appname;
    private String address;
    private String ip;
    private int port;
    private String logPath;
    private int logRetentionDays;

    private ExecutorBiz executorBiz;
    private ApplicationContext applicationContext;
    private volatile boolean started = false;

    public void setAdminAddresses(String adminAddresses) {
        this.adminAddresses = adminAddresses;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setAppname(String appname) {
        this.appname = appname;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }

    public void setLogRetentionDays(int logRetentionDays) {
        this.logRetentionDays = logRetentionDays;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public ExecutorBiz getExecutorBiz() {
        return executorBiz;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
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
        String serverPort = env.getProperty("server.port", "8080");
        String contextPath = env.getProperty("server.servlet.context-path", "");

        log.info(">>>>>>>>>>> xxl-job (no-netty) application ready, serverPort={}, contextPath={}", serverPort, contextPath);

        try {
            start(Integer.parseInt(serverPort), contextPath);
        } catch (Exception e) {
            log.error(">>>>>>>>>>> xxl-job (no-netty) start failed.", e);
            throw new RuntimeException(e);
        }
    }

    public void start(int serverPort, String contextPath) throws Exception {
        XxlJobFileAppender.initLogPath(logPath);

        initAdminBizList();

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

        ExecutorRegistryThread.getInstance().start(appname, resolvedAddress);

        log.info(">>>>>>>>>>> xxl-job executor (no-netty) start success, address={}", resolvedAddress);
    }

    private void initAdminBizList() throws Exception {
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

    private void registJobHandler(XxlJob xxlJob, Object bean, Method executeMethod) {
        if (xxlJob == null) {
            return;
        }

        String name = xxlJob.value();
        if (name.trim().length() == 0) {
            throw new RuntimeException("xxl-job method-jobhandler name invalid, for[" + bean.getClass() + "#" + executeMethod.getName() + "] .");
        }
        if (XxlJobExecutor.loadJobHandler(name) != null) {
            throw new RuntimeException("xxl-job jobhandler[" + name + "] naming conflicts.");
        }

        executeMethod.setAccessible(true);

        Method initMethod = null;
        Method destroyMethod = null;

        if (xxlJob.init().trim().length() > 0) {
            try {
                initMethod = bean.getClass().getDeclaredMethod(xxlJob.init());
                initMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("xxl-job method-jobhandler initMethod invalid, for[" + bean.getClass() + "#" + executeMethod.getName() + "] .");
            }
        }
        if (xxlJob.destroy().trim().length() > 0) {
            try {
                destroyMethod = bean.getClass().getDeclaredMethod(xxlJob.destroy());
                destroyMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("xxl-job method-jobhandler destroyMethod invalid, for[" + bean.getClass() + "#" + executeMethod.getName() + "] .");
            }
        }

        XxlJobExecutor.registJobHandler(name, new MethodJobHandler(bean, executeMethod, initMethod, destroyMethod));
    }

    @Override
    public void destroy() {
        ExecutorRegistryThread.getInstance().toStop();

        try {
            Field jobThreadRepoField = XxlJobExecutor.class.getDeclaredField("jobThreadRepository");
            jobThreadRepoField.setAccessible(true);
            @SuppressWarnings("unchecked")
            ConcurrentMap<Integer, JobThread> jobThreadRepository = (ConcurrentMap<Integer, JobThread>) jobThreadRepoField.get(null);
            if (jobThreadRepository != null && jobThreadRepository.size() > 0) {
                for (Map.Entry<Integer, JobThread> item : jobThreadRepository.entrySet()) {
                    JobThread oldJobThread = XxlJobExecutor.removeJobThread(item.getKey(), "web container destroy and kill the job.");
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
            Map<String, ?> jobHandlerRepository = (Map<String, ?>) jobHandlerRepoField.get(null);
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