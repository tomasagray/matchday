package net.tomasbot.matchday.db;

import java.io.Serializable;
import net.tomasbot.matchday.model.Md5Id;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class SynonymIdGenerator implements IdentifierGenerator {

  @Override
  public Serializable generate(SharedSessionContractImplementor session, @NotNull Object o)
      throws HibernateException {
    return new Md5Id(o.toString());
  }
}
