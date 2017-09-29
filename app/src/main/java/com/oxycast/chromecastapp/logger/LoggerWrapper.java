package com.oxycast.chromecastapp.logger;

import org.apache.log4j.Logger;

/**
 * Created by Sergey on 06.04.2017.
 */

public class LoggerWrapper {
    Logger logger;
    public LoggerWrapper(Class aaa)
    {
        try {
            logger = ALogger.getLogger(aaa);
            if (logger != null) {
                logger.info("Initialize logger " + aaa.getName());
            }
        }
        catch (Exception ex)
        {}
    }

    public void info(String text)
    {
        try{
        if(logger!=null)
        {
            logger.info(text);
        }}
        catch (Exception ex){}
    }

    public void info(String text,Throwable e)
    {
        try{
        if(logger!=null)
        {
            logger.info(text,e);
        }}
        catch (Exception ex){}
    }

    public void error(String text)
    {
        try{
        if(logger!=null)
        {
            logger.error(text);
        }}
        catch (Exception ex){}
    }

    public void error(String text,Throwable e)
    {
        try{
        if(logger!=null)
        {
            logger.error(text,e);
        }}
        catch (Exception ex){}
    }

    public void debug(String text)
    {
        try {
            if (logger != null) {
                logger.debug(text);
            }
        }
        catch (Exception e){}
    }

    public void debug(String text,Throwable e)
    {
        try {
            if (logger != null) {
                logger.debug(text, e);
            }
        }
        catch (Exception ex){}
    }

    public void fatal(String text)
    {
        try{
        if(logger!=null)
        {
            logger.fatal(text);
        }}
        catch (Exception ex){}
    }

    public void fatal(String text,Throwable e)
    {
        try {
            if (logger != null) {
                logger.fatal(text, e);
            }
        }
        catch (Exception ex){}
    }
}
