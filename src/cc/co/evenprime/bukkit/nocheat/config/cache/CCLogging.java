package cc.co.evenprime.bukkit.nocheat.config.cache;

import java.util.logging.Logger;

import cc.co.evenprime.bukkit.nocheat.config.Configuration;
import cc.co.evenprime.bukkit.nocheat.log.LogLevel;

/**
 * Configurations specific for logging. Every world gets one of these.
 * 
 * @author Evenprime
 * 
 */
public class CCLogging {

    public final LogLevel fileLevel;
    public final LogLevel consoleLevel;
    public final LogLevel chatLevel;
    public final Logger   filelogger;
    public final boolean  active;

    public CCLogging(Configuration data, Logger worldSpecificFileLogger) {

        active = data.getBoolean(Configuration.LOGGING_ACTIVE);
        fileLevel = data.getLogLevel(Configuration.LOGGING_FILELEVEL);
        consoleLevel = data.getLogLevel(Configuration.LOGGING_CONSOLELEVEL);
        chatLevel = data.getLogLevel(Configuration.LOGGING_CHATLEVEL);

        filelogger = worldSpecificFileLogger;
    }
}
