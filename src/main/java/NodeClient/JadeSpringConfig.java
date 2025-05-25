package NodeClient;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class JadeSpringConfig implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public AgentContainer agentContainer(){
        Runtime runtime = Runtime.instance();
        Profile profile = new ProfileImpl();
        // replace with naming server's ip
        String mainHost = "localhost";
        profile.setParameter(Profile.MAIN_HOST, mainHost);
        // port for JADE agent communications, Remote Method Invocation 1099
        String port = applicationContext.getEnvironment().getProperty("jade.local.port", "1099");
        profile.setParameter(Profile.LOCAL_PORT, port);
        return runtime.createMainContainer(profile);
    }
}
