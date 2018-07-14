package com.divinitor.discord.wahrbot.core.util;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Functional interface for getting an SQL connection.
 */
public interface SQLConnectionProvider {

    /**
     * Get an SQL connection
     * @return The connection
     * @throws SQLException If there was an error getting a connection
     */
    Connection get() throws SQLException;

}
