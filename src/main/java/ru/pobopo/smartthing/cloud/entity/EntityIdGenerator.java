package ru.pobopo.smartthing.cloud.entity;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;


// Генератор id для всех сущносей
// Использует реализованную в бд функцию getnextid()
public class EntityIdGenerator implements IdentifierGenerator {
    @Override
    public Serializable generate(SharedSessionContractImplementor sharedSessionContractImplementor, Object o) throws HibernateException {
        try (Connection connection = sharedSessionContractImplementor.getJdbcConnectionAccess().obtainConnection()) {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery("select getnextid() as new_id");
            if (rs.next()) {
                return rs.getString("new_id");
            }
        } catch (SQLException sqlException) {
            throw new RuntimeException(sqlException);
        }

        return null;
    }
}
