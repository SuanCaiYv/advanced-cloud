package com.learn.gateway.config;

import com.learn.gateway.support.MyJwtCreator;
import com.usthe.sureness.matcher.DefaultPathRoleMatcher;
import com.usthe.sureness.matcher.PathTreeProvider;
import com.usthe.sureness.mgt.SurenessSecurityManager;
import com.usthe.sureness.processor.DefaultProcessorManager;
import com.usthe.sureness.processor.Processor;
import com.usthe.sureness.processor.support.JwtProcessor;
import com.usthe.sureness.processor.support.NoneProcessor;
import com.usthe.sureness.processor.support.PasswordProcessor;
import com.usthe.sureness.provider.ducument.DocumentPathTreeProvider;
import com.usthe.sureness.subject.SubjectCreate;
import com.usthe.sureness.subject.SubjectFactory;
import com.usthe.sureness.subject.SurenessSubjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 *
 */
@Configuration
public class SurenessConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(SurenessConfiguration.class);

    public SurenessConfiguration() {
        init();
    }

    private void init() {

        /**
         * 这里进行配置处理器链表
         */

        // 这里面指出用户角色等信息是yml提供的，但是我们想要的是数据库来提供，所以不使用这种方式
        // SurenessAccountProvider accountProvider = new DocumentAccountProvider();
        List<Processor> processorList = new LinkedList<>();
        NoneProcessor noneProcessor = new NoneProcessor();
        // processorList.add(noneProcessor);
        // 这里指出我们需要一个JWT处理器来进行JWT验证
        JwtProcessor jwtProcessor = new JwtProcessor();
        processorList.add(jwtProcessor);
        // 在这里我们也不需要Username-Password这种匹配方式，我们这个系统暂时只用JWT方式
        // 这里顺带说明一下，这里的Username-Password是通过Http请求头进行添加的
        PasswordProcessor passwordProcessor = new PasswordProcessor();
        // passwordProcessor.setAccountProvider(accountProvider);
        // processorList.add(passwordProcessor);
        DefaultProcessorManager processorManager = new DefaultProcessorManager(processorList);
        if (logger.isDebugEnabled()) {
            logger.debug("DefaultProcessorManager已初始化");
        }

        /**
         * 这里进行配置资源访问控制获取方式
         */

        // 对于访问资源控制，我们希望它是由yml提供的，所以我们启用字典树进行路径匹配
        // 这里佩服一下作者，字典树匹配性能还是很强的，我当时想到的实现是HashMap哈哈哈哈哈，果然我是菜鸡
        PathTreeProvider pathTreeProvider = new DocumentPathTreeProvider();
        DefaultPathRoleMatcher pathRoleMatcher = new DefaultPathRoleMatcher();
        pathRoleMatcher.setPathTreeProvider(pathTreeProvider);
        pathRoleMatcher.buildTree();
        if (logger.isDebugEnabled()) {
            logger.debug("DefaultPathRoleMatcher已初始化");
        }

        /**
         * 这里进行Subject构造器的添加
         */

        // 这里指出我们需要哪些Subject构造器
        SubjectFactory subjectFactory = new SurenessSubjectFactory();
        List<SubjectCreate> subjectCreates = Arrays.asList(
                // 在这里我们只要一个JwtProcessor就够了
                // new NoneSubjectReactiveCreator(),
                // new BasicSubjectReactiveCreator(),
                new MyJwtCreator());
        subjectFactory.registerSubjectCreator(subjectCreates);
        if (logger.isDebugEnabled()) {
            logger.debug("SurenessSubjectFactory已初始化");
        }

        /**
         * 这里进行最后的处理——把上述的配置整合到默认安全管理器中
         */

        SurenessSecurityManager securityManager = SurenessSecurityManager.getInstance();
        securityManager.setPathRoleMatcher(pathRoleMatcher);
        securityManager.setSubjectFactory(subjectFactory);
        securityManager.setProcessorManager(processorManager);
        if (logger.isDebugEnabled()) {
            logger.debug("SurenessSecurityManager已初始化");
        }
    }


}
