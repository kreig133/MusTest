package com.aplana.iask.mus.test.persistence.dao;

import java.sql.SQLException;

/**
 * @author rshamsutdinov
 * @version 1.0
 */
public class MusOpDaoException extends Exception {
    public MusOpDaoException(String message, SQLException cause) {
        super(message, cause);
    }
}
