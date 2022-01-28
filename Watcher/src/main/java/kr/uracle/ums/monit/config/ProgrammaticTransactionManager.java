package kr.uracle.ums.monit.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

@Component
public class ProgrammaticTransactionManager {
    @Autowired
    PlatformTransactionManager transactionManager;

    TransactionStatus status = null;
    
    DefaultTransactionDefinition dtd = null;
    
    public void start(String name) throws TransactionException {
    	DefaultTransactionDefinition dtd = new DefaultTransactionDefinition();
		dtd.setName(name);
		dtd.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        status = transactionManager.getTransaction(dtd);
    }

    public void commit() throws TransactionException {
        if (status != null && !status.isCompleted()) {
            transactionManager.commit(status);
            status =null;
        }
    }

    public void rollback() throws TransactionException {
        if (status != null && !status.isCompleted()) {
            transactionManager.rollback(status);
            status =null;
        }
    }
}
