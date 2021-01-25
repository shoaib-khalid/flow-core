package com.kalsym.flowcore.utils;

import com.kalsym.flowcore.VersionHolder;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Sarosh
 */
public class Logger {

    public static final org.slf4j.Logger application = LoggerFactory.getLogger("application");


//    public static void info(String prefix, String location, String message, String postfix) {
//        application.info("[v" + VersionHolder.VERSION + "][" + prefix + "] " + location + " " + message + " " + postfix + " ");
//    }
//
//    public static void warn(String prefix, String location, String message, String postfix) {
//        application.warn("[v" + VersionHolder.VERSION + "][" + prefix + "] " + location + " " + message + " " + postfix + " ");
//    }
//
//    public static void error(String prefix, String location, String message, String postfix, Exception e) {
//        application.error("[v" + VersionHolder.VERSION + "][" + prefix + "] " + location + " " + message + " " + postfix, e);
//    }
}
