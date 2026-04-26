package missive.server.db;

import java.util.List;

// generic data access interface
public interface DAO<T> {
    T findById(int id) throws Exception;
    List<T> findAll() throws Exception;
    int save(T entity) throws Exception;
    void update(T entity) throws Exception;
    void delete(int id) throws Exception;
}
