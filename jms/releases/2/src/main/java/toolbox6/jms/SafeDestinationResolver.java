package toolbox6.jms;

import org.springframework.jms.support.destination.JndiDestinationResolver;

import javax.jms.ConnectionFactory;
import javax.naming.NamingException;

/**
 * Created by pappmar on 11/11/2016.
 */
public class SafeDestinationResolver extends JndiDestinationResolver {
    @Override
    protected <T> T lookup(String jndiName, Class<T> requiredType) throws NamingException {
        ClassLoader ccl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(
                ConnectionFactory.class.getClassLoader()
        );

        try {
            return super.lookup(jndiName, requiredType);
        } finally {
            Thread.currentThread().setContextClassLoader(ccl);
        }
    }
}
