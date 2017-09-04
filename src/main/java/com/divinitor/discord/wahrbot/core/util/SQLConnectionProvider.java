package com.divinitor.discord.wahrbot.core.util;

import java.sql.Connection;
import java.sql.SQLException;

public interface SQLConnectionProvider {

    Connection get() throws SQLException;

}
