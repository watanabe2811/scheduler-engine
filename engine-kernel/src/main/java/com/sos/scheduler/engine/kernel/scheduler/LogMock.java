package com.sos.scheduler.engine.kernel.scheduler;

/**
* \class Log 
* 
* \brief Log - 
* 
* \details
*
* \section Log.java_intro_sec Introduction
*
* \section Log.java_samples Some Samples
*
* \code
*   .... code goes here ...
* \endcode
*
* <p style="text-align:center">
* <br />---------------------------------------------------------------------------
* <br /> APL/Software GmbH - Berlin
* <br />##### generated by ClaviusXPress (http://www.sos-berlin.com) #########
* <br />---------------------------------------------------------------------------
* </p>
* \author EQCPN
* \version 15.12.2010
* \see reference
*
* Created on 15.12.2010 12:47:16
 */

public class LogMock {
	public void debug(String text) {
		System.out.println(text);
	}
	
	public void info(String text) {
		System.out.println(text);
	}
	
	public void warn(String text) {
		System.out.println(text);
	}
	
	public void error(String text) {
		System.err.println(text);
	}
}